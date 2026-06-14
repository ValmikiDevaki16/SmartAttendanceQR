package com.devaki.smartattendance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: TextView
    private lateinit var btnBiometric: TextView
    private lateinit var tvRegister: TextView
    private lateinit var tvForgot: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDarkSystemBars()
        setContentView(R.layout.activity_login)
        fadeInScreen()
        bindViews()
        setupClickListeners()
    }

    private fun applyDarkSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor     = Color.parseColor("#0A0A1A")
        window.navigationBarColor = Color.parseColor("#0A0A1A")
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars     = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun fadeInScreen() {
        val root = findViewById<View>(android.R.id.content)
        root.alpha       = 0f
        root.translationY = 30f
        root.animate().alpha(1f).translationY(0f).setDuration(400).start()
    }

    private fun bindViews() {
        tilEmail     = findViewById(R.id.tilEmail)
        tilPassword  = findViewById(R.id.tilPassword)
        etEmail      = findViewById(R.id.etEmail)
        etPassword   = findViewById(R.id.etPassword)
        btnLogin     = findViewById(R.id.btnLogin)
        tvRegister   = findViewById(R.id.tvRegister)
        tvForgot     = findViewById(R.id.tvForgot)
        btnBiometric = findViewById(R.id.btnBiometric)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            animateButtonPress(it) { handleLogin() }
        }

        tvRegister.setOnClickListener {
            animateButtonPress(it) {
                startActivity(Intent(this, RegisterActivity::class.java))
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }
        }

        btnBiometric.setOnClickListener {
            animateButtonPress(it) {
                Toast.makeText(this, "Biometric login coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        tvForgot.setOnClickListener {
            Toast.makeText(this, "Check your registered email", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLogin() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        tilEmail.error    = null
        tilPassword.error = null

        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            shakeView(tilEmail); return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"
            shakeView(tilEmail); return
        }
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            shakeView(tilPassword); return
        }
        if (password.length < 6) {
            tilPassword.error = "Password must be 6+ characters"
            shakeView(tilPassword); return
        }

        btnLogin.text      = "Signing in..."
        btnLogin.alpha     = 0.7f
        btnLogin.isEnabled = false

        btnLogin.postDelayed({
            btnLogin.text      = "Sign in to your universe"
            btnLogin.alpha     = 1f
            btnLogin.isEnabled = true

            val prefs = getSharedPreferences("qr_attendance", MODE_PRIVATE)
            val role  = prefs.getString("user_role", "") ?: ""

            val destination = when (role) {
                "teacher" -> TeacherDashboardActivity::class.java
                "student" -> StudentDashboardActivity::class.java
                else      -> TeacherDashboardActivity::class.java
            }
            startActivity(Intent(this, destination))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1000)
    }

    private fun animateButtonPress(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction { action() }.start()
        }.start()
    }

    private fun shakeView(view: View) {
        val shake = floatArrayOf(0f, 12f, -12f, 10f, -10f, 6f, -6f, 0f)
        var index = 0
        val runnable = object : Runnable {
            override fun run() {
                if (index < shake.size) {
                    view.translationX = shake[index++]
                    view.postDelayed(this, 40)
                } else {
                    view.translationX = 0f
                }
            }
        }
        view.post(runnable)
    }
}