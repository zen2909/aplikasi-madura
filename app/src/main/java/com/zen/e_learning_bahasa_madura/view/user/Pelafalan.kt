package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.os.Bundle
import android.widget.ImageButton
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.HalPelafalanBinding

class Pelafalan : Activity() {

    lateinit var binding : HalPelafalanBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = HalPelafalanBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}