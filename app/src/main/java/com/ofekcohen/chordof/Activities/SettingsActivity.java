package com.ofekcohen.chordof.Activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.ofekcohen.chordof.BuildConfig;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.R;

import es.dmoral.toasty.Toasty;

public class SettingsActivity extends AppCompatActivity {

    private Context context;
    private ListView listViewSettings;
    private String[] settingsArray;
    private Button btnBack, btnFacebook, btnInstagram, btnAcum;
    private TextView tvVersionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        context = this;

        listViewSettings = findViewById(R.id.listViewSettings);
        btnBack = findViewById(R.id.btnBack);
        btnFacebook = findViewById(R.id.btnFacebook);
        btnInstagram = findViewById(R.id.btnInstagram);
        btnAcum = findViewById(R.id.btnAcum);
        tvVersionName = findViewById(R.id.tvVersionName);

        settingsArray = getResources().getStringArray(R.array.settings_array);

        listViewSettings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // פרופיל משתמש
                        if (FirebaseAuth.getInstance().getCurrentUser() != null)
                            startActivity(new Intent(context, UserActivity.class));
                        else {
                            Toasty.info(context, "התחבר בכדי להמשיך", Toast.LENGTH_SHORT).show();
                            login();
                        }
                        break;
                    case 1: // מדיניות פרטיות
                        Functions.openPrivacyPolicy(context);
                        break;
                    case 2: // דווח על בעיה
                        sendMail();
                        break;
                }
            }
        });

        MyOnClickListener listener = new MyOnClickListener();
        btnBack.setOnClickListener(listener);
        btnFacebook.setOnClickListener(listener);
        btnInstagram.setOnClickListener(listener);
        btnAcum.setOnClickListener(listener);

        tvVersionName.setText(BuildConfig.VERSION_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.RC_SIGN_IN) {
            if (resultCode == RESULT_OK)
                // User has logged successfully in so now we can open the UserActivity (Profile)
                startActivity(new Intent(context, UserActivity.class));
        }
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (btnBack == v) {
                finish();
            }
            else if (btnFacebook == v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/chordof/")));
            } else if (btnInstagram == v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/chordof/")));
            } else if (btnAcum == v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://acum.org.il/"));
                startActivity(browserIntent);
            }
        }
    }

    private void login() {
        Intent intent = Functions.login(this);
        if (intent != null)
            startActivityForResult(intent, Constants.RC_SIGN_IN);
        else
            Toasty.error(context, "אין חיבור זמין לאינטרנט").show();
    }

    /**
     * Send mail to 'chordof.mail@gmail.com' to report an issue
     */
    private void sendMail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "chordof.mail@gmail.com", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "דיווח על בעיה");
        try {
            startActivity(Intent.createChooser(intent, "שלח מייל"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toasty.error(context,"לא נמצאה אפליקציה התומכת בשליחת מיילים", Toast.LENGTH_SHORT).show();
        }
    }
}
