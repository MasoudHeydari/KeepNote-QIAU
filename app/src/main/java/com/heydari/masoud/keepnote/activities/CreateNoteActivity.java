package com.heydari.masoud.keepnote.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.icu.util.IslamicCalendar;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.heydari.masoud.keepnote.R;
import com.heydari.masoud.keepnote.database.NotesDatabase;
import com.heydari.masoud.keepnote.entities.Note;
import com.heydari.masoud.keepnote.listeners.NoteListener;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.jar.JarException;

public class CreateNoteActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    EditText edtNoteTitle, edtNoteSubtitle, edtNoteText;
    TextView txtDateTime;
    ImageView imgSave, imageBack, imageNote;
    TextView textWebUrl;
    LinearLayout layoutWebUrl;

    String selectedNoteColor = "#333333";
    String selectedImagePath = "";
    View viewSubtitleIndicator;

    AlertDialog dialogAddUrl;
    Note alreadyAvailableNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        edtNoteTitle = findViewById(R.id.inputNoteTitle);
        edtNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        edtNoteText = findViewById(R.id.inputNote);
        textWebUrl = findViewById(R.id.textWebUrl);

        txtDateTime = findViewById(R.id.textDateTime);

        imgSave = findViewById(R.id.imageSave);
        imageBack = findViewById(R.id.imageBack);
        imageNote = findViewById(R.id.imageNote);

        layoutWebUrl = findViewById(R.id.layoutWebUrl);


        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);

        imageBack.setOnClickListener(this);
        imgSave.setOnClickListener(this);

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        findViewById(R.id.imageRemoveWebUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textWebUrl.setText("");
                layoutWebUrl.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageNote.setImageBitmap(null);
                imageNote.setVisibility(View.GONE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
                selectedImagePath = "";
            }
        });

        setTextDateTime();
        initMiscellaneousLayout();
        setSubtitleIndicatorColor();

    }

    private void setTextDateTime() {
        // Ex: Saturday 26 December 2020 11:22 PM
        txtDateTime.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date()));
    }

    private void setViewOrUpdateNote() {
        edtNoteTitle.setText(alreadyAvailableNote.getTitle());
        edtNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        edtNoteText.setText(alreadyAvailableNote.getNoteText());
        txtDateTime.setText(alreadyAvailableNote.getDateTime());

        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }

        if (alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()) {
            textWebUrl.setText(alreadyAvailableNote.getWebLink());
            layoutWebUrl.setVisibility(View.VISIBLE);

        }
    }

    private void saveNote() {
        if (edtNoteTitle.getText().toString().trim().isEmpty()) {
            toast("Note title is Empty!");
            return;
        } else if (edtNoteSubtitle.getText().toString().trim().isEmpty() &&
                edtNoteText.getText().toString().trim().isEmpty()) {
            toast("Note can't be empty!");
            return;
        }

        final Note newNote = new Note();
        newNote.setTitle(edtNoteTitle.getText().toString());
        newNote.setSubtitle(edtNoteSubtitle.getText().toString());
        newNote.setNoteText(edtNoteText.getText().toString());
        newNote.setDateTime(txtDateTime.getText().toString());
        newNote.setColor(selectedNoteColor);
        newNote.setImagePath(selectedImagePath);

        if (layoutWebUrl.getVisibility() == View.VISIBLE) {
            newNote.setWebLink(textWebUrl.getText().toString());
        }

        if (alreadyAvailableNote != null) {
            newNote.setId(alreadyAvailableNote.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(newNote);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }

        new SaveNoteTask().execute();
    }

    private String getSPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
        }
        return filePath;
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imageBack:
                hideKeyboardFrom(this, view);
                onBackPressed();
                break;
            case R.id.imageSave:
                saveNote();
                hideKeyboardFrom(this, view);
                break;
            default:
        }
    }

    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(CreateNoteActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void initMiscellaneousLayout() {
        final LinearLayout layoutMiscellaneous = findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);
        layoutMiscellaneous.findViewById(R.id.txtMiscellaneous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });


        final ImageView imgColor1 = layoutMiscellaneous.findViewById(R.id.imgColor1);
        final ImageView imgColor2 = layoutMiscellaneous.findViewById(R.id.imgColor2);
        final ImageView imgColor3 = layoutMiscellaneous.findViewById(R.id.imgColor3);
        final ImageView imgColor4 = layoutMiscellaneous.findViewById(R.id.imgColor4);
        final ImageView imgColor5 = layoutMiscellaneous.findViewById(R.id.imgColor5);

        layoutMiscellaneous.findViewById(R.id.viewColor1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#333333";
                imgColor1.setImageResource(R.drawable.ic_done_24px);
                imgColor2.setImageResource(0);
                imgColor3.setImageResource(0);
                imgColor4.setImageResource(0);
                imgColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.viewColor2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#FDBE3B";
                imgColor1.setImageResource(0);
                imgColor2.setImageResource(R.drawable.ic_done_24px);
                imgColor3.setImageResource(0);
                imgColor4.setImageResource(0);
                imgColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.viewColor3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#ff4842";
                imgColor1.setImageResource(0);
                imgColor2.setImageResource(0);
                imgColor3.setImageResource(R.drawable.ic_done_24px);
                imgColor4.setImageResource(0);
                imgColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.viewColor4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#3a52fc";
                imgColor1.setImageResource(0);
                imgColor2.setImageResource(0);
                imgColor3.setImageResource(0);
                imgColor4.setImageResource(R.drawable.ic_done_24px);
                imgColor5.setImageResource(0);
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.viewColor5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#000000";
                imgColor1.setImageResource(0);
                imgColor2.setImageResource(0);
                imgColor3.setImageResource(0);
                imgColor4.setImageResource(0);
                imgColor5.setImageResource(R.drawable.ic_done_24px);
                setSubtitleIndicatorColor();
            }
        });

        if (alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()) {
            switch (alreadyAvailableNote.getColor()) {
                case "#333333":
                    layoutMiscellaneous.findViewById(R.id.viewColor1).performClick();
                    break;
                case "#FDBE3B":
                    layoutMiscellaneous.findViewById(R.id.viewColor2).performClick();
                    break;
                case "#ff4842":
                    layoutMiscellaneous.findViewById(R.id.viewColor3).performClick();
                    break;
                case "#3a52fc":
                    layoutMiscellaneous.findViewById(R.id.viewColor4).performClick();
                    break;
                case "#000000":
                    layoutMiscellaneous.findViewById(R.id.viewColor5).performClick();
                    break;

            }

        }

        layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(CreateNoteActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAGE_PERMISSION);

                } else {
                    selectImage();
                }
            }
        });

        layoutMiscellaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showAddUrlDialog();
            }
        });

        if (alreadyAvailableNote != null) {
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    deleteNote();
                }
            });
        }

    }

    private void deleteNote() {
        AlertDialog.Builder alert = new AlertDialog.Builder(CreateNoteActivity.this);
        alert.setMessage("Do you want to delete this note?");

        alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                @SuppressLint("StaticFieldLeak")
                class DeleteNote extends AsyncTask<Void, Void, Void> {

                    @Override
                    protected Void doInBackground(Void... voids) {
                        NotesDatabase.getDatabase(CreateNoteActivity.this).noteDao()
                                .deleteNote(alreadyAvailableNote);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        Intent intent = new Intent();
                        intent.putExtra("isNoteDeleted", true);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }

                new DeleteNote().execute();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alert.show();
    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);

        /*
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
            toast("if");
        } else {
            toast("else if");

        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                toast("Permission Denied!");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

                        selectedImagePath = getSPathFromUri(selectedImageUri);
                    } catch (Exception exception) {
                        toast(exception.getMessage());
                    }
                }
            }

        }
    }

    private void showAddUrlDialog() {
        if (dialogAddUrl == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(CreateNoteActivity.this)
                    .inflate(R.layout.layout_add_url, (ViewGroup) findViewById(R.id.layoutAddUrlContainer));

            builder.setView(view);

            dialogAddUrl = builder.create();
            if (dialogAddUrl.getWindow() != null) {
                dialogAddUrl.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText edtInputUrl = view.findViewById(R.id.inputURL);
            edtInputUrl.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toast("add clicked");
                    if (edtInputUrl.getText().toString().trim().isEmpty()) {
                        toast("Enter Url");
                    } else if (!Patterns.WEB_URL.matcher(edtInputUrl.getText().toString().trim()).matches()) {
                        toast("Enter Valid Url");
                    } else {
                        textWebUrl.setText(edtInputUrl.getText().toString());
                        layoutWebUrl.setVisibility(View.VISIBLE);
                        toast(edtNoteText.getText().toString());
                        dialogAddUrl.dismiss();
                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogAddUrl.dismiss();

                }
            });
        }

        dialogAddUrl.show();
    }

}
