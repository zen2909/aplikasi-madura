package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.FirebaseDatabase
import com.zen.e_learning_bahasa_madura.databinding.EditEvalPelafalanBinding
import com.zen.e_learning_bahasa_madura.model.EvalPelafalan
import com.google.firebase.database.DataSnapshot

class EditEvalPelafalan : Activity() {

    private lateinit var binding: EditEvalPelafalanBinding
    private val db = FirebaseDatabase.getInstance().reference
    private var id: String = ""
    private var mediaPlayer: MediaPlayer? = null
    private var audioUrl: String? = null
    private var selectedAudioUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditEvalPelafalanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        id = intent.getStringExtra("id_evalpilgan") ?: return

        loadData()
        hurufkhusus()
        setupAutoComplete()

        binding.soal.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        binding.jawaban.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.btnSimpan.setOnClickListener { updateData() }
        binding.btnAudio.setOnClickListener { previewAudio() }
    }

    private fun loadData() {
        db.child("evaluasi_pelafalan").child(id).get()
            .addOnSuccessListener {
                val data = it.getValue(EvalPelafalan::class.java)
                if (data != null) {
                    binding.soal.setText(data.soal)
                    audioUrl = data.jawaban
                    fetchTextJawaban(audioUrl ?: "")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchTextJawaban(audio: String) {
        val tasks = listOf(
            db.child("Madura_dasar").get(),
            db.child("Madura_menengah").get(),
            db.child("Madura_tinggi").get()
        )

        Tasks.whenAllSuccess<DataSnapshot>(tasks)
            .addOnSuccessListener { results ->
                results.forEach { snap ->
                    snap.children.forEach {
                        val kata = it.child("kosakata").getValue(String::class.java)
                        val audioKata = it.child("audio_pelafalan").getValue(String::class.java)
                        if (audioKata == audio) {
                            binding.jawaban.setText(kata)
                            return@addOnSuccessListener
                        }
                    }
                }
            }
    }

    private fun previewAudio() {
        val jawaban = binding.jawaban.text.toString().trim()
        if (jawaban.isEmpty()) {
            Toast.makeText(this, "Masukkan jawaban dulu", Toast.LENGTH_SHORT).show()
            return
        }

        fetchAudioUrl(jawaban) { audioUrl ->
            if (audioUrl != null) {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    setOnPreparedListener { start() }
                    setOnCompletionListener { release() }
                    setOnErrorListener { _, _, _ ->
                        Toast.makeText(this@EditEvalPelafalan, "Gagal memutar audio", Toast.LENGTH_SHORT).show()
                        true
                    }
                    prepareAsync()
                }
            } else {
                Toast.makeText(this, "Audio tidak ditemukan untuk: $jawaban", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchAudioUrl(kosakata: String, callback: (String?) -> Unit) {
        val db = FirebaseDatabase.getInstance().reference
        val paths = listOf("Madura_dasar", "Madura_menengah", "Madura_tinggi")
        val audioList = mutableListOf<String>()

        val tasks = paths.map { path ->
            db.child(path).get()
        }

        Tasks.whenAllSuccess<DataSnapshot>(tasks)
            .addOnSuccessListener { snapshots ->
                snapshots.forEach { snapshot ->
                    for (child in snapshot.children) {
                        val kata = child.child("kosakata").getValue(String::class.java)?.lowercase()?.trim()
                        val audio = child.child("audio_pelafalan").getValue(String::class.java)
                        if (kata == kosakata.lowercase().trim() && !audio.isNullOrBlank()) {
                            audioList.add(audio)
                        }
                    }
                }
                callback(audioList.firstOrNull())
            }
            .addOnFailureListener {
                callback(null)
            }
    }




    private fun hurufkhusus() {
        inserthuruf(binding.hurufasoal, binding.soal, "â")
        inserthuruf(binding.hurufesoal, binding.soal, "è")
        inserthuruf(binding.hurufajwb1, binding.jawaban, "â")
        inserthuruf(binding.hurufejwb1, binding.jawaban, "è")
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

    private fun updateData() {
        val soal = binding.soal.text.toString().trim()
        val input = binding.jawaban.text.toString().trim()

        if (soal.isEmpty() || input.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseDatabase.getInstance().reference
        val paths = listOf("Madura_dasar", "Madura_menengah", "Madura_tinggi")
        val tasks = paths.map { db.child(it).get() }

        Tasks.whenAllSuccess<DataSnapshot>(tasks)
            .addOnSuccessListener { snapshots ->
                val lowerInput = input.lowercase()
                val audio = snapshots
                    .flatMap { it.children }
                    .firstOrNull { snap ->
                        val kosa = snap.child("kosakata").getValue(String::class.java)?.lowercase()
                        val audioVal = snap.child("audio_pelafalan").getValue(String::class.java)
                        kosa == lowerInput && !audioVal.isNullOrBlank()
                    }?.child("audio_pelafalan")?.getValue(String::class.java)

                if (audio.isNullOrBlank()) {
                    Toast.makeText(this, "Audio tidak ditemukan untuk $input", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val updatedData = EvalPelafalan(
                    id_evalpilgan = id,
                    soal = soal,
                    kategori = "Pelafalan",
                    jawaban = audio
                )

                db.child("evaluasi_pelafalan").child(id).setValue(updatedData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengakses Firebase", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupAutoComplete() {
        val allKosakata = mutableSetOf<String>()
        val db = FirebaseDatabase.getInstance().reference

        val dasarTask = db.child("Madura_dasar").get()
        val menengahTask = db.child("Madura_menengah").get()
        val tinggiTask = db.child("Madura_tinggi").get()

        Tasks.whenAllSuccess<DataSnapshot>(dasarTask, menengahTask, tinggiTask)
            .addOnSuccessListener { results ->
                results.forEach { snapshot ->
                    snapshot.children.mapNotNullTo(allKosakata) {
                        it.child("kosakata").getValue(String::class.java)
                    }
                }

                val adapter = HighlightAdapter(this, allKosakata.toList())
                binding.jawaban.setAdapter(adapter)
                binding.jawaban.threshold = 1

                binding.jawaban.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        adapter.currentKeyword = s?.toString() ?: ""
                        selectedAudioUrl = null
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
    }





    class HighlightAdapter(
        context: Context,
        private val originalList: List<String>
    ) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, ArrayList(originalList)) {

        var currentKeyword: String = ""

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val textView = view.findViewById<TextView>(android.R.id.text1)
            val itemText = getItem(position) ?: ""

            val index = itemText.lowercase().indexOf(currentKeyword.lowercase())
            if (index >= 0 && currentKeyword.isNotBlank()) {
                val spannable = SpannableString(itemText)
                spannable.setSpan(
                    ForegroundColorSpan(Color.BLUE),
                    index, index + currentKeyword.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                textView.text = spannable
            } else {
                textView.text = itemText
            }

            return view
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val keyword = constraint?.toString()?.lowercase()?.trim() ?: ""
                    val filtered = if (keyword.isEmpty()) {
                        originalList
                    } else {
                        originalList.filter { it.lowercase().contains(keyword) }
                    }

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
}
