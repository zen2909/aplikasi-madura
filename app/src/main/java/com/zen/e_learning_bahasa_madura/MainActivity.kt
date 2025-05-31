package com.zen.e_learning_bahasa_madura

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import com.zen.e_learning_bahasa_madura.view.Evaluasi
import com.zen.e_learning_bahasa_madura.view.Pelafalan
import com.zen.e_learning_bahasa_madura.view.Terjemahan
import com.zen.e_learning_bahasa_madura.view.TingkatBahasa

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page) // Sesuaikan dengan nama file XML Anda

        // Mendapatkan referensi tombol terjemahan
        val btnTerjemahan = findViewById<View>(R.id.btnTerjemahan)

        // Menambahkan onClickListener untuk tombol terjemahan
        btnTerjemahan.setOnClickListener {
            // Intent untuk berpindah ke Activity Terjemahan
            val intent = Intent(this, Terjemahan::class.java)
            startActivity(intent)
        }

        val btnTb = findViewById<View>(R.id.btnTb)
        btnTb.setOnClickListener {
            val intent = Intent(this, TingkatBahasa::class.java)
            startActivity(intent)
        }

        val btnPelafalan = findViewById<View>(R.id.btnPelafalan)
        btnPelafalan.setOnClickListener {
            val intent = Intent(this, Pelafalan::class.java)
            startActivity(intent)
        }

        val btnEval = findViewById<View>(R.id.btnEval)
        btnEval.setOnClickListener {
            val intent = Intent(this, Evaluasi::class.java)
            startActivity(intent)
        }

    }
}