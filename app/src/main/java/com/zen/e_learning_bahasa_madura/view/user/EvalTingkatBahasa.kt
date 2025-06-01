package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.HalEvalTbBinding

class EvalTingkatBahasa : Activity() {

    lateinit var binding : HalEvalTbBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        binding = HalEvalTbBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}