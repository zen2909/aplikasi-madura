package com.zen.e_learning_bahasa_madura.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import com.zen.e_learning_bahasa_madura.R

class Evaluasi : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hal_eval)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        btnBack.setOnClickListener {
            finish()
        }

        val btnTerjemahan = findViewById<Button>(R.id.btn_eval1)
        btnTerjemahan.setOnClickListener {
            val intent = Intent(this, evalTerjemahan::class.java)
            startActivity(intent)
        }

        val btnTb = findViewById<Button>(R.id.btn_eval2)
        btnTb.setOnClickListener {
            val intent = Intent(this, evalTingkatBahasa::class.java)
            startActivity(intent)
        }

        val btnPelafalan = findViewById<Button>(R.id.btn_eval3)
        btnPelafalan.setOnClickListener {
            val intent = Intent(this, evalPelafalan::class.java)
            startActivity(intent)
        }

    }
}