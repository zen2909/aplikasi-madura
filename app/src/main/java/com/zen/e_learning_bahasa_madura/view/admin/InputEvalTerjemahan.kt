package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.databinding.InputEvalTerjemahanBinding
import com.zen.e_learning_bahasa_madura.model.EvalPilgan
import com.zen.e_learning_bahasa_madura.model.Evaluasi
import com.zen.e_learning_bahasa_madura.model.KoleksiSoal
import com.zen.e_learning_bahasa_madura.util.NavHelper

class InputEvalTerjemahan : Activity() {

    private lateinit var binding: InputEvalTerjemahanBinding
    private lateinit var db: DatabaseReference
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var listKoleksi: MutableList<KoleksiSoal> = mutableListOf()
    private var selectedIdKoleksi: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = InputEvalTerjemahanBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        db = FirebaseDatabase.getInstance().reference

        NavHelper.setup(
            activity = this,
            menuInputKosakata = binding.menuInputKosakata,
            menuEval = binding.menuEval,
            menuList = binding.menuList,
            menuSoal = binding.menuSoal,
            currentClass = InputEvalTerjemahan::class.java
        )

        binding.soal.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        binding.jawaban1.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        binding.jawaban2.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        binding.jawaban3.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        binding.jawaban4.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.inputtb.setOnClickListener {
            startActivity(Intent(this, InputEvalTb::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.inputpelafalan.setOnClickListener {
            startActivity(Intent(this, InputEvalPelafalan::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        hurufkhusus()
        JawabanListeners()
        setupSpinnerKoleksi()
        binding.btnTambahKoleksi.setOnClickListener { showDialogTambahKoleksi() }
        binding.btnSimpan.setOnClickListener { simpanSoal() }
    }

    private fun setupSpinnerKoleksi() {
        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerkoleksi.adapter = spinnerAdapter

        db.child("koleksi_soal").orderByChild("kategori").equalTo("Terjemahan")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listKoleksi.clear()
                    spinnerAdapter.clear()

                    if (!snapshot.exists()) {
                        spinnerAdapter.add("Belum ada koleksi soal")
                        selectedIdKoleksi = null
                        spinnerAdapter.notifyDataSetChanged()
                        return
                    }

                    spinnerAdapter.add("Pilih Koleksi Soal") // dummy item
                    for (data in snapshot.children) {
                        val item = data.getValue(KoleksiSoal::class.java)
                        item?.let {
                            listKoleksi.add(it)
                            spinnerAdapter.add(it.nama ?: "Tanpa Nama")
                        }
                    }
                    spinnerAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        binding.spinnerkoleksi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedIdKoleksi = if (position > 0) {
                    listKoleksi.getOrNull(position - 1)?.id_koleksi
                } else null
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showDialogTambahKoleksi() {
        val dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null)
        val input = EditText(this)
        input.hint = "Nama Koleksi Soal"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        AlertDialog.Builder(this)
            .setTitle("Tambah Koleksi Soal")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = input.text.toString().trim()
                if (nama.isNotEmpty()) {
                    val id = db.child("koleksi_soal").push().key ?: return@setPositiveButton
                    val koleksi = KoleksiSoal(id_koleksi = id, nama = nama, kategori = "Terjemahan", jumlah_soal = 0)
                    db.child("koleksi_soal").child(id).setValue(koleksi)
                } else {
                    Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun simpanSoal() {
        val soal = binding.soal.text.toString().trim()
        val opsi1 = binding.jawaban1.text.toString().trim()
        val opsi2 = binding.jawaban2.text.toString().trim()
        val opsi3 = binding.jawaban3.text.toString().trim()
        val opsi4 = binding.jawaban4.text.toString().trim()
        val jawabanBenar = binding.jawabanBenar.selectedItem?.toString()?.trim() ?: ""
        val bobot = binding.bobot.text.toString().trim()

        // Tangkap nilai ID koleksi saat ini ke dalam val agar bisa dismart-cast
        val currentIdKoleksi = selectedIdKoleksi
        if (currentIdKoleksi.isNullOrEmpty()) {
            Toast.makeText(this, "Pilih koleksi soal terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        if (soal.isEmpty() || opsi1.isEmpty() || opsi2.isEmpty() || opsi3.isEmpty() ||
            opsi4.isEmpty() || jawabanBenar.isEmpty() || bobot.isEmpty()
        ) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        insertSoal(
            soal = soal,
            opsi1 = opsi1,
            opsi2 = opsi2,
            opsi3 = opsi3,
            opsi4 = opsi4,
            jawabanBenar = jawabanBenar,
            bobot = bobot,
            idKoleksi = currentIdKoleksi
        )
    }

    private fun insertSoal(
        soal: String,
        opsi1: String,
        opsi2: String,
        opsi3: String,
        opsi4: String,
        jawabanBenar: String,
        bobot: String,
        idKoleksi: String
    ) {
        val db = FirebaseDatabase.getInstance().reference
        val pilganRef = db.child("evaluasi_pilgan")
        val evaluasiRef = db.child("evaluasi")

        val idEvalPilgan = pilganRef.push().key ?: return
        val idEvaluasi = evaluasiRef.push().key ?: return

        val soalData = EvalPilgan(
            id_evalpilgan = idEvalPilgan,
            soal = soal,
            jwb_1 = opsi1,
            jwb_2 = opsi2,
            jwb_3 = opsi3,
            jwb_4 = opsi4,
            jwb_benar = jawabanBenar,
            bobot = bobot,
            id_koleksi = idKoleksi
        )

        val evaluasiData = Evaluasi(
            id_evaluasi = idEvaluasi,
            id_pilgan = idEvalPilgan,
            id_pelafalan = null
        )

        pilganRef.child(idEvalPilgan).setValue(soalData)
            .addOnSuccessListener {
                evaluasiRef.child(idEvaluasi).setValue(evaluasiData)
                    .addOnSuccessListener {
                        val koleksiRef = FirebaseDatabase.getInstance().getReference("koleksi_soal").child(selectedIdKoleksi ?: "")
                        koleksiRef.child("jumlah_soal").get().addOnSuccessListener { jumlahSnapshot ->
                            val jumlahSaatIni = jumlahSnapshot.getValue(Int::class.java) ?: 0
                            koleksiRef.child("jumlah_soal").setValue(jumlahSaatIni + 1)
                        }.addOnFailureListener {
                            Toast.makeText(this, "Gagal menambah jumlah soal di koleksi", Toast.LENGTH_SHORT).show()
                        }
                        Toast.makeText(this, "Soal berhasil disimpan", Toast.LENGTH_SHORT).show()
                        clearForm()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal menyimpan ke evaluasi", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menyimpan soal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun spinnerjawaban() {
        val opsi = mutableListOf("Pilih Jawaban Benar")
        if (binding.jawaban1.text.isNotEmpty()) opsi.add(binding.jawaban1.text.toString())
        if (binding.jawaban2.text.isNotEmpty()) opsi.add(binding.jawaban2.text.toString())
        if (binding.jawaban3.text.isNotEmpty()) opsi.add(binding.jawaban3.text.toString())
        if (binding.jawaban4.text.isNotEmpty()) opsi.add(binding.jawaban4.text.toString())
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opsi)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.jawabanBenar.adapter = adapter
    }

    private fun JawabanListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { spinnerjawaban() }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.jawaban1.addTextChangedListener(watcher)
        binding.jawaban2.addTextChangedListener(watcher)
        binding.jawaban3.addTextChangedListener(watcher)
        binding.jawaban4.addTextChangedListener(watcher)
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

    private fun clearForm() {
        binding.soal.text.clear()
        binding.jawaban1.text.clear()
        binding.jawaban2.text.clear()
        binding.jawaban3.text.clear()
        binding.jawaban4.text.clear()
        binding.bobot.text.clear()
    }
}
