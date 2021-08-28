package com.ofekcohen.chordof.Fragments;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ofekcohen.chordof.Activities.CreateSongActivity;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.R;

import es.dmoral.toasty.Toasty;

import static android.app.Activity.RESULT_OK;
import static com.ofekcohen.chordof.Core.Constants.*;

public class UploadSongFragment extends Fragment {

    // Firebase
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference dbUsersPath = db.collection(FIRESTORE_USERS_PATH);

    private Context context;
    private EditText txtSongName, txtSingerName;
    private Button btnOK;
    private TextView tvHallOfFame;

    public UploadSongFragment() {}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload_song, container, false);
        context = getContext();
        user = FirebaseAuth.getInstance().getCurrentUser();

        txtSongName = view.findViewById(R.id.txtSongName);
        txtSingerName = view.findViewById(R.id.txtSingerName);
        btnOK = view.findViewById(R.id.btnNext);
        tvHallOfFame = view.findViewById(R.id.tvHallOfFame);

        MyOnClickListener listener = new MyOnClickListener();
        btnOK.setOnClickListener(listener);
        tvHallOfFame.setOnClickListener(listener);

        isHallOfFame();

        return view;
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (btnOK == v)
            {
                user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    if (!txtSongName.getText().toString().trim().isEmpty() && !txtSingerName.getText().toString().trim().isEmpty()) {
                        Intent intent = new Intent(getContext(), CreateSongActivity.class);
                        intent.putExtra(SONG_NAME_EXTRA_TAG, txtSongName.getText().toString().trim());
                        intent.putExtra(SINGER_NAME_EXTRA_TAG, txtSingerName.getText().toString().trim());
                        startActivity(intent);
                    }
                    else {
                        Toasty.error(getContext(), "כל השדות חייבים להיות מלאים").show();
                    }
                }
                else {
                    Toasty.info(context, "עלייך להתחבר בכדי להעלות שיר חדש", Toast.LENGTH_SHORT).show();
                    login();
                }
            }
            else if (tvHallOfFame == v) {
                setFragment(new HallOfFameFragment());
            }
        }
    }

    private void login() {
        Intent intent = Functions.login(getActivity());
        if (intent != null)
            startActivityForResult(intent, Constants.RC_SIGN_IN);
        else
            Toasty.error(context, "אין חיבור זמין לאינטרנט").show();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                user = FirebaseAuth.getInstance().getCurrentUser();

                // Save user properties in Firebase
                Functions.saveUserProperties(user);
            }
            else if (response != null) { // If response is null the user canceled the
                Toasty.error(context, response.getError().getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setFragment(Fragment fragment)
    {
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.fadein_300, R.anim.fadeout_300);
        fragmentTransaction.replace(R.id.frmContainer, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /**
     * Make 'tvHallOfFame' visible if the current user is an uploader.
     * <p>Uses cache 'SharedPreferences' to check if the user ever uploaded a song.
     * <p>If we don't find a cache file, We will check with firebase and if he actually an uploader, we will cache it. Otherwise, we won't cache (Maybe he will upload later, so we would check again each time).
     */
    private void isHallOfFame() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            boolean isUserUploader = sharedPref.getBoolean(SHARED_PREFERENCES_IS_USER_UPLOADER, false);
            if (isUserUploader)
                tvHallOfFame.setVisibility(View.VISIBLE);
            else {
                dbUsersPath.document(user.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot != null && documentSnapshot.get("uploads_count") != null) {
                            int uploadsCount = ((Long) documentSnapshot.get("uploads_count")).intValue();
                            if (uploadsCount > 0) {
                                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putBoolean(SHARED_PREFERENCES_IS_USER_UPLOADER, true);
                                editor.apply();

                                tvHallOfFame.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            }
        }
    }
}
