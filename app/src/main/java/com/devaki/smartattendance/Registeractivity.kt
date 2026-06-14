package com.devaki.smartattendance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var rgRole: RadioGroup
    private lateinit var rbTeacher: RadioButton
    private lateinit var rbStudent: RadioButton
    private lateinit var btnRegister: TextView
    private lateinit var tvLogin: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDarkSystemBars()
        setContentView(R.layout.activity_register)
        fadeInScreen()
        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()
        bindViews()
        setupClickListeners()
    }

    private fun applyDarkSystemBars() {
        window.statusBarColor = Color.parseColor("#0D1117")
        window.navigationBarColor = Color.parseColor("#0D1117")
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
        tilName     = findViewById(R.id.tilName)
        tilEmail    = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etName      = findViewById(R.id.etName)
        etEmail     = findViewById(R.id.etEmail)
        etPassword  = findViewById(R.id.etPassword)
        rgRole      = findViewById(R.id.rgRole)
        rbTeacher   = findViewById(R.id.rbTeacher)
        rbStudent   = findViewById(R.id.rbStudent)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin     = findViewById(R.id.tvLogin)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener { animateButtonPress(it) { handleRegister() } }
        tvLogin.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
        rgRole.setOnCheckedChangeListener { _, checkedId ->
            val sel   = Color.parseColor("#6366F1")
            val unsel = Color.parseColor("#161B22")
            when (checkedId) {
                R.id.rbTeacher -> { rbTeacher.setBackgroundColor(sel); rbStudent.setBackgroundColor(unsel) }
                R.id.rbStudent -> { rbStudent.setBackgroundColor(sel); rbTeacher.setBackgroundColor(unsel) }
            }
        }
    }

    private fun handleRegister() {
        val name     = etName.text.toString().trim()
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val role     = if (rbTeacher.isChecked) "teacher" else "student"

        tilName.error = null; tilEmail.error = null; tilPassword.error = null

        if (name.isEmpty())   { tilName.error = "Name is required";    shakeView(tilName);     return }
        if (name.length < 3)  { tilName.error = "Min 3 characters";    shakeView(tilName);     return }
        if (email.isEmpty())  { tilEmail.error = "Email is required";  shakeView(tilEmail);    return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"; shakeView(tilEmail); return
        }
        if (password.isEmpty())  { tilPassword.error = "Password required"; shakeView(tilPassword); return }
        if (password.length < 6) { tilPassword.error = "Min 6 characters";  shakeView(tilPassword); return }

        btnRegister.text = "Creating account..."; btnRegister.alpha = 0.7f; btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val userData = hashMapOf(
                    "uid"   to uid,
                    "name"  to name,
                    "email" to email,
                    "role"  to role
                )

                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {

                        // ── Save to SharedPreferences ──────────────────
                        getSharedPreferences("qr_attendance", MODE_PRIVATE).edit().apply {
                            putString("user_uid",   uid)
                            putString("user_name",  name)
                            putString("user_email", email)
                            putString("user_role",  role)
                            apply()
                        }

                        resetButton()

                        Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show()

                        // ── Navigate based on role ─────────────────────
                        val dest = if (role == "teacher")
                            TeacherDashboardActivity::class.java
                        else
                            StudentDashboardActivity::class.java

                        startActivity(Intent(this, dest))
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        resetButton()
                        Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                resetButton()
                val msg = when {
                    e.message?.contains("already in use") == true -> "Email already registered. Please login."
                    e.message?.contains("network")        == true -> "No internet connection."
                    else -> "Registration failed: ${e.message}"
                }
                tilEmail.error = msg
                shakeView(tilEmail)
            }
    }

    private fun resetButton() {
        btnRegister.text      = "Create my account"
        btnRegister.alpha     = 1f
        btnRegister.isEnabled = true
    }

    private fun animateButtonPress(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction { action() }.start()
        }.start()
    }

    private fun shakeView(view: View) {
        val shake = floatArrayOf(0f, 12f, -12f, 10f, -10f, 6f, -6f, 0f)
        var index = 0
        val r = object : Runnable {
            override fun run() {
                if (index < shake.size) {
                    view.translationX = shake[index++]
                    view.postDelayed(this, 40)
                } else {
                    view.translationX = 0f
                }
            }
        }
        view.post(r)
    }
}