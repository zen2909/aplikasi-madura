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
import android.view.MotionEvent
import com.zen.e_learning_bahasa_madura.util.AudioRecorderUtil

class InputKosakata : Activity() {

    private lateinit var binding : InputKosakataBinding
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


        binding.ktdasar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val converted = convertToCarakan(s.toString())
                binding.cardasar.text = converted
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ktmenengah.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val converted = convertToCarakan(s.toString())
                binding.carmenengah.text = converted
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.kttinggi.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val converted = convertToCarakan(s.toString())
                binding.cartinggi.text = converted
            }

            override fun afterTextChanged(s: Editable?) {}
        })


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
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
            } else {
                showRecordingDialog("dasar")
            }
        }

        btnAudioMenengah.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
            } else {
                showRecordingDialog("menengah")
            }
        }

        btnAudioTinggi.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
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
                    Toast.makeText(this, "Rekaman $type berhasil diunggah", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                dialog.dismiss()
                Toast.makeText(this, "Gagal mengunggah rekaman $type: ${it.message}", Toast.LENGTH_SHORT).show()
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
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {

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
                            Toast.makeText(this, "Izin rekam suara belum diberikan", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Tidak dapat merekam suara: ${e.message}", Toast.LENGTH_SHORT).show()
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