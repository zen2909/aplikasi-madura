package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.HalEvalTerjemahanBinding

class EvalTerjemahan : Activity(){

    lateinit var binding : HalEvalTerjemahanBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        binding = HalEvalTerjemahanBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}