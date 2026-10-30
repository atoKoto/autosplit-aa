package com.custom.autosplit

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputSearch = findViewById<EditText>(R.id.inputSearch)
        val btnSend = findViewById<Button>(R.id.btnSend)
        val btnMode1 = findViewById<Button>(R.id.btnMode1)
        val btnMode2 = findViewById<Button>(R.id.btnMode2)
        val btnMode3 = findViewById<Button>(R.id.btnMode3)

        fun sendSearchQuery() {
            val queryText = inputSearch.text.toString().trim()
            if (queryText.isNotEmpty()) {
                val intent = Intent("com.custom.autosplit.LOAD_URL").apply {
                    putExtra("query", queryText)
                }
                sendBroadcast(intent)
                inputSearch.text.clear()
            }
        }

        fun switchDisplayMode(modeKey: String) {
            val intent = Intent("com.custom.autosplit.SET_MODE").apply {
                putExtra("mode", modeKey)
            }
            sendBroadcast(intent)
        }

        btnSend.setOnClickListener { sendSearchQuery() }
        inputSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                sendSearchQuery()
                true
            } else {
                false
            }
        }

        btnMode1.setOnClickListener { switchDisplayMode("MODE_1") }
        btnMode2.setOnClickListener { switchDisplayMode("MODE_2") }
        btnMode3.setOnClickListener { switchDisplayMode("MODE_3") }
    }
}
