package com.example.skycast.ui.Screens


import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.activity.enableEdgeToEdge
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.skycast.MainActivity
import com.example.skycast.R
import com.example.skycast.data.api.WeatherApi
import com.example.skycast.data.entity.WeatherResponse
import com.example.skycast.data.repository.WeatherRepo
import com.example.skycast.databinding.ActivityWeatherScreenBinding
import com.example.skycast.ui.Viewmodel.AuthViewModel
import com.example.skycast.ui.Viewmodel.AuthViewModelFactory
import com.example.skycast.ui.Viewmodel.WeatherViewModel
import com.example.skycast.ui.Viewmodel.WeatherViewModelFactory
import com.example.skycast.utils.NetworkUtils
import com.example.skycast.utils.Resource
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityWeatherScreenBinding
    private lateinit var viewModel: WeatherViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWeatherScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setupAuthViewModel()
        setupDrawer()
        observeUserProfile()
        setupViewModel()
        setupUI()
        observeWeather()

    }

    private fun setupDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.nav_open,
            R.string.nav_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.logout -> {
                    auth.signOut()
                    // Navigate back to login screen
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    private fun setupViewModel() {
        val weatherApi = NetworkUtils.getRetrofitInstance().create(WeatherApi::class.java)
        val repository = WeatherRepo(weatherApi)
        val factory = WeatherViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[WeatherViewModel::class.java]

    }
    private fun setupAuthViewModel() {
        val factory = AuthViewModelFactory()
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]
        auth = Firebase.auth

        // Fetch user profile when activity starts
        auth.currentUser?.let { user ->
            authViewModel.fetchUserProfile(user.uid)
        }
    }
    private fun setupUI() {
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchEditText.text.toString()
                if (query.isNotEmpty()) {
                    // Show loading before making the API call
                    showLoading()
                    // Hide keyboard
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
                    // Update city
                    viewModel.updateCity(query)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        binding.clearButton.setOnClickListener {
            binding.searchEditText.text.clear()
        }
    }

    private fun observeWeather() {
        lifecycleScope.launch {
            viewModel.weatherState.collect { state ->
                when (state) {
                    is Resource.Loading -> showLoading()
                    is Resource.Success -> {
                        hideLoading()
                        state.data?.let { updateWeatherUI(it) }
                    }

                    is Resource.Error -> {
                        hideLoading()
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun observeUserProfile() {
        lifecycleScope.launch {
            authViewModel.userProfile.collect { state ->
                when (state) {
                    is Resource.Success -> {
                        state.data?.let { user ->
                            user.profilePictureUrl?.let { url ->
                                Glide.with(this@WeatherScreen)
                                    .load(url)
                                    .circleCrop()
                                    .into(binding.menuButton)
                            }
                        }
                    }
                    is Resource.Error -> {
                        binding.menuButton.setImageResource(R.drawable.baseline_menu_24)
                    }
                    is Resource.Loading -> {
                        binding.progressBar1.visibility = View.VISIBLE
                        animateProgressBar()
                        // Optional: Show loading state for profile picture
                    }
                }
            }
        }
    }
    private fun updateWeatherUI(weather: WeatherResponse) {
        binding.apply {
            temperature.text = "${weather.main.temp.toInt()}°"
            weatherstate.text = weather.weather.firstOrNull()?.description?.capitalize() ?: ""
            feelsLike.text = "Feels like ${weather.main.feels_like.toInt()}°"
            pressure.text = "${weather.main.pressure.toInt()} hPa"
            humidity.text = "${weather.main.humidity}%"
            windSpeed.text = "${weather.wind.speed} km/h"

            // New parameters
            sunrise.text = formatTime(weather.sys.sunrise)
            sunset.text = formatTime(weather.sys.sunset)
            visibility.text = formatVisibility(weather.visibility)

            // Load weather GIF based on condition
            val gifResource = getWeatherGif(weather.weather.firstOrNull()?.main ?: "")
            Glide.with(this@WeatherScreen)
                .asGif()
                .load(gifResource)
                .into(iconvector)
        }
    }

    private fun formatTime(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = Date(timestamp * 1000) // Convert to milliseconds
            sdf.format(date)
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun formatVisibility(visibility: Int): String {
        return when {
            visibility >= 1000 -> "${visibility / 1000} km"
            else -> "$visibility m"
        }
    }
    private fun getWeatherGif(condition: String): Int {
        return when (condition.toLowerCase()) {
            // Most common clear conditions
            "clear" -> R.raw.clear_16939742
            "sunny" -> R.raw.sun_14659784

            // Common cloud conditions
            "clouds" -> R.raw.clouds_17102874
            "scattered clouds" -> R.raw.clouds_17102874
            "broken clouds" -> R.raw.clouds_17102874
            "overcast clouds" -> R.raw.clouds_17102874

            // Common rain conditions
            "rain" -> R.raw.rain_6455055
            "light rain" -> R.raw.rain_6455055
            "moderate rain" -> R.raw.rain_6455055
            "heavy rain" -> R.raw.storm_17858208

            // Common snow conditions
            "snow" -> R.raw.snow_17659715
            "light snow" -> R.raw.snow_17659715
            "heavy snow" -> R.raw.snow_17659715

            // Common misty/foggy conditions
            "mist" -> R.raw.fog_18670299
            "fog" -> R.raw.fog_18670299
            "haze" -> R.raw.fog_18670299

            // Storm conditions
            "thunderstorm" -> R.raw.storm_17858208

            // Default to sunny if condition not matched
            else -> R.raw.clear_16939742
        }
    }

    private fun showLoading() {
        binding.progressBar1.visibility = View.VISIBLE
        binding.weatherCard.alpha = 0.6f  // Dim the weather card
        // Dim the details card
        animateProgressBar()
        // Add loading UI if needed
    }

    private fun hideLoading() {
        binding.progressBar1.visibility = View.GONE
        binding.weatherCard.alpha = 1.0f


        binding.progressBar1.clearAnimation()
        // Hide loading UI if needed
    }
    private fun animateProgressBar() {
        try {
            val progressDrawable = binding.progressBar1.indeterminateDrawable.mutate()
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

    private fun showError(message: String) {
        // Show error using Snackbar or Toast
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).apply {
            setAction("Retry") {
                viewModel.fetchWeather()  // Retry loading weather data
            }
            show()
        }
    }
}
