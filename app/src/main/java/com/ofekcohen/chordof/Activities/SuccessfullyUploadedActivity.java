package com.ofekcohen.chordof.Activities;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ofekcohen.chordof.R;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.INFO_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.SINGER_NAME_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.SONG_NAME_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.TITLE_EXTRA_TAG;

public class SuccessfullyUploadedActivity extends AppCompatActivity {

    private Context context;
    private Button btnOK;
    private TextView tvSongName, tvSingerName, tvSuccessfullyUploaded, tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein_500, R.anim.fadeout_500);
        setContentView(R.layout.activity_successfully_uploaded);
        context = this;

        // Extra
        String songName = "", singerName = "", title = "", info = "";
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                songName = extras.getString(SONG_NAME_EXTRA_TAG);
                singerName = extras.getString(SINGER_NAME_EXTRA_TAG);
                title = extras.getString(TITLE_EXTRA_TAG);
                info = extras.getString(INFO_EXTRA_TAG);
            }
            else {
                Toasty.error(this, "שגיאה בפרטי השיר", Toast.LENGTH_LONG).show();
                onBackPressed();
            }
        }
        tvSongName = findViewById(R.id.tvSongName);
        tvSingerName = findViewById(R.id.tvSingerName);
        tvSuccessfullyUploaded = findViewById(R.id.tvSuccessfullyUploaded);
        tvInfo = findViewById(R.id.tvInfo);
        tvSongName.setText(songName);
        tvSingerName.setText(singerName);
        if (title != null && info != null) {
            tvSuccessfullyUploaded.setText(title);
            tvInfo.setText(info);
        }

        btnOK = findViewById(R.id.btnOK);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
