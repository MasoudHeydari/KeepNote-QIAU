package com.heydari.masoud.keepnote.listeners;

import com.heydari.masoud.keepnote.entities.Note;

public interface NoteListener {
    void onNoteClicked(Note note, int position);
}
