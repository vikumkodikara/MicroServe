package com.example.microserve

object AdDisplayHelper {

    /** Shows city/area on cards without raw GPS coordinates. */
    fun formatLocationForCard(location: String): String {
        if (location.isBlank()) return location
        val withoutPin = location.replace(
            Regex("^Pinned at \\d+(?:\\.\\d+)?, \\d+(?:\\.\\d+)?,?\\s*", RegexOption.IGNORE_CASE),
            ""
        ).trim()
        return withoutPin.ifBlank { location }
    }
}
