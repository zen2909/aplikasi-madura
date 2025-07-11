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
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.HalEvalPelafalanBinding
import com.zen.e_learning_bahasa_madura.util.AudioEvaluator
import com.zen.e_learning_bahasa_madura.util.AudioRecorderUtil
import com.zen.e_learning_bahasa_madura.util.MFCCProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File


class EvalPelafalan : Activity() {

    private lateinit var binding: HalEvalPelafalanBinding
    private lateinit var db: DatabaseReference
    private lateinit var scope: CoroutineScope
    private lateinit var audioRecorder: AudioRecorderUtil
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

        // Misal, ambil audio dari database dan simpan ke file lokal
        kataDariDatabase = File(cacheDir, "target.wav")
        // download dan simpan audio dari Firebase Storage ke file ini

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
                        startRecording()
                        Toast.makeText(this, "Mulai merekam", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isRecording) {
                        stopRecording()
                    }
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
                    var idKoleksiAktif: String? = null
                    for (data in snapshot.children) {
                        val aktif = data.child("aktif").getValue(Boolean::class.java) ?: false
                        if (aktif) {
                            idKoleksiAktif = data.key
                            break
                        }
                    }

                    if (idKoleksiAktif != null) {
                        ambilSoalDariKoleksi(idKoleksiAktif)
                    } else {
                        Toast.makeText(
                            this@EvalPelafalan,
                            "Tidak ada koleksi soal aktif",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish() // Tutup halaman jika tidak ada soal
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@EvalPelafalan,
                        "Gagal mengambil data: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            })
    }

    private fun ambilSoalDariKoleksi(idKoleksi: String) {
        db.child("evaluasi").orderByChild("id_koleksi").equalTo(idKoleksi)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val idPelafalanList = mutableListOf<String>()
                    for (data in snapshot.children) {
                        val id = data.child("id_pelafalan").getValue(String::class.java)
                        id?.let { idPelafalanList.add(it) }
                    }

                    if (idPelafalanList.isEmpty()) {
                        Toast.makeText(
                            this@EvalPelafalan,
                            "Belum ada soal dalam koleksi ini",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val counter = intArrayOf(0)
                    for (id in idPelafalanList) {
                        db.child("evaluasi_pelafalan").child(id)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snap: DataSnapshot) {
                                    val soal = snap.getValue(SoalData::class.java)
                                    soal?.let { soalList.add(it) }

                                    counter[0]++
                                    if (counter[0] == idPelafalanList.size) {
                                        soalList.shuffle()
                                        tampilkanSoal(soalList[0])
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(
                                        this@EvalPelafalan,
                                        "Gagal memuat soal: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@EvalPelafalan,
                        "Gagal mengambil daftar soal: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
            Toast.makeText(this, "Rekaman dimulai", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            isRecording = false
            Toast.makeText(this, "Gagal mulai rekaman: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun stopRecording() {
        try {
            AudioRecorderUtil.stopRecording()
            isRecording = false

            if (::latestRecording.isInitialized && latestRecording.exists()) {
                Toast.makeText(this, "Rekaman selesai", Toast.LENGTH_SHORT).show()

                // Tampilkan preview suara dulu
                showAudioPreviewDialog(latestRecording, this) {
                    // Setelah preview selesai (atau dilewati), lakukan evaluasi
                    val soalSekarang = soalList[currentQuestionIndex]
                    bandingkanAudio(latestRecording, soalSekarang.jawaban)
                }

            } else {
                Toast.makeText(this, "File rekaman tidak ditemukan", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            isRecording = false
            Toast.makeText(this, "Gagal stop rekaman: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun bandingkanAudio(userFile: File, jawabanUrl: String) {
        val dialog = ProgressDialog(this).apply {
            setTitle("Evaluasi")
            setMessage("Mengunduh audio referensi...")
            setCancelable(false)
            show()
        }

        scope.launch(Dispatchers.IO) {
            try {
                val refFile = File.createTempFile("ref_audio", ".wav", cacheDir)
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(jawabanUrl)

                val downloadTask = storageRef.getFile(refFile)
                downloadTask.await()

                withContext(Dispatchers.Main) {
                    dialog.setMessage("Menganalisis suara...")
                }

                val userFeatures = AudioEvaluator.extractMFCCFromFile(userFile)
                val refFeatures = AudioEvaluator.extractMFCCFromFile(refFile)

                // ✅ Gunakan evaluasi komprehensif
                val result = AudioEvaluator.comprehensiveEvaluation(userFeatures, refFeatures)

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


    private fun showAudioPreviewDialog(audioFile: File, context: Context, onFinish: () -> Unit) {
        val mediaPlayer = MediaPlayer()

        val dialog = AlertDialog.Builder(context)
            .setTitle("Preview Rekaman")
            .setMessage("Putar rekaman sebelum dievaluasi?")
            .setPositiveButton("Putar", null)
            .setNegativeButton("Lewati") { _, _ ->
                try {
                    mediaPlayer.stop()
                } catch (_: Exception) {}
                mediaPlayer.release()
                onFinish()
            }
            .create()

        dialog.setOnShowListener {
            val btnPutar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnPutar.setOnClickListener {
                try {
                    mediaPlayer.setDataSource(audioFile.absolutePath)
                    mediaPlayer.setOnPreparedListener {
                        it.start()
                    }
                    mediaPlayer.setOnCompletionListener {
                        try {
                            it.stop()
                        } catch (_: Exception) {}
                        it.release()
                        onFinish()
                        dialog.dismiss()
                    }
                    mediaPlayer.prepareAsync()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memutar: ${e.message}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    onFinish()
                }
            }
        }

        dialog.setOnDismissListener {
            // Jangan akses isPlaying karena bisa IllegalState
            try {
                mediaPlayer.stop()
            } catch (_: Exception) {}
            try {
                mediaPlayer.release()
            } catch (_: Exception) {}
        }

        dialog.show()
    }

    private fun showHasilRekaman(score: Int, feedback: String, confidence: Double) {
        val view = layoutInflater.inflate(R.layout.hasil_rekaman, null)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressCircle)
        val percentageText = view.findViewById<TextView>(R.id.percentageText)
        val feedbackText = view.findViewById<TextView>(R.id.feedbackText)
        val confidenceText = view.findViewById<TextView>(R.id.confidenceText) // Tambahkan ini di layout XML
        val btnNext = view.findViewById<ImageButton>(R.id.btnNext)

        percentageText.text = "$score%"
        progressBar.progress = score
        feedbackText.text = feedback
        confidenceText.text = "Confidence: ${"%.1f".format(confidence)}%"

        val colorRes = when {
            score > 70 -> R.color.score_good
            score in 50..70 -> R.color.score_medium
            score in 30..50 -> R.color.score_bad
            else -> R.color.score_bad
        }

        percentageText.setTextColor(ContextCompat.getColor(this, colorRes))
        feedbackText.setTextColor(ContextCompat.getColor(this, colorRes))
        confidenceText.setTextColor(ContextCompat.getColor(this, R.color.black)) // Confidence tetap netral

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

    data class SoalData(
        val id_evalpelafalan: String = "",
        val soal: String = "",
        val jawaban: String = ""
    )
}