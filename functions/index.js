// Firebase Cloud Functions – MicroServe M-Points Escrow System
// All functions run with Firebase Admin privileges so client security rules are bypassed.

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp }      = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

initializeApp();

const db = getFirestore();

// ─── Collection / field constants (must match Android app exactly) ──────────
//   ServiceRequest.COLLECTION  = "service_requests"
//   UserProfile.COLLECTION     = "users"
//   ServiceTransaction (see TransactionRepository) = "transactions"
const COL_USERS        = "users";            // UserProfile.COLLECTION
const COL_REQUESTS     = "service_requests"; // ServiceRequest.COLLECTION
const COL_TRANSACTIONS = "transactions";     // TransactionRepository collection
const COL_PLATFORM     = "platform";

const DOC_ESCROW       = "escrow";

const F_POINTS         = "cashPoints";       // UserProfile.FIELD_CASH_POINTS
const F_ESCROW_POINTS  = "escrowPoints";

const STATUS_IN_PROGRESS = "in_progress";
const STATUS_ADMIN_APPROVED = "admin_approved";

// ─────────────────────────────────────────────────────────────────────────────
// Callable: processEscrowPayment
//
// Called by customer when they click "Pay Now" in BillActivity.
// Validates that the caller is the request's requester, then atomically:
//   1. Deducts M-Points from the customer
//   2. Adds them to the escrow account
//   3. Updates the ServiceRequest status to "in_progress"
//   4. Creates a ServiceTransaction record
//
// Input:  { requestId: string }
// Output: { transactionId: string }
// ─────────────────────────────────────────────────────────────────────────────
exports.processEscrowPayment = onCall(async (request) => {
  // Auth check – must be logged in
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "You must be logged in to pay.");
  }

  const callerUid = request.auth.uid;
  const { requestId } = request.data;

  if (!requestId || typeof requestId !== "string") {
    throw new HttpsError("invalid-argument", "requestId is required.");
  }

  const requestRef     = db.collection(COL_REQUESTS).doc(requestId);
  const escrowRef      = db.collection(COL_PLATFORM).doc(DOC_ESCROW);

  // Run everything inside a Firestore transaction (read-then-write order)
  const transactionId = await db.runTransaction(async (txn) => {
    // ── STEP 1: ALL READS ────────────────────────────────────────────────────
    const requestSnap  = await txn.get(requestRef);
    if (!requestSnap.exists) {
      throw new HttpsError("not-found", "Service request not found.");
    }

    const requestData   = requestSnap.data();
    const requesterUid  = requestData.requesterUid;
    const providerUid   = requestData.acceptedProviderUid;
    const providerName  = requestData.acceptedProviderName  || "";
    const requesterName = requestData.requesterName         || "";
    const amount        = requestData.acceptedPoints        || 0;
    const title         = requestData.category              || requestData.title || "Service";
    const status        = requestData.status;

    // Security: only the actual requester can trigger payment
    if (callerUid !== requesterUid) {
      throw new HttpsError("permission-denied", "Only the customer can pay for this request.");
    }
    if (status !== "bid_selected") {
      throw new HttpsError("failed-precondition", `Cannot pay – request status is '${status}'.`);
    }
    if (amount <= 0) {
      throw new HttpsError("failed-precondition", "Invalid payment amount.");
    }

    const userRef   = db.collection(COL_USERS).doc(requesterUid);
    const userSnap  = await txn.get(userRef);
    const escrowSnap = await txn.get(escrowRef);

    // ── STEP 2: CALCULATIONS ─────────────────────────────────────────────────
    const currentBalance = (userSnap.data()?.[F_POINTS] ?? 0);
    if (currentBalance < amount) {
      throw new HttpsError(
        "failed-precondition",
        `Insufficient M Points. You have ${currentBalance}, need ${amount}.`
      );
    }
    const currentEscrow = (escrowSnap.exists ? (escrowSnap.data()?.[F_ESCROW_POINTS] ?? 0) : 0);

    // Build the new ServiceTransaction document
    const newTxnRef = db.collection(COL_TRANSACTIONS).doc();
    const providerCode = providerUid.substring(0, 8).toUpperCase();

    // ── STEP 3: ALL WRITES ───────────────────────────────────────────────────
    // Deduct from customer
    txn.update(userRef, { [F_POINTS]: currentBalance - amount });

    // Add to escrow
    txn.set(escrowRef, { [F_ESCROW_POINTS]: currentEscrow + amount }, { merge: true });

    // Create transaction record
    txn.set(newTxnRef, {
      id:            newTxnRef.id,
      requestId,
      requestTitle:  title,
      requesterUid,
      requesterName,
      providerUid,
      providerName,
      providerCode,
      amount,
      status:        "escrow",
      providerDone:  false,
      requesterDone: false,
      createdAt:     FieldValue.serverTimestamp(),
      updatedAt:     FieldValue.serverTimestamp(),
    });

    // Update request status
    txn.update(requestRef, {
      status:        STATUS_IN_PROGRESS,
      transactionId: newTxnRef.id,
      updatedAt:     FieldValue.serverTimestamp(),
    });

    return newTxnRef.id;
  });

  return { transactionId };
});

