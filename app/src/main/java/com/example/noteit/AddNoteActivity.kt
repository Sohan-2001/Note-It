package com.example.noteit

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class AddNoteActivity : AppCompatActivity() {
    private lateinit var etHeading: EditText
    private lateinit var etContent: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.parseColor("#FFA500")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        etHeading = findViewById(R.id.etHeading)
        etContent = findViewById(R.id.etContent)

        val note = intent.getParcelableExtra<Note>("note")
        note?.let {
            etHeading.setText(it.heading)
            etContent.setText(it.content)
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val newNote = Note(
                heading = etHeading.text.toString(),
                content = etContent.text.toString()
            )

            val notesList = loadNotes()
            if (note != null) {
                notesList.remove(note)
            }
            notesList.add(newNote)
            saveNotes(notesList)
            finish()
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

    private fun saveNotes(notesList: MutableList<Note>) {
        val file = File(filesDir, "notes.json")
        file.writeText(Gson().toJson(notesList))
    }
}
