package com.example.microserve

import androidx.annotation.DrawableRes
import com.example.microserve.RequestStore.UserRequest

object BidSampleData {

    data class Bid(
        val providerName: String,
        val priceRs: Int,
        val completionHours: Int = 0
    )

    data class RequesterProfile(
        val age: Int,
        @DrawableRes val avatarRes: Int
    )

    private val defaultBids = listOf(
        Bid("Anuja Silva", 4000),
        Bid("Kulathunga Herath", 3500)
    )

    private val bidsByRequestId = mutableMapOf<String, MutableList<Bid>>()

    private val ageByRequester = mapOf(
        "Sisira Kumara" to 45,
        "Demo User" to 32,
        "Kasun Rajapaksa" to 38,
        "Malini Fernando" to 29
    )

    fun getProfile(request: UserRequest): RequesterProfile {
        val category = CategoryCatalog.findByStoreKey(request.category)
        val avatarRes = category?.imageRes ?: R.drawable.user
        val age = ageByRequester[request.requesterName] ?: 35
        return RequesterProfile(age = age, avatarRes = avatarRes)
    }

    fun getBidsForRequest(request: UserRequest): List<Bid> {
        return bidsByRequestId.getOrPut(request.id) { defaultBids.toMutableList() }
    }

    fun addBid(
        requestId: String,
        priceRs: Int,
        completionHours: Int,
        providerName: String = "You"
    ) {
        bidsByRequestId.getOrPut(requestId) { defaultBids.toMutableList() }
            .add(0, Bid(providerName, priceRs, completionHours))
    }
}
