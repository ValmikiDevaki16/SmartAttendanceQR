package com.devaki.smartattendance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var rvClasses: RecyclerView
    private lateinit var tvTeacherName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvStatClasses: TextView
    private lateinit var tvStatStudents: TextView
    private lateinit var tvStatAvg: TextView
    private lateinit var fabAddClass: TextView
    private lateinit var btnLogout: TextView
    // layoutEmpty REMOVED — not in XML

    private val classList = mutableListOf(
        ClassData("CSE - A", "Data Structures", "AB12CD", 78, true),
        ClassData("CSE - B", "DBMS", "XY34ZW", 92, false),
        ClassData("CSE - C", "Computer Networks", "MN56PQ", 65, false)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDarkSystemBars()
        setContentView(R.layout.activity_teacher_dashboard)
        fadeInScreen()
        bindViews()
        loadTeacherInfo()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun applyDarkSystemBars() {
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
        root.animate().alpha(1f).setDuration(350).start()
    }

    private fun bindViews() {
        rvClasses      = findViewById(R.id.rvClasses)
        tvTeacherName  = findViewById(R.id.tvTeacherName)
        tvDate         = findViewById(R.id.tvDate)
        tvStatClasses  = findViewById(R.id.tvStatClasses)
        tvStatStudents = findViewById(R.id.tvStatStudents)
        tvStatAvg      = findViewById(R.id.tvStatAvg)
        fabAddClass    = findViewById(R.id.fabAddClass)
        btnLogout      = findViewById(R.id.btnLogout)
        // layoutEmpty = findViewById(R.id.layoutEmpty) ← REMOVED
    }

    private fun loadTeacherInfo() {
        val prefs = getSharedPreferences("qr_attendance", MODE_PRIVATE)
        val name  = prefs.getString("user_name", "Teacher") ?: "Teacher"
        tvTeacherName.text = "Hello, $name"

        val sdf = java.text.SimpleDateFormat("EEEE, dd MMM yyyy", java.util.Locale.getDefault())
        tvDate.text = sdf.format(java.util.Date())

        updateStats()
    }

    private fun updateStats() {
        tvStatClasses.text  = classList.size.toString()
        tvStatStudents.text = "87"
        tvStatAvg.text      = "82%"

        // layoutEmpty removed — just toggle RecyclerView visibility
        rvClasses.visibility = if (classList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        rvClasses.layoutManager = LinearLayoutManager(this)
        rvClasses.adapter = ClassAdapter(
            classList,
            onStartSession = { classData -> navigateToQRDisplay(classData) },
            onDeleteClass  = { classData, position -> confirmDeleteClass(classData, position) }
        )
    }

    private fun setupClickListeners() {
        fabAddClass.setOnClickListener {
            animateButtonPress(it) { showCreateClassDialog() }
        }
        btnLogout.setOnClickListener {
            animateButtonPress(it) { confirmLogout() }
        }
    }

    private fun navigateToQRDisplay(classData: ClassData) {
        val intent = Intent(this, QRDisplayActivity::class.java).apply {
            putExtra("class_name", classData.name)
            putExtra("subject",    classData.subject)
            putExtra("class_code", classData.classCode)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showCreateClassDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_class, null)

        val dialog = AlertDialog.Builder(this, R.style.DarkDialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etName    = dialogView.findViewById<TextInputEditText>(R.id.etClassName)
        val etSubject = dialogView.findViewById<TextInputEditText>(R.id.etSubject)
        val btnCreate = dialogView.findViewById<TextView>(R.id.btnCreateClass)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDialog)

        btnCreate.setOnClickListener {
            val name    = etName?.text?.toString()?.trim() ?: ""
            val subject = etSubject?.text?.toString()?.trim() ?: ""

            if (name.isEmpty() || subject.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val code     = generateClassCode()
            val newClass = ClassData(name, subject, code, 0, false)
            classList.add(0, newClass)
            rvClasses.adapter?.notifyItemInserted(0)
            rvClasses.scrollToPosition(0)
            updateStats()
            Toast.makeText(this, "Class created! Code: $code", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun confirmDeleteClass(classData: ClassData, position: Int) {
        AlertDialog.Builder(this, R.style.DarkDialog)
            .setTitle("Delete class?")
            .setMessage("Delete ${classData.name} — ${classData.subject}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                classList.removeAt(position)
                rvClasses.adapter?.notifyItemRemoved(position)
                updateStats()
                Toast.makeText(this, "Class deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this, R.style.DarkDialog)
            .setTitle("Sign out?")
            .setMessage("You'll need to sign in again.")
            .setPositiveButton("Sign out") { _, _ ->
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                getSharedPreferences("qr_attendance", MODE_PRIVATE).edit().clear().apply()
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateClassCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun animateButtonPress(view: View, action: () -> Unit) {
        view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction { action() }.start()
        }.start()
    }
}

// ─── Data Class ───────────────────────────────────────────────────────────────
data class ClassData(
    val name: String,
    val subject: String,
    val classCode: String,
    val attendancePercent: Int,
    val isLive: Boolean
)

// ─── RecyclerView Adapter ─────────────────────────────────────────────────────
class ClassAdapter(
    private val classes: MutableList<ClassData>,
    private val onStartSession: (ClassData) -> Unit,
    private val onDeleteClass: (ClassData, Int) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    private val cardColors = listOf(
        Color.parseColor("#1E3A5F"),
        Color.parseColor("#065F46"),
        Color.parseColor("#2D1B69"),
        Color.parseColor("#4C0519")
    )

    inner class ClassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardContainer:   View     = view.findViewById(R.id.cardContainer)
        val tvClassName:     TextView = view.findViewById(R.id.tvClassName)
        val tvSubject:       TextView = view.findViewById(R.id.tvSubject)
        val tvClassCode:     TextView = view.findViewById(R.id.tvClassCode)
        val tvLiveBadge:     TextView = view.findViewById(R.id.tvLiveBadge)
        val viewProgress:    View     = view.findViewById(R.id.viewProgress)
        val viewProgressBg:  View     = view.findViewById(R.id.viewProgressBg)
        val tvAttendancePct: TextView = view.findViewById(R.id.tvAttendancePct)
        val btnStartSession: TextView = view.findViewById(R.id.btnStartSession)
        val btnDelete:       TextView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val classData = classes[position]

        holder.cardContainer.setBackgroundColor(cardColors[position % cardColors.size])
        holder.tvClassName.text      = classData.name
        holder.tvSubject.text        = "${classData.subject} · ${classData.classCode}"
        holder.tvClassCode.text      = classData.classCode
        holder.tvAttendancePct.text  = "${classData.attendancePercent}%"
        holder.tvLiveBadge.visibility = if (classData.isLive) View.VISIBLE else View.GONE

        val pctColor = when {
            classData.attendancePercent >= 75 -> Color.parseColor("#10B981")
            classData.attendancePercent >= 60 -> Color.parseColor("#F59E0B")
            else                              -> Color.parseColor("#EF4444")
        }
        holder.tvAttendancePct.setTextColor(pctColor)

        holder.viewProgressBg.post {
            val totalWidth  = holder.viewProgressBg.width
            val targetWidth = (totalWidth * classData.attendancePercent / 100f).toInt()
            holder.viewProgress.layoutParams.width = 0
            holder.viewProgress.requestLayout()
            holder.viewProgress.postDelayed({
                holder.viewProgress.layoutParams.width = targetWidth
                holder.viewProgress.requestLayout()
            }, (600 + position * 100).toLong())
        }

        holder.btnStartSession.setOnClickListener {
            animatePress(it) { onStartSession(classData) }
        }
        holder.btnDelete.setOnClickListener {
            animatePress(it) { onDeleteClass(classData, holder.adapterPosition) }
        }
    }

    override fun getItemCount() = classes.size

    private fun animatePress(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction { action() }.start()
        }.start()
    }
}