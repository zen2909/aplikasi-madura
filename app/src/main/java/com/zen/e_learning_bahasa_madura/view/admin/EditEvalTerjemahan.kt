package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.databinding.EditEvalTerjemahanBinding
import com.zen.e_learning_bahasa_madura.model.EvalPilgan
import com.zen.e_learning_bahasa_madura.model.Evaluasi

class EditEvalTerjemahan : Activity() {

    private lateinit var binding: EditEvalTerjemahanBinding
    private val db = FirebaseDatabase.getInstance().reference

    private var idKoleksi: String = ""
    private var daftarSoal: MutableList<Pair<String, EvalPilgan>> = mutableListOf()
    private var currentIndex = 0
    private var currentIdPilgan: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditEvalTerjemahanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idKoleksi = intent.getStringExtra("id_koleksi") ?: ""

        setupInputType()
        hurufKhusus()
        setupButtons()

        if (idKoleksi.isNotEmpty()) {
            loadData()
        } else {
            Toast.makeText(this, "ID Koleksi tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupInputType() {
        val editTexts = listOf(
            binding.soal, binding.jawaban1, binding.jawaban2,
            binding.jawaban3, binding.jawaban4
        )
        editTexts.forEach {
            it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
    }

    private fun setupButtons() {
        binding.btnSimpan.setOnClickListener { updateData() }
        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding.btnPrev.setOnClickListener {
            if (currentIndex > 0) tampilkanSoal(--currentIndex)
        }
        binding.btnNext.setOnClickListener {
            if (currentIndex < daftarSoal.lastIndex) tampilkanSoal(++currentIndex)
        }
    }

    private fun loadData() {
        db.child("evaluasi").orderByChild("id_koleksi").equalTo(idKoleksi)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val evalList = snapshot.children.mapNotNull { it.getValue(Evaluasi::class.java) }
                        .filter { it.id_pilgan != null }

                    if (evalList.isEmpty()) {
                        Toast.makeText(this@EditEvalTerjemahan, "Tidak ada soal dalam koleksi ini", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }

                    val tasks = evalList.map { eval ->
                        val idPilgan = eval.id_pilgan!!
                        db.child("evaluasi_pilgan").child(idPilgan)
                            .get()
                            .continueWith { task ->
                                val soal = task.result?.getValue(EvalPilgan::class.java)
                                if (soal != null) idPilgan to soal else null
                            }
                    }

                    Tasks.whenAllSuccess<Pair<String, EvalPilgan>>(tasks)
                        .addOnSuccessListener { result ->
                            daftarSoal.clear()
                            daftarSoal.addAll(result.filterNotNull())
                            tampilkanSoal(0)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@EditEvalTerjemahan, "Gagal memuat soal", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditEvalTerjemahan, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun tampilkanSoal(index: Int) {
        if (index !in daftarSoal.indices) return

        val (id, soal) = daftarSoal[index]
        currentIdPilgan = id
        currentIndex = index

        binding.NomorSoal.text = "Soal ke-${index + 1} dari ${daftarSoal.size}"
        binding.soal.setText(soal.soal)
        binding.jawaban1.setText(soal.jwb_1)
        binding.jawaban2.setText(soal.jwb_2)
        binding.jawaban3.setText(soal.jwb_3)
        binding.jawaban4.setText(soal.jwb_4)
        binding.bobot.setText(soal.bobot)

        updateSpinnerJawaban(soal.jwb_benar)
        applyJawabanTextWatcher()
        updateNavigasiButton()
    }

    private fun updateSpinnerJawaban(jawabanBenar: String?) {
        val opsi = mutableListOf("Pilih Jawaban Benar")
        listOf(
            binding.jawaban1.text.toString(),
            binding.jawaban2.text.toString(),
            binding.jawaban3.text.toString(),
            binding.jawaban4.text.toString()
        ).filter { it.isNotBlank() }.forEach { opsi.add(it) }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opsi)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.jawabanBenar.adapter = adapter

        val selectedIndex = opsi.indexOf(jawabanBenar ?: "")
        binding.jawabanBenar.setSelection(if (selectedIndex > 0) selectedIndex else 0)
    }

    private fun applyJawabanTextWatcher() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSpinnerJawaban(binding.jawabanBenar.selectedItem?.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        binding.jawaban1.addTextChangedListener(watcher)
        binding.jawaban2.addTextChangedListener(watcher)
        binding.jawaban3.addTextChangedListener(watcher)
        binding.jawaban4.addTextChangedListener(watcher)
    }

    private fun updateNavigasiButton() {
        binding.btnPrev.isEnabled = currentIndex > 0
        binding.btnNext.isEnabled = currentIndex < daftarSoal.lastIndex
    }

    private fun updateData() {
        if (currentIdPilgan.isEmpty()) return

        val selectedPos = binding.jawabanBenar.selectedItemPosition
        if (selectedPos == 0) {
            Toast.makeText(this, "Pilih jawaban benar terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val updated = EvalPilgan(
            id_evalpilgan = currentIdPilgan,
            soal = binding.soal.text.toString(),
            jwb_1 = binding.jawaban1.text.toString(),
            jwb_2 = binding.jawaban2.text.toString(),
            jwb_3 = binding.jawaban3.text.toString(),
            jwb_4 = binding.jawaban4.text.toString(),
            jwb_benar = binding.jawabanBenar.selectedItem.toString(),
            bobot = binding.bobot.text.toString()
        )

        db.child("evaluasi_pilgan").child(currentIdPilgan).setValue(updated)
            .addOnSuccessListener {
                Toast.makeText(this, "Soal berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui soal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hurufKhusus() {
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
}
