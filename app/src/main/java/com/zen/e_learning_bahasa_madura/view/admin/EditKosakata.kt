package com.zen.e_learning_bahasa_madura.view.admin

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.EditKosakataBinding
import com.zen.e_learning_bahasa_madura.model.*
import com.zen.e_learning_bahasa_madura.util.AudioRecorderUtil
import com.zen.e_learning_bahasa_madura.util.NavHelper
import java.io.File
import java.util.UUID

class EditKosakata : Activity() {

    private lateinit var binding: EditKosakataBinding
    private val db = FirebaseDatabase.getInstance().reference

    private var idDasar = ""
    private var idMenengah = ""
    private var idTinggi = ""
    private var idIndo = ""

    private var audioUrlDasar: String? = null
    private var audioUrlMenengah: String? = null
    private var audioUrlTinggi: String? = null

    private var recorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioPath: String = ""
    private var recordingDialog: AlertDialog? = null
    private var pendingRecordingType: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditKosakataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idDasar = intent.getStringExtra("id_dasar") ?: ""
        idMenengah = intent.getStringExtra("id_menengah") ?: ""
        idTinggi = intent.getStringExtra("id_tinggi") ?: ""
        idIndo = intent.getStringExtra("id_indo") ?: ""

        loadData()

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

        binding.ktmenengah.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val converted = convertToCarakan(s.toString())
                binding.carmenengah.text = converted
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.kttinggi.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val converted = convertToCarakan(s.toString())
                binding.cartinggi.text = converted
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        hurufkhusus()


        binding.btnDasar.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
                pendingRecordingType = "dasar"
            } else {
                showRecordingDialog("dasar")
            }
        }


        binding.btnMenengah.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
                pendingRecordingType = "menengah"
            } else {
                showRecordingDialog("menengah")
            }
        }


        binding.btnTinggi.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
                pendingRecordingType = "tinggi"
            } else {
                showRecordingDialog("tinggi")
            }
        }


        binding.btnSimpan.setOnClickListener {
            updateData()
        }
    }


    private fun hurufkhusus() {
        binding.hurufedasar.setOnClickListener { inserthuruf("è") }
        binding.hurufadasar.setOnClickListener { inserthuruf("â") }

        binding.hurufemenengah.setOnClickListener { inserthuruf("è") }
        binding.hurufamenengah.setOnClickListener { inserthuruf("â") }

        binding.hurufetinggi.setOnClickListener { inserthuruf("è") }
        binding.hurufatinggi.setOnClickListener { inserthuruf("â") }
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

    private fun loadData() {

        db.child("Bahasa_madura").child(idIndo).get().addOnSuccessListener {
            if (it.exists() && it.value is Map<*, *>) {
                val data = it.getValue(BahasaMadura::class.java)
                binding.ktindonesia.setText(data?.kosakata_indonesia ?: "-")
            } else {
                Toast.makeText(this, "Data bahasa Indonesia tidak valid", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal ambil bahasa Indonesia", Toast.LENGTH_SHORT).show()
        }


        db.child("Madura_dasar").child(idDasar).get().addOnSuccessListener {
            val data = it.getValue(MaduraDasar::class.java)
            binding.ktdasar.setText(data?.kosakata)
            binding.cardasar.setText(data?.carakan_madura)
            audioUrlDasar = data?.audio_pelafalan
        }


        db.child("Madura_menengah").child(idMenengah).get().addOnSuccessListener {
            val data = it.getValue(MaduraMenengah::class.java)
            binding.ktmenengah.setText(data?.kosakata)
            binding.carmenengah.setText(data?.carakan_madura)
            audioUrlMenengah = data?.audio_pelafalan
        }


        db.child("Madura_tinggi").child(idTinggi).get().addOnSuccessListener {
            val data = it.getValue(MaduraTinggi::class.java)
            binding.kttinggi.setText(data?.kosakata)
            binding.cartinggi.setText(data?.carakan_madura)
            audioUrlTinggi = data?.audio_pelafalan
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




    private fun updateData() {
        val dasar = MaduraDasar(
            id_dasar = idDasar,
            kosakata = binding.ktdasar.text.toString(),
            carakan_madura = binding.cardasar.text.toString(),
            audio_pelafalan = audioUrlDasar ?: ""
        )

        val menengah = MaduraMenengah(
            id_menengah = idMenengah,
            kosakata = binding.ktmenengah.text.toString(),
            carakan_madura = binding.carmenengah.text.toString(),
            audio_pelafalan = audioUrlMenengah ?: ""
        )

        val tinggi = MaduraTinggi(
            id_tinggi = idTinggi,
            kosakata = binding.kttinggi.text.toString(),
            carakan_madura = binding.cartinggi.text.toString(),
            audio_pelafalan = audioUrlTinggi ?: ""
        )

        val bahasaUpdate = BahasaMadura(
            id = idIndo,
            id_dasar = idDasar,
            id_menengah = idMenengah,
            id_tinggi = idTinggi,
            kosakata_indonesia = binding.ktindonesia.text.toString()
        )

        db.child("Madura_dasar").child(idDasar).setValue(dasar)
        db.child("Madura_menengah").child(idMenengah).setValue(menengah)
        db.child("Madura_tinggi").child(idTinggi).setValue(tinggi)
        db.child("Bahasa_madura").child(idIndo).setValue(bahasaUpdate)

        Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        recorder?.release()
        mediaPlayer?.release()
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
