package com.zen.e_learning_bahasa_madura.view.admin

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
import android.text.InputType
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.util.NavHelper
import com.google.android.gms.tasks.Tasks

class InputKosakata : Activity() {

    private lateinit var binding : InputKosakataBinding
    private var audioDasarPath: String = ""
    private var audioMenengahPath: String = ""
    private var audioTinggiPath: String = ""
    private var recorder: MediaRecorder? = null
    private var currentRecordingType: String = ""
    private var posisiInputan: EditText? = null
    private var isConverting = false

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


        binding.ktdasar.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.ktmenengah.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.kttinggi.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        binding.ktindonesia.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES


        binding.ktdasar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val converted = convertToCarakan(s.toString())
                binding.cardasar.text = converted
            }

            override fun afterTextChanged(s: Editable?) {}
        })


//        binding.ktdasar.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//
//            override fun afterTextChanged(s: Editable?) {
//                if (isConverting) return
//                isConverting = true
//
//                val input = s?.toString() ?: ""
//                val carakan = carakan(input)
//                binding.cardasar.setText(carakan)
//
//                isConverting = false
//            }
//        })
//
//
//        binding.carmenengah.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//
//            override fun afterTextChanged(s: Editable?) {
//                if (isConverting) return
//                isConverting = true
//
//                val input = s?.toString() ?: ""
//                val carakan = carakan(input)
//                binding.cardasar.setText(carakan)
//
//                isConverting = false
//            }
//        })
//
//        binding.cartinggi.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//
//            override fun afterTextChanged(s: Editable?) {
//                if (isConverting) return
//                isConverting = true
//
//                val input = s?.toString() ?: ""
//                val carakan = carakan(input)
//                binding.cardasar.setText(carakan)
//
//                isConverting = false
//            }
//        })


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
            handleRecording("dasar")
        }

        btnAudioMenengah.setOnClickListener {
            handleRecording("menengah")
        }

        btnAudioTinggi.setOnClickListener {
            handleRecording("tinggi")
        }

        btnSimpan.setOnClickListener {
            insertKosakata()

        }
    }

    private fun hurufkhusus() {
        binding.hurufemdr.setOnClickListener { inserthuruf("é") }
        binding.hurufamdr.setOnClickListener { inserthuruf("â") }
    }

    private fun inserthuruf(char: String) {
        val view = currentFocus
        if (view is EditText) {
            val editText = view
            val start = editText.selectionStart
            val end = editText.selectionEnd

            val isAwal = start == 0

            val insertChar = if (isAwal) char.uppercase() else char.lowercase()

            editText.text.replace(start, end, insertChar)
            editText.setSelection(start + insertChar.length)
        }
    }

    private fun handleRecording(type: String) {
        if (recorder == null) {
            startRecording(type)
            Toast.makeText(this, "Rekam $type dimulai", Toast.LENGTH_SHORT).show()
        } else {
            stopRecording()
            Toast.makeText(this, "Rekam $currentRecordingType disimpan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startRecording(type: String) {
        val file = File(cacheDir, "audio_${type}_${UUID.randomUUID()}.3gp")
        currentRecordingType = type

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        when (type) {
            "dasar" -> audioDasarPath = file.absolutePath
            "menengah" -> audioMenengahPath = file.absolutePath
            "tinggi" -> audioTinggiPath = file.absolutePath
        }

        showRecordingDialog {
            stopRecording()
            Toast.makeText(this, "Rekaman $type selesai", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        currentRecordingType = ""
    }

    private fun showRecordingDialog(onStop: () -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_record, null)
        val btnStop = view.findViewById<Button>(R.id.btnStopRecording)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Merekam audio...")
            .setView(view)
            .setCancelable(false)
            .create()

        btnStop.setOnClickListener {
            onStop()
            dialog.dismiss()
        }

        dialog.show()
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

        val dialog = ProgressDialog(this).apply {
            setTitle("Menyimpan Kosakata")
            setMessage("Mohon tunggu, sedang mengunggah audio dan menyimpan data...")
            setCancelable(false)
            show()
        }

        val db = FirebaseDatabase.getInstance().reference
        val idDasar = db.child("Madura_dasar").push().key!!
        val idMenengah = db.child("Madura_menengah").push().key!!
        val idTinggi = db.child("Madura_tinggi").push().key!!
        val idIndo = db.child("Bahasa_madura").push().key!!

        val storage = FirebaseStorage.getInstance().reference
        val taskDasar = storage.child("audio/audio_dasar_${System.currentTimeMillis()}.3gp")
            .putFile(Uri.fromFile(File(audioDasarPath))).continueWithTask { it.result?.storage?.downloadUrl }

        val taskMenengah = storage.child("audio/audio_menengah_${System.currentTimeMillis()}.3gp")
            .putFile(Uri.fromFile(File(audioMenengahPath))).continueWithTask { it.result?.storage?.downloadUrl }

        val taskTinggi = storage.child("audio/audio_tinggi_${System.currentTimeMillis()}.3gp")
            .putFile(Uri.fromFile(File(audioTinggiPath))).continueWithTask { it.result?.storage?.downloadUrl }

        Tasks.whenAllSuccess<Uri>(taskDasar, taskMenengah, taskTinggi)
            .addOnSuccessListener { urls ->
                val urlDasar = urls[0].toString()
                val urlMenengah = urls[1].toString()
                val urlTinggi = urls[2].toString()

                val dasarMap = MaduraDasar(idDasar, dasar, cardasar, urlDasar)
                val menengahMap = MaduraMenengah(idMenengah, menengah, carmenengah, urlMenengah)
                val tinggiMap = MaduraTinggi(idTinggi, tinggi, cartinggi, urlTinggi)
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
                        Toast.makeText(this, "Gagal simpan: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                dialog.dismiss()
                Toast.makeText(this, "Gagal upload audio: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun uploadAudio(filePath: Uri, folder: String, onUploaded: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileName = "${UUID.randomUUID()}.3gp"
        val audioRef = storageRef.child("$folder/$fileName")

        audioRef.putFile(filePath)
            .addOnSuccessListener {
                audioRef.downloadUrl.addOnSuccessListener { uri ->
                    onUploaded(uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload $folder gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearForm() {
        binding.ktdasar.text.clear()
        binding.ktmenengah.text.clear()
        binding.kttinggi.text.clear()
        binding.ktindonesia.text.clear()
        binding.carmenengah.text.clear()
        binding.cartinggi.text.clear()
    }

    fun convertToCarakan(input: String): String {
        val baseMap = listOf(
            "ng" to "ꦁ", "nga" to "ꦔ",
            "ny" to "ꦚ", "nya" to "ꦚ",
            "th" to "ꦛ", "tha" to "ꦛ",
            "dh" to "ꦝ", "dha" to "ꦝ",
            "jh" to "ꦗ", "jha" to "ꦗ",
            "bh" to "ꦧ", "bha" to "ꦧ",

            "a" to "ꦲ", "â" to "ꦲ",
            "b" to "ꦧ", "ba" to "ꦧ",
            "c" to "ꦕ", "ca" to "ꦕ",
            "d" to "ꦢ", "da" to "ꦢ",
            "g" to "ꦒ", "ga" to "ꦒ",
            "h" to "ꦲ", "ha" to "ꦲ",
            "j" to "ꦗ", "ja" to "ꦗ",
            "k" to "ꦏ", "ka" to "ꦏ",
            "l" to "ꦭ", "la" to "ꦭ",
            "m" to "ꦩ", "ma" to "ꦩ",
            "n" to "ꦤ", "na" to "ꦤ",
            "p" to "ꦥ", "pa" to "ꦥ",
            "r" to "ꦫ", "ra" to "ꦫ",
            "s" to "ꦱ", "sa" to "ꦱ",
            "t" to "ꦠ", "ta" to "ꦠ",
            "w" to "ꦮ", "wa" to "ꦮ",
            "y" to "ꦪ", "ya" to "ꦪ"
        ).sortedByDescending { it.first.length }

        val sandhanganMap = mapOf(
            "i" to "ꦶ", "u" to "ꦸ", "é" to "ꦺ", "e" to "ꦼ", "o" to "ꦺꦴ", "eu" to "ꦼꦴ"
        )

        val pasangan = mapOf(
            "ꦏ" to "꧀ꦏ", "ꦒ" to "꧀ꦒ", "ꦔ" to "꧀ꦔ", "ꦕ" to "꧀ꦕ", "ꦗ" to "꧀ꦗ", "ꦚ" to "꧀ꦚ",
            "ꦛ" to "꧀ꦛ", "ꦝ" to "꧀ꦝ", "ꦠ" to "꧀ꦠ", "ꦡ" to "꧀ꦡ", "ꦢ" to "꧀ꦢ", "ꦣ" to "꧀ꦣ",
            "ꦤ" to "꧀ꦤ", "ꦥ" to "꧀ꦥ", "ꦧ" to "꧀ꦧ", "ꦨ" to "꧀ꦨ", "ꦩ" to "꧀ꦩ", "ꦪ" to "꧀ꦪ",
            "ꦫ" to "꧀ꦫ", "ꦭ" to "꧀ꦭ", "ꦮ" to "꧀ꦮ", "ꦯ" to "꧀ꦯ", "ꦱ" to "꧀ꦱ", "ꦲ" to "꧀ꦲ"
        )

        val result = StringBuilder()
        val words = input.lowercase().split(" ")

        for (word in words) {
            val converted = StringBuilder()
            var i = 0
            while (i < word.length) {
                var matched = false

                // 1. Cek sandhangan dulu
                sandhanganMap.entries.firstOrNull { word.startsWith(it.key, i) }?.let {
                    converted.append(it.value)
                    i += it.key.length
                    matched = true
                }

                // 2. Cek konsonan majemuk dan dasar
                if (!matched) {
                    baseMap.firstOrNull { word.startsWith(it.first, i) }?.let {
                        converted.append(it.second)
                        i += it.first.length
                        matched = true
                    }
                }

                // 3. Cek pasangan konsonan mati di tengah
                if (!matched && i > 0 && i < word.length - 1) {
                    val currChar = word[i].toString()
                    baseMap.firstOrNull { it.first == currChar }?.let {
                        converted.append("꧀").append(it.second)
                        i++
                        matched = true
                    }
                }

                if (!matched) {
                    converted.append(word[i])
                    i++
                }
            }

            // Tambahkan pangkon jika huruf akhir konsonan tanpa pasangan
            val lastChar = converted.lastOrNull()?.toString() ?: ""
            if (lastChar in pasangan.keys) {
                converted.append("꧀")
            }

            result.append(converted).append(" ")
        }

        return result.toString().trim()
    }




}