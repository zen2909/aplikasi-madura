package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.HalEvalBinding

class Evaluasi : Activity() {

    lateinit var binding : HalEvalBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        binding = HalEvalBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnEval1.setOnClickListener {
            val intent = Intent(this, EvalTerjemahan::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnEval2.setOnClickListener {
            val intent = Intent(this, EvalTingkatBahasa::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnEval3.setOnClickListener {
            val intent = Intent(this, EvalPelafalan::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

    }
}