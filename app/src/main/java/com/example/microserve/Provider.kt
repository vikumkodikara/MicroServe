package com.example.microserve

import java.io.Serializable

data class Provider(
    val id: String,
    val name: String,
    val location: String,
    val rating: Float,
    val category: String,
    val services: String = "",
    val priceModel: String = "",
    val schedule: String = ""
): Serializable
