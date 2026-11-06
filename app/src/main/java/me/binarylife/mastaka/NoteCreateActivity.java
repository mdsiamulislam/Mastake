
package me.binarylife.mastaka;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class NoteCreateActivity extends AppCompatActivity {

    private EditText etNoteTitle, etNoteContent;
    private MaterialButton btnSaveNote;
    private MaterialToolbar noteToolbar;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_create);

        noteToolbar = findViewById(R.id.noteToolbar);
        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        btnSaveNote = findViewById(R.id.btnSaveNote);
        progressBar = findViewById(R.id.progressBar);

        // Back Button
        noteToolbar.setNavigationOnClickListener(v -> finish());

        // Save Button
        btnSaveNote.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {

        // Disable the save button to prevent multiple clicks
        btnSaveNote.setVisibility(MaterialButton.INVISIBLE);
        progressBar.setVisibility(ProgressBar.VISIBLE);

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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore firebasedb = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            Map<String , Object> note = Map.of(
                    "title", title,
                    "content", content,
                    "lastEdited", System.currentTimeMillis()
            );

            firebasedb.collection("users").document(userId).collection("notes")
                    .add(note)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(NoteCreateActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        System.err.println("Error saving note: " + e.getMessage());
                        Toast.makeText(NoteCreateActivity.this, "Error saving note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Re-enable the save button and hide the progress bar
                        btnSaveNote.setVisibility(MaterialButton.VISIBLE);
                        progressBar.setVisibility(ProgressBar.INVISIBLE);
                    });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AuthActivity.class));
        }


//        finish();
    }
}
