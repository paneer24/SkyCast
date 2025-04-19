package com.example.skycast.ui.Screens

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
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
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
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
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    viewModel.signInWithGoogle(token)
                }
            } catch (e: ApiException) {
                showError("Google sign in failed: ${e.message}")
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