package com.example.skycast.data.repository

import com.example.skycast.data.entity.User
import com.example.skycast.utils.Resource
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    suspend fun signInWithGoogle(idToken: String): Resource<User> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            authResult.user?.let { firebaseUser ->
                if (authResult.additionalUserInfo?.isNewUser == true) {
                    val user = User(
                        uid = firebaseUser.uid,
                        name = firebaseUser.displayName,
                        email = firebaseUser.email,
                        profilePictureUrl = firebaseUser.photoUrl?.toString()
                    )
                    saveUserToFirestore(user)
                    Resource.Success(user)
                } else {
                    Resource.Success(getUserFromFirestore(firebaseUser.uid))
                }
            } ?: Resource.Error("Authentication failed")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    private suspend fun saveUserToFirestore(user: User) {
        firestore.collection("users")
            .document(user.uid)
            .set(user)
            .await()
    }

    private suspend fun getUserFromFirestore(uid: String): User {
        return firestore.collection("users")
            .document(uid)
            .get()
            .await()
            .toObject (User::class.java) ?: throw Exception("User data not found")
    }

    fun isUserAuthenticated() = auth.currentUser != null

    fun getCurrentUser(): User? {
        return auth.currentUser?.let { firebaseUser ->
            User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName,
                email = firebaseUser.email,
                profilePictureUrl = firebaseUser.photoUrl?.toString()
            )
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }
}