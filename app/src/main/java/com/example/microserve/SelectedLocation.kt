package com.example.microserve

import android.content.Intent

data class SelectedLocation(
    val province: String,
    val district: String,
    val city: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
) {
    fun formattedLocation(): String {
        val parts = mutableListOf<String>()
        if (address.isNotBlank()) parts.add(address)
        if (city.isNotBlank()) parts.add(city)
        if (district.isNotBlank()) parts.add(district)
        if (province.isNotBlank()) parts.add(province)
        return parts.joinToString(", ")
    }

    companion object {
        private const val EXTRA_PROVINCE = "loc_province"
        private const val EXTRA_DISTRICT = "loc_district"
        private const val EXTRA_CITY = "loc_city"
        private const val EXTRA_ADDRESS = "loc_address"
        private const val EXTRA_LAT = "loc_lat"
        private const val EXTRA_LNG = "loc_lng"

        fun putExtras(intent: Intent, location: SelectedLocation) {
            intent.putExtra(EXTRA_PROVINCE, location.province)
            intent.putExtra(EXTRA_DISTRICT, location.district)
            intent.putExtra(EXTRA_CITY, location.city)
            intent.putExtra(EXTRA_ADDRESS, location.address)
            intent.putExtra(EXTRA_LAT, location.latitude)
            intent.putExtra(EXTRA_LNG, location.longitude)
        }

        fun fromIntent(intent: Intent): SelectedLocation? {
            if (!intent.hasExtra(EXTRA_LAT) || !intent.hasExtra(EXTRA_LNG)) return null
            return SelectedLocation(
                province = intent.getStringExtra(EXTRA_PROVINCE).orEmpty(),
                district = intent.getStringExtra(EXTRA_DISTRICT).orEmpty(),
                city = intent.getStringExtra(EXTRA_CITY).orEmpty(),
                address = intent.getStringExtra(EXTRA_ADDRESS).orEmpty(),
                latitude = intent.getDoubleExtra(EXTRA_LAT, 0.0),
                longitude = intent.getDoubleExtra(EXTRA_LNG, 0.0)
            )
        }
    }
}
