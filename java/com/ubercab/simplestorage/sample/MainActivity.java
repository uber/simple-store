package com.ubercab.simplestorage.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.ubercab.simplestorage.impl.SimpleStoreImplFactory;
import com.ubercab.simplestorage.ScopeConfig;
import com.ubercab.simplestorage.SimpleStore;


public class MainActivity extends Activity {

    private static final String SCOPE_EXTRA = "scope";
    private TextView textView;
    private SimpleStore simpleStore;
    private EditText editText;
    private View button;
    int scope = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scope = getIntent().getIntExtra(SCOPE_EXTRA, 0);
        setTitle("Sample Scope "+ scope);
        textView = findViewById(R.id.activity_main_text);
        editText = findViewById(R.id.activity_main_edit);
        button = findViewById(R.id.activity_main_save);
        button.setOnClickListener((v) -> saveMessage());
        findViewById(R.id.activity_main_nest).setOnClickListener((v) -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(SCOPE_EXTRA, scope + 1);
            startActivity(intent);
        });
        findViewById(R.id.activity_main_clear)
                .setOnClickListener((v) -> simpleStore.deleteAll(new SimpleStore.Callback<Void>() {
            @Override
            public void onSuccess(Void msg) {
                loadMessage();
            }

            @Override
            public void onError(Throwable t) {
                textView.setText(t.toString());
            }
        }, getMainExecutor()));
        initialize();
    }

    private void initialize() {
        StringBuilder nesting = new StringBuilder();
        for (int i = 0; i< scope; i++) {
            nesting.append("/nest");
        }
        Log.e("Test", nesting.toString());
        simpleStore = SimpleStoreImplFactory.get(this, "main" + nesting.toString(), ScopeConfig.DEFAULT);
        loadMessage();
    }

    private void saveMessage() {
        button.setEnabled(false);
        editText.setEnabled(false);
        simpleStore.putString("some_thing", editText.getText().toString(), new SimpleStore.Callback<String>() {
            @Override
            public void onSuccess(String s) {
                editText.setText("");
                button.setEnabled(true);
                editText.setEnabled(true);
                loadMessage();
            }

            @Override
            public void onError(Throwable t) {
                textView.setText(t.toString());
                button.setEnabled(true);
                editText.setEnabled(true);
            }
        }, getMainExecutor());
    }

    private void loadMessage() {
        simpleStore.getString("some_thing", new SimpleStore.Callback<String>() {
            @Override
            public void onSuccess(String msg) {
                textView.setText(msg);
            }

            @Override
            public void onError(Throwable t) {
                textView.setText(t.toString());
            }
        }, getMainExecutor());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        simpleStore.close();
    }
}
