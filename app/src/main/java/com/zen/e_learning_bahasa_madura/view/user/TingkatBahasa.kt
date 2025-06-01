package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.os.Bundle
import android.widget.ImageButton
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.HalTingkatbhsBinding

class TingkatBahasa : Activity() {

    lateinit var binding : HalTingkatbhsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = HalTingkatbhsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}