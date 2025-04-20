package com.example.skycast.data.repository

import android.util.Log
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
                    try {
                        saveUserToFirestore(user)
                        Resource.Success(user)
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Failed to save new user to Firestore", e)
                        Resource.Error("Failed to create user profile: ${e.message}")
                    }
                } else {
                    try {
                        Resource.Success(getUserFromFirestore(firebaseUser.uid))
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Failed to get existing user from Firestore", e)
                        // If user document doesn't exist, create it
                        val user = User(
                            uid = firebaseUser.uid,
                            name = firebaseUser.displayName,
                            email = firebaseUser.email,
                            profilePictureUrl = firebaseUser.photoUrl?.toString()
                        )
                        try {
                            saveUserToFirestore(user)
                            Resource.Success(user)
                        } catch (e: Exception) {
                            Log.e("AuthRepository", "Failed to create user document after failed fetch", e)
                            Resource.Error("Failed to create user profile: ${e.message}")
                        }
                    }
                }
            } ?: Resource.Error("Authentication failed")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google Sign-In failed", e)
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    private suspend fun saveUserToFirestore(user: User) {
        try {
            Log.d("AuthRepository", "Attempting to save user to Firestore: ${user.uid}")
            val result = firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            Log.d("AuthRepository", "Successfully saved user to Firestore: ${user.uid}")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving user to Firestore", e)
            throw Exception("Failed to save user to Firestore: ${e.message}")
        }
    }

    private suspend fun getUserFromFirestore(uid: String): User {
        return try {
            Log.d("AuthRepository", "Attempting to fetch user from Firestore: $uid")
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                if (user != null) {
                    Log.d("AuthRepository", "Successfully fetched user from Firestore: $uid")
                    user
                } else {
                    Log.e("AuthRepository", "User document exists but could not be converted to User object")
                    throw Exception("User data not found")
                }
            } else {
                Log.e("AuthRepository", "User document does not exist: $uid")
                throw Exception("User document does not exist")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching user from Firestore", e)
            throw Exception("Failed to get user from Firestore: ${e.message}")
        }
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