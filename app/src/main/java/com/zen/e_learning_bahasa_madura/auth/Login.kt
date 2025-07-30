package com.zen.e_learning_bahasa_madura.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.zen.e_learning_bahasa_madura.databinding.LoginAdminBinding
import com.zen.e_learning_bahasa_madura.view.admin.InputKosakata
import com.zen.e_learning_bahasa_madura.view.user.HomePage

class Login : Activity(){

    lateinit var binding : LoginAdminBinding
    lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {

        binding = LoginAdminBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.linkregister.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        binding.backhome.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        binding.btnlogin.setOnClickListener {
            val email = binding.emaillogin.text.toString()
            val password = binding.passwordlogin.text.toString()

            if (email.isEmpty()){
                binding.emaillogin.error = "Email Harus Diisi"
                binding.emaillogin.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()){
                binding.passwordlogin.error = "password Harus Diisi"
                binding.emaillogin.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 8){
                binding.passwordlogin.error = "password minimal 8 karakter"
                binding.emaillogin.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty() && password.isEmpty()){
                binding.emaillogin.error = "Email Harus Diisi"
                binding.passwordlogin.error = "password Harus Diisi"
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                binding.emaillogin.error = "Email Tidak Valid"
                binding.emaillogin.requestFocus()
                return@setOnClickListener
            }

            LoginAdmin(email, password)
        }
    }

    private fun LoginAdmin (email:String, password:String){
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this){
                if (it.isSuccessful){
                    Toast.makeText(this, "Selamat Datang $email", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, InputKosakata::class.java)
                    startActivity(intent)
                }else{
                    Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}