package com.zen.e_learning_bahasa_madura.view.user

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
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Filter
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.HalPelafalanBinding

class Pelafalan : Activity() {

    lateinit var binding : HalPelafalanBinding
    private var selectedAudioUrl: String? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = HalPelafalanBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        hurufkhusus()
        setupAutoComplete()
        setupListeners()

        binding.etInputText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.btnAudio.setOnClickListener { previewAudio() }
    }

    private fun hurufkhusus() {

        inserthuruf(binding.hurufeinput, binding.etInputText, "è")
        inserthuruf(binding.hurufainput, binding.etInputText, "â")

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

    private fun setupAutoComplete() {
        val allKosakata = mutableListOf<String>()
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

                val adapter = HighlightAdapter(this, allKosakata.distinct())
                binding.etInputText.addTextChangedListener(object : TextWatcher {
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        adapter.currentKeyword = s?.toString() ?: ""
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun afterTextChanged(s: Editable?) {}
                })

                binding.etInputText.setAdapter(adapter)
                binding.etInputText.threshold = 1
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

    private fun setupListeners() {
        binding.etInputText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                selectedAudioUrl = null
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun previewAudio() {
        val jawaban = binding.etInputText.text.toString().trim()
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
                        Toast.makeText(this@Pelafalan, "Gagal memutar audio", Toast.LENGTH_SHORT).show()
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
        val db = FirebaseDatabase.getInstance().reference
        val keyword = jawaban.trim().lowercase()

        val paths = listOf("Madura_dasar", "Madura_menengah", "Madura_tinggi")
        val tasks = paths.map { db.child(it).get() }

        Tasks.whenAllSuccess<DataSnapshot>(tasks)
            .addOnSuccessListener { snapshots ->
                var matchedAudio: String? = null

                for (snapshot in snapshots) {
                    for (data in snapshot.children) {
                        val kosakata = data.child("kosakata").getValue(String::class.java)
                            ?.trim()?.lowercase()

                        val audio = data.child("audio_pelafalan").getValue(String::class.java)

                        if (kosakata != null && kosakata == keyword) {
                            matchedAudio = audio
                            break
                        }
                    }
                    if (matchedAudio != null) break
                }

                callback(matchedAudio)
            }
            .addOnFailureListener {
                callback(null)
            }
    }


}