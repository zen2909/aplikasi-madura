package com.zen.e_learning_bahasa_madura.view

import android.app.Activity
import android.os.Bundle
import android.widget.ImageButton
import com.zen.e_learning_bahasa_madura.R

class Terjemahan : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hal_terjemahan)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)

        btnBack.setOnClickListener {
            finish()
        }
    }



}