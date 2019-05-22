/*
 * Copyright (C) 2019. Uber Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uber.simplestore.sample;

import static com.uber.simplestore.executors.StorageExecutors.mainExecutor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.uber.simplestore.ScopeConfig;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.impl.SimpleStoreFactory;

public class JavaActivity extends AppCompatActivity {

  private static final String SCOPE_EXTRA = "scope";
  private TextView textView;
  private SimpleStore simpleStore;
  private EditText editText;
  private View button;
  int scope = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_java);
    scope = getIntent().getIntExtra(SCOPE_EXTRA, 0);
    setTitle("Sample Scope " + scope);
    textView = findViewById(R.id.activity_main_text);
    editText = findViewById(R.id.activity_main_edit);
    button = findViewById(R.id.activity_main_save);
    button.setOnClickListener((v) -> saveMessage());
    findViewById(R.id.activity_main_nest)
        .setOnClickListener(
            (v) -> {
              Intent intent = new Intent(this, JavaActivity.class);
              intent.putExtra(SCOPE_EXTRA, scope + 1);
              startActivity(intent);
            });
    findViewById(R.id.activity_main_clear)
        .setOnClickListener(
            (v) ->
                Futures.addCallback(
                    simpleStore.deleteAll(),
                    new FutureCallback<Void>() {
                      @Override
                      public void onSuccess(@NonNull Void result) {
                        loadMessage();
                      }

                      @Override
                      public void onFailure(Throwable t) {
                        textView.setText(t.toString());
                      }
                    },
                    mainExecutor()));
    initialize();
  }

  private void initialize() {
    StringBuilder nesting = new StringBuilder();
    for (int i = 0; i < scope; i++) {
      nesting.append("/nest");
    }
    Log.w("Nesting: ", nesting.toString());
    simpleStore = SimpleStoreFactory.create(this, "main" + nesting.toString(), ScopeConfig.DEFAULT);
    loadMessage();
  }

  private void saveMessage() {
    button.setEnabled(false);
    editText.setEnabled(false);
    ListenableFuture<String> put =
        simpleStore.putString("some_thing", editText.getText().toString());
    Futures.addCallback(
        put,
        new FutureCallback<String>() {
          @Override
          public void onSuccess(@NonNull String s) {
            editText.setText("");
            button.setEnabled(true);
            editText.setEnabled(true);
            loadMessage();
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e("JavaActivity", "Save failure", t);
            textView.setText(t.toString());
            button.setEnabled(true);
            editText.setEnabled(true);
          }
        },
        mainExecutor());
  }

  private void loadMessage() {
    Futures.addCallback(
        simpleStore.getString("some_thing"),
        new FutureCallback<String>() {
          @Override
          public void onSuccess(@NonNull String msg) {
            textView.setText(msg);
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e("JavaActivity", "Load failure", t);
            textView.setText(t.toString());
          }
        },
        mainExecutor());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    simpleStore.close();
  }
}
