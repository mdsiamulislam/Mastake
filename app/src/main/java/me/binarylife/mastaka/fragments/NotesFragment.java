package me.binarylife.mastaka.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import me.binarylife.mastaka.activities.NoteCreateActivity;
import me.binarylife.mastaka.adapters.NotesAdapter;
import me.binarylife.mastaka.models.Note;
import me.binarylife.mastaka.repository.NoteRepository;
import me.binarylife.mastaka.R;

public class NotesFragment extends Fragment {

    private RecyclerView rvNotes;
    private NotesAdapter notesAdapter;
    private List<Note> noteList;
    private NoteRepository noteRepository;
    private boolean isAiAssist = false;

    public NotesFragment() {
        // Required empty constructor
    }

    public static NotesFragment newInstance(boolean isAiAssist) {
        NotesFragment fragment = new NotesFragment();
        Bundle args = new Bundle();
        args.putBoolean("is_ai_assist", isAiAssist);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isAiAssist = getArguments().getBoolean("is_ai_assist", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_notes,
                container,
                false
        );

        rvNotes = view.findViewById(R.id.rvNotes);
        noteList = new ArrayList<>();

        noteRepository = new NoteRepository(requireContext());

        notesAdapter = new NotesAdapter(noteList, new NotesAdapter.OnNoteActionListener() {
            @Override
            public void onEditClick(Note note) {
                Intent intent = new Intent(requireContext(), NoteCreateActivity.class);
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_content", note.getDescription());
                intent.putExtra("is_ai", note.isAiSupported());
                intent.putExtra("note_created_at", note.getCreatedAt());
                intent.putExtra("note_version", note.getVersion());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(Note note) {
                noteRepository.deleteNote(note);
                Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show();
            }
        });

        rvNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotes.setAdapter(notesAdapter);

        observeNotes();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            noteRepository.fetchAndSyncFromCloud(userId);
            noteRepository.triggerSync();
        }

        return view;
    }

    private void observeNotes() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        noteRepository.getNotesLiveData(userId).observe(getViewLifecycleOwner(), notes -> {
            noteList.clear();
            if (notes != null) {
                for (Note note : notes) {
                    if (note.isAiSupported() == isAiAssist) {
                        noteList.add(note);
                    }
                }
            }
            notesAdapter.notifyDataSetChanged();
        });
    }
}
