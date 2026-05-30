package com.example.microserve

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Bid(
    val id: String = "",
    val requestId: String = "",
    val providerUid: String = "",
    val providerName: String = "",
    val points: Int = 0,
    val completionHours: Int = 0,
    val status: String = BidStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        FIELD_REQUEST_ID to requestId,
        FIELD_PROVIDER_UID to providerUid,
        FIELD_PROVIDER_NAME to providerName,
        FIELD_POINTS to points,
        FIELD_COMPLETION_HOURS to completionHours,
        FIELD_STATUS to status,
        FIELD_CREATED_AT to createdAt
    )

    companion object {
        const val SUBCOLLECTION = "bids"

        const val FIELD_REQUEST_ID = "requestId"
        const val FIELD_PROVIDER_UID = "providerUid"
        const val FIELD_PROVIDER_NAME = "providerName"
        const val FIELD_POINTS = "points"
        const val FIELD_COMPLETION_HOURS = "completionHours"
        const val FIELD_STATUS = "status"
        const val FIELD_CREATED_AT = "createdAt"

        fun fromMap(id: String, data: Map<String, Any?>): Bid {
            return Bid(
                id = id,
                requestId = data[FIELD_REQUEST_ID] as? String ?: "",
                providerUid = data[FIELD_PROVIDER_UID] as? String ?: "",
                providerName = data[FIELD_PROVIDER_NAME] as? String ?: "",
                points = (data[FIELD_POINTS] as? Number)?.toInt() ?: 0,
                completionHours = (data[FIELD_COMPLETION_HOURS] as? Number)?.toInt() ?: 0,
                status = data[FIELD_STATUS] as? String ?: BidStatus.PENDING,
                createdAt = (data[FIELD_CREATED_AT] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

object BidStatus {
    const val PENDING = "pending"
    const val ACCEPTED = "accepted"
    const val REJECTED = "rejected"
}
