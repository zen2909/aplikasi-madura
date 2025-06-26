package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.InputEvalTerjemahanBinding
import com.zen.e_learning_bahasa_madura.util.NavHelper

class InputEvalTerjemahan : Activity() {

    lateinit var binding: InputEvalTerjemahanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = InputEvalTerjemahanBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        NavHelper.setup(
            activity = this,
            menuInputKosakata = binding.menuInputKosakata,
            menuEval = binding.menuEval,
            menuList = binding.menuList,
            menuSoal = binding.menuSoal,
            currentClass = InputEvalTerjemahan::class.java
        )

        binding.inputtb.setOnClickListener {
            val intent = Intent(this, InputEvalTb::class.java)
            startActivity(intent)
        }

        binding.inputpelafalan.setOnClickListener {
            val intent = Intent(this, InputEvalPelafalan::class.java)
            startActivity(intent)
        }

    }
}