package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zen.e_learning_bahasa_madura.databinding.InputKosakataBinding
import android.media.MediaRecorder
import android.net.Uri
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.zen.e_learning_bahasa_madura.model.BahasaMadura
import com.zen.e_learning_bahasa_madura.model.MaduraDasar
import com.zen.e_learning_bahasa_madura.model.MaduraMenengah
import com.zen.e_learning_bahasa_madura.model.MaduraTinggi
import java.io.File
import java.util.UUID
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class InputKosakata : Activity() {

    lateinit var binding : InputKosakataBinding
    private var audioDasarPath: String = ""
    private var audioMenengahPath: String = ""
    private var audioTinggiPath: String = ""
    private var recorder: MediaRecorder? = null
    private var currentRecordingType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = InputKosakataBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.inputkosakata.setOnClickListener {
            val intent = Intent(this, InputKosakata::class.java)
            startActivity(intent)
        }

        binding.inputeval.setOnClickListener {
            val intent = Intent(this, InputEvalTerjemahan::class.java)
            startActivity(intent)
        }

        binding.listkosakata.setOnClickListener {
            val intent = Intent(this, ListKosakata::class.java)
            startActivity(intent)
        }

        binding.listsoaleval.setOnClickListener {
            val intent = Intent(this, SoalEvaluasi::class.java)
            startActivity(intent)
        }

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
        val outputFile = File(cacheDir, "audio_${type}_${UUID.randomUUID()}.3gp")
        currentRecordingType = type

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        when (type) {
            "dasar" -> audioDasarPath = outputFile.absolutePath
            "menengah" -> audioMenengahPath = outputFile.absolutePath
            "tinggi" -> audioTinggiPath = outputFile.absolutePath
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

        val db = FirebaseDatabase.getInstance().reference
        val idDasar = db.child("Madura_dasar").push().key!!
        val idMenengah = db.child("Madura_menengah").push().key!!
        val idTinggi = db.child("Madura_tinggi").push().key!!
        val bahasaId = UUID.randomUUID().toString()

        val uriDasar = Uri.fromFile(File(audioDasarPath))
        val uriMenengah = Uri.fromFile(File(audioMenengahPath))
        val uriTinggi = Uri.fromFile(File(audioTinggiPath))

        uploadAudio(uriDasar, "audio_dasar") { urlDasar ->
            uploadAudio(uriMenengah, "audio_menengah") { urlMenengah ->
                uploadAudio(uriTinggi, "audio_tinggi") { urlTinggi ->

                    val dasarMap = MaduraDasar(idDasar, dasar, cardasar, urlDasar)
                    val menengahMap = MaduraMenengah(idMenengah, menengah, carmenengah, urlMenengah)
                    val tinggiMap = MaduraTinggi(idTinggi, tinggi, cartinggi, urlTinggi)
                    val bahasaMap = BahasaMadura(idDasar, idMenengah, idTinggi, indo)

                    db.child("Madura_dasar").child(idDasar).setValue(dasarMap)
                    db.child("Madura_menengah").child(idMenengah).setValue(menengahMap)
                    db.child("Madura_tinggi").child(idTinggi).setValue(tinggiMap)
                    db.child("Bahasa_madura").child(bahasaId).setValue(bahasaMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Kosakata berhasil disimpan", Toast.LENGTH_SHORT).show()
                            clearForm()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal simpan: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
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
        binding.cardasar.text.clear()
        binding.carmenengah.text.clear()
        binding.cartinggi.text.clear()
    }

}