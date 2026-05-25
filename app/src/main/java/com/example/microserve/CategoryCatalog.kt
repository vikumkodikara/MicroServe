package com.example.microserve

import androidx.annotation.DrawableRes

object CategoryCatalog {

    data class Category(
        val id: String,
        val displayName: String,
        val storeKeys: List<String>,
        @DrawableRes val imageRes: Int
    )

    val all: List<Category> = listOf(
        Category("plumbing", "Plumbing", listOf("Plumbing"), R.drawable.nethmina_plumbing),
        Category("gardening", "Gardening", listOf("Gardening"), R.drawable.nethmina_gardening),
        Category("cleaning", "Cleaning", listOf("Cleaning"), R.drawable.nethmina_cleaning),
        Category("painting", "Painting", listOf("House Painting", "Painting"), R.drawable.nethmina_painting),
        Category("electric", "Electric Work", listOf("Electrical", "Electric Work"), R.drawable.nethmina_electric),
        Category("handyman", "Handyman", listOf("Handyman"), R.drawable.nethmina_handyman),
        Category("hvac", "HVAC", listOf("HVAC"), R.drawable.img_hvac),
        Category("mechanic", "Mechanic", listOf("Mechanic"), R.drawable.img_mechanic),
        Category("carpentry", "Carpentry", listOf("Carpentry"), R.drawable.img_carpentry)
    )

    fun findById(id: String): Category? = all.firstOrNull { it.id == id }

    fun findByStoreKey(key: String): Category? {
        return all.firstOrNull { category ->
            category.storeKeys.any { it.equals(key, ignoreCase = true) }
        }
    }
}
