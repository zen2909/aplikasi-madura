package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.*
import android.text.InputType
import android.view.View
import android.widget.*
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.databinding.InputEvalTerjemahanBinding
import com.zen.e_learning_bahasa_madura.model.EvalPilgan
import com.zen.e_learning_bahasa_madura.model.Evaluasi
import com.zen.e_learning_bahasa_madura.model.KoleksiSoal
import com.zen.e_learning_bahasa_madura.util.NavHelper
import com.zen.e_learning_bahasa_madura.R

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

        initNavigation()
        initInputType()
        initListeners()
        setupSpinnerKoleksi()
    }

    private fun initNavigation() {
        NavHelper.setup(
            activity = this,
            menuInputKosakata = binding.menuInputKosakata,
            menuEval = binding.menuEval,
            menuList = binding.menuList,
            menuSoal = binding.menuSoal,
            currentClass = InputEvalTerjemahan::class.java
        )

        binding.inputtb.setOnClickListener { goTo(InputEvalTb::class.java) }
        binding.inputpelafalan.setOnClickListener { goTo(InputEvalPelafalan::class.java) }
    }

    private fun goTo(clazz: Class<*>) {
        startActivity(Intent(this, clazz))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun initInputType() {
        val editTexts = listOf(
            binding.soal, binding.jawaban1, binding.jawaban2,
            binding.jawaban3, binding.jawaban4
        )
        editTexts.forEach {
            it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
    }

    private fun initListeners() {
        hurufkhusus()
        initJawabanTextWatchers()
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

                    spinnerAdapter.add("Pilih Koleksi Soal")
                    snapshot.children.mapNotNullTo(listKoleksi) {
                        it.getValue(KoleksiSoal::class.java)
                    }
                    listKoleksi.forEach {
                        spinnerAdapter.add(it.nama ?: "Tanpa Nama")
                    }

                    spinnerAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        binding.spinnerkoleksi.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedIdKoleksi = if (position > 0) listKoleksi.getOrNull(position - 1)?.id_koleksi else null
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showDialogTambahKoleksi() {
        val view = layoutInflater.inflate(R.layout.dialog_koleksi, null)
        val btnBatal = view.findViewById<TextView>(R.id.btnBatal)
        val btnSimpan = view.findViewById<TextView>(R.id.btnSimpan)
        val inputan = view.findViewById<EditText>(R.id.inputKoleksi)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnSimpan.setOnClickListener {
            val koleksi = inputan.text.toString().trim()
            if (koleksi.isNotEmpty()) {
                val id = db.child("koleksi_soal").push().key ?: return@setOnClickListener
                val koleksi = KoleksiSoal(id, koleksi, "Terjemahan", 0)
                db.child("koleksi_soal").child(id).setValue(koleksi)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Nama koleksi tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }


    private fun simpanSoal() {
        val soal = binding.soal.text.toString().trim()
        val opsi1 = binding.jawaban1.text.toString().trim()
        val opsi2 = binding.jawaban2.text.toString().trim()
        val opsi3 = binding.jawaban3.text.toString().trim()
        val opsi4 = binding.jawaban4.text.toString().trim()
        val jawabanBenar = binding.jawabanBenar.selectedItem?.toString()?.trim() ?: ""
        val bobot = binding.bobot.text.toString().trim()

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

        val pilganRef = db.child("evaluasi_pilgan")
        val evaluasiRef = db.child("evaluasi")

        val idEvalPilgan = pilganRef.push().key ?: return
        val idEvaluasi = evaluasiRef.push().key ?: return

        val soalData = EvalPilgan(idEvalPilgan, soal, opsi1, opsi2, opsi3, opsi4, jawabanBenar, bobot)
        val evaluasiData = Evaluasi(idEvaluasi, currentIdKoleksi, idEvalPilgan, null)

        pilganRef.child(idEvalPilgan).setValue(soalData)
            .addOnSuccessListener {
                evaluasiRef.child(idEvaluasi).setValue(evaluasiData)
                    .addOnSuccessListener {
                        val koleksiRef = db.child("koleksi_soal").child(currentIdKoleksi)
                        koleksiRef.child("jumlah_soal").get().addOnSuccessListener { jumlahSnapshot ->
                            val jumlahSaatIni = jumlahSnapshot.getValue(Int::class.java) ?: 0
                            koleksiRef.child("jumlah_soal").setValue(jumlahSaatIni + 1)
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

    private fun initJawabanTextWatchers() {
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
        spinnerjawaban()
    }
}