// ─────────────────────────────────────────────────────────────────────────────
// Callable: releaseEscrowToProvider
//
// Called by customer when they click "Confirm & Rate" in BillActivity.
// Validates caller is the requester, then atomically:
//   1. Transfers M-Points from escrow to provider
//   2. Updates ServiceRequest status to "admin_approved"
//   3. Updates ServiceTransaction status to "completed"
//
// Input:  { requestId: string, transactionId: string }
// Output: { success: true }
// ─────────────────────────────────────────────────────────────────────────────
exports.releaseEscrowToProvider = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "You must be logged in.");
  }

  const callerUid = request.auth.uid;
  const { requestId, transactionId } = request.data;

  if (!requestId || !transactionId) {
    throw new HttpsError("invalid-argument", "requestId and transactionId are required.");
  }

  const requestRef     = db.collection(COL_REQUESTS).doc(requestId);
  const txnRef         = db.collection(COL_TRANSACTIONS).doc(transactionId);
  const escrowRef      = db.collection(COL_PLATFORM).doc(DOC_ESCROW);

  await db.runTransaction(async (txn) => {
    // ── STEP 1: ALL READS ────────────────────────────────────────────────────
    const requestSnap  = await txn.get(requestRef);
    const txnSnap      = await txn.get(txnRef);
    const escrowSnap   = await txn.get(escrowRef);

    if (!requestSnap.exists) throw new HttpsError("not-found", "Service request not found.");
    if (!txnSnap.exists)     throw new HttpsError("not-found", "Transaction not found.");

    const requestData  = requestSnap.data();
    const txnData      = txnSnap.data();
    const requesterUid = requestData.requesterUid;
    const providerUid  = txnData.providerUid;
    const amount       = txnData.amount || 0;
    const status       = requestData.status;

    // Security: only the actual requester can confirm completion
    if (callerUid !== requesterUid) {
      throw new HttpsError("permission-denied", "Only the customer can confirm this job.");
    }
    if (status !== "provider_done") {
      throw new HttpsError("failed-precondition", `Cannot confirm – status is '${status}'.`);
    }

    const providerRef  = db.collection(COL_USERS).doc(providerUid);
    const providerSnap = await txn.get(providerRef);

    // ── STEP 2: CALCULATIONS ─────────────────────────────────────────────────
    const currentEscrow   = (escrowSnap.exists ? (escrowSnap.data()?.[F_ESCROW_POINTS] ?? 0) : 0);
    const providerBalance = (providerSnap.data()?.[F_POINTS] ?? 0);

    if (currentEscrow < amount) {
      throw new HttpsError("failed-precondition", "Insufficient escrow balance.");
    }

    // ── STEP 3: ALL WRITES ───────────────────────────────────────────────────
    // Deduct from escrow
    txn.update(escrowRef, { [F_ESCROW_POINTS]: currentEscrow - amount });

    // Credit to provider
    txn.update(providerRef, { [F_POINTS]: providerBalance + amount });

    // Mark transaction completed
    txn.update(txnRef, {
      status:        "completed",
      requesterDone: true,
      updatedAt:     FieldValue.serverTimestamp(),
    });

    // Mark request admin_approved
    txn.update(requestRef, {
      status:    STATUS_ADMIN_APPROVED,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  return { success: true };
});
