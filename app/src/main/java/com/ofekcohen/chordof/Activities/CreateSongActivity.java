package com.ofekcohen.chordof.Activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ofekcohen.chordof.Classes.ChordClickableSpan;
import com.ofekcohen.chordof.Classes.ChordSpan;
import com.ofekcohen.chordof.Classes.FetchFromMusixmatch;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.R;
import com.werdpressed.partisan.rundo.RunDo;

import org.jmusixmatch.MusixMatch;
import org.jmusixmatch.MusixMatchException;
import org.jmusixmatch.entity.lyrics.Lyrics;
import org.jmusixmatch.entity.track.Track;
import org.jmusixmatch.entity.track.TrackData;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.CHORDS_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.CHORDS_SECTIONS;
import static com.ofekcohen.chordof.Core.Constants.SINGER_NAME_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.SONG_NAME_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.YOUTUBE_EXTRA_TAG;

public class CreateSongActivity extends AppCompatActivity implements RunDo.TextLink {

    private Context context;
    private ConstraintLayout clChordEditor;
    private LinearLayout linearLayoutChords;
    private TextView tvSongName, tvSingerName;
    private static EditText txtChords;
    private Button btnOK, btnUpload, btnAddChord, btnUndo, btnRedo;
    private boolean isKeyboardVisible;
    private AlertDialog dialogAddChord;

    private Song song;

    // Chord Editor
    private List<Button> chordsButtonList;
    private ChordOnClickListener chordOnClickListener;
    private String chordToAdd; // The selected chord

