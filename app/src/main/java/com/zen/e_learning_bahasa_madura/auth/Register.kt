package com.zen.e_learning_bahasa_madura.auth

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.zen.e_learning_bahasa_madura.databinding.RegisterAdminBinding
import com.zen.e_learning_bahasa_madura.model.Admin

class Register : Activity() {

    lateinit var binding: RegisterAdminBinding
    lateinit var auth: FirebaseAuth
    lateinit var data: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = RegisterAdminBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        data = FirebaseDatabase.getInstance().getReference("Admin")



        binding.btnregister.setOnClickListener {
            val email = binding.emailregister.text.toString()
            val password = binding.passwordregister.text.toString()

            if (email.isEmpty()) {
                binding.emailregister.error = "Email Harus Diisi"
                binding.emailregister.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.passwordregister.error = "password Harus Diisi"
                binding.emailregister.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 8) {
                binding.passwordregister.error = "password minimal 8 karakter"
                binding.emailregister.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty() && password.isEmpty()) {
                binding.emailregister.error = "Email Harus Diisi"
                binding.passwordregister.error = "password Harus Diisi"
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailregister.error = "Email Tidak Valid"
                binding.emailregister.requestFocus()
                return@setOnClickListener
            }

            RegisterFirebase(email, password)
        }

        binding.linkadmin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

    }

    private fun RegisterFirebase(email: String, password: String) {
        val progressdialog = ProgressDialog(this)
        progressdialog.setTitle("Registrasi Admin")
        progressdialog.setMessage("Mohon Tunggu")
        progressdialog.setCanceledOnTouchOutside(false)
        progressdialog.show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    saveData(email, password, progressdialog)
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveData(email: String, password: String, progressdialog: ProgressDialog) {

        val id = auth.currentUser?.uid.toString()
        data = FirebaseDatabase.getInstance().reference.child("Admin")
        val admin = HashMap<String, Any>()

        admin["id"] = id
        admin["email"] = email
        admin["password"] = password

        data.child(id).setValue(admin).addOnCompleteListener {
            if (it.isSuccessful) {
                progressdialog.dismiss()
                Toast.makeText(this, "Data berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Login::class.java)
                intent.addFlags(intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                val message = it.exception!!.toString()
                Toast.makeText(this, "Data gagal ditambahkan : $message", Toast.LENGTH_SHORT).show()
                progressdialog.dismiss()
            }
        }
    }
}