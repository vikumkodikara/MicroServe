package com.example.microserve

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object RatingRepository {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private fun collection() = firestore.collection(Rating.COLLECTION)

    /**
     * Submit a new rating for a provider.
     */
    fun submitRating(
        rating: Rating,
        onSuccess: (Rating) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val docRef = collection().document()
        val payload = rating.copy(id = docRef.id)
        docRef.set(payload.toMap())
            .addOnSuccessListener { onSuccess(payload) }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to submit rating") }
    }

    /**
     * Calculate average rating and total count for a provider.
     */
    fun getProviderAverage(
        providerUid: String,
        onSuccess: (average: Float, count: Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        collection()
            .whereEqualTo(Rating.FIELD_PROVIDER_UID, providerUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val ratings = snapshot.documents.mapNotNull { doc ->
                    (doc.getDouble(Rating.FIELD_STARS))?.toFloat()
                }
                val avg = if (ratings.isNotEmpty()) ratings.average().toFloat() else 0f
                onSuccess(avg, ratings.size)
            }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to load ratings") }
    }

    /**
     * Get all ratings for a specific provider.
     */
    fun getProviderRatings(
        providerUid: String,
        onSuccess: (List<Rating>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        collection()
            .whereEqualTo(Rating.FIELD_PROVIDER_UID, providerUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val ratings = snapshot.documents.map { doc ->
                    Rating.fromMap(doc.id, doc.data.orEmpty())
                }.sortedByDescending { it.createdAt }
                onSuccess(ratings)
            }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to load ratings") }
    }

    /**
     * Listen for real-time rating updates for a provider.
     */
    fun listenProviderRatings(
        providerUid: String,
        onUpdate: (average: Float, count: Int) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection()
            .whereEqualTo(Rating.FIELD_PROVIDER_UID, providerUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to listen for ratings")
                    return@addSnapshotListener
                }
                val ratings = snapshot?.documents?.mapNotNull { doc ->
                    (doc.getDouble(Rating.FIELD_STARS))?.toFloat()
                }.orEmpty()
                val avg = if (ratings.isNotEmpty()) ratings.average().toFloat() else 0f
                onUpdate(avg, ratings.size)
            }
    }
}
