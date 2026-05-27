package com.example.microserve

import com.google.firebase.firestore.IgnoreExtraProperties
import java.text.DecimalFormat
import java.util.UUID

@IgnoreExtraProperties
data class ServiceTransaction(
    val id: String = "",
    val transactionCode: String = "",
    val requestId: String = "",
    val requestTitle: String = "",
    val requesterUid: String = "",
    val requesterName: String = "",
    val providerUid: String = "",
    val providerName: String = "",
    val providerCode: String = "",
    val amount: Int = 0,
    val status: String = ServiceTransactionStatus.ESCROW,
    val paidAt: Long? = null,
    val providerDoneAt: Long? = null,
    val requesterConfirmedAt: Long? = null,
    val adminApprovedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            FIELD_TRANSACTION_CODE to transactionCode,
            FIELD_REQUEST_ID to requestId,
            FIELD_REQUEST_TITLE to requestTitle,
            FIELD_REQUESTER_UID to requesterUid,
            FIELD_REQUESTER_NAME to requesterName,
            FIELD_PROVIDER_UID to providerUid,
            FIELD_PROVIDER_NAME to providerName,
            FIELD_PROVIDER_CODE to providerCode,
            FIELD_AMOUNT to amount,
            FIELD_STATUS to status,
            FIELD_PAID_AT to paidAt,
            FIELD_PROVIDER_DONE_AT to providerDoneAt,
            FIELD_REQUESTER_CONFIRMED_AT to requesterConfirmedAt,
            FIELD_ADMIN_APPROVED_AT to adminApprovedAt,
            FIELD_CREATED_AT to createdAt
        )
    }

    companion object {
        const val COLLECTION = "transactions"

        const val FIELD_TRANSACTION_CODE = "transactionCode"
        const val FIELD_REQUEST_ID = "requestId"
        const val FIELD_REQUEST_TITLE = "requestTitle"
        const val FIELD_REQUESTER_UID = "requesterUid"
        const val FIELD_REQUESTER_NAME = "requesterName"
        const val FIELD_PROVIDER_UID = "providerUid"
        const val FIELD_PROVIDER_NAME = "providerName"
        const val FIELD_PROVIDER_CODE = "providerCode"
        const val FIELD_AMOUNT = "amount"
        const val FIELD_STATUS = "status"
        const val FIELD_PAID_AT = "paidAt"
        const val FIELD_PROVIDER_DONE_AT = "providerDoneAt"
        const val FIELD_REQUESTER_CONFIRMED_AT = "requesterConfirmedAt"
        const val FIELD_ADMIN_APPROVED_AT = "adminApprovedAt"
        const val FIELD_CREATED_AT = "createdAt"

        fun fromMap(id: String, data: Map<String, Any?>): ServiceTransaction {
            return ServiceTransaction(
                id = id,
                transactionCode = data[FIELD_TRANSACTION_CODE] as? String ?: "",
                requestId = data[FIELD_REQUEST_ID] as? String ?: "",
                requestTitle = data[FIELD_REQUEST_TITLE] as? String ?: "",
                requesterUid = data[FIELD_REQUESTER_UID] as? String ?: "",
                requesterName = data[FIELD_REQUESTER_NAME] as? String ?: "",
                providerUid = data[FIELD_PROVIDER_UID] as? String ?: "",
                providerName = data[FIELD_PROVIDER_NAME] as? String ?: "",
                providerCode = data[FIELD_PROVIDER_CODE] as? String ?: "",
                amount = (data[FIELD_AMOUNT] as? Number)?.toInt() ?: 0,
                status = data[FIELD_STATUS] as? String ?: ServiceTransactionStatus.ESCROW,
                paidAt = (data[FIELD_PAID_AT] as? Number)?.toLong(),
                providerDoneAt = (data[FIELD_PROVIDER_DONE_AT] as? Number)?.toLong(),
                requesterConfirmedAt = (data[FIELD_REQUESTER_CONFIRMED_AT] as? Number)?.toLong(),
                adminApprovedAt = (data[FIELD_ADMIN_APPROVED_AT] as? Number)?.toLong(),
                createdAt = (data[FIELD_CREATED_AT] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        fun formatAmount(amount: Int): String {
            val formatter = DecimalFormat("#,##0")
            return "Rs. ${formatter.format(amount)}"
        }

        fun generateProviderCode(userId: String): String {
            val number = kotlin.math.abs(userId.hashCode()) % 9_000_000 + 1_000_000
            return "P$number"
        }

        fun generateTransactionCode(existingCount: Int): String {
            return "TX-${300 + existingCount + 1}"
        }
    }
}

object ServiceTransactionStatus {
    const val ESCROW = "escrow"
    const val AWAITING_ADMIN = "awaiting_admin"
    const val SUCCESS = "success"
}
