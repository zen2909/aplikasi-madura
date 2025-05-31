package com.zen.e_learning_bahasa_madura.view

import android.app.Activity
import android.os.Bundle
import android.widget.ImageButton
import com.zen.e_learning_bahasa_madura.R

class Pelafalan : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hal_pelafalan)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)

        btnBack.setOnClickListener {
            finish()
        }
    }
}