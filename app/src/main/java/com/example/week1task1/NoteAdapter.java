package com.example.week1task1;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<MainActivity.Note> notes;
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(MainActivity.Note note);
    }

    public NoteAdapter(List<MainActivity.Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note_card, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        if (position < notes.size()) {
            MainActivity.Note note = notes.get(position);
            holder.bind(note, listener);
        }
    }

    @Override
    public int getItemCount() {
        return notes != null ? notes.size() : 0;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, preview, date;
        View colorTag;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.card_title);
            preview = itemView.findViewById(R.id.card_preview);
            date = itemView.findViewById(R.id.card_date);
            colorTag = itemView.findViewById(R.id.card_tag_color);
        }

        public void bind(final MainActivity.Note note, final OnNoteClickListener listener) {
            if (note == null) return;

            String displayTitle = (note.title == null || note.title.trim().isEmpty()) ? "Untitled" : note.title;
            String displayBody = (note.body == null || note.body.trim().isEmpty()) ? "No content" : note.body;
            
            title.setText(displayTitle);
            preview.setText(displayBody);
            date.setText(note.date != null ? note.date : "");
            
            if (colorTag != null && colorTag.getBackground() != null) {
                colorTag.getBackground().setColorFilter(note.color, PorterDuff.Mode.SRC_IN);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNoteClick(note);
                }
            });
        }
    }
}
