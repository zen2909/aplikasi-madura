package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.InputEvalPelafalanBinding
import com.zen.e_learning_bahasa_madura.util.NavHelper

class InputEvalPelafalan : Activity() {

    lateinit var binding : InputEvalPelafalanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = InputEvalPelafalanBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        NavHelper.setup(
            activity = this,
            menuInputKosakata = binding.menuInputKosakata,
            menuEval = binding.menuEval,
            menuList = binding.menuList,
            menuSoal = binding.menuSoal,
            currentClass = InputEvalPelafalan::class.java
        )

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