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
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import com.zen.e_learning_bahasa_madura.databinding.ItemEvalBinding
import com.zen.e_learning_bahasa_madura.databinding.ListEvaluasiBinding
import com.zen.e_learning_bahasa_madura.util.NavHelper

class SoalEvaluasi : Activity() {

    private lateinit var binding: ListEvaluasiBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: EvalAdapter
    private val fullListData = mutableListOf<SoalEvaluasiItem>()
    private val soalList = mutableListOf<SoalEvaluasiItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ListEvaluasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference
        adapter = EvalAdapter(soalList, this)
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
        fetchSoal()

        binding.btnRefresh.setOnClickListener {
            soalList.clear()
            adapter.notifyDataSetChanged()
            fetchSoal()
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
                it.soal.contains(query, ignoreCase = true) ||
                        it.kategori.contains(query, ignoreCase = true)
            }
        }
        soalList.clear()
        soalList.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun fetchSoal() {
        binding.loadingBar.visibility = View.VISIBLE
        soalList.clear()
        fullListData.clear()

        val db = FirebaseDatabase.getInstance().reference

        db.child("evaluasi").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    binding.loadingBar.visibility = View.GONE
                    return
                }

                val tasks = mutableListOf<Task<DataSnapshot>>()
                val metaData = mutableListOf<Pair<String, String>>() // kategori to idEval

                snapshot.children.forEach { eval ->
                    val idPilgan = eval.child("id_pilgan").getValue(String::class.java)
                    val idPelafalan = eval.child("id_pelafalan").getValue(String::class.java)

                    if (!idPilgan.isNullOrEmpty()) {
                        tasks.add(db.child("evaluasi_pilgan").child(idPilgan).get())
                        metaData.add("Pilgan" to idPilgan)
                    }

                    if (!idPelafalan.isNullOrEmpty()) {
                        tasks.add(db.child("evaluasi_pelafalan").child(idPelafalan).get())
                        metaData.add("Pelafalan" to idPelafalan)
                    }
                }

                Tasks.whenAllSuccess<DataSnapshot>(tasks)
                    .addOnSuccessListener { results ->
                        val resultSoal = mutableListOf<SoalEvaluasiItem>()

                        results.forEachIndexed { index, dataSnap ->
                            val (fallbackKategori, idEval) = metaData[index]
                            val soal = dataSnap.child("soal").getValue(String::class.java) ?: ""
                            val kategori = dataSnap.child("kategori").getValue(String::class.java) ?: fallbackKategori

                            resultSoal.add(
                                SoalEvaluasiItem(
                                    nomor = index + 1,
                                    kategori = kategori,
                                    soal = soal,
                                    idEval = idEval
                                )
                            )
                        }

                        val finalList = resultSoal.sortedBy { it.soal.lowercase() }
                            .mapIndexed { i, item -> item.copy(nomor = i + 1) }

                        soalList.addAll(finalList)
                        fullListData.addAll(finalList)

                        binding.rvSoal.postDelayed({
                            adapter.notifyDataSetChanged()
                            binding.loadingBar.visibility = View.GONE
                        }, 300)
                    }
                    .addOnFailureListener {
                        binding.loadingBar.visibility = View.GONE
                        Toast.makeText(this@SoalEvaluasi, "Gagal memuat soal", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.loadingBar.visibility = View.GONE
                Toast.makeText(this@SoalEvaluasi, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    data class SoalEvaluasiItem(
        val nomor: Int = 0,
        val kategori: String = "",
        val soal: String = "",
        val idEval: String = ""
    )

    class EvalAdapter(
        private val data: List<SoalEvaluasiItem>,
        private val activity: Activity
    ) : RecyclerView.Adapter<EvalAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemEvalBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemEvalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]

            holder.binding.tvNomor.text = item.nomor.toString()
            holder.binding.tvsoal.text = item.soal
            holder.binding.tvkategori.text = item.kategori

            holder.binding.btnEdit.setOnClickListener {
                val context = holder.itemView.context
                val intent = when (item.kategori) {
                    "Pelafalan" -> Intent(context, EditEvalPelafalan::class.java)
                    "Tingkat Bahasa" -> Intent(context, EditEvalTb::class.java)
                    else -> Intent(context, EditEvalTerjemahan::class.java)
                }
                intent.putExtra("id_evalpilgan", item.idEval)
                if (context is Activity) {
                    context.startActivityForResult(intent, 1001)
                }
            }

            holder.binding.btnDelete.setOnClickListener {
                val db = FirebaseDatabase.getInstance().reference
                val context = holder.itemView.context

                val path = when (item.kategori) {
                    "Pelafalan" -> "evaluasi_pelafalan"
                    else -> "evaluasi_pilgan"
                }

                db.child(path).child(item.idEval).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Soal berhasil dihapus", Toast.LENGTH_SHORT).show()
                        if (activity is SoalEvaluasi) {
                            activity.fetchSoal()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal menghapus soal", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            fetchSoal()
        }
    }
}
