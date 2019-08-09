package com.uber.simplestore.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures

import com.uber.simplestore.NamespaceConfig
import com.uber.simplestore.proto.Demo
import com.uber.simplestore.proto.SimpleProtoStore
import com.uber.simplestore.proto.impl.SimpleProtoStoreFactory
import com.uber.simplestore.executors.StorageExecutors.mainExecutor as mainExecutor


/**
 * Store and retrieve a text field.
 */
@Suppress("UnstableApiUsage")
class KotlinActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private lateinit var simpleStore: SimpleProtoStore
    private lateinit var editText: EditText
    private lateinit var button: View
    private var namespace = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kotlin)
        namespace = intent.getIntExtra(NAMESPACE_EXTRA, 0)
        title = "Sample Namespace $namespace"
        textView = findViewById(R.id.activity_main_text)
        editText = findViewById(R.id.activity_main_edit)
        button = findViewById(R.id.activity_main_save)
        button.setOnClickListener { saveMessage() }
        findViewById<View>(R.id.activity_main_nest).setOnClickListener {
            val intent = Intent(this, KotlinActivity::class.java)
            intent.putExtra(NAMESPACE_EXTRA, namespace + 1)
            startActivity(intent)
        }
        findViewById<View>(R.id.activity_main_clear)
            .setOnClickListener {
                Futures.addCallback(simpleStore.deleteAll(), object : FutureCallback<Void> {
                    override fun onSuccess(msg: Void?) {
                        loadMessage()
                    }

                    override fun onFailure(t: Throwable) {
                        textView.text = t.toString()
                    }
                }, mainExecutor())
            }
        initialize()
    }

    private fun initialize() {
        val nesting = StringBuilder()
        for (i in 0 until namespace) {
            nesting.append("/nest")
        }
        Log.e("Test", nesting.toString())
        simpleStore = SimpleProtoStoreFactory.create(this, "main$nesting", NamespaceConfig.DEFAULT)
        loadMessage()
    }

    private fun saveMessage() {
        button.isEnabled = false
        editText.isEnabled = false
        val proto = Demo.Data.newBuilder().setField(editText.text.toString()).build()
        Futures.addCallback(
            simpleStore.put("some_thing", proto),
            object : FutureCallback<Demo.Data> {
                override fun onSuccess(s: Demo.Data?) {
                    editText.setText("")
                    button.isEnabled = true
                    editText.isEnabled = true
                    loadMessage()
                }

                override fun onFailure(t: Throwable) {
                    textView.text = t.toString()
                    button.isEnabled = true
                    editText.isEnabled = true
                }
            },
            mainExecutor()
        )
    }

    private fun loadMessage() {
        Futures.addCallback(simpleStore.get("some_thing", Demo.Data.parser()), object : FutureCallback<Demo.Data> {
            override fun onSuccess(msg: Demo.Data?) {
                textView.text = msg?.field
            }

            override fun onFailure(t: Throwable) {
                textView.text = t.toString()
            }
        }, mainExecutor())
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleStore.close()
    }

    companion object {
        private const val NAMESPACE_EXTRA = "namespace"
    }
}
