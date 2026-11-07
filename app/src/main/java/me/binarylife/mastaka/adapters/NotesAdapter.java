package me.binarylife.mastaka.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import me.binarylife.mastaka.models.Note;
import me.binarylife.mastaka.R;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private final List<Note> noteList;
    private final OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public NotesAdapter(List<Note> noteList, OnNoteClickListener listener) {
        this.noteList = noteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);

        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull NoteViewHolder holder, int position) {

        Note note = noteList.get(position);

        holder.tvTitle.setText(note.getTitle());
        holder.tvDescription.setText(note.getDescription());

        holder.tvAiSupported.setVisibility(
                note.isAiSupported() ? View.VISIBLE : View.GONE
        );

        if (note.getSyncStatus() == 0) {
            holder.tvSyncStatus.setText("● Synced");
            holder.tvSyncStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            holder.tvSyncStatus.setText("○ Pending Sync");
            holder.tvSyncStatus.setTextColor(Color.parseColor("#FF9800")); // Orange
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvDescription, tvAiSupported, tvSyncStatus;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvDescription = itemView.findViewById(R.id.tvNoteDescription);
            tvAiSupported = itemView.findViewById(R.id.tvAiSupported);
            tvSyncStatus = itemView.findViewById(R.id.tvSyncStatus);
        }
    }
}
