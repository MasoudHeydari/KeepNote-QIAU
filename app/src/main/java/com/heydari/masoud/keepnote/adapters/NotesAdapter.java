package com.heydari.masoud.keepnote.adapters;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.heydari.masoud.keepnote.R;
import com.heydari.masoud.keepnote.entities.Note;
import com.heydari.masoud.keepnote.listeners.NoteListener;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    List<Note> allNotes;
    NoteListener noteListener;
    private Timer timer;
    private List<Note> notesSource;

    public NotesAdapter(List<Note> allNotes, NoteListener noteListener) {
        this.allNotes = allNotes;
        this.notesSource = allNotes;
        this.noteListener = noteListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.setNote(allNotes.get(position));
        holder.layoutNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                noteListener.onNoteClicked(allNotes.get(position), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return allNotes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView txtTitle, txtSubtitle, txtDateTime;
        LinearLayout layoutNote;
        RoundedImageView roundedImageView;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtSubtitle = itemView.findViewById(R.id.txtSubtitle);
            txtDateTime = itemView.findViewById(R.id.txtDateTime);
            layoutNote = itemView.findViewById(R.id.layoutNote);
            roundedImageView = itemView.findViewById(R.id.roundImageNote);
        }


        void setNote(Note note) {
            txtTitle.setText(note.getTitle());
            if (note.getSubtitle().trim().isEmpty()) {
                txtSubtitle.setVisibility(View.GONE);
            } else {
                txtSubtitle.setText(note.getSubtitle());
            }
            txtDateTime.setText(note.getDateTime());

            GradientDrawable gradientDrawable = (GradientDrawable) layoutNote.getBackground();
            if (note.getColor() != null) {
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            } else {
                gradientDrawable.setColor(Color.parseColor("#333333"));

            }

            if (note.getImagePath() == null) {
                roundedImageView.setVisibility(View.GONE);
            } else {
                roundedImageView.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                roundedImageView.setVisibility(View.VISIBLE);

            }
        }

    }

    public void searchNote(String searchKeyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (searchKeyword.trim().isEmpty()) {
                    allNotes = notesSource;
                } else {
                    ArrayList<Note> tempNotes = new ArrayList<>();
                    for (Note note : notesSource) {
                        if (note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getNoteText().toLowerCase().contains(searchKeyword.toLowerCase())) {
                            tempNotes.add(note);
                        }
                    }

                    allNotes = tempNotes;
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });

            }
        }, 500);
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
