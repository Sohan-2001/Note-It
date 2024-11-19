package com.example.noteit

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotesAdapter
    private lateinit var tvEmptyMessage: TextView
    private lateinit var searchView: SearchView
    private var notesList = mutableListOf<Note>()
    private var filteredNotesList = mutableListOf<Note>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.parseColor("#FFA500")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        recyclerView = findViewById(R.id.recyclerView)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)
        searchView = findViewById(R.id.searchView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        findViewById<ImageView>(R.id.addButton).setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            startActivity(intent)
        }

        // Set up the search functionality
        setupSearchView()

        // Load notes asynchronously
        loadNotesAsync()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.trim() ?: ""
                filterNotes(query)
                return true
            }
        })
    }

    private fun filterNotes(query: String) {
        if (TextUtils.isEmpty(query)) {
            // Show all notes when query is empty
            filteredNotesList = notesList
        } else {
            // Filter notes by matching the query with the note title or content
            filteredNotesList = notesList.filter { note ->
                note.heading.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true)
            }.toMutableList()
        }

        // Refresh the RecyclerView
        adapter = NotesAdapter(filteredNotesList, this@MainActivity::onNoteClick, this@MainActivity::onDeleteClick)
        recyclerView.adapter = adapter

        // Show or hide the empty message
        tvEmptyMessage.visibility = if (filteredNotesList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadNotesAsync() {
        coroutineScope.launch {
            notesList = withContext(Dispatchers.IO) {
                loadNotes()
            }
            filteredNotesList = notesList
            adapter = NotesAdapter(filteredNotesList, this@MainActivity::onNoteClick, this@MainActivity::onDeleteClick)
            recyclerView.adapter = adapter

            // Show or hide the empty message
            tvEmptyMessage.visibility = if (notesList.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun loadNotes(): MutableList<Note> {
        val file = File(filesDir, "notes.json")
        if (file.exists()) {
            val type = object : TypeToken<MutableList<Note>>() {}.type
            return Gson().fromJson(file.readText(), type)
        }
        return mutableListOf()
    }

    private fun saveNotesAsync() {
        coroutineScope.launch(Dispatchers.IO) {
            saveNotes()
        }
    }

    private fun saveNotes() {
        val file = File(filesDir, "notes.json")
        file.writeText(Gson().toJson(notesList))
    }

    override fun onResume() {
        super.onResume()
        loadNotesAsync()
    }

    private fun onNoteClick(note: Note) {
        val intent = Intent(this, AddNoteActivity::class.java)
        intent.putExtra("note", note)
        startActivity(intent)
    }

    private fun onDeleteClick(note: Note) {
        notesList.remove(note)
        saveNotesAsync()
        filterNotes(searchView.query.toString()) // Apply the current filter

        // Update visibility of the empty message
        tvEmptyMessage.visibility = if (notesList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
