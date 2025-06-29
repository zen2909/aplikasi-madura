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
import com.zen.e_learning_bahasa_madura.databinding.ItemEvalBinding
import com.zen.e_learning_bahasa_madura.databinding.ListEvaluasiBinding
import com.zen.e_learning_bahasa_madura.util.NavHelper

class SoalEvaluasi : Activity() {

    private lateinit var binding: ListEvaluasiBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: EvalAdapter
    private val fullListData = mutableListOf<EvalPilgan>()
    private val soalList = mutableListOf<EvalPilgan>()

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
        database.child("evaluasi_pilgan")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    soalList.clear()
                    fullListData.clear()

                    val tempList = snapshot.children.mapNotNull {
                        it.getValue(EvalPilgan::class.java)
                    }

                    val sortedList = tempList.sortedBy { it.soal.lowercase() }

                    val numberedList = sortedList.mapIndexed { index, soal ->
                        soal.copy(nomor = index + 1)
                    }

                    fullListData.addAll(numberedList)
                    soalList.addAll(numberedList)

                    binding.rvSoal.postDelayed({
                        adapter.notifyDataSetChanged()
                        binding.loadingBar.visibility = View.GONE
                    }, 300)
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.loadingBar.visibility = View.GONE
                    Toast.makeText(this@SoalEvaluasi, "Gagal ambil soal", Toast.LENGTH_SHORT).show()
                }
            })
    }

    data class EvalPilgan(
        val id_evalpilgan: String = "",
        val nomor: Int = 0,
        val soal: String = "",
        val kategori: String = "",
        val jwb_1: String = "",
        val jwb_2: String = "",
        val jwb_3: String = "",
        val jwb_4: String = "",
        val jwb_benar: String = "",
        val bobot: String = ""
    )

    class EvalAdapter(private val data: List<EvalPilgan> , private val activity: Activity) : RecyclerView.Adapter<EvalAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemEvalBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemEvalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            holder.binding.tvNomor.text = item.nomor.toString()
            holder.binding.tvsoal.text = item.soal
            holder.binding.tvkategori.text = item.kategori

            holder.binding.btnEdit.setOnClickListener {
                Toast.makeText(holder.itemView.context, "Edit: ${item.soal}", Toast.LENGTH_SHORT).show()
            }

            holder.binding.btnDelete.setOnClickListener {
                Toast.makeText(holder.itemView.context, "Hapus: ${item.soal}", Toast.LENGTH_SHORT).show()
            }

            holder.binding.btnEdit.setOnClickListener {
                val context = holder.itemView.context

                val targetClass = when (item.kategori) {
                    "Terjemahan" -> EditEvalTerjemahan::class.java
                    "Tingkat Bahasa" -> EditEvalTb::class.java
                    "Pelafalan" -> EditEvalPelafalan::class.java
                    else -> EditEvalTerjemahan::class.java
                }

                val intent = Intent(context, targetClass).apply {
                    putExtra("id_evalpilgan", item.id_evalpilgan)
                    putExtra("soal", item.soal)
                    putExtra("jwb_1", item.jwb_1)
                    putExtra("jwb_2", item.jwb_2)
                    putExtra("jwb_3", item.jwb_3)
                    putExtra("jwb_4", item.jwb_4)
                    putExtra("jwb_benar", item.jwb_benar)
                    putExtra("bobot", item.bobot)
                }

                if (context is Activity) {
                    context.startActivityForResult(intent, 1001)
                }
            }


            holder.binding.btnDelete.setOnClickListener {
                val context = holder.itemView.context
                val db = FirebaseDatabase.getInstance().reference
                db.child("evaluasi_pilgan").child(item.id_evalpilgan).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()

                        if (activity is SoalEvaluasi) {
                            activity.fetchSoal()
                        }
                    }
            }
        }

        override fun getItemCount(): Int = data.size
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            fetchSoal()
        }
    }
}
