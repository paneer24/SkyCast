package com.example.skycast
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.skycast.databinding.ActivityMainBinding
import com.example.skycast.ui.Screens.WeatherScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth


class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Check if user is already logged in
        if (Firebase.auth.currentUser != null) {
            setupWeatherUI()
        }
    }

    fun setupWeatherUI() {
        // Remove the NavHostFragment and set up your weather UI
        startWeatherActivity()
    }
    private fun startWeatherActivity() {
        val intent = Intent(this, WeatherScreen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}