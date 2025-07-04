package com.zen.e_learning_bahasa_madura.view.user

import android.app.Activity
import android.app.AlertDialog
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.animation.DecelerateInterpolator
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.HalEvalTbBinding
import java.lang.System

class EvalTingkatBahasa : Activity() {

    private lateinit var binding: HalEvalTbBinding
    private lateinit var db: DatabaseReference

    private var currentQuestionIndex = 0
    private val soalList = mutableListOf<SoalData>()
    private val jawabanUser = mutableListOf<String?>()
    private var waktuMulai: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HalEvalTbBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseDatabase.getInstance().reference

        setupRadioButtons()
        fetchSoalAktif()

        binding.icSend.setOnClickListener {
            val selectedAnswer = getSelectedAnswer()

            if (selectedAnswer == null) {
                Toast.makeText(this, "Silakan pilih jawaban terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            jawabanUser.add(selectedAnswer)

            currentQuestionIndex++
            if (currentQuestionIndex < soalList.size) {
                tampilkanSoal(soalList[currentQuestionIndex])
                clearSelection()
            } else {
                evaluasiJawaban()
            }
        }
    }

    private fun fetchSoalAktif() {
        waktuMulai = System.currentTimeMillis() // Mulai waktu

        db.child("koleksi_soal").orderByChild("kategori").equalTo("Tingkat Bahasa")
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
                        Toast.makeText(this@EvalTingkatBahasa, "Tidak ada koleksi aktif", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun ambilSoalDariKoleksi(idKoleksi: String) {
        db.child("evaluasi").orderByChild("id_koleksi").equalTo(idKoleksi)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val idPilganList = mutableListOf<String>()
                    for (data in snapshot.children) {
                        val idPilgan = data.child("id_pilgan").getValue(String::class.java)
                        idPilgan?.let { idPilganList.add(it) }
                    }

                    if (idPilganList.isEmpty()) {
                        Toast.makeText(this@EvalTingkatBahasa, "Belum ada soal dalam koleksi ini", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val counter = intArrayOf(0)
                    for (id in idPilganList) {
                        db.child("evaluasi_pilgan").child(id)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snap: DataSnapshot) {
                                    val soal = snap.getValue(SoalData::class.java)
                                    soal?.let { soalList.add(it) }

                                    counter[0]++
                                    if (counter[0] == idPilganList.size) {
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

        binding.Jawaban1.text = soal.jwb_1
        binding.Jawaban2.text = soal.jwb_2
        binding.Jawaban3.text = soal.jwb_3
        binding.Jawaban4.text = soal.jwb_4
    }

    private fun clearSelection() {
        binding.radiobutton1.isChecked = false
        binding.radiobutton2.isChecked = false
        binding.radiobutton3.isChecked = false
        binding.radiobutton4.isChecked = false
    }

    private fun getSelectedAnswer(): String? {
        return when {
            binding.radiobutton1.isChecked -> binding.Jawaban1.text.toString()
            binding.radiobutton2.isChecked -> binding.Jawaban2.text.toString()
            binding.radiobutton3.isChecked -> binding.Jawaban3.text.toString()
            binding.radiobutton4.isChecked -> binding.Jawaban4.text.toString()
            else -> null
        }
    }

    private fun setupRadioButtons() {
        val radioButtons = listOf(
            binding.radiobutton1,
            binding.radiobutton2,
            binding.radiobutton3,
            binding.radiobutton4
        )

        for (rb in radioButtons) {
            rb.setOnClickListener {
                radioButtons.forEach { it.isChecked = false }
                rb.isChecked = true
            }
        }
    }

    private fun evaluasiJawaban() {
        var benar = 0
        var salah = 0
        var totalBobot = 0
        var bobotDiperoleh = 0

        for (i in soalList.indices) {
            val soal = soalList[i]
            val bobotSoal = soal.bobot.toIntOrNull() ?: 0
            val jawabanBenar = soal.jwb_benar.trim()
            val jawabanUseri = jawabanUser.getOrNull(i)?.trim()

            totalBobot += bobotSoal
            Log.d("BobotDebug", "Soal ${i + 1}: Bobot = $bobotSoal, JawabanUser = $jawabanUseri, JawabanBenar = $jawabanBenar")

            if (jawabanUseri != null && jawabanUseri.equals(jawabanBenar, ignoreCase = true)) {
                bobotDiperoleh += bobotSoal
            }
        }

        Log.d("BobotDebug", "Total Bobot: $totalBobot")
        Log.d("BobotDebug", "Bobot Diperoleh: $bobotDiperoleh")

        val nilai = bobotDiperoleh

        val waktuSelesai = System.currentTimeMillis()
        val durasi = waktuSelesai - waktuMulai
        tampilkanDialogHasil(benar, salah, nilai, durasi)
    }


    private fun tampilkanDialogHasil(benar: Int, salah: Int, nilai: Int, durasiMillis: Long) {
        val dialogView = layoutInflater.inflate(R.layout.hasil_eval, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvBenar = dialogView.findViewById<TextView>(R.id.jawabanBenar)
        val tvSalah = dialogView.findViewById<TextView>(R.id.jawabanSalah)
        val tvWaktu = dialogView.findViewById<TextView>(R.id.waktuPengerjaan)
        val tvNilai = dialogView.findViewById<TextView>(R.id.tv_nilai)
        val progress = dialogView.findViewById<ProgressBar>(R.id.progress_circle)
        val btnSelesai = dialogView.findViewById<ImageButton>(R.id.btnSelesai)

        tvBenar.text = benar.toString()
        tvSalah.text = salah.toString()
        tvNilai.text = nilai.toString()
        progress.progress = nilai

        val animator = ObjectAnimator.ofInt(progress, "progress", 0, nilai)
        animator.duration = 1000
        animator.interpolator = DecelerateInterpolator()
        animator.start()

        val waktuDetik = durasiMillis / 1000
        val menit = waktuDetik / 60
        val detik = waktuDetik % 60
        tvWaktu.text = "$menit Menit $detik Detik"

        btnSelesai.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    data class SoalData(
        val id_evalpilgan: String = "",
        val soal: String = "",
        val jwb_1: String = "",
        val jwb_2: String = "",
        val jwb_3: String = "",
        val jwb_4: String = "",
        val jwb_benar: String = "",
        val bobot: String = ""
    )
}
