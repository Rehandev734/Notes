package com.example.notesapp.data.repository

import com.example.notesapp.data.model.User
import com.example.notesapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun login(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user ?: error("Login succeeded but user was null")
    }

    override suspend fun register(name: String, email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error("Registration succeeded but user was null")
        val appUser = User(
            uid = user.uid,
            name = name,
            email = email
        )
        firestore.collection("users").document(user.uid).set(appUser).await()
        user
    }

    override suspend fun googleSignIn(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: error("Google sign-in succeeded but user was null")
        val doc = firestore.collection("users").document(user.uid).get().await()
        if (!doc.exists()) {
            val appUser = User(
                uid = user.uid,
                name = user.displayName ?: "User",
                email = user.email ?: ""
            )
            firestore.collection("users").document(user.uid).set(appUser).await()
        }
        user
    }

    override fun logout() {
        auth.signOut()
    }
}
