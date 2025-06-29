package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.EditEvalTbBinding

class EditEvalPelafalan : Activity() {

    private lateinit var binding: EditEvalTbBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditEvalTbBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}