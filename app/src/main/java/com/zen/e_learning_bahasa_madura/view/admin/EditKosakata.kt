package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.zen.e_learning_bahasa_madura.databinding.EditKosakataBinding
import com.zen.e_learning_bahasa_madura.model.*

class EditKosakata : Activity() {

    private lateinit var binding: EditKosakataBinding
    private val db = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private var idDasar = ""
    private var idMenengah = ""
    private var idTinggi = ""
    private var idIndo = ""

    private var audioUrlDasar: String? = null
    private var audioUrlMenengah: String? = null
    private var audioUrlTinggi: String? = null

    private var recorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private var audioURL: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditKosakataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idDasar = intent.getStringExtra("id_dasar") ?: ""
        idMenengah = intent.getStringExtra("id_menengah") ?: ""
        idTinggi = intent.getStringExtra("id_tinggi") ?: ""
        idIndo = intent.getStringExtra("id_indo") ?: ""

        loadData()

        binding.btnDasar.setOnClickListener { startRecording("dasar") }
        binding.btnMenengah.setOnClickListener { startRecording("menengah") }
        binding.btnTinggi.setOnClickListener { startRecording("tinggi") }

        binding.btnPlayDasar.setOnClickListener { playAudio(audioUrlDasar) }
        binding.btnPlayMenengah.setOnClickListener { playAudio(audioUrlMenengah) }
        binding.btnPlayTinggi.setOnClickListener { playAudio(audioUrlTinggi) }

        binding.btnSimpan.setOnClickListener {
            updateData()
        }
    }

    private fun loadData() {
        db.child("Madura_dasar").child(idDasar).get().addOnSuccessListener {
            val data = it.getValue(MaduraDasar::class.java)
            binding.ktdasar.setText(data?.kosakata)
            binding.cardasar.setText(data?.carakan_madura)
            audioURL = data?.audio_pelafalan
        }

        db.child("Madura_menengah").child(idMenengah).get().addOnSuccessListener {
            val data = it.getValue(MaduraMenengah::class.java)
            binding.ktmenengah.setText(data?.kosakata)
            binding.carmenengah.setText(data?.carakan_madura)
            audioURL = data?.audio_pelafalan
        }

        db.child("Madura_tinggi").child(idTinggi).get().addOnSuccessListener {
            val data = it.getValue(MaduraTinggi::class.java)
            binding.kttinggi.setText(data?.kosakata)
            binding.cartinggi.setText(data?.carakan_madura)
            audioURL = data?.audio_pelafalan
        }

        db.child("Bahasa_madura").child(idIndo).get().addOnSuccessListener {
            val data = it.getValue(BahasaMadura::class.java)
            binding.ktindonesia.setText(data?.kosakata_indonesia)
        }
    }

    private fun playAudio(url: String?) {
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "Audio tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepare()
            start()
        }
    }


    private fun startRecording(level: String) {
        val outputFile = "${externalCacheDir?.absolutePath}/audio_${level}_${System.currentTimeMillis()}.3gp"
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile)
            prepare()
            start()
        }

        Toast.makeText(this, "Merekam audio $level...", Toast.LENGTH_SHORT).show()

        binding.root.postDelayed({
            try {
                recorder?.stop()
                recorder?.release()
                recorder = null
                uploadAudio(level, outputFile)
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal rekam audio: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, 4000)
    }


    private fun uploadAudio(level: String, path: String) {
        val file = Uri.fromFile(java.io.File(path))
        val ref = FirebaseStorage.getInstance().reference.child("audio/${file.lastPathSegment}")
        ref.putFile(file).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                when (level) {
                    "dasar" -> audioUrlDasar = uri.toString()
                    "menengah" -> audioUrlMenengah = uri.toString()
                    "tinggi" -> audioUrlTinggi = uri.toString()
                }
                Toast.makeText(this, "Audio $level diunggah", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal upload audio $level", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateData() {
        val dasarAudio = audioUrlDasar ?: ""
        val menengahAudio = audioUrlMenengah ?: ""
        val tinggiAudio = audioUrlTinggi ?: ""

        val dasar = MaduraDasar(idDasar, binding.ktdasar.text.toString(), binding.cardasar.text.toString(), dasarAudio)
        val menengah = MaduraMenengah(idMenengah, binding.ktmenengah.text.toString(), binding.carmenengah.text.toString(), menengahAudio)
        val tinggi = MaduraTinggi(idTinggi, binding.kttinggi.text.toString(), binding.cartinggi.text.toString(), tinggiAudio)


        val indoUpdate = mapOf("kosakata_indonesia" to binding.ktindonesia.text.toString())

        db.child("Madura_dasar").child(idDasar).setValue(dasar)
        db.child("Madura_menengah").child(idMenengah).setValue(menengah)
        db.child("Madura_tinggi").child(idTinggi).setValue(tinggi)
        db.child("Bahasa_madura").child(idIndo).updateChildren(indoUpdate)

        Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder?.release()
        mediaPlayer?.release()
    }
}
