package com.zen.e_learning_bahasa_madura.view.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.databinding.ActivityListKosakataBinding
import com.zen.e_learning_bahasa_madura.databinding.ItemKosakataBinding
import com.zen.e_learning_bahasa_madura.model.BahasaMadura
import com.zen.e_learning_bahasa_madura.model.MaduraDasar
import com.zen.e_learning_bahasa_madura.model.MaduraMenengah
import com.zen.e_learning_bahasa_madura.model.MaduraTinggi
import com.zen.e_learning_bahasa_madura.util.NavHelper

class ListKosakata : Activity() {

    private lateinit var binding: ActivityListKosakataBinding
    private lateinit var database: DatabaseReference
    private val fullListData = mutableListOf<KosakataFull>()
    private val listData = mutableListOf<KosakataFull>()
    private lateinit var adapter: KosakataAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListKosakataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference
        binding.rvKosakata.layoutManager = LinearLayoutManager(this)
        adapter = KosakataAdapter(listData)
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
        binding.loadingBar.visibility = android.view.View.VISIBLE

        database.child("Bahasa_madura").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listData.clear()
                var index = 1
                for (data in snapshot.children) {
                    val bahasa = data.getValue(BahasaMadura::class.java)
                    if (bahasa != null) {
                        val idDasar = bahasa.id_dasar
                        val idMenengah = bahasa.id_menengah
                        val idTinggi = bahasa.id_tinggi

                        database.child("Madura_dasar").child(idDasar).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshotDasar: DataSnapshot) {
                                val dasar = snapshotDasar.getValue(MaduraDasar::class.java)
                                if (dasar != null) {
                                    database.child("Madura_menengah").child(idMenengah).addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshotMenengah: DataSnapshot) {
                                            val menengah = snapshotMenengah.getValue(MaduraMenengah::class.java)
                                            if (menengah != null) {
                                                database.child("Madura_tinggi").child(idTinggi).addListenerForSingleValueEvent(object : ValueEventListener {
                                                    override fun onDataChange(snapshotTinggi: DataSnapshot) {
                                                        val tinggi = snapshotTinggi.getValue(MaduraTinggi::class.java)
                                                        if (tinggi != null) {
                                                            val item = KosakataFull(index++, dasar, menengah, tinggi, bahasa)
                                                            fullListData.add(item)
                                                            listData.add(item)
                                                            adapter.notifyItemInserted(listData.size - 1)
                                                        }

                                                        if (index > snapshot.childrenCount) {
                                                            binding.loadingBar.visibility = android.view.View.GONE
                                                        }
                                                    }
                                                    override fun onCancelled(error: DatabaseError) {}
                                                })
                                            }
                                        }
                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                    }
                }

                binding.loadingBar.visibility = View.GONE
                filterList(binding.searchView.query.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                binding.loadingBar.visibility = android.view.View.GONE
                Toast.makeText(this@ListKosakata, "Gagal ambil data", Toast.LENGTH_SHORT).show()
            }
        })

        listData.clear()
        listData.addAll(fullListData)
        adapter.notifyDataSetChanged()
    }

    data class KosakataFull(
        val nomor: Int,
        val dasar: MaduraDasar,
        val menengah: MaduraMenengah,
        val tinggi: MaduraTinggi,
        val indo: BahasaMadura
    )

    class KosakataAdapter(private val data: List<KosakataFull>) : RecyclerView.Adapter<KosakataAdapter.ViewHolder>() {

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

            holder.binding.btnEdit.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, EditKosakata::class.java)
                intent.putExtra("id_dasar", item.dasar.id_dasar)
                intent.putExtra("id_menengah", item.menengah.id_menengah)
                intent.putExtra("id_tinggi", item.tinggi.id_tinggi)
                intent.putExtra("kosakata_indo", item.indo.kosakata_indonesia)
                context.startActivity(intent)
            }

            holder.binding.btnDelete.setOnClickListener {
                val context = holder.itemView.context
                val db = FirebaseDatabase.getInstance().reference
                db.child("Madura_dasar").child(item.dasar.id_dasar).removeValue()
                db.child("Madura_menengah").child(item.menengah.id_menengah).removeValue()
                db.child("Madura_tinggi").child(item.tinggi.id_tinggi).removeValue()
                db.child("Kosakata_Indonesia").child(item.indo.kosakata_indonesia).removeValue()
                Toast.makeText(context, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
        }

        override fun getItemCount() = data.size
    }
}
