package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.material3.AlertDialog
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.auth.Login
import com.zen.e_learning_bahasa_madura.databinding.HomePageBinding
import com.zen.e_learning_bahasa_madura.util.BacksoundManager

class HomePage : Activity() {

    private var clickCount = 0
    private val requiredClicks = 10
    private val resetTimeout = 2000L
    private var lastClickTime = 0L

    lateinit var binding : HomePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = HomePageBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root) // Sesuaikan dengan nama file XML Anda

        BacksoundManager.start(this, R.raw.tanduk_majeng)

        binding.btnTerjemahan.setOnClickListener {
            val intent = Intent(this, Terjemahan::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnTb.setOnClickListener {
            val intent = Intent(this, TingkatBahasa::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnPelafalan.setOnClickListener {
            val intent = Intent(this, Pelafalan::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnEval.setOnClickListener {
            val intent = Intent(this, Evaluasi::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.triggeradmin.setOnClickListener {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastClickTime >= resetTimeout){
                clickCount = 0
            }

            lastClickTime = currentTime
            clickCount++

            if (clickCount >= requiredClicks){
             clickCount = 0
             showadminlogin()
            }
        }
    }

    private fun showadminlogin() {
        AlertDialog.Builder(this)
            .setTitle("Login Admin")
            .setMessage("Masukkan Email Dan Password Admin!!!")
            .setPositiveButton("Login") { _, _ ->
                startActivity(Intent(this, Login::class.java))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}