package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.zen.e_learning_bahasa_madura.databinding.EditEvalTerjemahanBinding
import com.zen.e_learning_bahasa_madura.model.BahasaMadura
import com.zen.e_learning_bahasa_madura.model.MaduraDasar
import com.google.firebase.database.FirebaseDatabase
import com.zen.e_learning_bahasa_madura.model.EvalPilgan

import com.zen.e_learning_bahasa_madura.model.MaduraMenengah
import com.zen.e_learning_bahasa_madura.model.MaduraTinggi

class EditEvalTerjemahan: Activity() {

    private lateinit var binding: EditEvalTerjemahanBinding
    private val db = FirebaseDatabase.getInstance().reference

    private var id = ""
    private var soal = ""
    private var jwb_1 = ""
    private var jwb_2 = ""
    private var jwb_3 = ""
    private var jwb_4 = ""
    private var jwb_benar = ""
    private var bobot = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditEvalTerjemahanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        id = intent.getStringExtra("id_evalpilgan") ?: ""
        soal = intent.getStringExtra("soal") ?: ""
        jwb_1 = intent.getStringExtra("jwb_1") ?: ""
        jwb_2 = intent.getStringExtra("jwb_2") ?: ""
        jwb_3 = intent.getStringExtra("jwb_3") ?: ""
        jwb_4 = intent.getStringExtra("jwb_4") ?: ""
        jwb_benar = intent.getStringExtra("jwb_benar") ?: ""
        bobot = intent.getStringExtra("bobot") ?: ""

        loadData()
        hurufkhusus()

        binding.soal.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.jawaban1.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.jawaban2.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.jawaban3.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.jawaban4.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.btnSimpan.setOnClickListener {
            updateData()
        }

    }

    private fun hurufkhusus() {

        inserthuruf(binding.hurufesoal, binding.soal, "è")
        inserthuruf(binding.hurufasoal, binding.soal, "â")

        inserthuruf(binding.hurufejwb1, binding.jawaban1, "è")
        inserthuruf(binding.hurufajwb1, binding.jawaban1, "â")

        inserthuruf(binding.hurufejwb2, binding.jawaban2, "è")
        inserthuruf(binding.hurufajwb2, binding.jawaban2, "â")

        inserthuruf(binding.hurufejwb3, binding.jawaban3, "è")
        inserthuruf(binding.hurufajwb3, binding.jawaban3, "â")

        inserthuruf(binding.hurufejwb4, binding.jawaban4, "è")
        inserthuruf(binding.hurufajwb4, binding.jawaban4, "â")

    }

    private fun inserthuruf(button: View, target: EditText, huruf: String) {
        button.setOnClickListener {
            val start = target.selectionStart
            val end = target.selectionEnd
            val insertChar = if (start == 0) huruf.uppercase() else huruf.lowercase()
            target.text.replace(start, end, insertChar)
            target.setSelection(start + insertChar.length)
            target.requestFocus()
        }
    }


    private fun loadData() {
        val id = intent.getStringExtra("id_evalpilgan") ?: return

        db.child("evaluasi_pilgan").child(id).get()
            .addOnSuccessListener {
                val data = it.getValue(EvalPilgan::class.java)
                if (data != null) {
                    binding.soal.setText(data.soal)
                    binding.jawaban1.setText(data.jwb_1)
                    binding.jawaban2.setText(data.jwb_2)
                    binding.jawaban3.setText(data.jwb_3)
                    binding.jawaban4.setText(data.jwb_4)
                    binding.bobot.setText(data.bobot)

                    spinnerJawaban(data.jwb_benar)
                    spinnerwatcher(data.jwb_benar)

                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }


    private fun updateData() {
        val id = intent.getStringExtra("id_evalpilgan") ?: return

        val selectedPos = binding.jawabanBenar.selectedItemPosition
        if (selectedPos == 0) {
            Toast.makeText(this, "Pilih jawaban benar terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val evaluasi = EvalPilgan(
            id_evalpilgan = id,
            kategori = "Terjemahan",
            soal = binding.soal.text.toString(),
            jwb_1 = binding.jawaban1.text.toString(),
            jwb_2 = binding.jawaban2.text.toString(),
            jwb_3 = binding.jawaban3.text.toString(),
            jwb_4 = binding.jawaban4.text.toString(),
            jwb_benar = binding.jawabanBenar.selectedItem.toString(),
            bobot = binding.bobot.text.toString()
        )

        db.child("evaluasi_pilgan").child(id).setValue(evaluasi)
        Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }


    private fun spinnerJawaban(jawabanBenar: String?) {
        val opsi = mutableListOf("Pilih Jawaban Benar")

        val j1 = binding.jawaban1.text.toString().trim()
        val j2 = binding.jawaban2.text.toString().trim()
        val j3 = binding.jawaban3.text.toString().trim()
        val j4 = binding.jawaban4.text.toString().trim()

        if (j1.isNotEmpty()) opsi.add(j1)
        if (j2.isNotEmpty()) opsi.add(j2)
        if (j3.isNotEmpty()) opsi.add(j3)
        if (j4.isNotEmpty()) opsi.add(j4)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opsi)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.jawabanBenar.adapter = adapter

        // Set pilihan spinner
        val selectedIndex = opsi.indexOf(jawabanBenar ?: "")
        binding.jawabanBenar.setSelection(if (selectedIndex > 0) selectedIndex else 0)

        // Hint warna abu-abu
        binding.jawabanBenar.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val tv = view as? TextView
                tv?.setTextColor(if (position == 0) 0xFF888888.toInt() else 0xFF000000.toInt())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun spinnerwatcher(existingJawabanBenar: String?) {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                spinnerJawaban(existingJawabanBenar)
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.jawaban1.addTextChangedListener(watcher)
        binding.jawaban2.addTextChangedListener(watcher)
        binding.jawaban3.addTextChangedListener(watcher)
        binding.jawaban4.addTextChangedListener(watcher)
    }



}