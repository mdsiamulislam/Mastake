package me.binarylife.mastaka;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class NoteCreateActivity extends AppCompatActivity {

    private EditText etNoteTitle, etNoteContent;
    private MaterialButton btnSaveNote;
    private MaterialToolbar noteToolbar;
    private ProgressBar progressBar;
    private MaterialCheckBox checkboxAiSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_create);

        noteToolbar = findViewById(R.id.noteToolbar);
        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        btnSaveNote = findViewById(R.id.btnSaveNote);
        progressBar = findViewById(R.id.progressBar);
        checkboxAiSupport = findViewById(R.id.checkboxAiSupport);

        // Back Button
        noteToolbar.setNavigationOnClickListener(v -> finish());

        // Save Button Click
        btnSaveNote.setOnClickListener(v -> {
            if (checkboxAiSupport.isChecked()) {
                saveNoteWithAiSupport();
            } else {
                saveNoteWithoutAiSupport();
            }
        });
    }


    private void saveNoteWithAiSupport() {

        String rawTitle = etNoteTitle.getText().toString().trim();
        String rawContent = etNoteContent.getText().toString().trim();

        if (rawTitle.isEmpty() || rawContent.isEmpty()) {
            Toast.makeText(
                    this,
                    "দয়া করে টাইটেল এবং কিছু লিখুন",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        setLoading(true);

        Toast.makeText(
                this,
                "AI নোট গুছিয়ে দিচ্ছে...",
                Toast.LENGTH_SHORT
        ).show();

        // Testing only. Never keep API keys inside a production APK.
        String apiKey = "AQ.Ab8RN6LFPuHhqwWGYZm17u-s2QubuMp6ac09NaC58d7E0q9dnA";

        new Thread(() -> {

            HttpURLConnection conn = null;

            try {

                URL url = new URL(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key="
                                + apiKey
                );

                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);

                // Create AI prompt
                String prompt =
                        "Act as a professional note writer. " +
                                "Rewrite and organize the following note into a clean, " +
                                "well-formatted and easy-to-read note. " +
                                "Preserve the original meaning and important information. " +
                                "Do not add unrelated information. " +
                                "Return only the organized note.\n\n" +
                                "Title: " + rawTitle + "\n\n" +
                                "Content: " + rawContent;

                // Build JSON request
                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);

                JSONArray parts = new JSONArray();
                parts.put(textPart);

                JSONObject contentObject = new JSONObject();
                contentObject.put("parts", parts);

                JSONArray contents = new JSONArray();
                contents.put(contentObject);

                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contents);

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();

                InputStream inputStream;

                if (responseCode >= 200 && responseCode < 300) {
                    inputStream = conn.getInputStream();
                } else {
                    inputStream = conn.getErrorStream();
                }

                StringBuilder response = new StringBuilder();

                if (inputStream != null) {

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                String responseString = response.toString();

                if (responseCode >= 200 && responseCode < 300) {

                    // Extract AI-generated text
                    JSONObject responseJson = new JSONObject(responseString);

                    JSONArray candidates = responseJson.getJSONArray("candidates");

                    JSONObject firstCandidate = candidates.getJSONObject(0);

                    JSONObject responseContent =
                            firstCandidate.getJSONObject("content");

                    JSONArray responseParts =
                            responseContent.getJSONArray("parts");

                    StringBuilder aiText = new StringBuilder();

                    for (int i = 0; i < responseParts.length(); i++) {

                        JSONObject part = responseParts.getJSONObject(i);

                        if (part.has("text")) {
                            aiText.append(part.getString("text"));
                        }
                    }

                    String organizedNote = aiText.toString().trim();

                    if (organizedNote.isEmpty()) {
                        throw new Exception("AI returned an empty note");
                    }

                    runOnUiThread(() -> {

                        setLoading(false);

                        // Save organized note to Firestore
                        saveNoteToFirestore(rawTitle, organizedNote);

                    });

                } else {

                    runOnUiThread(() -> {

                        setLoading(false);
                        System.err.println("API Error " + responseCode + ": " + responseString);

                        Toast.makeText(
                                NoteCreateActivity.this,
                                "API Error " + responseCode + ": " + responseString,
                                Toast.LENGTH_LONG
                        ).show();

                    });
                }

            } catch (Exception e) {

                runOnUiThread(() -> {

                    setLoading(false);

                    Toast.makeText(
                            NoteCreateActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });

            } finally {

                if (conn != null) {
                    conn.disconnect();
                }
            }

        }).start();
    }


    private void saveNoteWithoutAiSupport() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty()) {
            etNoteTitle.setError("Please enter note title");
            etNoteTitle.requestFocus();
            return;
        }

        if (content.isEmpty()) {
            etNoteContent.setError("Please enter note content");
            etNoteContent.requestFocus();
            return;
        }

        setLoading(true);
        saveNoteToFirestore(title, content);
    }

    private void saveNoteToFirestore(String title, String content) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firebasedb = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            Map<String, Object> note = Map.of(
                    "title", title,
                    "content", content,
                    "lastEdited", System.currentTimeMillis()
            );

            firebasedb.collection("users").document(userId).collection("notes")
                    .add(note)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(NoteCreateActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(NoteCreateActivity.this, "Error saving note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            setLoading(false);
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AuthActivity.class));
        }
    }

    // প্রোগ্রেসবার এবং বাটন কন্ট্রোল করার জন্য হেল্পার মেথড
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            btnSaveNote.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnSaveNote.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
}