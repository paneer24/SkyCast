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
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

    private val _authState = MutableStateFlow<Resource<User>>(Resource.Loading)
    val authState: StateFlow<Resource<User>> = _authState.asStateFlow()

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
                fetchUserProfile(user.uid)
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
            // After successful sign in, fetch the user profile
            auth.currentUser?.uid?.let { fetchUserProfile(it) }
        } catch (e: Exception) {
            _authState.value = Resource.Error(e.message ?: "Sign in failed")
        }
    }

    fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            _userProfile.value = Resource.Loading
            try {
                val document = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                if (document.exists()) {
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
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching profile", e)
                _userProfile.value = Resource.Error(e.message ?: "Failed to fetch profile")
            }
        }
    }

    fun isUserAuthenticated() = repository.isUserAuthenticated()
}