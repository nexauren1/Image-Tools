package com.nexauren.imagetools.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class FirestoreRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    fun syncUser() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).set(
            mapOf(
                "displayName" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "plan" to "free",
                "premium" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
    }

    fun observePremium(callback: (Boolean) -> Unit) {
        listener?.remove()
        val user = auth.currentUser
        if (user == null) {
            callback(false)
            return
        }
        listener = db.collection("users").document(user.uid).addSnapshotListener { snap, _ ->
            callback(snap?.getBoolean("premium") == true)
        }
    }

    fun updateDisplayName(name: String) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).update(
            mapOf("displayName" to name, "updatedAt" to FieldValue.serverTimestamp())
        )
    }

    fun stop() {
        listener?.remove()
        listener = null
    }
}