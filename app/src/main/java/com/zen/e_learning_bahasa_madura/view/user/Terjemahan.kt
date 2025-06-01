package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.os.Bundle
import android.widget.ImageButton
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.HalTerjemahanBinding

class Terjemahan : Activity() {

    lateinit var binding : HalTerjemahanBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = HalTerjemahanBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }



}