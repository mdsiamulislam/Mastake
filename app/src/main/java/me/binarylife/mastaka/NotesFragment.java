
package me.binarylife.mastaka;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotesFragment extends Fragment {

    private RecyclerView rvNotes;
    private NotesAdapter notesAdapter;
    private List<Note> noteList;

    private FirebaseFirestore db;

    public NotesFragment() {
        // Required empty constructor
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

        notesAdapter = new NotesAdapter(noteList);

        rvNotes.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        rvNotes.setAdapter(notesAdapter);

        db = FirebaseFirestore.getInstance();

        fetchNotes();

        return view;
    }

    private void fetchNotes() {

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    requireContext(),
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users")
                .document(userId)
                .collection("notes")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (!isAdded()) return;

                    noteList.clear();

                    for (QueryDocumentSnapshot document : querySnapshot) {

                        String id = document.getId();

                        String title = document.getString("title");

                        String description =
                                document.getString("content");

                        boolean isAiSupported =
                                Boolean.TRUE.equals(
                                        document.getBoolean("isAiSupported")
                                );

                        Note note = new Note(
                                id,
                                title != null ? title : "Untitled",
                                description != null ? description : "",
                                isAiSupported
                        );

                        noteList.add(note);
                    }

                    notesAdapter.notifyDataSetChanged();

                })
                .addOnFailureListener(e -> {

                    if (!isAdded()) return;

                    Toast.makeText(
                            requireContext(),
                            "Failed to load notes: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }
}
