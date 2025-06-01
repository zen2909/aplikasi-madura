package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.InputEvalPelafalanBinding

class InputEvalPelafalan : Activity() {

    lateinit var binding : InputEvalPelafalanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = InputEvalPelafalanBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.inputkosakata.setOnClickListener {
            val intent = Intent(this, InputKosakata::class.java)
            startActivity(intent)
        }

        binding.inputeval.setOnClickListener {
            val intent = Intent(this, InputEvalTerjemahan::class.java)
            startActivity(intent)
        }

        binding.listkosakata.setOnClickListener {
            val intent = Intent(this, Kosakata::class.java)
            startActivity(intent)
        }

        binding.listsoaleval.setOnClickListener {
            val intent = Intent(this, SoalEvaluasi::class.java)
            startActivity(intent)
        }

        binding.inputterjemahan.setOnClickListener {
            val intent = Intent(this, InputEvalTerjemahan::class.java)
            startActivity(intent)
        }

        binding.inputtb.setOnClickListener {
            val intent = Intent(this, InputEvalTb::class.java)
            startActivity(intent)
        }
    }
}