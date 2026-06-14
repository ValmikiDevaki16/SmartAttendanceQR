package com.devaki.smartattendance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONException
import org.json.JSONObject

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var tvStudentName: TextView
    private lateinit var tvOverallPercent: TextView
    private lateinit var btnScanQR: TextView
    private lateinit var btnJoinClass: TextView
    private lateinit var btnLogout: TextView
    private lateinit var tvScanResult: TextView
    private lateinit var rvSubjects: RecyclerView

    private val subjects = mutableListOf(
        SubjectData("Data Structures", 92, 23, 25),
        SubjectData("DBMS", 68, 17, 25),
        SubjectData("Computer Networks", 80, 20, 25),
        SubjectData("Operating Systems", 88, 22, 25)
    )

    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            processScannedQR(result.contents)
        } else {
            showScanResult("Scan cancelled", false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0A0F1E")
        window.navigationBarColor = Color.parseColor("#0A0F1E")
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
        setContentView(R.layout.activity_student_dashboard)

        val root = findViewById<View>(android.R.id.content)
        root.alpha = 0f
        root.animate().alpha(1f).setDuration(350).start()

        val prefs = getSharedPreferences("qr_attendance", MODE_PRIVATE)
        val name  = prefs.getString("user_name", "Student") ?: "Student"

        tvStudentName    = findViewById(R.id.tvStudentName)
        tvOverallPercent = findViewById(R.id.tvOverallPercent)
        btnScanQR        = findViewById(R.id.btnScanQR)
        btnJoinClass     = findViewById(R.id.btnJoinClass)
        btnLogout        = findViewById(R.id.btnLogout)
        tvScanResult     = findViewById(R.id.tvScanResult)
        rvSubjects       = findViewById(R.id.rvSubjects)

        tvStudentName.text = "Hi, $name!"

        val overall = subjects.map { it.percentage }.average().toInt()
        tvOverallPercent.text = "$overall%"
        tvOverallPercent.setTextColor(when {
            overall >= 75 -> Color.parseColor("#10B981")
            overall >= 60 -> Color.parseColor("#F59E0B")
            else          -> Color.parseColor("#EF4444")
        })

        rvSubjects.layoutManager = LinearLayoutManager(this)
        rvSubjects.adapter = SubjectAdapter(subjects)

        btnScanQR.setOnClickListener   { animatePress(it) { launchScanner() } }
        btnJoinClass.setOnClickListener { Toast.makeText(this, "Enter class code to join", Toast.LENGTH_SHORT).show() }
        btnLogout.setOnClickListener   { animatePress(it) { doLogout() } }
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Point camera at the teacher's QR code")
            setBeepEnabled(true)
            setOrientationLocked(true)
        }
        qrScanLauncher.launch(options)
    }

    private fun processScannedQR(raw: String) {
        Log.d("QR_SCAN", "Raw: $raw")
        val cleaned = raw.trim()

        try {
            val json = JSONObject(cleaned)
            Log.d("QR_SCAN", "Keys found: ${json.keys().asSequence().toList()}")

            // ── Try both key formats (camelCase from QRDisplayActivity) ──────
            // Teacher sends: sessionId, class_id, generated_at, expires_at
            val sessionId = json.optString("session_id", "")
                .ifEmpty { json.optString("sessionId", "") }

            val expiresAt = when {
                json.has("expires_at")   -> json.getLong("expires_at")
                json.has("expiresAt")    -> json.getLong("expiresAt")
                json.has("timestamp")    -> json.getLong("timestamp") + 5 * 60 * 1000L
                else                     -> Long.MAX_VALUE  // no expiry key — treat as valid
            }

            val classId = json.optString("class_id", "")
                .ifEmpty { json.optString("classId", "Unknown Class") }

            if (sessionId.isEmpty()) {
                Log.e("QR_SCAN", "No session ID found. Keys: ${json.keys().asSequence().toList()}")
                showScanResult("Invalid QR code! Not a SmartAttendance QR.", false)
                return
            }

            // ── Check expiry ──────────────────────────────────────────────────
            if (System.currentTimeMillis() > expiresAt) {
                showScanResult("QR expired! Ask teacher to regenerate.", false)
                return
            }

            Log.d("QR_SCAN", "SUCCESS — session: $sessionId, class: $classId")
            showScanResult("Attendance marked! Class: $classId", true)

        } catch (e: JSONException) {
            Log.e("QR_SCAN", "JSON parse failed for: '$cleaned'", e)
            showScanResult("Invalid QR code! Could not read data.", false)
        } catch (e: Exception) {
            Log.e("QR_SCAN", "Unexpected error: ${e.message}", e)
            showScanResult("Something went wrong. Try again.", false)
        }
    }

    private fun showScanResult(msg: String, success: Boolean) {
        tvScanResult.text = msg
        tvScanResult.visibility = View.VISIBLE
        tvScanResult.setTextColor(
            if (success) Color.parseColor("#10B981")
            else         Color.parseColor("#EF4444")
        )
        tvScanResult.removeCallbacks(hideScanResult)
        tvScanResult.postDelayed(hideScanResult, 5000)
    }

    private val hideScanResult = Runnable { tvScanResult.visibility = View.GONE }

    private fun doLogout() {
        getSharedPreferences("qr_attendance", MODE_PRIVATE).edit().clear().apply()
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun animatePress(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction { action() }.start()
        }.start()
    }
}

// ─── Data Class ───────────────────────────────────────────────────────────────
data class SubjectData(
    val name: String,
    val percentage: Int,
    val present: Int,
    val total: Int
)

// ─── RecyclerView Adapter ─────────────────────────────────────────────────────
class SubjectAdapter(
    private val subjects: MutableList<SubjectData>
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    inner class SubjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSubjectName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvPercent:     TextView = view.findViewById(R.id.tvPercent)
        val tvCount:       TextView = view.findViewById(R.id.tvCount)
        val viewProgress:  View     = view.findViewById(R.id.viewProgress)
        val viewProgressBg:View     = view.findViewById(R.id.viewProgressBg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = subjects[position]
        holder.tvSubjectName.text = subject.name
        holder.tvPercent.text     = "${subject.percentage}%"
        holder.tvCount.text       = "${subject.present}/${subject.total} classes"

        val color = when {
            subject.percentage >= 75 -> Color.parseColor("#10B981")
            subject.percentage >= 60 -> Color.parseColor("#F59E0B")
            else                     -> Color.parseColor("#EF4444")
        }
        holder.tvPercent.setTextColor(color)
        holder.viewProgress.setBackgroundColor(color)

        holder.viewProgressBg.post {
            val total  = holder.viewProgressBg.width
            val target = (total * subject.percentage / 100f).toInt()
            holder.viewProgress.postDelayed({
                val p = holder.viewProgress.layoutParams
                p.width = target
                holder.viewProgress.layoutParams = p
            }, (200 + position * 80).toLong())
        }
    }

    override fun getItemCount() = subjects.size
}