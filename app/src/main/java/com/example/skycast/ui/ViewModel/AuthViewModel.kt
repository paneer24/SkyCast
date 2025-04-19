package com.example.skycast.ui.Viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skycast.data.entity.User
import com.example.skycast.data.repository.AuthRepository
import com.example.skycast.utils.Resource
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<Resource<User>>(Resource.Loading)
    val authState: StateFlow<Resource<User>> = _authState.asStateFlow()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

    private val _userProfile = MutableStateFlow<Resource<User>>(Resource.Loading)
    val userProfile: StateFlow<Resource<User>> = _userProfile
    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        if (repository.isUserAuthenticated()) {
            val user = repository.getCurrentUser()
            if (user != null) {
                _authState.value = Resource.Success(user)
            } else {
                _authState.value = Resource.Error("User data not available")
            }
        } else {
            _authState.value = Resource.Success(null)
        }
    }

    fun signInWithGoogle(idToken: String) = viewModelScope.launch {
        try {
            _authState.value = Resource.Loading
            _authState.value = repository.signInWithGoogle(idToken)
        } catch (e: Exception) {
            _authState.value = Resource.Error(e.message ?: "Sign in failed")
        }
    }

    fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            _userProfile.value = Resource.Loading
            try {
                // Use coroutines with suspending function
                firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val user = document.toObject(User::class.java)
                            if (user != null) {
                                _userProfile.value = Resource.Success(user)
                                Log.d("AuthViewModel", "Profile loaded: ${user.profilePictureUrl}")
                            } else {
                                _userProfile.value = Resource.Error("Invalid user data")
                            }
                        } else {
                            _userProfile.value = Resource.Error("Profile not found")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("AuthViewModel", "Error fetching profile", e)
                        _userProfile.value = Resource.Error(e.message ?: "Failed to fetch profile")
                    }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in fetchUserProfile", e)
                _userProfile.value = Resource.Error(e.message ?: "An error occurred")
            }
        }
    }
    fun isUserAuthenticated() = repository.isUserAuthenticated()
}