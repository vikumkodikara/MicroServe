package com.example.microserve

import androidx.annotation.DrawableRes

object CategoryCatalog {

    data class Category(
        val id: String,
        val displayName: String,
        val nameResId: Int,
        val storeKeys: List<String>,
        @DrawableRes val imageRes: Int
    )

    val all: List<Category> = listOf(
        Category("plumbing", "Plumbing", R.string.category_plumbing, listOf("Plumbing"), R.drawable.nethmina_plumbing),
        Category("gardening", "Gardening", R.string.category_gardening, listOf("Gardening"), R.drawable.nethmina_gardening),
        Category("cleaning", "Cleaning", R.string.category_cleaning, listOf("Cleaning"), R.drawable.nethmina_cleaning),
        Category("painting", "Painting", R.string.category_painting, listOf("House Painting", "Painting"), R.drawable.nethmina_painting),
        Category("electric", "Electric Work", R.string.category_electric, listOf("Electrical", "Electric Work", "Electric"), R.drawable.nethmina_electric),
        Category("handyman", "Handyman", R.string.category_handyman, listOf("Handyman"), R.drawable.nethmina_handyman),
        Category("hvac", "HVAC", R.string.category_hvac, listOf("HVAC"), R.drawable.nethmina_hvac),
        Category("mechanic", "Mechanic", R.string.category_mechanic, listOf("Mechanic"), R.drawable.nethmina_mechanic),
        Category("carpentry", "Carpentry", R.string.category_carpentry, listOf("Carpentry"), R.drawable.nethmina_carpentry)
    )

    fun findById(id: String): Category? = all.firstOrNull { it.id == id }

    fun findByStoreKey(key: String): Category? {
        return all.firstOrNull { category ->
            category.storeKeys.any { it.equals(key, ignoreCase = true) }
        }
    }
}
