package com.example.skycast.ui.Screens

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.skycast.MainActivity
import com.example.skycast.R
import com.example.skycast.databinding.FragmentLoginBinding
import com.example.skycast.ui.Viewmodel.AuthViewModel
import com.example.skycast.utils.Resource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGoogleSignIn()
        setupClickListeners()
        observeAuthState()
        animateProgressBar()
    }

    private fun setupGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestId()
                .requestProfile()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build()

            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            Log.d("LoginFragment", "Google Sign-In client initialized successfully")
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error initializing Google Sign-In client", e)
            showError("Failed to initialize Google Sign-In: ${e.message}")
        }
    }

    private fun setupClickListeners() {
        binding.btnGoogleSignIn.setOnClickListener {
            signIn()
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is Resource.Success -> {
                        if (state.data != null) {
                            hideLoading()
                            (requireActivity() as MainActivity).setupWeatherUI()
                        }
                    }
                    is Resource.Error -> {
                        hideLoading()
                        showError(state.message)
                    }
                    is Resource.Loading -> showLoading()
                }
            }
        }
    }

    private fun signIn() {
        try {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
            Log.d("LoginFragment", "Starting Google Sign-In flow")
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error starting Google Sign-In", e)
            showError("Failed to start Google Sign-In: ${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                Log.d("LoginFragment", "Google Sign-In successful, account: ${account?.email}")
                
                account?.idToken?.let { token ->
                    viewModel.signInWithGoogle(token)
                } ?: run {
                    Log.e("LoginFragment", "No ID token received from Google Sign-In")
                    showError("Failed to get authentication token")
                }
            } catch (e: ApiException) {
                Log.e("LoginFragment", "Google Sign-In failed with error code: ${e.statusCode}", e)
                when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                        showError("Sign in was cancelled")
                    }
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> {
                        showError("Sign in failed. Please try again")
                    }
                    else -> {
                        showError("Google sign in failed: ${e.message}")
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGoogleSignIn.isEnabled = false
        animateProgressBar()
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnGoogleSignIn.isEnabled = true
        binding.progressBar.clearAnimation()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun animateProgressBar() {
        try {
            val progressDrawable = binding.progressBar.indeterminateDrawable.mutate()
            val gradientDrawable = (progressDrawable as LayerDrawable)
                .findDrawableByLayerId(android.R.id.progress) as GradientDrawable

            val colorAnim = ValueAnimator.ofObject(
                ArgbEvaluator(),
                Color.parseColor("#051923"),  // Google Green
                Color.parseColor("#003554"),  // Google Yellow
                Color.parseColor("#0582CA"),  // Google Red
                Color.parseColor("#00A6FB")   // Google Blue
            )

            colorAnim.duration = 2000
            colorAnim.repeatMode = ValueAnimator.REVERSE
            colorAnim.repeatCount = ValueAnimator.INFINITE
            colorAnim.interpolator = LinearInterpolator()

            colorAnim.addUpdateListener { animator ->
                gradientDrawable.setColor(animator.animatedValue as Int)
            }

            colorAnim.start()
        } catch (e: Exception) {
            // Fallback to default progress bar if animation fails
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}