package com.uber.simplestore.sample.kotlin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView

import com.uber.simplestore.impl.SimpleStoreFactory
import com.uber.simplestore.ScopeConfig
import com.uber.simplestore.SimpleStore


class KotlinActivity : Activity() {
    private lateinit var textView: TextView
    private lateinit var simpleStore: SimpleStore
    private lateinit var editText: EditText
    private lateinit var button: View
    private var scope = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kotlin)
        scope = intent.getIntExtra(SCOPE_EXTRA, 0)
        title = "Sample Scope $scope"
        textView = findViewById(R.id.activity_main_text)
        editText = findViewById(R.id.activity_main_edit)
        button = findViewById(R.id.activity_main_save)
        button.setOnClickListener { saveMessage() }
        findViewById<View>(R.id.activity_main_nest).setOnClickListener {
            val intent = Intent(this, KotlinActivity::class.java)
            intent.putExtra(Companion.SCOPE_EXTRA, scope + 1)
            startActivity(intent)
        }
        findViewById<View>(R.id.activity_main_clear)
                .setOnClickListener {
                    simpleStore.deleteAll(object : SimpleStore.Callback<Void> {
                        override fun onSuccess(msg: Void?) {
                            loadMessage()
                        }

                        override fun onError(t: Throwable) {
                            textView.text = t.toString()
                        }
                    }, mainExecutor)
                }
        initialize()
    }

    private fun initialize() {
        val nesting = StringBuilder()
        for (i in 0 until scope) {
            nesting.append("/nest")
        }
        Log.e("Test", nesting.toString())
        simpleStore = SimpleStoreFactory.create(this, "main" + nesting.toString(), ScopeConfig.DEFAULT)
        loadMessage()
    }

    private fun saveMessage() {
        button.isEnabled = false
        editText.isEnabled = false
        simpleStore.putString("some_thing", editText.text.toString(), object : SimpleStore.Callback<String> {
            override fun onSuccess(s: String?) {
                editText.setText("")
                button.isEnabled = true
                editText.isEnabled = true
                loadMessage()
            }

            override fun onError(t: Throwable) {
                textView.text = t.toString()
                button.isEnabled = true
                editText.isEnabled = true
            }
        }, mainExecutor)
    }

    private fun loadMessage() {
        simpleStore.getString("some_thing", object : SimpleStore.Callback<String> {
            override fun onSuccess(msg: String?) {
                textView.text = msg
            }

            override fun onError(t: Throwable) {
                textView.text = t.toString()
            }
        }, mainExecutor)
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleStore.close()
    }

    companion object {
        private const val SCOPE_EXTRA = "scope"
    }
}
