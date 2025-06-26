package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.InputEvalTbBinding
import com.zen.e_learning_bahasa_madura.util.NavHelper

class InputEvalTb : Activity() {

    lateinit var binding : InputEvalTbBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = InputEvalTbBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        NavHelper.setup(
            activity = this,
            menuInputKosakata = binding.menuInputKosakata,
            menuEval = binding.menuEval,
            menuList = binding.menuList,
            menuSoal = binding.menuSoal,
            currentClass = InputEvalTb::class.java
        )

        binding.inputterjemahan.setOnClickListener {
            val intent = Intent(this, InputEvalTerjemahan::class.java)
            startActivity(intent)
        }

        binding.inputpelafalan.setOnClickListener {
            val intent = Intent(this, InputEvalPelafalan::class.java)
            startActivity(intent)
        }
    }
}