package com.devaki.smartattendance

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.util.UUID

class QRDisplayActivity : AppCompatActivity() {

    private lateinit var ivQrCode: ImageView
    private lateinit var tvClassName: TextView
    private lateinit var tvSubject: TextView
    private lateinit var tvPresentCount: TextView
    private lateinit var tvAbsentCount: TextView
    private lateinit var tvTotalStudents: TextView
    private lateinit var tvTimer: TextView

    private lateinit var btnEndSession: TextView
    private lateinit var btnRefreshQr: TextView
    private lateinit var btnShareQr: TextView

    private var countdownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_qr_display)

        // Dark bars
        window.statusBarColor = Color.parseColor("#0A0A1A")
        window.navigationBarColor = Color.parseColor("#0A0A1A")

        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = false

        // Fade animation
        val rootView = findViewById<View>(android.R.id.content)

        rootView.alpha = 0f

        rootView.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        bindViews()

        loadSessionInfo()

        generateQRCode()

        startCountdown()

        setupClickListeners()
    }

    private fun bindViews() {

        ivQrCode = findViewById(R.id.ivQrCode)

        tvClassName = findViewById(R.id.tvClassName)

        tvSubject = findViewById(R.id.tvSessionInfo)

        tvPresentCount = findViewById(R.id.tvPresentCount)

        tvAbsentCount = findViewById(R.id.tvAbsentCount)

        tvTotalStudents = findViewById(R.id.tvTotalStudents)

        tvTimer = findViewById(R.id.tvTimer)

        btnEndSession = findViewById(R.id.btnEndSession)

        btnRefreshQr = findViewById(R.id.btnRefreshQr)

        btnShareQr = findViewById(R.id.btnShareQr)
    }

    private fun loadSessionInfo() {

        val className =
            intent.getStringExtra("class_name") ?: "CSE-A"

        tvClassName.text = className

        tvSubject.text = "Attendance Session Active"

        tvPresentCount.text = "0"

        tvAbsentCount.text = "0"

        tvTotalStudents.text = "60"
    }

    private fun generateQRCode() {

        try {

            // AFTER (fixed)
            val className = intent.getStringExtra("class_name") ?: "Unknown Class"
            val expiresAt = System.currentTimeMillis() + 5 * 60 * 1000L

            val qrData = JSONObject().apply {
                put("sessionId", UUID.randomUUID().toString())
                put("classId", className)
                put("timestamp", System.currentTimeMillis())
                put("expiresAt", expiresAt)
            }.toString()

            val writer = QRCodeWriter()

            val bitMatrix = writer.encode(
                qrData,
                BarcodeFormat.QR_CODE,
                512,
                512
            )

            val bitmap = Bitmap.createBitmap(
                512,
                512,
                Bitmap.Config.RGB_565
            )

            for (x in 0 until 512) {

                for (y in 0 until 512) {

                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y])
                            Color.BLACK
                        else
                            Color.WHITE
                    )
                }
            }

            ivQrCode.setImageBitmap(bitmap)

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Failed to generate QR",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startCountdown() {

        countdownTimer?.cancel()

        countdownTimer =
            object : CountDownTimer(300000, 1000) {

                override fun onTick(
                    millisUntilFinished: Long
                ) {

                    val minutes =
                        millisUntilFinished / 1000 / 60

                    val seconds =
                        (millisUntilFinished / 1000) % 60

                    tvTimer.text =
                        String.format(
                            "%02d:%02d",
                            minutes,
                            seconds
                        )
                }

                override fun onFinish() {

                    tvTimer.text = "Expired"

                    generateQRCode()

                    startCountdown()
                }

            }.start()
    }

    private fun setupClickListeners() {

        btnRefreshQr.setOnClickListener {

            animateButton(it)

            generateQRCode()

            startCountdown()

            Toast.makeText(
                this,
                "QR Regenerated",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnEndSession.setOnClickListener {

            animateButton(it)

            finish()
        }

        btnShareQr.setOnClickListener {

            animateButton(it)

            Toast.makeText(
                this,
                "QR Share Coming Soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun animateButton(view: View) {

        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    override fun onDestroy() {

        super.onDestroy()

        countdownTimer?.cancel()
    }
}