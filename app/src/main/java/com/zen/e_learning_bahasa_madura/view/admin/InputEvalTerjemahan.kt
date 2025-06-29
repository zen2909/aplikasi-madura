package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.zen.e_learning_bahasa_madura.databinding.InputEvalTerjemahanBinding
import com.zen.e_learning_bahasa_madura.model.EvalPilgan
import com.zen.e_learning_bahasa_madura.util.NavHelper

class InputEvalTerjemahan : Activity() {

    lateinit var binding: InputEvalTerjemahanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = InputEvalTerjemahanBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        NavHelper.setup(
            activity = this,
            menuInputKosakata = binding.menuInputKosakata,
            menuEval = binding.menuEval,
            menuList = binding.menuList,
            menuSoal = binding.menuSoal,
            currentClass = InputKosakata::class.java
        )

        binding.inputtb.setOnClickListener {
            val intent = Intent(this, InputEvalTb::class.java)
            startActivity(intent)
        }

        binding.inputpelafalan.setOnClickListener {
            val intent = Intent(this, InputEvalPelafalan::class.java)
            startActivity(intent)
        }

        hurufkhusus()
        JawabanListeners()

        binding.soal.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.jawaban1.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.jawaban2.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.jawaban3.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.jawaban4.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES


        binding.btnSimpan.setOnClickListener {
            simpanSoal()
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

    private fun simpanSoal() {
        val kategori = "Terjemahan"
        val soal = binding.soal.text.toString().trim()
        val opsi1 = binding.jawaban1.text.toString().trim()
        val opsi2 = binding.jawaban2.text.toString().trim()
        val opsi3 = binding.jawaban3.text.toString().trim()
        val opsi4 = binding.jawaban4.text.toString().trim()
        val jawabanBenar = binding.jawabanBenar.selectedItem?.toString()?.trim() ?: ""
        val bobot = binding.bobot.text.toString().trim()

        if (soal.isEmpty() || opsi1.isEmpty() || opsi2.isEmpty() || opsi3.isEmpty() ||
            opsi4.isEmpty() || jawabanBenar.isEmpty() || bobot.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        insertSoal(kategori, soal, opsi1, opsi2, opsi3, opsi4, jawabanBenar, bobot)
    }


    private fun insertSoal(
        kategori: String,
        soal: String,
        opsi1: String,
        opsi2: String,
        opsi3: String,
        opsi4: String,
        jawabanBenar: String,
        bobot: String
    ) {
        val dbRef = FirebaseDatabase.getInstance().getReference("evaluasi_pilgan")
        val soalId = dbRef.push().key ?: return

        val soalData = EvalPilgan(
            id_evalpilgan = soalId,
            kategori = kategori,
            soal = soal,
            jwb_1 = opsi1,
            jwb_2 = opsi2,
            jwb_3 = opsi3,
            jwb_4 = opsi4,
            jwb_benar = jawabanBenar,
            bobot = bobot
        )

        dbRef.child(soalId).setValue(soalData)
            .addOnSuccessListener {
                Toast.makeText(this, "Soal berhasil disimpan", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menyimpan soal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun spinnerjawaban() {
        val opsi = mutableListOf("Pilih Jawaban Benar")

        val jawaban1 = binding.jawaban1.text.toString().trim()
        val jawaban2 = binding.jawaban2.text.toString().trim()
        val jawaban3 = binding.jawaban3.text.toString().trim()
        val jawaban4 = binding.jawaban4.text.toString().trim()

        if (jawaban1.isNotEmpty()) opsi.add(jawaban1)
        if (jawaban2.isNotEmpty()) opsi.add(jawaban2)
        if (jawaban3.isNotEmpty()) opsi.add(jawaban3)
        if (jawaban4.isNotEmpty()) opsi.add(jawaban4)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opsi)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.jawabanBenar.adapter = adapter
    }

    private fun JawabanListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                spinnerjawaban()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.jawaban1.addTextChangedListener(watcher)
        binding.jawaban2.addTextChangedListener(watcher)
        binding.jawaban3.addTextChangedListener(watcher)
        binding.jawaban4.addTextChangedListener(watcher)
    }



    private fun clearForm() {
        binding.soal.text.clear()
        binding.jawaban1.text.clear()
        binding.jawaban2.text.clear()
        binding.jawaban3.text.clear()
        binding.jawaban4.text.clear()
        binding.bobot.text.clear()
    }
}