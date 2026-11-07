package me.binarylife.mastaka.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
import me.binarylife.mastaka.utils.UiMessage;

public class NoteCreateActivity extends AppCompatActivity {

    private static final String GEMINI_MODEL = "gemini-3.5-flash-lite";

    private EditText etNoteTitle, etNoteContent;
    private MaterialButton btnSaveNote;
    private MaterialToolbar noteToolbar;
    private ProgressBar progressBar;
    private MaterialCheckBox checkboxAiSupport;
    private MaterialCardView cardAiSupport;

    private NoteRepository noteRepository;
    private String existingNoteId = null;
    private long existingCreatedAt = 0;
    private long existingVersion = 1;

    private String saveLabel = "Save Note";
    private boolean isSaving = false;

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
        cardAiSupport = findViewById(R.id.cardAiSupport);

        noteRepository = new NoteRepository(this);

        // Back Button
        noteToolbar.setNavigationOnClickListener(v -> finish());

        cardAiSupport.setOnClickListener(v -> checkboxAiSupport.toggle());

        // Check if editing existing note
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("note_id")) {
            existingNoteId = intent.getStringExtra("note_id");
            String title = intent.getStringExtra("note_title");
            String content = intent.getStringExtra("note_content");
            boolean isAi = intent.getBooleanExtra("is_ai", false);

            existingCreatedAt = intent.getLongExtra("note_created_at", 0);
            existingVersion = intent.getLongExtra("note_version", 1);

            etNoteTitle.setText(title);
            etNoteContent.setText(content);
            checkboxAiSupport.setChecked(isAi);
            noteToolbar.setTitle("Edit Note");

            saveLabel = "Update Note";
            btnSaveNote.setText(saveLabel);
        }

        // Save Button Click
        btnSaveNote.setOnClickListener(v -> {
            if (isSaving) return;

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

            hideKeyboard();

            if (checkboxAiSupport.isChecked()) {
                processNoteWithAi(title, content);
            } else {
                saveNoteLocally(title, content, false);
            }
        });
    }

    private void processNoteWithAi(String rawTitle, String rawContent) {

        // TODO: push করার আগে এই key মুছে দিন
        final String apiKey = "";
        final String model = "gemini-3.5-flash-lite";

        if (apiKey.equals("PASTE_NEW_KEY_HERE") || apiKey.trim().isEmpty()) {
            UiMessage.showError(btnSaveNote, "AI key missing. Paste your API key in the code.");
            return;
        }

        setLoading(true, "AI is writing…");

        new Thread(() -> {
            String aiResult = null;   // সফল হলে AI-র লেখা
            String errorMsg = null;   // fail হলে কারণ

            // সাময়িক সমস্যা (429/500/503) হলে একবার আবার চেষ্টা
            for (int attempt = 1; attempt <= 2 && aiResult == null; attempt++) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":generateContent");

                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("x-goog-api-key", apiKey);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(45000);
                    conn.setDoOutput(true);

                    String prompt =
                            "Act as a professional note writer. "
                                    + "Rewrite and organize the following note into a clean, "
                                    + "well-formatted and easy-to-read note. "
                                    + "Preserve the original meaning and important information. "
                                    + "Keep the same language as the original note. "
                                    + "Do not add unrelated information. "
                                    + "Return only the organized note, without any extra comments.\n\n"
                                    + "Title: " + rawTitle + "\n\n"
                                    + "Content: " + rawContent;

                    JSONObject textPart = new JSONObject().put("text", prompt);
                    JSONObject contentObject = new JSONObject()
                            .put("parts", new JSONArray().put(textPart));
                    JSONObject requestBody = new JSONObject()
                            .put("contents", new JSONArray().put(contentObject));

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }

                    int code = conn.getResponseCode();
                    boolean ok = code >= 200 && code < 300;
                    InputStream stream = ok ? conn.getInputStream() : conn.getErrorStream();

                    StringBuilder sb = new StringBuilder();
                    if (stream != null) {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                        }
                    }
                    String body = sb.toString();
                    Log.d("NoteCreate", "Gemini http=" + code + " attempt=" + attempt);

                    if (!ok) {
                        Log.e("NoteCreate", "Gemini error body: " + body);

                        boolean retryable = (code == 429 || code == 500 || code == 503);
                        if (retryable && attempt < 2) {
                            Thread.sleep(2000);
                            continue;
                        }

                        String apiMsg = "";
                        try {
                            apiMsg = new JSONObject(body).getJSONObject("error").getString("message");
                        } catch (Exception ignored) { }

                        switch (code) {
                            case 400: errorMsg = "AI request rejected (400). Check API key and model. " + apiMsg; break;
                            case 401:
                            case 403: errorMsg = "AI key not allowed (" + code + "). Check the key."; break;
                            case 404: errorMsg = "AI model not found (404). Check the model name."; break;
                            case 429: errorMsg = "AI limit reached (429). Wait a minute and try again."; break;
                            case 500:
                            case 503: errorMsg = "AI server is busy (" + code + "). Try again shortly."; break;
                            default:  errorMsg = "AI failed (" + code + "). " + apiMsg; break;
                        }
                        break;
                    }

                    // সফল response পড়া
                    JSONObject json = new JSONObject(body);

                    if (json.has("promptFeedback")
                            && json.getJSONObject("promptFeedback").has("blockReason")) {
                        errorMsg = "AI blocked this text ("
                                + json.getJSONObject("promptFeedback").getString("blockReason") + ")";
                        break;
                    }

                    JSONArray candidates = json.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        errorMsg = "AI returned no result. Try again.";
                        break;
                    }

                    JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                    JSONArray parts = content != null ? content.optJSONArray("parts") : null;

                    StringBuilder text = new StringBuilder();
                    if (parts != null) {
                        for (int i = 0; i < parts.length(); i++) {
                            JSONObject part = parts.getJSONObject(i);
                            if (part.optBoolean("thought", false)) continue;
                            if (part.has("text")) text.append(part.getString("text"));
                        }
                    }

                    String result = text.toString().trim();
                    if (result.isEmpty()) {
                        errorMsg = "AI returned an empty answer. Try again.";
                        break;
                    }
                    aiResult = result;

                } catch (java.net.UnknownHostException | java.net.ConnectException e) {
                    errorMsg = "No internet connection. Connect and try again.";
                    break;
                } catch (java.net.SocketTimeoutException e) {
                    errorMsg = "AI took too long to respond. Try again.";
                } catch (Exception e) {
                    Log.e("NoteCreate", "Gemini exception", e);
                    errorMsg = "AI error: " + e.getClass().getSimpleName();
                    break;
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }

            final String finalResult = aiResult;
            final String finalError = (errorMsg != null) ? errorMsg : "AI failed. Try again.";

            runOnUiThread(() -> {
                if (isFinishing()) return;

                if (finalResult != null) {
                    // AI সফল হলেই save হবে
                    saveNoteLocally(rawTitle, finalResult, true);
                } else {
                    // Save হবে না। লেখা থাকবে, user আবার চেষ্টা করতে পারবে
                    setLoading(false, null);
                    UiMessage.showError(btnSaveNote, finalError);
                }
            });
        }).start();
    }

    private void saveNoteLocally(String title, String content, boolean isAiSupported) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            setLoading(false, null);
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

        IoTBackgroundSyncHelper.syncToIoT(this);

        setLoading(false, null);
        Toast.makeText(this, "Note saved locally & queued for sync", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setLoading(boolean isLoading, String loadingLabel) {
        isSaving = isLoading;

        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveNote.setEnabled(!isLoading);
        btnSaveNote.setText(isLoading && loadingLabel != null ? loadingLabel : saveLabel);

        etNoteTitle.setEnabled(!isLoading);
        etNoteContent.setEnabled(!isLoading);
        cardAiSupport.setEnabled(!isLoading);
        checkboxAiSupport.setEnabled(!isLoading);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
