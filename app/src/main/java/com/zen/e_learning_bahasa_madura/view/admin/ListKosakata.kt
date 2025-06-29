package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.databinding.ListKosakataBinding
import com.zen.e_learning_bahasa_madura.databinding.ItemKosakataBinding
import com.zen.e_learning_bahasa_madura.model.BahasaMadura
import com.zen.e_learning_bahasa_madura.model.MaduraDasar
import com.zen.e_learning_bahasa_madura.model.MaduraMenengah
import com.zen.e_learning_bahasa_madura.model.MaduraTinggi
import com.zen.e_learning_bahasa_madura.util.NavHelper
import com.zen.e_learning_bahasa_madura.view.admin.EditKosakata

class ListKosakata : Activity() {

    private lateinit var binding: ListKosakataBinding
    private lateinit var database: DatabaseReference
    private val fullListData = mutableListOf<KosakataFull>()
    private val listData = mutableListOf<KosakataFull>()
    private lateinit var adapter: KosakataAdapter
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ListKosakataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference
        binding.rvKosakata.layoutManager = LinearLayoutManager(this)
        adapter = KosakataAdapter(listData, ::playAudio, this)
        binding.rvKosakata.adapter = adapter

        NavHelper.setup(
            activity = this,
            menuInputKosakata = binding.menuInputKosakata,
            menuEval = binding.menuEval,
            menuList = binding.menuList,
            menuSoal = binding.menuSoal,
            currentClass = ListKosakata::class.java
        )
        setupSearchView()
        fetchKosakata()

        binding.btnRefresh.setOnClickListener {
            listData.clear()
            adapter.notifyDataSetChanged()
            fetchKosakata()
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
                it.dasar.kosakata.contains(query, ignoreCase = true) ||
                        it.indo.kosakata_indonesia.contains(query, ignoreCase = true)
            }
        }

        listData.clear()
        listData.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun fetchKosakata() {
        binding.loadingBar.visibility = View.VISIBLE

        database.child("Bahasa_madura").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fullListData.clear()
                listData.clear()

                val totalItems = snapshot.childrenCount
                var loadedItems = 0
                var index = 1

                if (totalItems == 0L) {
                    adapter.notifyDataSetChanged()
                    binding.loadingBar.visibility = View.GONE
                    return
                }

                for (data in snapshot.children) {
                    val bahasa = data.getValue(BahasaMadura::class.java) ?: continue

                    val idDasar = bahasa.id_dasar
                    val idMenengah = bahasa.id_menengah
                    val idTinggi = bahasa.id_tinggi

                    database.child("Madura_dasar").child(idDasar)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshotDasar: DataSnapshot) {
                                val dasar = snapshotDasar.getValue(MaduraDasar::class.java)

                                if (dasar == null) {
                                    loadedItems++
                                    if (loadedItems == totalItems.toInt()) updateKosakataList()
                                    return
                                }

                                database.child("Madura_menengah").child(idMenengah)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshotMenengah: DataSnapshot) {
                                            val menengah = snapshotMenengah.getValue(MaduraMenengah::class.java) ?: return

                                            database.child("Madura_tinggi").child(idTinggi)
                                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                                    override fun onDataChange(snapshotTinggi: DataSnapshot) {
                                                        val tinggi = snapshotTinggi.getValue(MaduraTinggi::class.java) ?: return

                                                        val kosakata = KosakataFull(index++, dasar, menengah, tinggi, bahasa)
                                                        fullListData.add(kosakata)

                                                        loadedItems++
                                                        if (loadedItems == totalItems.toInt()) {
                                                            updateKosakataList()
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                        loadedItems++
                                                        if (loadedItems == totalItems.toInt()) updateKosakataList()
                                                    }
                                                })
                                        }

                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.loadingBar.visibility = View.GONE
                Toast.makeText(this@ListKosakata, "Gagal ambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateKosakataList() {
        fullListData.sortBy { it.dasar.kosakata.lowercase() }

        fullListData.forEachIndexed { i, item ->
            fullListData[i] = item.copy(nomor = i + 1)
        }

        listData.clear()
        listData.addAll(fullListData)
        adapter.notifyDataSetChanged()
        binding.loadingBar.visibility = View.GONE
    }




    data class KosakataFull(
        val nomor: Int,
        val dasar: MaduraDasar,
        val menengah: MaduraMenengah,
        val tinggi: MaduraTinggi,
        val indo: BahasaMadura
    )

    class KosakataAdapter(private val data: List<KosakataFull>, private val onPlayClicked : (String?) -> Unit, private val activity: Activity) : RecyclerView.Adapter<KosakataAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemKosakataBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemKosakataBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            holder.binding.tvNomor.text = item.nomor.toString()
            holder.binding.tvKosakata.text = item.dasar.kosakata
            holder.binding.tvIndo.text = item.indo.kosakata_indonesia

            holder.binding.btnEdit.setOnClickListener {
                Toast.makeText(holder.itemView.context, "Edit: ${item.dasar.kosakata}", Toast.LENGTH_SHORT).show()
            }

            holder.binding.btnDelete.setOnClickListener {
                Toast.makeText(holder.itemView.context, "Hapus: ${item.dasar.kosakata}", Toast.LENGTH_SHORT).show()
            }

            holder.binding.btnPlay1.setOnClickListener { onPlayClicked(item.dasar.audio_pelafalan) }
            holder.binding.btnPlay2.setOnClickListener { onPlayClicked(item.menengah.audio_pelafalan) }
            holder.binding.btnPlay3.setOnClickListener { onPlayClicked(item.tinggi.audio_pelafalan) }

            holder.binding.btnEdit.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, EditKosakata::class.java)
                intent.putExtra("id_dasar", item.dasar.id_dasar)
                intent.putExtra("id_menengah", item.menengah.id_menengah)
                intent.putExtra("id_tinggi", item.tinggi.id_tinggi)
                intent.putExtra("id_indo", item.indo.id)
                if (context is Activity) {
                    context.startActivityForResult(intent, 1001)
                }
            }

            holder.binding.btnDelete.setOnClickListener {
                val context = holder.itemView.context
                val db = FirebaseDatabase.getInstance().reference
                db.child("Madura_dasar").child(item.dasar.id_dasar).removeValue()
                db.child("Madura_menengah").child(item.menengah.id_menengah).removeValue()
                db.child("Madura_tinggi").child(item.tinggi.id_tinggi).removeValue()
                db.child("Kosakata_Indonesia").child(item.indo.kosakata_indonesia).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()

                        if (activity is ListKosakata) {
                            activity.fetchKosakata()
                        }
                    }
            }
        }
        override fun getItemCount() = data.size
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
                Toast.makeText(this@ListKosakata, "Gagal memutar audio", Toast.LENGTH_SHORT).show()
                true
            }
            prepareAsync()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            fetchKosakata()
        }
    }

}
