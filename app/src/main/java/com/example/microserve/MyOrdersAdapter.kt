package com.example.microserve

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/**
 * Adapter for the provider-facing "My Orders" section.
 *
 * Status-driven UI:
 *  - OPEN          → Approve + Decline buttons visible
 *  - IN_PROGRESS   → "View Bill" button visible (customer has paid, escrow held)
 *  - PROVIDER_DONE / REQUESTER_CONFIRMED / ADMIN_APPROVED → "View Bill" (read-only)
 *  - BID_SELECTED  → status label only (waiting for customer to pay)
 *  - Everything else → status label only
 */
class MyOrdersAdapter(
    private var orders: List<ServiceRequest>,
    private val context: Context
) : RecyclerView.Adapter<MyOrdersAdapter.OrderViewHolder>() {

    // UID of the logged-in user, used to distinguish provider vs customer role
    private val currentUid: String =
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory:    TextView       = view.findViewById(R.id.tvOrderCategory)
        val tvRequester:   TextView       = view.findViewById(R.id.tvOrderRequester)
        val tvStatus:      TextView       = view.findViewById(R.id.tvOrderStatus)
        val tvDescription: TextView       = view.findViewById(R.id.tvOrderDescription)
        val tvPoints:      TextView       = view.findViewById(R.id.tvOrderPoints)
        val btnApprove:    MaterialButton = view.findViewById(R.id.btnApproveOrder)
        val btnDecline:    MaterialButton = view.findViewById(R.id.btnDeclineOrder)
        val btnViewBill:   MaterialButton = view.findViewById(R.id.btnViewOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.tvCategory.text  = order.category.ifBlank { order.title }
        holder.tvRequester.text = order.requesterName
        holder.tvStatus.text    = formatStatus(order.status)

        if (order.description.isNotBlank()) {
            holder.tvDescription.visibility = View.VISIBLE
            holder.tvDescription.text       = order.description
        } else {
            holder.tvDescription.visibility = View.GONE
        }

        if (order.acceptedPoints > 0) {
            holder.tvPoints.visibility = View.VISIBLE
            holder.tvPoints.text       = "${order.acceptedPoints} M Points"
        } else {
            holder.tvPoints.visibility = View.GONE
        }

        // Reset all buttons first
        holder.btnApprove.visibility  = View.GONE
        holder.btnDecline.visibility  = View.GONE
        holder.btnViewBill.visibility = View.GONE

        when {
            // ── PROVIDER role: request sent to this user ─────────────────────
            order.status == ServiceRequestStatus.OPEN
                    && order.acceptedProviderUid == currentUid -> {
                holder.btnApprove.visibility = View.VISIBLE
                holder.btnDecline.visibility = View.VISIBLE
                holder.btnApprove.setOnClickListener { showApprovalDialog(order) }
                holder.btnDecline.setOnClickListener { confirmDecline(order) }
            }

            // Provider: customer has paid, provider can mark complete
            (order.status == ServiceRequestStatus.IN_PROGRESS ||
             order.status == ServiceRequestStatus.PROVIDER_DONE ||
             order.status == ServiceRequestStatus.REQUESTER_CONFIRMED ||
             order.status == ServiceRequestStatus.ADMIN_APPROVED)
                    && order.acceptedProviderUid == currentUid -> {
                holder.btnViewBill.visibility = View.VISIBLE
                holder.btnViewBill.text = "View Bill"
                holder.btnViewBill.setOnClickListener { openBill(order) }
            }

            // ── CUSTOMER role: this user sent the request ────────────────────
            // Approved by provider → show Pay Now
            order.status == ServiceRequestStatus.BID_SELECTED
                    && order.requesterUid == currentUid -> {
                holder.btnViewBill.visibility = View.VISIBLE
                holder.btnViewBill.text = "Pay Now"
                holder.btnViewBill.setOnClickListener { openBill(order) }
            }

            // Customer: paid or beyond → show View Bill
            (order.status == ServiceRequestStatus.IN_PROGRESS ||
             order.status == ServiceRequestStatus.PROVIDER_DONE ||
             order.status == ServiceRequestStatus.REQUESTER_CONFIRMED ||
             order.status == ServiceRequestStatus.ADMIN_APPROVED)
                    && order.requesterUid == currentUid -> {
                holder.btnViewBill.visibility = View.VISIBLE
                holder.btnViewBill.text = "View Bill"
                holder.btnViewBill.setOnClickListener { openBill(order) }
            }

            // All other states: no action buttons
            else -> { /* status label only */ }
        }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<ServiceRequest>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    // ── Approve ─────────────────────────────────────────────────────────────

    private fun showApprovalDialog(order: ServiceRequest) {
        val dialogView  = LayoutInflater.from(context).inflate(R.layout.dialog_approve_order, null)
        val etPoints    = dialogView.findViewById<EditText>(R.id.etApprovalPoints)

        AlertDialog.Builder(context)
            .setTitle("Approve Request")
            .setMessage("Set M Points cost for: ${order.requesterName}")
            .setView(dialogView)
            .setPositiveButton("Approve") { _, _ ->
                val pointsText = etPoints.text.toString().trim()
                val points     = pointsText.toIntOrNull() ?: 0
                if (points <= 0) {
                    Toast.makeText(context, "Please enter a valid M Points amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                approveRequest(order, points)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun approveRequest(order: ServiceRequest, points: Int) {
        ServiceRequestRepository.update(
            requestId = order.id,
            fields    = mapOf(
                ServiceRequest.FIELD_STATUS          to ServiceRequestStatus.BID_SELECTED,
                ServiceRequest.FIELD_ACCEPTED_POINTS to points
            ),
            onSuccess = {
                Toast.makeText(context, "Request approved! Customer notified.", Toast.LENGTH_SHORT).show()
            },
            onFailure = { err ->
                Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ── Decline ─────────────────────────────────────────────────────────────

    private fun confirmDecline(order: ServiceRequest) {
        AlertDialog.Builder(context)
            .setTitle("Decline Request")
            .setMessage("Are you sure you want to decline the request from ${order.requesterName}?")
            .setPositiveButton("Decline") { _, _ ->
                ServiceRequestRepository.update(
                    requestId = order.id,
                    fields    = mapOf(ServiceRequest.FIELD_STATUS to "declined"),
                    onSuccess = {
                        Toast.makeText(context, "Request declined.", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── View Bill ────────────────────────────────────────────────────────────

    private fun openBill(order: ServiceRequest) {
        val intent = Intent(context, BillActivity::class.java).apply {
            putExtra("REQUEST_ID",    order.id)
            putExtra("PROVIDER_NAME", order.acceptedProviderName)
            putExtra("CATEGORY",      order.category)
        }
        context.startActivity(intent)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun formatStatus(status: String): String = when (status) {
        ServiceRequestStatus.OPEN               -> "Pending Approval"
        ServiceRequestStatus.BID_SELECTED       -> "Request Approved ✓"
        ServiceRequestStatus.IN_PROGRESS        -> "Payment Received"
        ServiceRequestStatus.PROVIDER_DONE      -> "Work Completed"
        ServiceRequestStatus.REQUESTER_CONFIRMED -> "Confirmed"
        ServiceRequestStatus.ADMIN_APPROVED     -> "Completed"
        "declined"                              -> "Declined"
        else -> status.split('_').joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
