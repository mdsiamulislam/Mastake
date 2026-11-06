
package me.binarylife.mastaka;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class NoteCreateActivity extends AppCompatActivity {

    private EditText etNoteTitle, etNoteContent;
    private TextView tvLastEdited;
    private MaterialButton btnSaveNote;
    private MaterialToolbar noteToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_create);

        noteToolbar = findViewById(R.id.noteToolbar);
        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        tvLastEdited = findViewById(R.id.tvLastEdited);
        btnSaveNote = findViewById(R.id.btnSaveNote);

        // Back Button
        noteToolbar.setNavigationOnClickListener(v -> finish());

        // Save Button
        btnSaveNote.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {

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

        // Database integration will be added later
        Toast.makeText(
                this,
                "Note is ready to save!",
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }
}
