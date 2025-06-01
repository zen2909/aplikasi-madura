package com.zen.e_learning_bahasa_madura.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.RegisterAdminBinding

class Register : Activity(){

    lateinit var binding : RegisterAdminBinding
    lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = RegisterAdminBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnregister.setOnClickListener {
            val email = binding.emailregister.text.toString()
            val password = binding.passwordregister.text.toString()

            if (email.isEmpty()){
                binding.emailregister.error = "Email Harus Diisi"
                binding.emailregister.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()){
                binding.passwordregister.error = "password Harus Diisi"
                binding.emailregister.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 8){
                binding.passwordregister.error = "password minimal 8 karakter"
                binding.emailregister.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty() && password.isEmpty()){
                binding.emailregister.error = "Email Harus Diisi"
                binding.passwordregister.error = "password Harus Diisi"
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                binding.emailregister.error = "Email Tidak Valid"
                binding.emailregister.requestFocus()
                return@setOnClickListener
            }

            RegisterFirebase (email, password)
        }

        binding.linkadmin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

    }

    private fun RegisterFirebase (email : String, password : String){
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this){
                if (it.isSuccessful){
                    Toast.makeText(this, "Register Berhasil", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                }else{
                    Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }

    }
}