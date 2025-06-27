package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.EditKosakataBinding
import com.zen.e_learning_bahasa_madura.model.*
import java.io.File

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
    private var currentAudioPath: String = ""
    private var recordingDialog: AlertDialog? = null

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

        hurufkhusus()

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

    private fun hurufkhusus() {
        binding.hurufemdr.setOnClickListener { inserthuruf("è") }
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


    private fun playAudio(url: String?) {
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "Audio tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_audio, null)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Memuat Audio")
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.show()

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener { start() }
            setOnCompletionListener {
                dialog.dismiss()
                release()
            }
            setOnErrorListener { _, _, _ ->
                dialog.dismiss()
                Toast.makeText(this@EditKosakata, "Gagal memutar audio", Toast.LENGTH_SHORT).show()
                true
            }
            prepareAsync()
        }
    }




    private fun startRecording(level: String) {
        val outputFile = "${externalCacheDir?.absolutePath}/audio_${level}_${System.currentTimeMillis()}.3gp"
        currentAudioPath = outputFile

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile)
            prepare()
            start()
        }

        showRecordingDialog {
            stopRecording(level, outputFile)
        }

        Toast.makeText(this, "Merekam audio $level...", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording(level: String, path: String) {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null

            val progress = ProgressDialog(this).apply {
                setTitle("Mengunggah Audio")
                setMessage("Mohon tunggu...")
                setCancelable(false)
                show()
            }

            val file = Uri.fromFile(File(path))
            val ref = FirebaseStorage.getInstance().reference.child("audio/${file.lastPathSegment}")
            ref.putFile(file).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    when (level) {
                        "dasar" -> audioUrlDasar = uri.toString()
                        "menengah" -> audioUrlMenengah = uri.toString()
                        "tinggi" -> audioUrlTinggi = uri.toString()
                    }
                    progress.dismiss()
                    Toast.makeText(this, "Audio $level diunggah", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                progress.dismiss()
                Toast.makeText(this, "Gagal upload audio $level", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Gagal merekam: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }




    private fun showRecordingDialog(onStop: () -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_record, null)
        val stopBtn = view.findViewById<Button>(R.id.btnStopRecording)

        recordingDialog = AlertDialog.Builder(this)
            .setTitle("Rekaman Audio")
            .setView(view)
            .setCancelable(false)
            .create()

        stopBtn.setOnClickListener {
            onStop()
            recordingDialog?.dismiss()
        }

        recordingDialog?.show()
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
}
