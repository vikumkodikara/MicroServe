// Firebase Cloud Functions – MicroServe M-Points Payment System
// Admin SDK bypasses ALL Firestore security rules, eliminating PERMISSION_DENIED errors.

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp }      = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

initializeApp();

const db = getFirestore();

// ─── Firestore paths (exact match with Android constants) ────────────────────
const COL_USERS        = "users";            // UserProfile.COLLECTION
const COL_REQUESTS     = "service_requests"; // ServiceRequest.COLLECTION
const COL_TRANSACTIONS = "transactions";     // TransactionRepository
const COL_PLATFORM     = "platform";
const DOC_ESCROW       = "escrow";
const F_POINTS         = "cashPoints";       // UserProfile.FIELD_CASH_POINTS
const F_ESCROW_POINTS  = "escrowPoints";
const STATUS_IN_PROGRESS  = "in_progress";
const STATUS_ADMIN_APPROVED = "admin_approved";

// ─────────────────────────────────────────────────────────────────────────────
// Callable: processPayment
//
// Called by the customer when they click "Pay Now" in BillActivity.
//
// Parameters from Android:  { requestId, customerId, providerId, cost }
//
// What it does (all atomic, Admin SDK – no permission errors):
//   1. Validates caller == customerId
//   2. Checks customer has enough M-Points
//   3. Deducts M-Points from customer
//   4. Adds M-Points to escrow
//   5. Creates ServiceTransaction record
//   6. Updates ServiceRequest status → "in_progress"
//
// Returns: { transactionId: string }
// ─────────────────────────────────────────────────────────────────────────────
exports.processPayment = onCall(async (request) => {
  // ── Auth guard ─────────────────────────────────────────────────────────────
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "You must be logged in to pay.");
  }

  const callerUid = request.auth.uid;
  const { requestId, customerId, providerId, cost } = request.data;

  // ── Input validation ───────────────────────────────────────────────────────
  if (!requestId || typeof requestId !== "string") {
    throw new HttpsError("invalid-argument", "requestId is required.");
  }
  if (!customerId || typeof customerId !== "string") {
    throw new HttpsError("invalid-argument", "customerId is required.");
  }
  if (!providerId || typeof providerId !== "string") {
    throw new HttpsError("invalid-argument", "providerId is required.");
  }
  if (!cost || typeof cost !== "number" || cost <= 0) {
    throw new HttpsError("invalid-argument", "cost must be a positive number.");
  }

  // ── Security: only the real customer can trigger payment ───────────────────
  if (callerUid !== customerId) {
    throw new HttpsError(
      "permission-denied",
      "Only the customer can initiate payment for this request."
    );
  }

  // ── Document references ────────────────────────────────────────────────────
  const customerRef = db.collection(COL_USERS).doc(customerId);
  const escrowRef   = db.collection(COL_PLATFORM).doc(DOC_ESCROW);
  const requestRef  = db.collection(COL_REQUESTS).doc(requestId);
  const txnRef      = db.collection(COL_TRANSACTIONS).doc(); // auto-ID

  let transactionId = "";

  await db.runTransaction(async (txn) => {
    // ── STEP 1: ALL READS ────────────────────────────────────────────────────
    const [customerSnap, escrowSnap, requestSnap] = await Promise.all([
      txn.get(customerRef),
      txn.get(escrowRef),
      txn.get(requestRef),
    ]);

    if (!requestSnap.exists) {
      throw new HttpsError("not-found", "Service request not found.");
    }

    const requestData = requestSnap.data();

    // ── STEP 2: CALCULATIONS & BUSINESS LOGIC ────────────────────────────────
    const currentStatus = requestData.status;
    if (currentStatus !== "bid_selected") {
      throw new HttpsError(
        "failed-precondition",
        `Cannot pay – request status is '${currentStatus}'. Expected 'bid_selected'.`
      );
    }

    const balance     = (customerSnap.data()?.cashPoints ?? 0);
    const escrowTotal = (escrowSnap.exists ? (escrowSnap.data()?.escrowPoints ?? 0) : 0);

    if (balance < cost) {
      throw new HttpsError(
        "failed-precondition",
        `Insufficient M Points. You have ${balance}, need ${cost}.`
      );
    }

    const providerCode    = customerId.substring(0, 8).toUpperCase();
    const requesterName   = requestData.requesterName   || "";
    const providerName    = requestData.acceptedProviderName || "";
    const title           = requestData.category || requestData.title || "Service";

    transactionId = txnRef.id;

    // ── STEP 3: ALL WRITES ───────────────────────────────────────────────────
    // Deduct M-Points from customer
    txn.update(customerRef, { [F_POINTS]: balance - cost });

    // Credit escrow account
    txn.set(escrowRef, { [F_ESCROW_POINTS]: escrowTotal + cost }, { merge: true });

    // Create ServiceTransaction record
    txn.set(txnRef, {
      id:            transactionId,
      requestId,
      requestTitle:  title,
      requesterUid:  customerId,
      requesterName,
      providerUid:   providerId,
      providerName,
      providerCode,
      amount:        cost,
      status:        "escrow",
      providerDone:  false,
      requesterDone: false,
      createdAt:     FieldValue.serverTimestamp(),
      updatedAt:     FieldValue.serverTimestamp(),
    });

    // Update ServiceRequest status to in_progress
    txn.update(requestRef, {
      status:        STATUS_IN_PROGRESS,
      transactionId: transactionId,
      updatedAt:     FieldValue.serverTimestamp(),
    });
  });

  return { transactionId };
});

