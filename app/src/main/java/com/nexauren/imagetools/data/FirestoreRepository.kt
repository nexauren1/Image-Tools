package com.nexauren.imagetools.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source

class FirestoreRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    /*
     * Firestore client rules intentionally make users/{uid} read-only.
     * Account profile and entitlement writes are handled by the trusted backend.
     *
     * Keep this method for compatibility with existing call sites, but it no
     * longer performs a client-side write.
     */
    fun syncUser() {
        // User account data is managed by the backend.
    }

    /*
     * Premium entitlement is stored by the trusted backend directly on:
     * users/{uid}.premium
     *
     * The client may read this field but cannot write it.
     */
    fun refreshPremiumFromServer(callback: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            callback(false)
            return
        }
        db.collection("users").document(user.uid).get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                callback(snapshot.exists() && snapshot.getBoolean("premium") == true)
            }
            .addOnFailureListener { callback(false) }
    }

    fun observePremium(callback: (Boolean) -> Unit) {
        listener?.remove()

        val user = auth.currentUser
        if (user == null) {
            callback(false)
            return
        }

        val userDocument = db.collection("users").document(user.uid)

        refreshPremiumFromServer(callback)

        listener = userDocument.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null || snapshot == null || snapshot.metadata.isFromCache) {
                return@addSnapshotListener
            }
            callback(snapshot.exists() && snapshot.getBoolean("premium") == true)
        }
    }

    /*
     * Profile changes are server-managed under the current Firestore rules.
     * Keep the method for compatibility with existing UI call sites.
     */
    fun updateDisplayName(name: String) {
        // Profile updates are managed by the backend.
    }

    fun stop() {
        listener?.remove()
        listener = null
    }
}
