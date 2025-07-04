package com.zen.e_learning_bahasa_madura.model

data class KoleksiSoal(
    val id_koleksi: String? = null,
    val nama: String? = null,
    val kategori: String? = null,
    val jumlah_soal: Int = 0,
    val aktif: Boolean = false
)