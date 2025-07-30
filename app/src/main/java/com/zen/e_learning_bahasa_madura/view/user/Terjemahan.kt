package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.content.Context
import android.graphics.Color
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
import android.widget.Filter.FilterResults
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.databinding.HalTerjemahanBinding
import com.zen.e_learning_bahasa_madura.model.BahasaMadura
import com.zen.e_learning_bahasa_madura.model.MaduraDasar

class Terjemahan : Activity() {

    private lateinit var binding: HalTerjemahanBinding
    private lateinit var dbRef: DatabaseReference
    private var isMaduraToIndo = true

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = HalTerjemahanBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        dbRef = FirebaseDatabase.getInstance().reference

        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnSwapLanguages.setOnClickListener {
            isMaduraToIndo = !isMaduraToIndo
            swapLanguages()
            setupAutoComplete()
        }

        binding.etInputText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.btnTranslate.setOnClickListener {
            val input = binding.etInputText.text.toString().trim()
            if (input.isNotEmpty()) {
                translate(input)
            } else {
                Toast.makeText(this, "Teks tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        setupAutoComplete()
        swapLanguages()
        hurufkhusus()
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

    private fun swapLanguages() {
        binding.tvSourceLang.text = if (isMaduraToIndo) "MADURA" else "INDONESIA"
        binding.tvTargetLang.text = if (isMaduraToIndo) "INDONESIA" else "MADURA"
    }

    private fun translate(input: String) {
        val formattedInput = input.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        if (isMaduraToIndo) translateMaduraToIndo(formattedInput)
        else translateIndoToMadura(formattedInput)
    }

    private fun translateMaduraToIndo(kosakatamadura: String) {
        fun queryLevel(path: String, idField: String, callback: (String?) -> Unit) {
            dbRef.child(path).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (item in snapshot.children) {
                        val kata = item.child("kosakata").value?.toString()?.trim() ?: continue
                        if (kata.equals(kosakatamadura.trim(), ignoreCase = true)) {
                            val id = item.key ?: continue
                            dbRef.child("Bahasa_madura").orderByChild(idField).equalTo(id)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(data: DataSnapshot) {
                                        for (entry in data.children) {
                                            val result = entry.getValue(BahasaMadura::class.java)
                                            if (!result?.kosakata_indonesia.isNullOrEmpty()) {
                                                callback(result.kosakata_indonesia)
                                                return
                                            }
                                        }
                                        callback(null)
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            return
                        }
                    }
                    callback(null)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        queryLevel("Madura_dasar", "id_dasar") { dasar ->
            if (dasar != null) binding.tvOutputText.text = dasar
            else queryLevel("Madura_menengah", "id_menengah") { menengah ->
                if (menengah != null) binding.tvOutputText.text = menengah
                else queryLevel("Madura_tinggi", "id_tinggi") { tinggi ->
                    binding.tvOutputText.text = tinggi ?: "Tidak ditemukan"
                }
            }
        }
    }



    private fun translateIndoToMadura(kosakataindo: String) {
        val targetWord = kosakataindo.trim()

        dbRef.child("Bahasa_madura").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (item in snapshot.children) {
                    val kata = item.child("kosakata_indonesia").value?.toString()?.trim() ?: continue
                    if (kata.equals(targetWord, ignoreCase = true)) {
                        val idDasar = item.child("id_dasar").value?.toString() ?: ""
                        if (idDasar.isNotEmpty()) {
                            dbRef.child("Madura_dasar").child(idDasar)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(child: DataSnapshot) {
                                        val dasar = child.getValue(MaduraDasar::class.java)
                                        binding.tvOutputText.text = dasar?.kosakata ?: "Tidak ditemukan"
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        } else {
                            binding.tvOutputText.text = "Tidak ditemukan"
                        }
                        return
                    }
                }
                binding.tvOutputText.text = "Tidak ditemukan"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }



    private fun setupAutoComplete() {
        val db = FirebaseDatabase.getInstance().reference

        val allKosakata = mutableListOf<String>()
        val tasks = if (isMaduraToIndo) {
            listOf("Madura_dasar", "Madura_menengah", "Madura_tinggi").map { db.child(it).get() }
        } else {
            listOf(db.child("Bahasa_madura").get())
        }

        val field = if (isMaduraToIndo) "kosakata" else "kosakata_indonesia"

        Tasks.whenAllSuccess<DataSnapshot>(tasks)
            .addOnSuccessListener { results ->
                results.forEach { snapshot ->
                    snapshot.children.mapNotNullTo(allKosakata) {
                        it.child(field).getValue(String::class.java)
                    }
                }

                val adapter = HighlightAdapter(this, allKosakata.distinct())
                binding.etInputText.setAdapter(adapter)
                binding.etInputText.threshold = 1
                binding.etInputText.addTextChangedListener(object : TextWatcher {
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        adapter.currentKeyword = s?.toString() ?: ""
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun afterTextChanged(s: Editable?) {}
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
