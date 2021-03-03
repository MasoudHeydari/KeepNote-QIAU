package com.heydari.masoud.keepnote.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.heydari.masoud.keepnote.R;
import com.heydari.masoud.keepnote.adapters.NotesAdapter;
import com.heydari.masoud.keepnote.database.NotesDatabase;
import com.heydari.masoud.keepnote.entities.Note;
import com.heydari.masoud.keepnote.listeners.NoteListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NoteListener, TextWatcher {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    public static final int REQUEST_CODE_DELETE_NOTE = 4;

    private RecyclerView allNotesRecyclerView;
    private List<Note> allNotes = new ArrayList<>();
    private NotesAdapter notesAdapter;

    private int noteClickedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        allNotesRecyclerView = findViewById(R.id.noteRecyclerView);

        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent createNoteActivity = new Intent(getApplicationContext(), CreateNoteActivity.class);
                startActivityForResult(createNoteActivity, REQUEST_CODE_ADD_NOTE);
            }
        });

        allNotesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        notesAdapter = new NotesAdapter(allNotes, this);
        allNotesRecyclerView.setAdapter(notesAdapter);

        getAllNotes(REQUEST_CODE_SHOW_NOTES, false);

        EditText edtSearchNote = findViewById(R.id.inputSearch);
        edtSearchNote.addTextChangedListener(this);
    }

    private void getAllNotes(final int requestCode, boolean isNoteDeleted) {
        @SuppressLint("StaticFieldLeak")
        class FetchAllNotesTask extends AsyncTask<Void, Void, List<Note>> {
            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase.getDatabase(getApplicationContext()).noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);

                if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                    allNotes.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    allNotes.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    allNotesRecyclerView.smoothScrollToPosition(0);
                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    allNotes.remove(noteClickedPosition);

                    if (!isNoteDeleted) {
                        allNotes.add(noteClickedPosition, notes.get(noteClickedPosition));
                    }
                    notesAdapter.notifyDataSetChanged();
                }
            }

        }
        new FetchAllNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getAllNotes(REQUEST_CODE_ADD_NOTE, false);
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
                getAllNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
            }
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);

    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        notesAdapter.cancelTimer();
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (allNotes != null) {
            notesAdapter.searchNote(s.toString());

        }
    }
}