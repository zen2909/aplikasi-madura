package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.databinding.EditEvalPelafalanBinding
import com.zen.e_learning_bahasa_madura.model.EvalPelafalan
import com.zen.e_learning_bahasa_madura.util.BacksoundManager

class EditEvalPelafalan : Activity() {

    private lateinit var binding: EditEvalPelafalanBinding
    private val db = FirebaseDatabase.getInstance().reference
    private var idKoleksi: String = ""
    private var dataList = listOf<DataSnapshot>()
    private var currentIndex = 0
    private var mediaPlayer: MediaPlayer? = null
    private var allKosakata = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditEvalPelafalanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idKoleksi = intent.getStringExtra("id_koleksi") ?: return

        hurufkhusus()
        setupAutoComplete()
        setupNavigation()

        binding.soal.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        binding.jawaban.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.btnSimpan.setOnClickListener { updateData() }
        binding.btnAudio.setOnClickListener { previewAudio() }
        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        loadSoalFromKoleksi()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
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
            val insertChar = if (start == 0) huruf.uppercase() else huruf.lowercase()
            target.text.replace(start, target.selectionEnd, insertChar)
            target.setSelection(start + insertChar.length)
        }
    }

    private fun setupNavigation() {
        binding.btnNext.setOnClickListener {
            if (currentIndex < dataList.lastIndex) {
                currentIndex++
                loadCurrentData()
            }
        }

        binding.btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                loadCurrentData()
            }
        }
    }

    private fun loadSoalFromKoleksi() {
        db.child("evaluasi")
            .orderByChild("id_koleksi")
            .equalTo(idKoleksi)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ids = snapshot.children.mapNotNull { it.child("id_pelafalan").getValue(String::class.java) }
                    if (ids.isEmpty()) {
                        Toast.makeText(this@EditEvalPelafalan, "Tidak ada soal pelafalan dalam koleksi ini", Toast.LENGTH_SHORT).show()
                        return
                    }

                    db.child("evaluasi_pelafalan").get().addOnSuccessListener { allSoal ->
                        val filtered = ids.mapNotNull { id -> allSoal.child(id).takeIf { it.exists() } }
                        if (filtered.isEmpty()) {
                            Toast.makeText(this@EditEvalPelafalan, "Data soal tidak ditemukan", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        dataList = filtered
                        currentIndex = 0
                        loadCurrentData()
                    }.addOnFailureListener {
                        Toast.makeText(this@EditEvalPelafalan, "Gagal memuat soal pelafalan", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadCurrentData() {
        val snap = dataList[currentIndex]
        val data = snap.getValue(EvalPelafalan::class.java) ?: return

        binding.soal.setText(data.soal)
        fetchTextJawaban(data.jawaban.orEmpty())

        binding.NomorSoal.text = "Soal ke-${currentIndex + 1} dari ${dataList.size}"
    }

    private fun fetchTextJawaban(audioUrl: String) {
        val paths = listOf("Madura_dasar", "Madura_menengah", "Madura_tinggi")
        val tasks = paths.map { db.child(it).get() }

        Tasks.whenAllSuccess<DataSnapshot>(tasks).addOnSuccessListener { results ->
            for (snap in results) {
                for (child in snap.children) {
                    val kosakata = child.child("kosakata").getValue(String::class.java)
                    val audio = child.child("audio_pelafalan").getValue(String::class.java)
                    if (audio == audioUrl) {
                        binding.jawaban.setText(kosakata)
                        return@addOnSuccessListener
                    }
                }
            }
        }
    }

    private fun updateData() {
        val soal = binding.soal.text.toString().trim()
        val jawabanText = binding.jawaban.text.toString().trim()

        if (soal.isEmpty() || jawabanText.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        fetchAudioUrl(jawabanText) { audio ->
            if (audio == null) {
                Toast.makeText(this, "Audio tidak ditemukan untuk \"$jawabanText\"", Toast.LENGTH_SHORT).show()
                return@fetchAudioUrl
            }

            val id = dataList[currentIndex].key ?: return@fetchAudioUrl
            val updated = EvalPelafalan(
                id_evalpelafalan = id,
                soal = soal,
                jawaban = audio
            )

            db.child("evaluasi_pelafalan").child(id).setValue(updated)
                .addOnSuccessListener {
                    Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchAudioUrl(kosakata: String, callback: (String?) -> Unit) {
        val paths = listOf("Madura_dasar", "Madura_menengah", "Madura_tinggi")
        val tasks = paths.map { db.child(it).get() }

        Tasks.whenAllSuccess<DataSnapshot>(tasks).addOnSuccessListener { snapshots ->
            for (snapshot in snapshots) {
                for (child in snapshot.children) {
                    val kosa = child.child("kosakata").getValue(String::class.java)?.lowercase()?.trim()
                    val audio = child.child("audio_pelafalan").getValue(String::class.java)
                    if (kosa == kosakata.lowercase().trim() && !audio.isNullOrBlank()) {
                        callback(audio)
                        return@addOnSuccessListener
                    }
                }
            }
            callback(null)
        }.addOnFailureListener {
            callback(null)
        }
    }

    private fun previewAudio() {
        val text = binding.jawaban.text.toString().trim()
        fetchAudioUrl(text) { audioUrl ->
            if (audioUrl != null) {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Memutar Audio")
                    .setMessage("Sedang memutar audio untuk \"$text\".\nHarap tunggu hingga selesai...")
                    .setCancelable(false)
                    .create()

                dialog.show()

                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    setOnPreparedListener {
                        BacksoundManager.pauseImmediately()
                        start() }
                    setOnCompletionListener {
                        BacksoundManager.resume()
                        dialog.dismiss()
                        release()
                    }
                    setOnErrorListener { _, _, _ ->
                        BacksoundManager.resume()
                        Toast.makeText(this@EditEvalPelafalan, "Gagal memutar audio", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        true
                    }
                    prepareAsync()
                }
            } else {
                Toast.makeText(this, "Audio tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAutoComplete() {
        val paths = listOf("Madura_dasar", "Madura_menengah", "Madura_tinggi")
        val tasks = paths.map { db.child(it).get() }

        Tasks.whenAllSuccess<DataSnapshot>(tasks).addOnSuccessListener { results ->
            allKosakata.clear()
            for (snap in results) {
                for (child in snap.children) {
                    val kata = child.child("kosakata").getValue(String::class.java)
                    if (!kata.isNullOrBlank()) allKosakata.add(kata)
                }
            }

            val adapter = HighlightAdapter(this, allKosakata.toList())
            binding.jawaban.setAdapter(adapter)
            binding.jawaban.threshold = 1
            binding.jawaban.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    adapter.currentKeyword = s?.toString() ?: ""
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
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
                    val filtered = if (keyword.isBlank()) {
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
