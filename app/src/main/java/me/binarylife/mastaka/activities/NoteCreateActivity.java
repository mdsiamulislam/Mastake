package me.binarylife.mastaka.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import me.binarylife.mastaka.R;
import me.binarylife.mastaka.models.Note;
import me.binarylife.mastaka.repository.NoteRepository;
import me.binarylife.mastaka.utils.IoTBackgroundSyncHelper;

public class NoteCreateActivity extends AppCompatActivity {

    private EditText etNoteTitle, etNoteContent;
    private MaterialButton btnSaveNote;
    private MaterialToolbar noteToolbar;
    private ProgressBar progressBar;
    private MaterialCheckBox checkboxAiSupport;

    private NoteRepository noteRepository;
    private String existingNoteId = null;
    private long existingCreatedAt = 0;
    private long existingVersion = 1;

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

        noteRepository = new NoteRepository(this);

        // Back Button
        noteToolbar.setNavigationOnClickListener(v -> finish());

        // Check if editing existing note
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("note_id")) {
            existingNoteId = intent.getStringExtra("note_id");
            String title = intent.getStringExtra("note_title");
            String content = intent.getStringExtra("note_content");
            boolean isAi = intent.getBooleanExtra("is_ai", false);

            etNoteTitle.setText(title);
            etNoteContent.setText(content);
            checkboxAiSupport.setChecked(isAi);
            noteToolbar.setTitle("Edit Note");
        }

        // Save Button Click
        btnSaveNote.setOnClickListener(v -> {
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

            if (checkboxAiSupport.isChecked()) {
                processNoteWithAi(title, content);
            } else {
                saveNoteLocally(title, content, checkboxAiSupport.isChecked());
            }
        });
    }

    private void processNoteWithAi(String rawTitle, String rawContent) {
        setLoading(true);
        Toast.makeText(this, "AI নোট গুছিয়ে দিচ্ছে...", Toast.LENGTH_SHORT).show();

        String apiKey = "YOUR_API_KEY";

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                                + apiKey
                );

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);

                String prompt =
                        "Act as a professional note writer. " +
                                "Rewrite and organize the following note into a clean, " +
                                "well-formatted and easy-to-read note. " +
                                "Preserve the original meaning and important information. " +
                                "Do not add unrelated information. " +
                                "Return only the organized note.\n\n" +
                                "Title: " + rawTitle + "\n\n" +
                                "Content: " + rawContent;

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

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                InputStream inputStream = (responseCode >= 200 && responseCode < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

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

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject responseJson = new JSONObject(response.toString());
                    JSONArray candidates = responseJson.getJSONArray("candidates");
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject responseContent = firstCandidate.getJSONObject("content");
                    JSONArray responseParts = responseContent.getJSONArray("parts");

                    StringBuilder aiText = new StringBuilder();
                    for (int i = 0; i < responseParts.length(); i++) {
                        JSONObject part = responseParts.getJSONObject(i);
                        if (part.has("text")) {
                            aiText.append(part.getString("text"));
                        }
                    }

                    String organizedNote = aiText.toString().trim();
                    if (organizedNote.isEmpty()) {
                        organizedNote = rawContent;
                    }

                    String finalOrganizedNote = organizedNote;
                    runOnUiThread(() -> saveNoteLocally(rawTitle, finalOrganizedNote, true));

                } else {
                    // Fallback to saving without AI if API fails/offline
                    runOnUiThread(() -> {
                        Toast.makeText(this, "AI unavailable (Offline). Saving original note.", Toast.LENGTH_LONG).show();
                        saveNoteLocally(rawTitle, rawContent, false);
                    });
                }

            } catch (Exception e) {
                // Fallback to saving without AI if offline or exception
                runOnUiThread(() -> {
                    Toast.makeText(this, "Offline mode: Saving original note without AI.", Toast.LENGTH_SHORT).show();
                    saveNoteLocally(rawTitle, rawContent, false);
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void saveNoteLocally(String title, String content, boolean isAiSupported) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            setLoading(false);
            Toast.makeText(this, "User not authenticated. Please login.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();
        boolean isNew = (existingNoteId == null);
        String noteId = isNew ? UUID.randomUUID().toString() : existingNoteId;

        Note note = new Note(
                noteId,
                userId,
                title,
                content,
                isAiSupported
        );

        if (!isNew) {
            note.setCreatedAt(existingCreatedAt != 0 ? existingCreatedAt : System.currentTimeMillis());
            note.setVersion(existingVersion + 1);
        }

        noteRepository.insertOrUpdateNote(note, isNew);

        // Auto try to send notes data to IoT device in background when adding/saving a note
        IoTBackgroundSyncHelper.syncToIoT(this);

        setLoading(false);
        Toast.makeText(this, "Note saved locally & queued for sync", Toast.LENGTH_SHORT).show();
        finish();
    }

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