    /* Undo / Redo */
    private RunDo mRunDo;
    @Override
    public EditText getEditTextForRunDo() {
        return txtChords;
    }
    /* Undo / Redo - END */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein_500, R.anim.fadeout_500);
        setContentView(R.layout.activity_create_song);
        context = this;

        // Extra
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.getString(SONG_NAME_EXTRA_TAG) != null && extras.getString(SINGER_NAME_EXTRA_TAG) != null) {
                song = new Song(
                        extras.getString(SONG_NAME_EXTRA_TAG),
                        extras.getString(SINGER_NAME_EXTRA_TAG),
                        "",
                        "",
                        "",
                        0,
                        -1,
                        0,
                        null,
                        null,
                        "");
                Functions.getYouTubeURL(context, song);
            }
            else {
                Toasty.error(this, "שגיאה בפרטי השיר", Toast.LENGTH_LONG).show();
                onBackPressed();
            }
        }
        chordsButtonList = new ArrayList<>();
        chordToAdd = "";
        dialogAddChord();

        linearLayoutChords = findViewById(R.id.linearLayoutChords);
        clChordEditor = findViewById(R.id.clChordEditor);
        btnUpload = findViewById(R.id.btnUpload);
        btnAddChord = findViewById(R.id.btnAddChord);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        tvSongName = findViewById(R.id.tvSongName);
        tvSingerName = findViewById(R.id.tvSingerName);
        txtChords = findViewById(R.id.txtChords);
        btnOK = findViewById(R.id.btnOK);

        tvSongName.setText(song.getName());
        tvSingerName.setText(song.getSinger());

        MyOnClickListener listener = new MyOnClickListener();
        btnOK.setOnClickListener(listener);
        txtChords.setOnClickListener(listener);
        btnAddChord.setOnClickListener(listener);
        btnUndo.setOnClickListener(listener);
        btnRedo.setOnClickListener(listener);
        btnUpload.setOnClickListener(listener);

        chordOnClickListener = new ChordOnClickListener();

        getLyrics();
    }

    @Override
    public void onBackPressed() {
        // Display alert before exit of song's creation
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    CreateSongActivity.super.onBackPressed();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // No button clicked
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("האם אתה בטוח כי ברצונך לצאת?\n(שים לב כי השיר ימחק)").setPositiveButton("כן", dialogClickListener)
                .setNegativeButton("לא", dialogClickListener).show();
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            // Lyrics Editor
            if (btnOK == v)
            {
                //if (txtChords.getText().toString().split(" ").length >= 10) { // השיר חייב להכיל לפחות 10 מילים
                    song.setChords(duplicateLines(txtChords.getText().toString()));
                    txtChords.setText(song.getChords());
                    txtChords.setHint("כאן נוסיף למילות השיר את האקורדים");

                    // Bring chord editor tools
                    btnOK.setVisibility(View.GONE);
                    clChordEditor.setVisibility(View.VISIBLE);
                    btnUpload.setVisibility(View.VISIBLE);

                    // Undo / Redo
                    mRunDo = RunDo.Factory.getInstance(getFragmentManager());
                    mRunDo.setQueueSize(20);
                    mRunDo.setTimerLength(1000);
                //}
                //else
                //    Toasty.error(context, "השיר קצר מידי").show();
            }

            // Chords Editor
            else if (txtChords == v) {
                if (isKeyboardVisible && chordToAdd != null)
                    insertChordToText(chordToAdd, txtChords.getSelectionStart());
            }
            else if (btnAddChord == v) {
                dialogAddChord.show();
            }
            else if (btnUndo == v) {
                mRunDo.undo();
                song.setChords(txtChords.getText().toString());
            }
            else if (btnRedo == v) {
                mRunDo.redo();
                song.setChords(txtChords.getText().toString());
            }
            else if (btnUpload == v) {
                song.setChords(txtChords.getText().toString());
                makeChordsInteractive();
                if (song.getChordsListOnSong().size() > 0) {
                    song.setChords(trimEndEveryLine(txtChords.getText().toString()));
                    Intent intent = new Intent(context, UploadSongActivity.class);
                    intent.putExtra(SONG_NAME_EXTRA_TAG, song.getName());
                    intent.putExtra(SINGER_NAME_EXTRA_TAG, song.getSinger());
                    intent.putExtra(CHORDS_EXTRA_TAG, song.getChords());
                    intent.putExtra(YOUTUBE_EXTRA_TAG, song.getYoutube());
                    context.startActivity(intent);
                }
                else
                    Toasty.error(context, "השיר חייב להכיל לפחות אקורד אחד").show();
            }
        }
    }

    /**
     * Create empty lines between lines of the lyrics (To make space for the chords).
     * @param text lyrics
     * @return lyrics with empty lines in-between.
     */
    private String duplicateLines(String text) {
        BufferedReader bufReader = new BufferedReader(new StringReader(text.trim()));
        String line, textDuplicateLines = "";
        try {
            String spaces;
            // First line
            if ((line = bufReader.readLine()) != null) {
                spaces = spaces(line.length() * 2);
                textDuplicateLines += spaces + "\n" + line.trim();
            }

            // Rest of the lines
            while( (line = bufReader.readLine()) != null ) {
                spaces = spaces(line.length() * 2);
                textDuplicateLines += "\n" + spaces + "\n" + line.trim();
            }
        } catch (Exception e) {
            Log.d(Constants.TAG, e.getMessage());
        }
        return textDuplicateLines;
    }
    /**
     * Generate a string with spaces
     * @param spacesCount How many spaces to create
     * @return String of the spaces (Ex. "     ")
     */
    private String spaces(int spacesCount) {
        String spaces = "";
        for (int i = 0; i < spacesCount; i++) {
            spaces += " ";
        }
        return spaces;
    }
    private String deleteEmptyLinesFromTheEnd(String text) {
        // Delete '\n' from the end
        for (int i = text.length() - 1; i >= 0; i--)
            if (text.charAt(i) != '\n')
                return text.substring(0, text.length() - (text.length() - i) + 1);
        return text;
    }
    /**
     * Delete all the spaces from the end of all the lines.
     * <p>*For lyric line we delete the end
     * <p>*For chord line we delete the beginning
     * @param text - txtChords
     * @return text with no spaces at the end of lines
     */
    private String trimEndEveryLine(String text) {
        BufferedReader bufReader = new BufferedReader(new StringReader(text));
        String line = null, trimmedText = "";
        try {
            while ((line = bufReader.readLine()) != null) {
                if (!Functions.isChordLine(line))
                    trimmedText += line.replaceFirst("\\s*$", "") + "\n";
                else
                    trimmedText += line.replaceFirst("^\\s*", "") + "\n";
            }
            return trimmedText;
        } catch (Exception e) {
            Log.d(Constants.TAG, e.getMessage());
            return text;
        }
    }

    /**
     * @param isKeyboardVisible TRUE - Keyboard is visible
     */
    private void toggleKeyboardVisibility(boolean isKeyboardVisible) {
        if (isKeyboardVisible) {
            this.isKeyboardVisible = false;
            txtChords.setShowSoftInputOnFocus(true); // Show keyboard on press
//            Toasty.info(context, "המקלדת תוצג כעת").show();
        }
        else
        {
            this.isKeyboardVisible = true;
            txtChords.setShowSoftInputOnFocus(false); // Don't show keyboard on press
//            Toasty.info(context, "המקלדת תוסתר כעת").show();
        }
    }


    /**
     * When user click on a chord from the list of the editor
     */
    private class ChordOnClickListener implements View.OnClickListener, View.OnLongClickListener {
        @Override
        public void onClick(View v) {
            // Reset all the other chords
            for (int i = 0; i < chordsButtonList.size(); i++) {
                chordsButtonList.get(i).setBackground(getDrawable(R.drawable.small_button_white));
                chordsButtonList.get(i).setTextColor(getResources().getColor(R.color.colorPrimary));
            }

            Button btnChord = ((Button) v);
            // If the user pressed on the same chord we want to show the keyboard
            if (chordToAdd.equals(btnChord.getText().toString())) {
                toggleKeyboardVisibility(true);
                chordToAdd = "";
            }
            // Change the selected chord
            else {
                toggleKeyboardVisibility(false);
                chordToAdd = btnChord.getText().toString();
                btnChord.setBackground(getDrawable(R.drawable.small_button_white_pressed));
                btnChord.setTextColor(Color.WHITE);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            int btnIndex = chordsButtonList.indexOf(v);
            linearLayoutChords.removeViewAt(btnIndex + 1); // +1 Because the '+' button
            chordsButtonList.remove(btnIndex);

            // Reset all the other chords
            for (int i = 0; i < chordsButtonList.size(); i++) {
                chordsButtonList.get(i).setBackground(getDrawable(R.drawable.small_button_white));
                chordsButtonList.get(i).setTextColor(getResources().getColor(R.color.colorPrimary));
            }
            chordToAdd = "";
            return true;
        }
    }
    /**
     * Add chord button to the editor list
     * @param chord Chord to add.
     */
    private void addChordToList(String chord)
    {
        // Set the properties for button
        LayoutInflater inflater = LayoutInflater.from(this);
        View clChord = inflater.inflate(R.layout.chord_button_create_song, linearLayoutChords, false);
        clChord.setTag(chord);
        Button btnChord = ((Button) clChord.findViewById(R.id.btnChord));
        btnChord.setText(chord);

        // Add button to the layout
        linearLayoutChords.addView(clChord);

        // Add button to chordsButtonList
        chordsButtonList.add(btnChord);
        btnChord.setOnClickListener(chordOnClickListener);
        btnChord.setOnLongClickListener(chordOnClickListener);
    }
    /**
     * Insert the chord within the index provided by 'cursorPosition'.
     * @param chord Chord to add.
     * @param cursorPosition Index of where to insert the chord.
     */
    private void insertChordToText(String chord, int cursorPosition) {
        String chords = txtChords.getText().toString();
        if (chords.length() > cursorPosition) {
            String section1 = chords.substring(0, cursorPosition);
            String section2 = chords.substring(cursorPosition);
            song.setChords(section1 + chord + section2);

            makeChordsInteractive(); // Highlight chords & txtChord.setText
        }
    }
    /**
     * Make chords clickable and highlight (Ab, C#m ...)
     * <p>Make sections words bold and underline (פיזמון, פתיחה ...)
     */
    private void makeChordsInteractive() {
        SpannableString ss = new SpannableString(song.getChords());
        makeChordsClickableAndHighlight(ss, song.getChords());
        makeSectionWordsBold(ss, song.getChords());
        txtChords.setText(ss);
        txtChords.setMovementMethod(LinkMovementMethod.getInstance());
    }
    /**
     * Find sections words within the text and <p>
     * Make sections words bold and underline. (פיזמון, פתיחה ...)
     */
    private void makeSectionWordsBold(SpannableString ss, String chords) {
        for (int i = 0; i < CHORDS_SECTIONS.length; i++) {
            int startIndex = chords.indexOf(CHORDS_SECTIONS[i]);
            while (startIndex != -1) {
                if (ss.length() != 0) {
                    ss.setSpan(new UnderlineSpan(), startIndex, startIndex + CHORDS_SECTIONS[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new StyleSpan(Typeface.BOLD), startIndex, startIndex + CHORDS_SECTIONS[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                startIndex = chords.indexOf(CHORDS_SECTIONS[i], startIndex + 1);
            }
        }
    }
    /**
     * Find chords within the text and <p>
     * Make chords clickable and highlight.
     */
    private void makeChordsClickableAndHighlight(SpannableString ss, String chords)
    {
        song.calChordsListOnSong();
        List<String> chordsOnSong = song.getChordsListOnSong();
        for (int i = 0; i < chordsOnSong.size(); i++) {
            int startIndex = chords.indexOf(chordsOnSong.get(i));
            while (startIndex != -1) {
                ChordSpan chordClickableSpan = new ChordSpan(this, chordsOnSong.get(i));
                if (ss.length() != 0)
                    ss.setSpan(chordClickableSpan, startIndex, startIndex + chordsOnSong.get(i).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                startIndex = chords.indexOf(chordsOnSong.get(i), startIndex + 1);
            }
        }
    }

    /* Dialog - Add Chord */
    Spinner spinnerBase, spinnerBaseExt, spinnerType, spinnerBaseComplex, spinnerBaseExtComplex;
    TextView tvChordToAdd;
    /**
     * Dialog of adding a new chord to the editor
     */
    private void dialogAddChord() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(CreateSongActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_add_chord, null);
        mBuilder.setView(mView);
        dialogAddChord = mBuilder.create();

        tvChordToAdd = mView.findViewById(R.id.tvChordToAdd);
        spinnerBase = mView.findViewById(R.id.spinnerBase);
        spinnerBaseExt = mView.findViewById(R.id.spinnerBaseExt);
        spinnerType = mView.findViewById(R.id.spinnerType);
        spinnerBaseComplex = mView.findViewById(R.id.spinnerBaseComplex);
        spinnerBaseExtComplex = mView.findViewById(R.id.spinnerBaseExtComplex);

        SpinnerOnItemSelected spinnerOnItemSelected = new SpinnerOnItemSelected();
        spinnerBase.setOnItemSelectedListener(spinnerOnItemSelected);
        spinnerBaseExt.setOnItemSelectedListener(spinnerOnItemSelected);
        spinnerType.setOnItemSelectedListener(spinnerOnItemSelected);
        spinnerBaseComplex.setOnItemSelectedListener(spinnerOnItemSelected);
        spinnerBaseExtComplex.setOnItemSelectedListener(spinnerOnItemSelected);

        Button btnAddChord = mView.findViewById(R.id.btnAddChord);
        btnAddChord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogAddChord.hide();
                addChordToList(tvChordToAdd.getText().toString());
            }
        });
    }
    /**
     * Change the title of dialog acording to spinners selection
     */
    private class SpinnerOnItemSelected implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (spinnerBaseComplex.getSelectedItem().toString().isEmpty()) {
                tvChordToAdd.setText(
                        spinnerBase.getSelectedItem().toString() +
                        spinnerBaseExt.getSelectedItem().toString() +
                        spinnerType.getSelectedItem().toString());
            }
            else {
                tvChordToAdd.setText(
                        spinnerBase.getSelectedItem().toString() +
                        spinnerBaseExt.getSelectedItem().toString() +
                        spinnerType.getSelectedItem().toString() +
                        "/" +
                        spinnerBaseComplex.getSelectedItem() +
                        spinnerBaseExtComplex.getSelectedItem());
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    }
    /* Dialog - Add Chord - END */

    private void getLyrics() {
        new FetchFromMusixmatch.FetchLyricsFromMusixmatch(context, song).execute();
    }
    public static void setLyricsFromMusixmatch(String lyrics) {
        txtChords.setText(lyrics);
    }
}
