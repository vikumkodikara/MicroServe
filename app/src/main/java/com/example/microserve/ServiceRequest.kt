package com.example.microserve

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ServiceRequest(
    val id: String = "",
    val requesterUid: String = "",
    val requesterName: String = "",
    val title: String = "",
    val category: String = "",
    val contact: String = "",
    val province: String = "",
    val district: String = "",
    val city: String = "",
    val location: String = "",
    val description: String = "",
    val status: String = ServiceRequestStatus.OPEN,
    val acceptedBidId: String = "",
    val acceptedProviderUid: String = "",
    val acceptedProviderName: String = "",
    val acceptedPoints: Int = 0,
    val transactionId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            FIELD_REQUESTER_UID to requesterUid,
            FIELD_REQUESTER_NAME to requesterName,
            FIELD_TITLE to title,
            FIELD_CATEGORY to category,
            FIELD_CONTACT to contact,
            FIELD_PROVINCE to province,
            FIELD_DISTRICT to district,
            FIELD_CITY to city,
            FIELD_LOCATION to location,
            FIELD_DESCRIPTION to description,
            FIELD_STATUS to status,
            FIELD_ACCEPTED_POINTS to acceptedPoints,
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to updatedAt
        )
        if (acceptedBidId.isNotBlank()) map[FIELD_ACCEPTED_BID_ID] = acceptedBidId
        if (acceptedProviderUid.isNotBlank()) map[FIELD_ACCEPTED_PROVIDER_UID] = acceptedProviderUid
        if (acceptedProviderName.isNotBlank()) map[FIELD_ACCEPTED_PROVIDER_NAME] = acceptedProviderName
        if (transactionId.isNotBlank()) map[FIELD_TRANSACTION_ID] = transactionId
        return map
    }

    fun fullLocation(): String {
        return listOf(location, city, district, province)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }

    companion object {
        const val COLLECTION = "service_requests"

        const val FIELD_REQUESTER_UID = "requesterUid"
        const val FIELD_REQUESTER_NAME = "requesterName"
        const val FIELD_TITLE = "title"
        const val FIELD_CATEGORY = "category"
        const val FIELD_CONTACT = "contact"
        const val FIELD_PROVINCE = "province"
        const val FIELD_DISTRICT = "district"
        const val FIELD_CITY = "city"
        const val FIELD_LOCATION = "location"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_STATUS = "status"
        const val FIELD_ACCEPTED_BID_ID = "acceptedBidId"
        const val FIELD_ACCEPTED_PROVIDER_UID = "acceptedProviderUid"
        const val FIELD_ACCEPTED_PROVIDER_NAME = "acceptedProviderName"
        const val FIELD_ACCEPTED_POINTS = "acceptedPoints"
        const val FIELD_TRANSACTION_ID = "transactionId"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"

        fun fromMap(id: String, data: Map<String, Any?>): ServiceRequest {
            return ServiceRequest(
                id = id,
                requesterUid = data[FIELD_REQUESTER_UID] as? String ?: "",
                requesterName = data[FIELD_REQUESTER_NAME] as? String ?: "",
                title = data[FIELD_TITLE] as? String ?: "",
                category = data[FIELD_CATEGORY] as? String ?: "",
                contact = data[FIELD_CONTACT] as? String ?: "",
                province = data[FIELD_PROVINCE] as? String ?: "",
                district = data[FIELD_DISTRICT] as? String ?: "",
                city = data[FIELD_CITY] as? String ?: "",
                location = data[FIELD_LOCATION] as? String ?: "",
                description = data[FIELD_DESCRIPTION] as? String ?: "",
                status = data[FIELD_STATUS] as? String ?: ServiceRequestStatus.OPEN,
                acceptedBidId = data[FIELD_ACCEPTED_BID_ID] as? String ?: "",
                acceptedProviderUid = data[FIELD_ACCEPTED_PROVIDER_UID] as? String ?: "",
                acceptedProviderName = data[FIELD_ACCEPTED_PROVIDER_NAME] as? String ?: "",
                acceptedPoints = (data[FIELD_ACCEPTED_POINTS] as? Number)?.toInt() ?: 0,
                transactionId = data[FIELD_TRANSACTION_ID] as? String ?: "",
                createdAt = (data[FIELD_CREATED_AT] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data[FIELD_UPDATED_AT] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

object ServiceRequestStatus {
    const val OPEN = "open"
    const val BID_SELECTED = "bid_selected"
    const val IN_PROGRESS = "in_progress"
    const val PROVIDER_DONE = "provider_done"
    const val REQUESTER_CONFIRMED = "requester_confirmed"
    const val ADMIN_APPROVED = "admin_approved"
}
