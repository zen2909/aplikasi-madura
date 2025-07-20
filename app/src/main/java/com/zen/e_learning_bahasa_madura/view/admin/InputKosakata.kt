package com.zen.e_learning_bahasa_madura.view.admin

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.InputKosakataBinding
import android.media.MediaRecorder
import android.net.Uri
import android.widget.Button
import android.widget.Toast
import android.widget.EditText
import android.text.TextWatcher
import android.text.Editable
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.zen.e_learning_bahasa_madura.model.BahasaMadura
import com.zen.e_learning_bahasa_madura.model.MaduraDasar
import com.zen.e_learning_bahasa_madura.model.MaduraMenengah
import com.zen.e_learning_bahasa_madura.model.MaduraTinggi
import java.io.File
import java.util.UUID
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.text.InputType
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.util.NavHelper
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.LinearLayout
import com.zen.e_learning_bahasa_madura.util.AudioRecorderUtil
import com.zen.e_learning_bahasa_madura.util.LajarSpan

class InputKosakata : Activity() {

    private lateinit var binding: InputKosakataBinding
    private var recorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioPath: String = ""

    private var audioUrlDasar: String? = null
    private var audioUrlMenengah: String? = null
    private var audioUrlTinggi: String? = null

    private var recordingDialog: AlertDialog? = null
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null
    private var seconds = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        binding = InputKosakataBinding.inflate(layoutInflater)
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


        binding.ktdasar.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.ktmenengah.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.kttinggi.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.ktindonesia.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES


        binding.cardasar.setOnClickListener { showCarakanKeyboardDialog(binding.cardasar) }
        binding.carmenengah.setOnClickListener { showCarakanKeyboardDialog(binding.carmenengah) }
        binding.cartinggi.setOnClickListener { showCarakanKeyboardDialog(binding.cartinggi) }


