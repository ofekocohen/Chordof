package com.ofekcohen.chordof.Activities;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.CHORDS_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_SONG_PATH;
import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_SONG_PENDING_PATH;
import static com.ofekcohen.chordof.Core.Constants.SINGER_NAME_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.SONG_NAME_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.YOUTUBE_EXTRA_TAG;

public class UploadSongActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user;

    private Context context;
    private Song song;
    private Button btnUploadSong;
    private TextView tvSongName;
    private TextView tvSingerName;
    private TextView txtAuthor;
    private TextView txtComposer;
    private TextView txtEasyTone;
    private TextView txtYouTube;
    private TextView tvPrivacyPolicy;
    private CheckBox cbPrivacyPolicy;
    private LinearLayout linearLayoutButtons;
    private ProgressBar progressBarUploadSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein_500, R.anim.fadeout_500);
        setContentView(R.layout.activity_upload_song);
        context = this;

        user = FirebaseAuth.getInstance().getCurrentUser();

        // Extra
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                song = new Song(
                        extras.getString(SONG_NAME_EXTRA_TAG),
                        extras.getString(SINGER_NAME_EXTRA_TAG),
                        "",
                        "",
                        extras.getString(YOUTUBE_EXTRA_TAG),
                        0,
                        -1,
                        0,
                        user.getUid(),
                        user.getDisplayName(),
                        extras.getString(CHORDS_EXTRA_TAG));
                song.setEasyTone(song.calEasyTone()); // Calculate and set EasyTone
            }
            else {
                Toasty.error(this, "שגיאה בפרטי השיר", Toast.LENGTH_LONG).show();
                onBackPressed();
            }
        }

        tvSongName = findViewById(R.id.tvSongName);
        tvSingerName = findViewById(R.id.tvSingerName);
        txtAuthor = findViewById(R.id.txtAuthor);
        txtComposer = findViewById(R.id.txtComposer);
        txtEasyTone = findViewById(R.id.txtEasyTone);
        txtYouTube = findViewById(R.id.txtYouTube);
        btnUploadSong = findViewById(R.id.btnUploadSong);
        tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);
        cbPrivacyPolicy = findViewById(R.id.cbPrivacyPolicy);

        btnUploadSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadSong();
            }
        });
        tvPrivacyPolicy.setOnClickListener(new MyOnClickListener());

        // UI
        tvSongName.setText(song.getName());
        tvSingerName.setText(song.getSinger());
        txtEasyTone.setText(song.getEasyTone() + "");
        txtYouTube.setText(song.getYoutube());
        linearLayoutButtons = findViewById(R.id.linearLayoutButtons);
        progressBarUploadSong = findViewById(R.id.progressBarUploadSong);
        linearLayoutButtons.setVisibility(View.VISIBLE);
        progressBarUploadSong.setVisibility(View.INVISIBLE);
    }

    private class MyOnClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v) {
            if (tvPrivacyPolicy == v) {
                Functions.openPrivacyPolicy(context);
            }
        }
    }

    private void uploadSong() {
        String author, composer, easyTone, youtube;
        author = txtAuthor.getText().toString().trim();
        composer = txtComposer.getText().toString().trim();
        easyTone = txtEasyTone.getText().toString().trim();
        youtube = txtYouTube.getText().toString().trim();
        if (author.isEmpty() || composer.isEmpty() || youtube.isEmpty()) {
            Toasty.error(context, "מלא בבקשה את כל השדות").show();
            return;
        }
        else if (!youtube.contains("https://www.youtube.com/watch?v=") && !youtube.contains("https://youtu.be/")) {
            Toasty.error(context, "הקלד לינק מYouTube בלבד").show();
            return;
        }
        if (!cbPrivacyPolicy.isChecked()) {
            Toasty.error(context, "אשר את מדיניות הפרטיות").show();
            return;
        }
        if (easyTone.isEmpty())
            easyTone = "0";

        // UI
        progressBarUploadSong.setVisibility(View.VISIBLE);
        linearLayoutButtons.setVisibility(View.INVISIBLE);

        // Upload Song
        song = new Song(
                song.getName().trim(),
                song.getSinger().trim(),
                author.trim(),
                composer.trim(),
                youtube.trim(),
                0,
                -1,
                Integer.parseInt(easyTone),
                song.getUploaderUid(),
                song.getUploaderName(),
                song.getChords());
        saveSongToDB();
    }
    private void saveSongToDB()
    {
        DocumentReference df;
        /*if (user != null && Functions.isAdmin(user.getUid()))
            df = db.collection(FIRESTORE_SONG_PATH).document();
        else*/
            df = db.collection(FIRESTORE_SONG_PENDING_PATH).document();
        Map<String, Object> songMap = song.getSongMap();
        songMap.put("user_uploader_uid", user.getUid());
        songMap.put("user_uploader_name", user.getDisplayName());
        songMap.put("views_day", 0);
        songMap.put("views_array", new ArrayList<Integer>(Collections.nCopies(30, 0)));
        df.set(songMap).addOnSuccessListener(this, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Intent intent = new Intent(context, SuccessfullyUploadedActivity.class);
                intent.putExtra(SONG_NAME_EXTRA_TAG, song.getName());
                intent.putExtra(SINGER_NAME_EXTRA_TAG, song.getSinger());
                startActivity(intent);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toasty.error(context, "שגיאה בהעלאת השיר").show();
                linearLayoutButtons.setVisibility(View.VISIBLE);
                progressBarUploadSong.setVisibility(View.GONE);
            }
        });
    }
}