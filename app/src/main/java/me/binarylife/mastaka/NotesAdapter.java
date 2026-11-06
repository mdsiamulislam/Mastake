
package me.binarylife.mastaka;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private final List<Note> noteList;

    public NotesAdapter(List<Note> noteList) {
        this.noteList = noteList;
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
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvDescription, tvAiSupported;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvDescription = itemView.findViewById(R.id.tvNoteDescription);
            tvAiSupported = itemView.findViewById(R.id.tvAiSupported);
        }
    }
}
