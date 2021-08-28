package com.ofekcohen.chordof.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ofekcohen.chordof.Fragments.SearchResultFragment;
import com.ofekcohen.chordof.R;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_USERS_PATH;
import static com.ofekcohen.chordof.Core.Constants.SEARCH_BY_USER_UPLOADER_UID;
import static com.ofekcohen.chordof.Core.Constants.SEARCH_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.SONG_REF_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.USER_ACTIVITY_UID_EXTRA_TAG;

public class UserActivity extends AppCompatActivity {

    private Context context;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userUid;
    private ImageView imgUserProfile;
    private EditText txtUsername;
    private TextView tvUploadsCount;
    private Button btnLogout, btnEdit, btnBack;
    private ProgressBar progressBar;
    private FrameLayout frmContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        this.context = this;

        // Extra
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                userUid = extras.getString(USER_ACTIVITY_UID_EXTRA_TAG);
            }
        }

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        imgUserProfile = findViewById(R.id.imgUserProfile);
        txtUsername = findViewById(R.id.txtUsername);
        tvUploadsCount = findViewById(R.id.tvUploadsCount);
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);
        btnEdit = findViewById(R.id.btnEdit);
        progressBar = findViewById(R.id.progressBar);
        frmContainer = findViewById(R.id.frmContainer);

        txtUsername.setVisibility(View.GONE);
        tvUploadsCount.setVisibility(View.GONE);
        frmContainer.setVisibility(View.GONE);
        btnLogout.setVisibility(View.GONE);
        btnEdit.setVisibility(View.GONE);

        MyOnClickListener listener = new MyOnClickListener();
        btnBack.setOnClickListener(listener);
        tvUploadsCount.setOnClickListener(listener);
        btnLogout.setOnClickListener(listener);
//        btnEdit.setOnClickListener(listener);

        if (userUid != null
                && (currentUser != null && !userUid.equals(currentUser.getUid()))
                || currentUser == null)
            updateUI();
        else {
            userUid = currentUser.getUid();
            updateUItoCurrentUser();
        }
    }

    @Override
    public void onBackPressed() {
        // We have only one fragment - when we are done with it, we should make 'frmContainer' invisible to see the activity.
        if (frmContainer.getVisibility() == View.VISIBLE) {
            btnBack.setVisibility(View.VISIBLE);
            frmContainer.setVisibility(View.INVISIBLE);
        }
        else
            finish();
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            if (btnBack == v) {
                finish();
            }
            else if (btnLogout == v) {
                logout();
                setResult(RESULT_OK);
                onBackPressed();
            }
            else if (tvUploadsCount == v) {
                Fragment searchResultFragment = new SearchResultFragment();
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(R.anim.fadein_300, R.anim.fadeout_300);
                fragmentTransaction.replace(R.id.frmContainer, searchResultFragment);
//                fragmentTransaction.addToBackStack(null);

                // Extras
                Bundle bundle = new Bundle();
                bundle.putString(SEARCH_EXTRA_TAG, userUid);
                bundle.putBoolean(SEARCH_BY_USER_UPLOADER_UID, true);
                searchResultFragment.setArguments(bundle);

                fragmentTransaction.commit();
                btnBack.setVisibility(View.INVISIBLE);
                frmContainer.setVisibility(View.VISIBLE);
            }
            else if (btnEdit == v) {
                if (btnEdit.getTag() == null || !(Boolean) btnEdit.getTag()) {
                    btnEdit.setTag(true);
                    btnEdit.setText("שמור");
                    txtUsername.setFocusableInTouchMode(true);
                }
                else {
                    btnEdit.setTag(false);
                    btnEdit.setText("ערוך");
                    txtUsername.setFocusable(false);
                    txtUsername.setText(txtUsername.getText()); // Remove white underline

                    //// Save the new username to firebase
                    //db.document(FIRESTORE_USERS_PATH + currentUser.getUid()).update("name", txtUsername.getText().toString());
                }
            }
        }
    }
    private void updateUI() {
        db.document(FIRESTORE_USERS_PATH + userUid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.equals(""))
                            txtUsername.setText(name);
                        else
                            txtUsername.setText("עדכן שם משתמש!");

                        String profileUrl = doc.getString("photo_url");
                        if (profileUrl != null && !profileUrl.equals(""))
                            Glide.with(context).load(profileUrl).into(imgUserProfile);

                        try { // If the user hasn't uploaded a song, 'doc.getString' will throw an exception
                            int uploadsCount = ((Long) doc.getLong("uploads_count")).intValue();
                            tvUploadsCount.setText("העלה " + uploadsCount + " שירים");
                        }
                        catch (Exception e) {
                            tvUploadsCount.setText("העלה 0 שירים");
                        }

                        progressBar.setVisibility(View.GONE);
                        txtUsername.setVisibility(View.VISIBLE);
                        tvUploadsCount.setVisibility(View.VISIBLE);
                    }
                    else {
                        Toasty.error(context, "שגיאה במשתמש", Toast.LENGTH_LONG).show();
                        onBackPressed();
                    }
                }
                else {
                    Toasty.error(context, "שגיאה במשתמש", Toast.LENGTH_LONG).show();
                    onBackPressed();
                }
            }
        });
    }
    private void updateUItoCurrentUser() {
        Glide.with(this).load(currentUser.getPhotoUrl()).into(imgUserProfile);
        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().equals(""))
            txtUsername.setText(currentUser.getDisplayName());
        else
            txtUsername.setText("עדכן שם משתמש!");

        tvUploadsCount.setText("העלת 0 שירים");
        db.document(FIRESTORE_USERS_PATH + currentUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        try { // If the user hasn't uploaded a song, 'doc.getString' will throw an exception
                            int uploadsCount = ((Long) doc.getLong("uploads_count")).intValue();
                            tvUploadsCount.setText("העלת " + uploadsCount + " שירים");
                        } catch (Exception e) { }
                    }
                }
                progressBar.setVisibility(View.GONE);
                tvUploadsCount.setVisibility(View.VISIBLE);
            }
        });

        txtUsername.setVisibility(View.VISIBLE);
        btnLogout.setVisibility(View.VISIBLE);
//        btnEdit.setVisibility(View.VISIBLE);
    }
    private void logout()
    {
        FirebaseAuth.getInstance().signOut();
        currentUser = null;
        Toasty.info(context, "התנתקת מהמשתמש שלך", Toast.LENGTH_SHORT).show();
    }
}