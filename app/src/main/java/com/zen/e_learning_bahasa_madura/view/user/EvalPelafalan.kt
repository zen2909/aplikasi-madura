package com.zen.e_learning_bahasa_madura.view.user

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.HalEvalPelafalanBinding
import com.zen.e_learning_bahasa_madura.util.AudioEvaluator
import com.zen.e_learning_bahasa_madura.util.AudioRecorderUtil
import com.zen.e_learning_bahasa_madura.util.BacksoundManager
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File

class EvalPelafalan : Activity() {

    private lateinit var binding: HalEvalPelafalanBinding
    private lateinit var db: DatabaseReference
    private lateinit var scope: CoroutineScope
    private lateinit var outputFile: File
    private lateinit var kataDariDatabase: File
    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var currentQuestionIndex = 0
    private val soalList = mutableListOf<SoalData>()
    private lateinit var latestRecording: File

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HalEvalPelafalanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseDatabase.getInstance().reference
        scope = CoroutineScope(Dispatchers.Main + Job())
        outputFile = File(cacheDir, "user_recording.wav")
        kataDariDatabase = File(cacheDir, "target.wav")

        setupUI()
        fetchSoalAktif()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        cleanupRecorder()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun setupUI() {
        binding.btnRekam.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isRecording) {
                        BacksoundManager.pauseImmediately()
                        startRecording()
                        showToast("Mulai merekam")
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isRecording) stopRecording()
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchSoalAktif() {
        db.child("koleksi_soal").orderByChild("kategori").equalTo("Pelafalan")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val idKoleksiAktif = snapshot.children.firstOrNull {
                        it.child("aktif").getValue(Boolean::class.java) == true
                    }?.key

                    if (idKoleksiAktif != null) ambilSoalDariKoleksi(idKoleksiAktif)
                    else {
                        showToast("Tidak ada koleksi soal aktif")
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Gagal mengambil data: ${error.message}")
                    finish()
                }
            })
    }

    private fun ambilSoalDariKoleksi(idKoleksi: String) {
        db.child("evaluasi").orderByChild("id_koleksi").equalTo(idKoleksi)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ids = snapshot.children.mapNotNull {
                        it.child("id_evalpelafalan").getValue(String::class.java)
                    }

                    if (ids.isEmpty()) {
                        showToast("Belum ada soal dalam koleksi ini")
                        return
                    }

                    val counter = intArrayOf(0)
                    for (id in ids) {
                        db.child("evaluasi_pelafalan").child(id)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snap: DataSnapshot) {
                                    snap.getValue(SoalData::class.java)?.let { soalList.add(it) }
                                    counter[0]++
                                    if (counter[0] == ids.size) {
                                        soalList.shuffle()
                                        tampilkanSoal(soalList[0])
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    showToast("Gagal memuat soal: ${error.message}")
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Gagal mengambil daftar soal: ${error.message}")
                }
            })
    }

    private fun tampilkanSoal(soal: SoalData) {
        binding.nomorSoal.text = (currentQuestionIndex + 1).toString().padStart(2, '0')
        binding.etInputText.setText(soal.soal)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        try {
            latestRecording = File(cacheDir, "recording_${System.currentTimeMillis()}.wav")
            AudioRecorderUtil.startRecording(latestRecording)
            isRecording = true
            showToast("Rekaman dimulai")
        } catch (e: Exception) {
            isRecording = false
            showToast("Gagal mulai rekaman: ${e.message}")
        }
    }

    private fun stopRecording() {
        try {
            AudioRecorderUtil.stopRecording()
            if (AudioRecorderUtil.getAudioDuration(latestRecording) < 1.0) {
                showToast("Rekaman terlalu pendek. Ulangi.")
                return
            }
            isRecording = false

            if (::latestRecording.isInitialized && latestRecording.exists()) {
                showToast("Rekaman selesai")
                showAudioPreviewDialog(latestRecording, this) {
                    val soalSekarang = soalList[currentQuestionIndex]
                    bandingkanAudio(latestRecording, soalSekarang.jawaban)
                }
            } else {
                showToast("File rekaman tidak ditemukan")
            }
        } catch (e: Exception) {
            isRecording = false
            showToast("Gagal stop rekaman: ${e.message}")
        }
    }

    private fun bandingkanAudio(userFile: File, jawabanUrl: String) {
        val view = layoutInflater.inflate(R.layout.dialog_progress_audio, null)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        scope.launch(Dispatchers.IO) {
            try {
                val refFile = File.createTempFile("ref_audio", ".wav", cacheDir)
                FirebaseStorage.getInstance().getReferenceFromUrl(jawabanUrl).getFile(refFile).await()

                val processedFile = File(cacheDir, "processed_audio.wav")
                val denoised = AudioRecorderUtil.applyNoiseReduction(
                    AudioRecorderUtil.normalizeAudio(userFile.readBytes())
                )
                AudioRecorderUtil.writeWavFile(processedFile, denoised)

                val userFeatures = AudioEvaluator.preprocessFeatures(AudioEvaluator.extractMFCCFromFile(processedFile))
                val refFeatures = AudioEvaluator.preprocessFeatures(AudioEvaluator.extractMFCCFromFile(refFile))

                val result = AudioEvaluator.comprehensiveEvaluation(userFeatures, refFeatures)

                processedFile.delete()
                userFile.delete()
                refFile.delete()

                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    showHasilRekaman(result.score, result.feedback, result.confidence)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    showErrorDialog("Gagal evaluasi: ${e.message}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun showAudioPreviewDialog(audioFile: File, context: Context, onFinish: () -> Unit) {
        val mediaPlayer = MediaPlayer()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_preview_audio, null)

        val btnPutar = view.findViewById<TextView>(R.id.btnPutar)
        val btnLewati = view.findViewById<TextView>(R.id.btnLewati)

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        btnPutar.setOnClickListener {
            try {
                BacksoundManager.pauseImmediately()
                mediaPlayer.setDataSource(audioFile.absolutePath)
                mediaPlayer.setOnPreparedListener {
                    it.start()
                }
                mediaPlayer.setOnCompletionListener {
                    try {
                        if (it.isPlaying) it.stop()
                    } catch (_: Exception) {}
                    it.release()
                    BacksoundManager.resume()
                    onFinish()
                    dialog.dismiss()
                }
                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memutar: ${e.message}", Toast.LENGTH_SHORT).show()
                BacksoundManager.resume()
                dialog.dismiss()
                onFinish()
            }
        }

        btnLewati.setOnClickListener {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
            } catch (_: Exception) {}
            mediaPlayer.release()
            BacksoundManager.resume()
            onFinish()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
            } catch (_: Exception) {}
            try {
                mediaPlayer.release()
            } catch (_: Exception) {}
            BacksoundManager.resume()
        }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showHasilRekaman(score: Int, feedback: String, confidence: Double) {
        val view = layoutInflater.inflate(R.layout.hasil_rekaman, null)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressCircle)
        val percentageText = view.findViewById<TextView>(R.id.percentageText)
        val feedbackText = view.findViewById<TextView>(R.id.feedbackText)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNext)

        percentageText.text = "$score%"
        progressBar.progress = score
        feedbackText.text = feedback

        val colorRes = when {
            score > 70 -> R.color.score_good
            score in 50..70 -> R.color.score_medium
            score in 30..50 -> R.color.score_bad
            else -> R.color.score_bad
        }

        percentageText.setTextColor(ContextCompat.getColor(this, colorRes))
        feedbackText.setTextColor(ContextCompat.getColor(this, colorRes))

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        btnNext.setOnClickListener {
            dialog.dismiss()
            nextQuestion()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }



    private fun nextQuestion() {
        if (currentQuestionIndex < soalList.size - 1) {
            currentQuestionIndex++
            tampilkanSoal(soalList[currentQuestionIndex])
        } else {
            showCompletionDialog()
        }
    }

    private fun showCompletionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Selesai")
            .setMessage("Anda telah menyelesaikan semua soal.")
            .setPositiveButton("Kembali") { _, _ ->
                startActivity(Intent(this, Evaluasi::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun cleanupRecorder() {
        try {
            recorder?.apply {
                if (isRecording) stop()
                release()
            }
            recorder = null
            isRecording = false
        } catch (_: Exception) {
        }
    }

    private fun saveByteArrayAsWav(data: ByteArray, targetFile: File) {
        targetFile.outputStream().use {
            it.write(data)
        }
    }

    data class SoalData(
        val id_evalpelafalan: String = "",
        val soal: String = "",
        val jawaban: String = ""
    )
}