package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Switch
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.databinding.ItemEvalBinding
import com.zen.e_learning_bahasa_madura.databinding.ListEvaluasiBinding
import com.zen.e_learning_bahasa_madura.util.NavHelper

class SoalEvaluasi : Activity() {

    private lateinit var binding: ListEvaluasiBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: KoleksiAdapter
    private val fullListData = mutableListOf<KoleksiItem>()
    private val koleksiList = mutableListOf<KoleksiItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ListEvaluasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference
        adapter = KoleksiAdapter(koleksiList, this)
        binding.rvSoal.layoutManager = LinearLayoutManager(this)
        binding.rvSoal.adapter = adapter

        NavHelper.setup(
            activity = this,
            menuInputKosakata = binding.menuInputKosakata,
            menuEval = binding.menuEval,
            menuList = binding.menuList,
            menuSoal = binding.menuSoal,
            currentClass = SoalEvaluasi::class.java
        )

        setupSearchView()
        fetchKoleksi()

        binding.btnRefresh.setOnClickListener {
            koleksiList.clear()
            adapter.notifyDataSetChanged()
            fetchKoleksi()
            Toast.makeText(this, "Data diperbarui", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText.orEmpty())
                return true
            }
        })
    }

    private fun filterList(query: String) {
        val filtered = if (query.isBlank()) {
            fullListData
        } else {
            fullListData.filter {
                it.namaKoleksi.contains(query, ignoreCase = true) ||
                        it.kategori.contains(query, ignoreCase = true)
            }
        }
        koleksiList.clear()
        koleksiList.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun fetchKoleksi() {
        binding.loadingBar.visibility = View.VISIBLE
        koleksiList.clear()
        fullListData.clear()

        database.child("koleksi_soal").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    binding.loadingBar.visibility = View.GONE
                    return
                }

                val tempList = mutableListOf<KoleksiItem>()

                snapshot.children.forEach { data ->
                    val idKoleksi = data.key ?: return@forEach
                    val namaKoleksi = data.child("nama").getValue(String::class.java) ?: "-"
                    val kategori = data.child("kategori").getValue(String::class.java) ?: "-"
                    val jumlah = data.child("jumlah_soal").getValue(Int::class.java) ?: 0
                    val aktif = data.child("aktif").getValue(Boolean::class.java) ?: false

                    tempList.add(
                        KoleksiItem(
                            nomor = 0,
                            idKoleksi = idKoleksi,
                            namaKoleksi = namaKoleksi,
                            kategori = kategori,
                            jumlahSoal = jumlah,
                            aktif = aktif
                        )
                    )
                }

                val sorted = tempList.sortedBy { it.namaKoleksi.lowercase() }
                    .mapIndexed { index, item -> item.copy(nomor = index + 1) }

                koleksiList.addAll(sorted)
                fullListData.addAll(sorted)

                binding.rvSoal.postDelayed({
                    adapter.notifyDataSetChanged()
                    binding.loadingBar.visibility = View.GONE
                }, 300)
            }

            override fun onCancelled(error: DatabaseError) {
                binding.loadingBar.visibility = View.GONE
                Toast.makeText(this@SoalEvaluasi, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    data class KoleksiItem(
        val nomor: Int = 0,
        val idKoleksi: String = "",
        val namaKoleksi: String = "",
        val kategori: String = "",
        val jumlahSoal: Int = 0,
        val aktif: Boolean = false
    )

    class KoleksiAdapter(
        private val data: List<KoleksiItem>,
        private val activity: Activity
    ) : RecyclerView.Adapter<KoleksiAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemEvalBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemEvalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            val context = holder.itemView.context
            val db = FirebaseDatabase.getInstance().reference

            holder.binding.tvNomor.text = item.nomor.toString()
            holder.binding.tvNamaKoleksi.text = item.namaKoleksi
            holder.binding.tvkategori.text = "${item.kategori} (${item.jumlahSoal} soal)"

            holder.binding.switchAktif.setOnCheckedChangeListener(null)
            holder.binding.switchAktif.isChecked = item.aktif

            holder.binding.switchAktif.setOnCheckedChangeListener { _, isChecked ->
                db.child("koleksi_soal").get().addOnSuccessListener { snapshot ->
                    val koleksiDalamKategori = snapshot.children.filter {
                        it.child("kategori").getValue(String::class.java).equals(item.kategori, ignoreCase = true)
                    }

                    if (isChecked) {
                        // Aktifkan koleksi ini, nonaktifkan yang lain di kategori sama
                        koleksiDalamKategori.forEach { snap ->
                            val id = snap.key ?: return@forEach
                            val aktifkan = id == item.idKoleksi
                            db.child("koleksi_soal").child(id).child("aktif").setValue(aktifkan)
                        }
                        Toast.makeText(context, "Koleksi aktif diperbarui", Toast.LENGTH_SHORT).show()
                        if (activity is SoalEvaluasi) activity.fetchKoleksi()

                    } else {
                        // Cek jika setidaknya ada satu koleksi lain yang aktif
                        val masihAdaLainYangAktif = koleksiDalamKategori.any {
                            val id = it.key
                            val aktif = it.child("aktif").getValue(Boolean::class.java) == true
                            id != item.idKoleksi && aktif
                        }

                        if (masihAdaLainYangAktif) {
                            db.child("koleksi_soal").child(item.idKoleksi).child("aktif").setValue(false)
                            if (activity is SoalEvaluasi) activity.fetchKoleksi()
                        } else {
                            // Tolak nonaktif jika ini satu-satunya yang aktif
                            Toast.makeText(context, "Minimal satu koleksi harus aktif", Toast.LENGTH_SHORT).show()
                            holder.binding.switchAktif.isChecked = true
                        }
                    }
                }
            }

            holder.binding.btnEdit.setOnClickListener {
                val intent = when {
                    item.kategori.equals("Terjemahan", ignoreCase = true) ->
                        Intent(activity, EditEvalTerjemahan::class.java)
                    item.kategori.equals("Tingkat Bahasa", ignoreCase = true) ->
                        Intent(activity, EditEvalTb::class.java)
                    item.kategori.equals("Pelafalan", ignoreCase = true) ->
                        Intent(activity, EditEvalPelafalan::class.java)
                    else -> null
                }
                intent?.putExtra("id_koleksi", item.idKoleksi)
                intent?.let {
                    activity.startActivity(it)
                    activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }

            holder.binding.btnDelete.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Hapus Koleksi Soal")
                    .setMessage("Yakin ingin menghapus koleksi '${item.namaKoleksi}' dan semua soalnya?")
                    .setPositiveButton("Hapus") { _, _ ->
                        val idKoleksi = item.idKoleksi

                        db.child("evaluasi").orderByChild("id_koleksi").equalTo(idKoleksi)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(evalSnap: DataSnapshot) {
                                    val idPilganList = mutableListOf<String>()
                                    val idPelafalanList = mutableListOf<String>()

                                    for (data in evalSnap.children) {
                                        val idPilgan = data.child("id_pilgan").getValue(String::class.java)
                                        val idPelafalan = data.child("id_pelafalan").getValue(String::class.java)

                                        idPilgan?.let { idPilganList.add(it) }
                                        idPelafalan?.let { idPelafalanList.add(it) }

                                        db.child("evaluasi").child(data.key!!).removeValue()
                                    }

                                    idPilganList.forEach { id ->
                                        db.child("evaluasi_pilgan").child(id).removeValue()
                                    }
                                    idPelafalanList.forEach { id ->
                                        db.child("evaluasi_pelafalan").child(id).removeValue()
                                    }

                                    db.child("koleksi_soal").child(idKoleksi).removeValue()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Koleksi berhasil dihapus", Toast.LENGTH_SHORT).show()
                                            if (activity is SoalEvaluasi) activity.fetchKoleksi()
                                        }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            fetchKoleksi()
        }
    }
}
