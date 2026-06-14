package com.devaki.smartattendance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: TextView
    private lateinit var btnBiometric: TextView
    private lateinit var tvRegister: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyUltraDarkSystemBars()

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        // Already logged in — fetch role from Firestore and route correctly
        if (auth.currentUser != null) {
            navigateFromFirestore()   // ✅ reads Firestore, not just SharedPrefs
            return
        }

        setContentView(R.layout.activity_main)
        fadeInScreen()
        bindViews()
        setupClickListeners()
    }

    private fun applyUltraDarkSystemBars() {
        window.statusBarColor = Color.parseColor("#0A0A1A")
        window.navigationBarColor = Color.parseColor("#0A0A1A")
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun fadeInScreen() {
        val root = findViewById<View>(android.R.id.content)
        root.alpha = 0f
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
            Toast.makeText(this, "Biometric login coming soon!", Toast.LENGTH_SHORT).show()
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
            tilPassword.error = "Min 6 characters"
            shakeView(tilPassword); return
        }

        btnLogin.text      = "Signing in..."
        btnLogin.alpha     = 0.7f
        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // Always read role from Firestore — source of truth
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("name") ?: "User"
                        val role = doc.getString("role") ?: "student"

                        // Update SharedPreferences with fresh data
                        getSharedPreferences("qr_attendance", MODE_PRIVATE).edit().apply {
                            putString("user_uid",   uid)
                            putString("user_name",  name)
                            putString("user_email", email)
                            putString("user_role",  role)
                            apply()
                        }

                        resetButton()
                        Toast.makeText(this, "Welcome back, $name!", Toast.LENGTH_SHORT).show()
                        routeToDashboard(role)
                    }
                    .addOnFailureListener {
                        resetButton()
                        Toast.makeText(this, "Failed to get user data", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                resetButton()
                val msg = when {
                    e.message?.contains("no user record")  == true ||
                            e.message?.contains("identifier")      == true ->
                        "No account found with this email."
                    e.message?.contains("password is invalid") == true ||
                            e.message?.contains("incorrect")           == true ->
                        "Wrong password. Try again."
                    e.message?.contains("network") == true ->
                        "No internet connection."
                    else -> "Login failed: ${e.message}"
                }
                tilPassword.error = msg
                shakeView(tilPassword)
            }
    }

    /**
     * Called when user is already logged in (app relaunch).
     * Always fetches role from Firestore — never trusts SharedPrefs default.
     */
    private fun navigateFromFirestore() {
        val uid = auth.currentUser?.uid ?: run {
            // No valid user — go to login
            auth.signOut()
            setContentView(R.layout.activity_main)
            fadeInScreen(); bindViews(); setupClickListeners()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "student"
                val name = doc.getString("name") ?: "User"

                // Refresh SharedPrefs with correct role
                getSharedPreferences("qr_attendance", MODE_PRIVATE).edit().apply {
                    putString("user_uid",  uid)
                    putString("user_name", name)
                    putString("user_role", role)
                    apply()
                }

                routeToDashboard(role)
            }
            .addOnFailureListener {
                // Firestore failed — fall back to SharedPrefs as last resort
                val prefs = getSharedPreferences("qr_attendance", MODE_PRIVATE)
                val role  = prefs.getString("user_role", null)

                if (role != null) {
                    routeToDashboard(role)
                } else {
                    // Can't determine role — force re-login
                    auth.signOut()
                    setContentView(R.layout.activity_main)
                    fadeInScreen(); bindViews(); setupClickListeners()
                    Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /** Single place that does the actual navigation */
    private fun routeToDashboard(role: String) {
        val dest = if (role == "teacher")
            TeacherDashboardActivity::class.java
        else
            StudentDashboardActivity::class.java

        startActivity(Intent(this, dest))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun resetButton() {
        btnLogin.text      = "Sign in to your universe"
        btnLogin.alpha     = 1f
        btnLogin.isEnabled = true
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