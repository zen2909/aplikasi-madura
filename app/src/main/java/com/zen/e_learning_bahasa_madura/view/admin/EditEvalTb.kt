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
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.databinding.EditEvalTbBinding
import com.zen.e_learning_bahasa_madura.model.EvalPilgan

class EditEvalTb : Activity() {

    private lateinit var binding: EditEvalTbBinding
    private val db = FirebaseDatabase.getInstance().reference

    private var id = ""
    private var idKoleksi: String = ""
    private var daftarSoal: List<EvalPilgan> = listOf()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditEvalTbBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idKoleksi = intent.getStringExtra("id_koleksi") ?: ""

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

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                tampilkanSoal(currentIndex)
            }
        }

        binding.btnNext.setOnClickListener {
            if (currentIndex < daftarSoal.lastIndex) {
                currentIndex++
                tampilkanSoal(currentIndex)
            }
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
        if (idKoleksi.isEmpty()) return

        db.child("evaluasi_pilgan").orderByChild("id_koleksi").equalTo(idKoleksi)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    daftarSoal = snapshot.children.mapNotNull { it.getValue(EvalPilgan::class.java) }
                    if (daftarSoal.isNotEmpty()) {
                        currentIndex = 0
                        tampilkanSoal(currentIndex)
                    } else {
                        Toast.makeText(this@EditEvalTb, "Tidak ada soal dalam koleksi ini", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditEvalTb, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun tampilkanSoal(index: Int) {
        if (index in daftarSoal.indices) {
            val data = daftarSoal[index]
            id = data.id_evalpilgan ?: ""

            // Tampilkan nomor soal ke-x
            binding.NomorSoal.text = "Soal ke-${index + 1} dari ${daftarSoal.size}"

            // Tampilkan data
            binding.soal.setText(data.soal)
            binding.jawaban1.setText(data.jwb_1)
            binding.jawaban2.setText(data.jwb_2)
            binding.jawaban3.setText(data.jwb_3)
            binding.jawaban4.setText(data.jwb_4)
            binding.bobot.setText(data.bobot)

            spinnerJawaban(data.jwb_benar)
            spinnerwatcher(data.jwb_benar)

            updateNavigasiButton()
        }
    }

    private fun updateNavigasiButton() {
        binding.btnPrev.isEnabled = currentIndex > 0
        binding.btnNext.isEnabled = currentIndex < daftarSoal.lastIndex
    }

    private fun updateData() {
        if (id.isEmpty()) return

        val selectedPos = binding.jawabanBenar.selectedItemPosition
        if (selectedPos == 0) {
            Toast.makeText(this, "Pilih jawaban benar terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val evaluasi = EvalPilgan(
            id_evalpilgan = id,
            id_koleksi = idKoleksi,
            soal = binding.soal.text.toString(),
            jwb_1 = binding.jawaban1.text.toString(),
            jwb_2 = binding.jawaban2.text.toString(),
            jwb_3 = binding.jawaban3.text.toString(),
            jwb_4 = binding.jawaban4.text.toString(),
            jwb_benar = binding.jawabanBenar.selectedItem.toString(),
            bobot = binding.bobot.text.toString()
        )

        db.child("evaluasi_pilgan").child(id).setValue(evaluasi)
        Toast.makeText(this, "Soal berhasil diperbarui", Toast.LENGTH_SHORT).show()
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

        val selectedIndex = opsi.indexOf(jawabanBenar ?: "")
        binding.jawabanBenar.setSelection(if (selectedIndex > 0) selectedIndex else 0)

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
