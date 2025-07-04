package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.InputEvalPelafalanBinding
import com.zen.e_learning_bahasa_madura.model.EvalPelafalan
import com.zen.e_learning_bahasa_madura.model.Evaluasi
import com.zen.e_learning_bahasa_madura.model.KoleksiSoal
import com.zen.e_learning_bahasa_madura.util.NavHelper

class InputEvalPelafalan : Activity() {

    private lateinit var binding: InputEvalPelafalanBinding
    private lateinit var db: DatabaseReference
    private var selectedAudioUrl: String? = null
    private var mediaPlayer: MediaPlayer? = null
    @Volatile private var selectedIdKoleksi: String? = null
    private val listKoleksi: MutableList<KoleksiSoal> = mutableListOf()
    private lateinit var spinnerAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = InputEvalPelafalanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseDatabase.getInstance().reference

        // Setup Navigasi Menu
        NavHelper.setup(
            activity = this,
            menuInputKosakata = binding.menuInputKosakata,
            menuEval = binding.menuEval,
            menuList = binding.menuList,
            menuSoal = binding.menuSoal,
            currentClass = InputEvalPelafalan::class.java
        )

        // Navigasi antar jenis input
        binding.inputtb.setOnClickListener {
            startActivity(Intent(this, InputEvalTb::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.inputterjemahan.setOnClickListener {
            startActivity(Intent(this, InputEvalTerjemahan::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Setup Fitur Input
        hurufkhusus()
        setupAutoComplete()
        setupListeners()
        setupSpinnerKoleksi()

        binding.soal.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        binding.jawaban.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        // Tombol
        binding.btnAudio.setOnClickListener { previewAudio() }
        binding.btnTambahKoleksi.setOnClickListener { showDialogTambahKoleksi() }
        binding.btnSimpan.setOnClickListener { insertSoalPelafalan() }
    }

    private fun hurufkhusus() {
        inserthuruf(binding.hurufesoal, binding.soal, "è")
        inserthuruf(binding.hurufasoal, binding.soal, "â")
        inserthuruf(binding.hurufejwb1, binding.jawaban, "è")
        inserthuruf(binding.hurufajwb1, binding.jawaban, "â")
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

    private fun setupSpinnerKoleksi() {
        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerkoleksi.adapter = spinnerAdapter

        db.child("koleksi_soal").orderByChild("kategori").equalTo("Pelafalan")
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
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedIdKoleksi = if (position > 0) listKoleksi.getOrNull(position - 1)?.id_koleksi else null
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showDialogTambahKoleksi() {
        val input = EditText(this).apply {
            hint = "Nama Koleksi Soal"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        AlertDialog.Builder(this)
            .setTitle("Tambah Koleksi Soal")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = input.text.toString().trim()
                if (nama.isNotEmpty()) {
                    val id = db.child("koleksi_soal").push().key ?: return@setPositiveButton
                    val koleksi = KoleksiSoal(id_koleksi = id, nama = nama, kategori = "Pelafalan", jumlah_soal = 0)
                    db.child("koleksi_soal").child(id).setValue(koleksi)
                } else {
                    Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupAutoComplete() {
        val allKosakata = mutableListOf<String>()
        val dasar = db.child("Madura_dasar").get()
        val menengah = db.child("Madura_menengah").get()
        val tinggi = db.child("Madura_tinggi").get()

        Tasks.whenAllSuccess<DataSnapshot>(dasar, menengah, tinggi).addOnSuccessListener { snapshots ->
            snapshots.forEach { snapshot ->
                snapshot.children.mapNotNullTo(allKosakata) {
                    it.child("kosakata").getValue(String::class.java)
                }
            }

            val adapter = HighlightAdapter(this, allKosakata.distinct())
            binding.jawaban.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    adapter.currentKeyword = s?.toString() ?: ""
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
            })
            binding.jawaban.setAdapter(adapter)
            binding.jawaban.threshold = 1
        }
    }

    class HighlightAdapter(context: Context, private val originalList: List<String>) :
        ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, ArrayList(originalList)) {

        var currentKeyword: String = ""

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val textView = view.findViewById<TextView>(android.R.id.text1)
            val itemText = getItem(position) ?: ""

            val index = itemText.lowercase().indexOf(currentKeyword.lowercase())
            textView.text = if (index >= 0 && currentKeyword.isNotBlank()) {
                SpannableString(itemText).apply {
                    setSpan(ForegroundColorSpan(Color.BLUE), index, index + currentKeyword.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else itemText

            return view
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val keyword = constraint?.toString()?.lowercase()?.trim() ?: ""
                    val filtered = if (keyword.isEmpty()) originalList else originalList.filter { it.lowercase().contains(keyword) }
                    return FilterResults().apply {
                        values = filtered
                        count = filtered.size
                    }
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                    clear()
                    addAll(results.values as List<String>)
                    notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.jawaban.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                selectedAudioUrl = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun previewAudio() {
        val jawaban = binding.jawaban.text.toString().trim()
        if (jawaban.isEmpty()) {
            Toast.makeText(this, "Masukkan jawaban dulu", Toast.LENGTH_SHORT).show()
            return
        }

        fetchAudioUrl(jawaban) { audioUrl ->
            if (audioUrl != null) {
                val view = layoutInflater.inflate(R.layout.dialog_audio, null)
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Memutar Audio")
                    .setView(view)
                    .setCancelable(false)
                    .create()
                dialog.show()

                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    setOnPreparedListener { start() }
                    setOnCompletionListener {
                        dialog.dismiss()
                        release()
                    }
                    setOnErrorListener { _, _, _ ->
                        dialog.dismiss()
                        Toast.makeText(this@InputEvalPelafalan, "Gagal memutar audio", Toast.LENGTH_SHORT).show()
                        true
                    }
                    prepareAsync()
                }
            } else {
                Toast.makeText(this, "Audio tidak ditemukan untuk: $jawaban", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchAudioUrl(jawaban: String, callback: (String?) -> Unit) {
        val keyword = jawaban.trim().lowercase()
        val paths = listOf("Madura_dasar", "Madura_menengah", "Madura_tinggi")
        val tasks = paths.map { db.child(it).get() }

        Tasks.whenAllSuccess<DataSnapshot>(tasks).addOnSuccessListener { snapshots ->
            val audio = snapshots.flatMap { it.children }.firstNotNullOfOrNull { data ->
                val kosakata = data.child("kosakata").getValue(String::class.java)?.lowercase()
                val audioUrl = data.child("audio_pelafalan").getValue(String::class.java)
                if (kosakata == keyword) audioUrl else null
            }
            callback(audio)
        }.addOnFailureListener { callback(null) }
    }

    private fun insertSoalPelafalan() {
        val soal = binding.soal.text.toString().trim()
        val inputJawaban = binding.jawaban.text.toString().trim()
        val idKoleksi = selectedIdKoleksi

        if (idKoleksi.isNullOrBlank() || binding.spinnerkoleksi.selectedItemPosition == 0) {
            Toast.makeText(this, "Silakan pilih koleksi soal terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        if (soal.isEmpty() || inputJawaban.isEmpty()) {
            Toast.makeText(this, "Soal dan jawaban tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        fetchAudioUrl(inputJawaban) { audio ->
            if (audio.isNullOrBlank()) {
                Toast.makeText(this, "Audio tidak ditemukan untuk: $inputJawaban", Toast.LENGTH_SHORT).show()
                return@fetchAudioUrl
            }

            val pelafalanRef = db.child("evaluasi_pelafalan")
            val evaluasiRef = db.child("evaluasi")

            val idPelafalan = pelafalanRef.push().key ?: return@fetchAudioUrl
            val idEvaluasi = evaluasiRef.push().key ?: return@fetchAudioUrl

            val soalData = EvalPelafalan(idPelafalan, soal, audio)
            val evaluasiData = Evaluasi(idEvaluasi, idKoleksi, null, idPelafalan)

            pelafalanRef.child(idPelafalan).setValue(soalData).addOnSuccessListener {
                evaluasiRef.child(idEvaluasi).setValue(evaluasiData).addOnSuccessListener {
                    db.child("koleksi_soal").child(idKoleksi).child("jumlah_soal").get().addOnSuccessListener {
                        val jumlahSaatIni = it.getValue(Int::class.java) ?: 0
                        db.child("koleksi_soal").child(idKoleksi).child("jumlah_soal").setValue(jumlahSaatIni + 1)
                    }
                    Toast.makeText(this, "Soal pelafalan berhasil disimpan", Toast.LENGTH_SHORT).show()
                    clearForm()
                }
            }
        }
    }

    private fun clearForm() {
        binding.soal.text.clear()
        binding.jawaban.text.clear()
        binding.spinnerkoleksi.setSelection(0)
        selectedAudioUrl = null
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}
