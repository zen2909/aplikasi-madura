package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.ListEvaluasiBinding
import com.zen.e_learning_bahasa_madura.util.NavHelper

class SoalEvaluasi : Activity() {

    lateinit var binding: ListEvaluasiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ListEvaluasiBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        NavHelper.setup(
            activity = this,
            menuInputKosakata = binding.menuInputKosakata,
            menuEval = binding.menuEval,
            menuList = binding.menuList,
            menuSoal = binding.menuSoal,
            currentClass = SoalEvaluasi::class.java
        )
    }
}