package com.hamidi.forexrobot

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class LoginActivity : AppCompatActivity() {

    private val CORRECT_PASSWORD = "Hamidi2026@#AK"
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_login)

        val etPassword  = findViewById<EditText>(R.id.etPassword)
        val btnLogin    = findViewById<Button>(R.id.btnLogin)
        val ivEye       = findViewById<ImageView>(R.id.ivEye)

        ivEye.setOnClickListener {
            passwordVisible = !passwordVisible
            etPassword.inputType = if (passwordVisible) {
                ivEye.alpha = 1.0f
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                ivEye.alpha = 0.5f
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val input = etPassword.text.toString()
            if (input == CORRECT_PASSWORD) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "⛔ رمز غلط دی! Wrong Password!", Toast.LENGTH_LONG).show()
                etPassword.text.clear()
            }
        }
    }
}
