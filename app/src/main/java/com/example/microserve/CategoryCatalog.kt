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
        Category("plumbing", "Plumbing", R.string.category_plumbing, listOf("Plumbing"), R.drawable.plumber),
        Category("gardening", "Gardening", R.string.category_gardening, listOf("Gardening"), R.drawable.gardening),
        Category("cleaning", "Cleaning", R.string.category_cleaning, listOf("Cleaning"), R.drawable.cleaning),
        Category("painting", "Painting", R.string.category_painting, listOf("House Painting", "Painting"), R.drawable.painting),
        Category("electric", "Electric Work", R.string.category_electric, listOf("Electrical", "Electric Work", "Electric"), R.drawable.electrician),
        Category("handyman", "Handyman", R.string.category_handyman, listOf("Handyman"), R.drawable.handyman),
        Category("hvac", "HVAC", R.string.category_hvac, listOf("HVAC"), R.drawable.hvac),
        Category("mechanic", "Mechanic", R.string.category_mechanic, listOf("Mechanic"), R.drawable.mechanic),
        Category("carpentry", "Carpentry", R.string.category_carpentry, listOf("Carpentry"), R.drawable.carpentry)
    )

    fun findById(id: String): Category? = all.firstOrNull { it.id == id }

    fun findByStoreKey(key: String): Category? {
        return all.firstOrNull { category ->
            category.storeKeys.any { it.equals(key, ignoreCase = true) }
        }
    }

    /** Display order and labels for Post Ads / form spinners (matches Request Main). */
    val spinnerOrder: List<String> = listOf(
        "plumbing", "gardening", "cleaning", "painting", "electric",
        "handyman", "carpentry", "mechanic", "hvac"
    )

    fun categoriesForSpinner(): List<Category> =
        spinnerOrder.mapNotNull { id -> findById(id) }

    fun spinnerLabel(category: Category): String = when (category.id) {
        "electric" -> "Electric"
        "painting" -> "Painting"
        else -> category.storeKeys.first()
    }

    fun storeKeyForSpinnerLabel(label: String): String? {
        val trimmed = label.trim()
        if (trimmed.isBlank() || trimmed.startsWith("-")) return null
        return categoriesForSpinner()
            .firstOrNull { spinnerLabel(it).equals(trimmed, ignoreCase = true) }
            ?.storeKeys
            ?.first()
            ?: findByStoreKey(trimmed)?.storeKeys?.first()
    }
}
