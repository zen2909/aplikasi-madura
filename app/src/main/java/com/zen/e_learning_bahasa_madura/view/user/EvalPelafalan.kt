package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.app.AlertDialog
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.databinding.HalEvalPelafalanBinding
import java.io.File
import java.util.*

class EvalPelafalan : Activity() {

    private lateinit var binding: HalEvalPelafalanBinding
    private lateinit var db: DatabaseReference

    private var recorder: MediaRecorder? = null
    private var currentQuestionIndex = 0
    private val soalList = mutableListOf<SoalData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HalEvalPelafalanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseDatabase.getInstance().reference

        fetchSoalAktif()

        binding.btnRekam.setOnClickListener {
            handleRecording()
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
                        Toast.makeText(this@EvalPelafalan, "Tidak ada koleksi aktif", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
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
                        Toast.makeText(this@EvalPelafalan, "Belum ada soal dalam koleksi ini", Toast.LENGTH_SHORT).show()
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

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun tampilkanSoal(soal: SoalData) {
        binding.nomorSoal.text = (currentQuestionIndex + 1).toString().padStart(2, '0')
        binding.etInputText.setText(soal.soal)
    }

    private fun handleRecording() {
        if (recorder == null) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        val file = File(cacheDir, "recording_${UUID.randomUUID()}.3gp")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        showRecordingDialog {
            stopRecording()
        }
    }

    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            Toast.makeText(this, "Rekaman dihentikan", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menghentikan rekaman: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        recorder = null
    }

    private fun showRecordingDialog(onStop: () -> Unit) {
        val view = layoutInflater.inflate(com.zen.e_learning_bahasa_madura.R.layout.dialog_record, null)
        val btnStop = view.findViewById<Button>(com.zen.e_learning_bahasa_madura.R.id.btnStopRecording)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        btnStop.setOnClickListener {
            onStop()
            dialog.dismiss()
        }

        dialog.show()
    }

    data class SoalData(
        val id_evalpelafalan: String = "",
        val soal: String = "",
        val jawaban: String = ""
    )
}
