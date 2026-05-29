package com.example.microserve

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Rating(
    val id: String = "",
    val requestId: String = "",
    val providerUid: String = "",
    val requesterUid: String = "",
    val stars: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        FIELD_REQUEST_ID to requestId,
        FIELD_PROVIDER_UID to providerUid,
        FIELD_REQUESTER_UID to requesterUid,
        FIELD_STARS to stars,
        FIELD_COMMENT to comment,
        FIELD_CREATED_AT to createdAt
    )

    companion object {
        const val COLLECTION = "ratings"

        const val FIELD_REQUEST_ID = "requestId"
        const val FIELD_PROVIDER_UID = "providerUid"
        const val FIELD_REQUESTER_UID = "requesterUid"
        const val FIELD_STARS = "stars"
        const val FIELD_COMMENT = "comment"
        const val FIELD_CREATED_AT = "createdAt"

        fun fromMap(id: String, data: Map<String, Any?>): Rating = Rating(
            id = id,
            requestId = data[FIELD_REQUEST_ID] as? String ?: "",
            providerUid = data[FIELD_PROVIDER_UID] as? String ?: "",
            requesterUid = data[FIELD_REQUESTER_UID] as? String ?: "",
            stars = (data[FIELD_STARS] as? Number)?.toFloat() ?: 0f,
            comment = data[FIELD_COMMENT] as? String ?: "",
            createdAt = (data[FIELD_CREATED_AT] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