        hurufkhusus()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 101)
            }
        }

        val btnSimpan = binding.btnSimpan
        val btnAudioDasar = binding.audiodasar
        val btnAudioMenengah = binding.audiomenengah
        val btnAudioTinggi = binding.audiotinggi

        btnAudioDasar.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    1001
                )
            } else {
                showRecordingDialog("dasar")
            }
        }

        btnAudioMenengah.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    1001
                )
            } else {
                showRecordingDialog("menengah")
            }
        }

        btnAudioTinggi.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    1001
                )
            } else {
                showRecordingDialog("tinggi")
            }
        }

        btnSimpan.setOnClickListener {
            insertKosakata()

        }
    }

    private fun hurufkhusus() {

        inserthuruf(binding.hurufedasar, binding.ktdasar, "è")
        inserthuruf(binding.hurufadasar, binding.ktdasar, "â")

        inserthuruf(binding.hurufemenengah, binding.ktmenengah, "è")
        inserthuruf(binding.hurufamenengah, binding.ktmenengah, "â")

        inserthuruf(binding.hurufetinggi, binding.kttinggi, "è")
        inserthuruf(binding.hurufatinggi, binding.kttinggi, "â")
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

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording(type: String) {
        val outputWavFile = File(cacheDir, "audio_${type}_${UUID.randomUUID()}.wav")
        currentAudioPath = type

        when (type) {
            "dasar" -> audioUrlDasar = outputWavFile.absolutePath
            "menengah" -> audioUrlMenengah = outputWavFile.absolutePath
            "tinggi" -> audioUrlTinggi = outputWavFile.absolutePath
        }

        AudioRecorderUtil.startRecording(outputWavFile)
    }

    private fun stopRecording() {
        AudioRecorderUtil.stopRecording()

        val path = when (currentAudioPath) {
            "dasar" -> audioUrlDasar
            "menengah" -> audioUrlMenengah
            "tinggi" -> audioUrlTinggi
            else -> ""
        }

        val type = currentAudioPath
        currentAudioPath = ""

        if (path.isNullOrEmpty()) {
            Toast.makeText(this, "Path audio tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }


        val dialog = ProgressDialog(this).apply {
            setTitle("Mengunggah Audio")
            setMessage("Sedang mengunggah audio $type...")
            setCancelable(false)
            show()
        }

        val fileUri = Uri.fromFile(File(path))
        val storageRef = FirebaseStorage.getInstance().reference
        val audioRef = storageRef.child("audio/audio_${type}_${System.currentTimeMillis()}.wav")

        audioRef.putFile(fileUri)
            .addOnSuccessListener {
                audioRef.downloadUrl.addOnSuccessListener { uri ->
                    when (type) {
                        "dasar" -> audioUrlDasar = uri.toString()
                        "menengah" -> audioUrlMenengah = uri.toString()
                        "tinggi" -> audioUrlTinggi = uri.toString()
                    }
                    dialog.dismiss()
                    Toast.makeText(this, "Rekaman $type berhasil diunggah", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .addOnFailureListener {
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Gagal mengunggah rekaman $type: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun showRecordingDialog(type: String) {
        val view = layoutInflater.inflate(R.layout.dialog_record, null)
        val timerText = view.findViewById<TextView>(R.id.timer)
        val btnHoldToRecord = view.findViewById<Button>(R.id.btnHoldToRecord)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Rekam Pelafalan")
            .setView(view)
            .setNegativeButton("Tutup") { d, _ -> d.dismiss() }
            .create()

        recordingDialog = dialog
        dialog.show()

        var seconds = 0
        val handler = Handler(Looper.getMainLooper())
        var timerRunnable: Runnable? = null

        // Handle tahan tombol rekam
        btnHoldToRecord.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    try {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                            )
                            == PackageManager.PERMISSION_GRANTED
                        ) {

                            startRecording(type)

                            seconds = 0
                            timerRunnable = object : Runnable {
                                override fun run() {
                                    timerText.text = "Durasi: ${seconds++}s"
                                    handler.postDelayed(this, 1000)
                                }
                            }
                            handler.postDelayed(timerRunnable!!, 1000)
                        } else {
                            Toast.makeText(
                                this,
                                "Izin rekam suara belum diberikan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this,
                            "Tidak dapat merekam suara: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopRecording()
                    timerRunnable?.let { handler.removeCallbacks(it) }
                    timerRunnable = null
                }
            }
            true
        }
    }

    private fun insertKosakata() {
        val dasar = binding.ktdasar.text.toString().trim()
        val menengah = binding.ktmenengah.text.toString().trim()
        val tinggi = binding.kttinggi.text.toString().trim()
        val indo = binding.ktindonesia.text.toString().trim()
        val cardasar = binding.cardasar.text.toString().trim()
        val carmenengah = binding.carmenengah.text.toString().trim()
        val cartinggi = binding.cartinggi.text.toString().trim()

        if (dasar.isEmpty() || menengah.isEmpty() || tinggi.isEmpty() || indo.isEmpty()) {
            Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (audioUrlDasar.isNullOrEmpty() || audioUrlMenengah.isNullOrEmpty() || audioUrlTinggi.isNullOrEmpty()) {
            Toast.makeText(this, "Mohon lengkapi semua rekaman audio", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = ProgressDialog(this).apply {
            setTitle("Menyimpan Kosakata")
            setMessage("Sedang menyimpan data ke database...")
            setCancelable(false)
            show()
        }

        val db = FirebaseDatabase.getInstance().reference
        val idDasar = db.child("Madura_dasar").push().key!!
        val idMenengah = db.child("Madura_menengah").push().key!!
        val idTinggi = db.child("Madura_tinggi").push().key!!
        val idIndo = db.child("Bahasa_madura").push().key!!

        val dasarMap = MaduraDasar(idDasar, dasar, cardasar, audioUrlDasar!!)
        val menengahMap = MaduraMenengah(idMenengah, menengah, carmenengah, audioUrlMenengah!!)
        val tinggiMap = MaduraTinggi(idTinggi, tinggi, cartinggi, audioUrlTinggi!!)
        val bahasaMap = BahasaMadura(idIndo, idDasar, idMenengah, idTinggi, indo)

        db.child("Madura_dasar").child(idDasar).setValue(dasarMap)
        db.child("Madura_menengah").child(idMenengah).setValue(menengahMap)
        db.child("Madura_tinggi").child(idTinggi).setValue(tinggiMap)
        db.child("Bahasa_madura").child(idIndo).setValue(bahasaMap)
            .addOnSuccessListener {
                dialog.dismiss()
                Toast.makeText(this, "Kosakata berhasil disimpan", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener {
                dialog.dismiss()
                Toast.makeText(this, "Gagal menyimpan: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun clearForm() {
        binding.ktdasar.text.clear()
        binding.ktmenengah.text.clear()
        binding.kttinggi.text.clear()
        binding.ktindonesia.text.clear()
    }

    private fun showCarakanKeyboardDialog(targetTextView: TextView) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_carakan_keyboard, null)
        val previewText = dialogView.findViewById<TextView>(R.id.tvPreview)

        val layoutDasar = dialogView.findViewById<LinearLayout>(R.id.layoutDasar)
        val layoutSandhangan = dialogView.findViewById<LinearLayout>(R.id.layoutSandhangan)
        val layoutPasangan = dialogView.findViewById<LinearLayout>(R.id.layoutPasangan)

        val btnTabDasar = dialogView.findViewById<Button>(R.id.btnTabDasar)
        val btnTabSandhangan = dialogView.findViewById<Button>(R.id.btnTabSandhangan)
        val btnTabPasangan = dialogView.findViewById<Button>(R.id.btnTabPasangan)

        val btnBackspace = dialogView.findViewById<Button>(R.id.btnBackspace)
        val btnClear = dialogView.findViewById<Button>(R.id.btnClear)
        val btnSpace = dialogView.findViewById<Button>(R.id.btnSpace)

        layoutDasar.removeAllViews()
        layoutSandhangan.removeAllViews()
        layoutPasangan.removeAllViews()

        createButtonGroup(AKSARA_DASAR, layoutDasar, previewText)
        createButtonGroup(SANDHANGAN, layoutSandhangan, previewText)
        createButtonGroup(PASANGAN, layoutPasangan, previewText)

        btnBackspace.setOnClickListener {
            val text = previewText.text.toString()
            if (text.isNotEmpty()) previewText.text = text.dropLast(1)
        }

        btnClear.setOnClickListener {
            previewText.text = ""
        }

        btnSpace.setOnClickListener {
            previewText.append(" ")
        }

        btnTabDasar.setOnClickListener {
            layoutDasar.visibility = View.VISIBLE
            layoutSandhangan.visibility = View.GONE
            layoutPasangan.visibility = View.GONE
        }

        btnTabSandhangan.setOnClickListener {
            layoutDasar.visibility = View.GONE
            layoutSandhangan.visibility = View.VISIBLE
            layoutPasangan.visibility = View.GONE
        }

        btnTabPasangan.setOnClickListener {
            layoutDasar.visibility = View.GONE
            layoutSandhangan.visibility = View.GONE
            layoutPasangan.visibility = View.VISIBLE
        }

        val btnSimpan = dialogView.findViewById<Button>(R.id.btnSimpan)
        val btnBatal = dialogView.findViewById<Button>(R.id.btnBatal)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSimpan.setOnClickListener {
            targetTextView.text = previewText.text
            alertDialog.dismiss()
        }

        btnBatal.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

    }

    private fun createButtonGroup(
        characters: Array<String>,
        container: LinearLayout,
        previewText: TextView
    ) {
        val labelMap = mapOf(
            // AKSARA_DASAR
            "ꦲ" to "A", "ꦤ" to "Na", "ꦕ" to "Ca", "ꦫ" to "Ra", "ꦏ" to "Ka",
            "ꦢ" to "Da", "ꦠ" to "Ta", "ꦱ" to "Sa", "ꦮ" to "Wa", "ꦭ" to "La",
            "ꦥ" to "Pa", "ꦝ" to "Dha", "ꦗ" to "Ja", "ꦪ" to "Ya", "ꦚ" to "Nya",
            "ꦩ" to "Ma", "ꦒ" to "Ga", "ꦧ" to "Ba", "ꦛ" to "Tha", "ꦔ" to "Nga",

            // AKSARA_SWARA
            "ꦄ" to "A", "ꦆ" to "I", "ꦈ" to "U", "ꦌ" to "E", "ꦎ" to "O",

            // SANDHANGAN
            "ꦶ" to "Cethak", "ꦸ" to "Soko", "ꦺ" to "Taleng", "ꦼ" to "Petpet", "ꦴ" to "Longo",
            "ꦺꦴ" to "Lenge Longo", "ꦁ" to "Cekcek", "ꦃ" to "Bisat", "꧀" to "Pangkon", "/" to "Lajar", "ꦽ" to "Pengkal",

            // PASANGAN
            "꧀ꦏ" to "Ka", "꧀ꦤ" to "Na", "꧀ꦕ" to "Ca", "꧀ꦫ" to "Ra",
            "꧀ꦢ" to "Da", "꧀ꦠ" to "Ta", "꧀ꦱ" to "Sa", "꧀ꦮ" to "Wa",
            "꧀ꦭ" to "La", "꧀ꦥ" to "Pa", "꧀ꦝ" to "Dha", "꧀ꦗ" to "Ja",
            "꧀ꦪ" to "Ya", "꧀ꦚ" to "Nya", "꧀ꦩ" to "Ma", "꧀ꦒ" to "Ga",
            "꧀ꦧ" to "Ba", "꧀ꦛ" to "Tha", "꧀ꦔ" to "Nga", "꧀ꦲ" to "Ha"
        )

        val rowSize = 5
        var currentRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        characters.forEachIndexed { index, char ->
            val verticalLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(4, 4, 4, 4)
                }
            }

            val label = TextView(this).apply {
                text = labelMap[char] ?: ""
                textSize = 12f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            }

            val btn = Button(this).apply {
                text = char
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                setOnClickListener {
                    val currentText = previewText.text.toString()
                    val lastChar = currentText.lastOrNull()?.toString()

                    if (char == "/") {
                        val builder = SpannableStringBuilder(previewText.text)
                        val start = builder.length
                        builder.append("/") // dummy
                        builder.setSpan(LajarSpan(), start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        previewText.text = builder
                    } else {
                        previewText.append(char)
                    }
                }
            }

            verticalLayout.addView(label)
            verticalLayout.addView(btn)
            currentRow.addView(verticalLayout)

            if ((index + 1) % rowSize == 0 || index == characters.lastIndex) {
                container.addView(currentRow)
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
            }
        }
    }

    companion object {
        // Aksara dasar (Carakan)
        val AKSARA_DASAR = arrayOf(
            "ꦲ", "ꦤ", "ꦕ", "ꦫ", "ꦏ",
            "ꦢ", "ꦠ", "ꦱ", "ꦮ", "ꦭ",
            "ꦥ", "ꦝ", "ꦗ", "ꦪ", "ꦚ",
            "ꦩ", "ꦒ", "ꦧ", "ꦛ", "ꦔ"
        )

        // Sandhangan (modifikasi vokal)
        val SANDHANGAN = arrayOf(
            "ꦺ",   // lenge
            "ꦼ",   // petpet
            "ꦶ",   // cethak
            "ꦸ",   // soko
            "ꦺꦴ", // lenge longo
            "ꦴ",   // longo
            "ꦁ",   // cekcek
            "ꦃ",   // wignyan
            "꧀",   // pangkon
            "/",   // layar
            "ꦽ"    // pengkal
        )

        // Pasangan (dhempengan/gantungan)
        val PASANGAN = arrayOf(
            "꧀ꦲ", "꧀ꦤ", "꧀ꦕ", "꧀ꦫ", "꧀ꦏ",
            "꧀ꦢ", "꧀ꦠ", "꧀ꦱ", "꧀ꦮ", "꧀ꦭ",
            "꧀ꦥ", "꧀ꦝ", "꧀ꦗ", "꧀ꦪ", "꧀ꦚ",
            "꧀ꦩ", "꧀ꦒ", "꧀ꦧ", "꧀ꦛ", "꧀ꦔ"
        )

    }
}

//    fun convertToCarakan(input: String): String {
//        val baseMap = listOf(
//            "ng" to "ꦁ", "nga" to "ꦔ",
//            "ny" to "ꦚ", "nya" to "ꦚ",
//            "th" to "ꦛ", "tha" to "ꦛ",
//            "dh" to "ꦝ", "dha" to "ꦝ",
//            "jh" to "ꦗ", "jha" to "ꦗ",
//            "bh" to "ꦧ", "bha" to "ꦧ",
//
//            "a" to "ꦲ", "â" to "ꦲ",
//            "b" to "ꦧ", "ba" to "ꦧ",
//            "c" to "ꦕ", "ca" to "ꦕ",
//            "d" to "ꦢ", "da" to "ꦢ",
//            "g" to "ꦒ", "ga" to "ꦒ",
//            "h" to "ꦲ", "ha" to "ꦲ",
//            "j" to "ꦗ", "ja" to "ꦗ",
//            "k" to "ꦏ", "ka" to "ꦏ",
//            "l" to "ꦭ", "la" to "ꦭ",
//            "m" to "ꦩ", "ma" to "ꦩ",
//            "n" to "ꦤ", "na" to "ꦤ",
//            "p" to "ꦥ", "pa" to "ꦥ",
//            "r" to "ꦫ", "ra" to "ꦫ",
//            "s" to "ꦱ", "sa" to "ꦱ",
//            "t" to "ꦠ", "ta" to "ꦠ",
//            "w" to "ꦮ", "wa" to "ꦮ",
//            "y" to "ꦪ", "ya" to "ꦪ"
//        ).sortedByDescending { it.first.length }
//
//        val sandhanganMap = mapOf(
//            "i" to "ꦶ", "u" to "ꦸ", "é" to "ꦺ", "e" to "ꦼ", "o" to "ꦺꦴ", "eu" to "ꦼꦴ"
//        )
//
//        val pasangan = mapOf(
//            "ꦏ" to "꧀ꦏ", "ꦒ" to "꧀ꦒ", "ꦔ" to "꧀ꦔ", "ꦕ" to "꧀ꦕ", "ꦗ" to "꧀ꦗ", "ꦚ" to "꧀ꦚ",
//            "ꦛ" to "꧀ꦛ", "ꦝ" to "꧀ꦝ", "ꦠ" to "꧀ꦠ", "ꦡ" to "꧀ꦡ", "ꦢ" to "꧀ꦢ", "ꦣ" to "꧀ꦣ",
//            "ꦤ" to "꧀ꦤ", "ꦥ" to "꧀ꦥ", "ꦧ" to "꧀ꦧ", "ꦨ" to "꧀ꦨ", "ꦩ" to "꧀ꦩ", "ꦪ" to "꧀ꦪ",
//            "ꦫ" to "꧀ꦫ", "ꦭ" to "꧀ꦭ", "ꦮ" to "꧀ꦮ", "ꦯ" to "꧀ꦯ", "ꦱ" to "꧀ꦱ", "ꦲ" to "꧀ꦲ"
//        )
//
//        val result = StringBuilder()
//        val words = input.lowercase().split(" ")
//
//        for (word in words) {
//            val converted = StringBuilder()
//            var i = 0
//            while (i < word.length) {
//                var matched = false
//
//                // 1. Cek sandhangan dulu
//                sandhanganMap.entries.firstOrNull { word.startsWith(it.key, i) }?.let {
//                    converted.append(it.value)
//                    i += it.key.length
//                    matched = true
//                }
//
//                // 2. Cek konsonan majemuk dan dasar
//                if (!matched) {
//                    baseMap.firstOrNull { word.startsWith(it.first, i) }?.let {
//                        converted.append(it.second)
//                        i += it.first.length
//                        matched = true
//                    }
//                }
//
//                // 3. Cek pasangan konsonan mati di tengah
//                if (!matched && i > 0 && i < word.length - 1) {
//                    val currChar = word[i].toString()
//                    baseMap.firstOrNull { it.first == currChar }?.let {
//                        converted.append("꧀").append(it.second)
//                        i++
//                        matched = true
//                    }
//                }
//
//                if (!matched) {
//                    converted.append(word[i])
//                    i++
//                }
//            }
//
//            // Tambahkan pangkon jika huruf akhir konsonan tanpa pasangan
//            val lastChar = converted.lastOrNull()?.toString() ?: ""
//            if (lastChar in pasangan.keys) {
//                converted.append("꧀")
//            }
//
//            result.append(converted).append(" ")
//        }
//
//        return result.toString().trim()
//    }