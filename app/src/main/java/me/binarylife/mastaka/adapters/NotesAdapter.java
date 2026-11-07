package me.binarylife.mastaka.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.binarylife.mastaka.models.Note;
import me.binarylife.mastaka.R;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private final List<Note> noteList;
    private final OnNoteActionListener listener;
    private final Map<String, Boolean> expandedStateMap = new HashMap<>();

    public interface OnNoteActionListener {
        void onEditClick(Note note);
        void onDeleteClick(Note note);
    }

    public NotesAdapter(List<Note> noteList, OnNoteActionListener listener) {
        this.noteList = noteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);

        holder.tvTitle.setText(note.getTitle());
        holder.tvDescription.setText(note.getDescription());

        holder.tvAiSupported.setVisibility(
                note.isAiSupported() ? View.VISIBLE : View.GONE
        );

        // Sync indicator dot
        if (note.getSyncStatus() == 0) {
            holder.viewSyncIndicator.setBackgroundResource(R.drawable.sync_dot_synced);
        } else {
            holder.viewSyncIndicator.setBackgroundResource(R.drawable.sync_dot_pending);
        }

        // Expandable / Collapsible state
        boolean isExpanded = expandedStateMap.containsKey(note.getId()) && Boolean.TRUE.equals(expandedStateMap.get(note.getId()));
        if (isExpanded) {
            holder.tvDescription.setMaxLines(Integer.MAX_VALUE);
            holder.tvDescription.setEllipsize(null);
        } else {
            holder.tvDescription.setMaxLines(3);
            holder.tvDescription.setEllipsize(TextUtils.TruncateAt.END);
        }

        holder.itemView.setOnClickListener(v -> {
            boolean currentExpanded = expandedStateMap.containsKey(note.getId()) && Boolean.TRUE.equals(expandedStateMap.get(note.getId()));
            expandedStateMap.put(note.getId(), !currentExpanded);
            notifyItemChanged(position);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(note);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvDescription, tvAiSupported;
        View viewSyncIndicator;
        ImageView btnEdit, btnDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvDescription = itemView.findViewById(R.id.tvNoteDescription);
            tvAiSupported = itemView.findViewById(R.id.tvAiSupported);
            viewSyncIndicator = itemView.findViewById(R.id.viewSyncIndicator);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
