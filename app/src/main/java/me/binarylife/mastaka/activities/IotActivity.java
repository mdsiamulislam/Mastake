package me.binarylife.mastaka.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import me.binarylife.mastaka.R;
import me.binarylife.mastaka.database.NoteDatabase;
import me.binarylife.mastaka.databinding.ActivityIotBinding;
import me.binarylife.mastaka.models.Note;

public class IotActivity extends AppCompatActivity {

    private ActivityIotBinding binding;
    private static final String TAG = "IotActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View Binding ইনিশিয়ালাইজ করা
        binding = ActivityIotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Toolbar সেটআপ
        setSupportActionBar(binding.iotToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.iotToolbar.setNavigationOnClickListener(v -> finish());

        // Connect Button Click
        binding.btnConnect.setOnClickListener(v -> {
            Toast.makeText(this, "Connecting to IoT device...", Toast.LENGTH_SHORT).show();
            // এখানে কানেকশন লজিক বসাতে পারেন
        });

        // Sync Button Click
        binding.btnSync.setOnClickListener(v -> {
            Toast.makeText(this, "Syncing data...", Toast.LENGTH_SHORT).show();
            // এখানে সিঙ্ক লজিক বসাতে পারেন
        });

        // লোকাল রুম ডাটাবেজ থেকে নোট ডেটা ফেচ করে প্রিন্ট করার মেথড কল
        fetchAndPrintLocalNotes();
    }

    private void fetchAndPrintLocalNotes() {
        NoteDatabase db = NoteDatabase.getInstance(this);

        // রুম ডাটাবেজের কাজ সবসময় ব্যাকগ্রাউন্ড থ্রেডে করতে হয়
        new Thread(() -> {
            List<Note> noteList = db.noteDao().getNotesByUserSync("current_user_id"); // এখানে আপনার ইউজার আইডি বসান

            // কনসোলে বা Logcat-এ প্রিন্ট করা
            Log.d(TAG, "========== Local Notes List ==========");
            System.out.println("========== Local Notes List ==========");

            for (Note note : noteList) {
                String logMessage = "ID: " + note.getId() +
                        " | Title: " + note.getTitle() +
                        " | Content: " + note.getDescription() +
                        " | AI Supported: " + note.isAiSupported();

                Log.d(TAG, logMessage);
                System.out.println(logMessage);
            }

            Log.d(TAG, "Total Notes Found: " + noteList.size());
            System.out.println("Total Notes Found: " + noteList.size());

        }).start();
    }
}