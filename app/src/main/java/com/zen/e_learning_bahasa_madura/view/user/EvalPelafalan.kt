package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.HalEvalPelafalanBinding

class EvalPelafalan : Activity(){

    lateinit var binding : HalEvalPelafalanBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HalEvalPelafalanBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}