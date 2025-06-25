package com.zen.e_learning_bahasa_madura.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.zen.e_learning_bahasa_madura.databinding.RegisterAdminBinding
import java.security.MessageDigest

class Register : Activity() {

    private lateinit var binding: RegisterAdminBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = RegisterAdminBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnregister.setOnClickListener {
            val email = binding.emailregister.text.toString().trim()
            val password = binding.passwordregister.text.toString().trim()

            if (email.isEmpty()) {
                binding.emailregister.error = "Email harus diisi"
                binding.emailregister.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailregister.error = "Email tidak valid"
                binding.emailregister.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.passwordregister.error = "Password harus diisi"
                binding.passwordregister.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 8) {
                binding.passwordregister.error = "Password minimal 8 karakter"
                binding.passwordregister.requestFocus()
                return@setOnClickListener
            }

            registerAdmin(email, password)
        }

        binding.linkadmin.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }
    }

    private fun registerAdmin(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    Toast.makeText(this, "Gagal mendapatkan UID", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val hashedPassword = hashPassword(password)
                val admin = mapOf(
                    "id" to uid,
                    "email" to email,
                    "password" to hashedPassword
                )

                FirebaseDatabase.getInstance().reference
                    .child("Admin")
                    .child(uid)
                    .setValue(admin)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Admin berhasil terdaftar", Toast.LENGTH_SHORT).show()
                        Log.d("REGISTER", "Admin data stored for uid: $uid")
                        startActivity(Intent(this, Login::class.java))
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal simpan ke database: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("REGISTER", "Database write failed: ${e.message}")
                    }

            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal registrasi: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("REGISTER", "Auth failed: ${e.message}")
            }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(password.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