// ─────────────────────────────────────────────────────────────────────────────
// Callable: releasePayment
//
// Called by the customer when they click "Confirm & Rate" in BillActivity.
//
// Parameters from Android:  { requestId, transactionId }
//
// What it does (all atomic, Admin SDK):
//   1. Validates caller is the request's requester
//   2. Transfers M-Points from escrow → provider wallet
//   3. Updates ServiceTransaction status → "completed"
//   4. Updates ServiceRequest status → "admin_approved"
//
// Returns: { success: true }
// ─────────────────────────────────────────────────────────────────────────────
exports.releasePayment = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "You must be logged in.");
  }

  const callerUid = request.auth.uid;
  const { requestId, transactionId } = request.data;

  if (!requestId || !transactionId) {
    throw new HttpsError("invalid-argument", "requestId and transactionId are required.");
  }

  const requestRef = db.collection(COL_REQUESTS).doc(requestId);
  const txnRef     = db.collection(COL_TRANSACTIONS).doc(transactionId);
  const escrowRef  = db.collection(COL_PLATFORM).doc(DOC_ESCROW);

  await db.runTransaction(async (txn) => {
    // ── STEP 1: ALL READS ────────────────────────────────────────────────────
    const [requestSnap, txnSnap, escrowSnap] = await Promise.all([
      txn.get(requestRef),
      txn.get(txnRef),
      txn.get(escrowRef),
    ]);

    if (!requestSnap.exists) throw new HttpsError("not-found", "Service request not found.");
    if (!txnSnap.exists)     throw new HttpsError("not-found", "Transaction not found.");

    const requestData = requestSnap.data();
    const txnData     = txnSnap.data();

    // Security: only the requester can confirm completion
    if (callerUid !== requestData.requesterUid) {
      throw new HttpsError("permission-denied", "Only the customer can confirm job completion.");
    }
    if (requestData.status !== "provider_done") {
      throw new HttpsError(
        "failed-precondition",
        `Cannot confirm – status is '${requestData.status}'. Expected 'provider_done'.`
      );
    }

    const providerUid = txnData.providerUid;
    const amount      = txnData.amount || 0;
    const providerRef = db.collection(COL_USERS).doc(providerUid);
    const providerSnap = await txn.get(providerRef);

    // ── STEP 2: CALCULATIONS ─────────────────────────────────────────────────
    const escrowBalance   = (escrowSnap.exists ? (escrowSnap.data()?.[F_ESCROW_POINTS] ?? 0) : 0);
    const providerBalance = (providerSnap.data()?.[F_POINTS] ?? 0);

    if (escrowBalance < amount) {
      throw new HttpsError("failed-precondition", "Insufficient escrow balance.");
    }

    // ── STEP 3: ALL WRITES ───────────────────────────────────────────────────
    txn.update(escrowRef,   { [F_ESCROW_POINTS]: escrowBalance - amount });
    txn.update(providerRef, { [F_POINTS]: providerBalance + amount });
    txn.update(txnRef,     { status: "completed", requesterDone: true, updatedAt: FieldValue.serverTimestamp() });
    txn.update(requestRef, { status: STATUS_ADMIN_APPROVED, updatedAt: FieldValue.serverTimestamp() });
  });

  return { success: true };
});
