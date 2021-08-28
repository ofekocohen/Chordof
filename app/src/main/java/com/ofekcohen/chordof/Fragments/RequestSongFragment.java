package com.ofekcohen.chordof.Fragments;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ofekcohen.chordof.Activities.SuccessfullyUploadedActivity;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;

import es.dmoral.toasty.Toasty;

import static android.app.Activity.RESULT_OK;
import static com.ofekcohen.chordof.Core.Constants.*;

public class RequestSongFragment extends Fragment {

    // Firebase
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference dbUsersPath = db.collection(FIRESTORE_USERS_PATH);

    private Context context;
    private RewardedAd rewardedAd;
    private Song song;
    private EditText txtSongName, txtSingerName;
    private Button btnOK, btnBack;
    private ProgressBar progressBarUploadSong;
    private boolean userHaveSeenRewardAd;

    public RequestSongFragment() {}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request_song, container, false);
        context = getContext();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Load AdMob RewardAd
        loadRewardAd();

        txtSongName = view.findViewById(R.id.txtSongName);
        txtSingerName = view.findViewById(R.id.txtSingerName);
        btnOK = view.findViewById(R.id.btnRequestSong);
        btnBack = view.findViewById(R.id.btnBack);
        progressBarUploadSong = view.findViewById(R.id.progressBarUploadSong);

        userHaveSeenRewardAd = false;

        MyOnClickListener listener = new MyOnClickListener();
        btnOK.setOnClickListener(listener);
        btnBack.setOnClickListener(listener);

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
                        if (song == null)
                            createSong();
                        if (!userHaveSeenRewardAd)
                            showRewardAd(); // After the ad, the request song sends to firebase
                        else {
                            sendRequestToDB();
                            btnOK.setVisibility(View.GONE);
                            progressBarUploadSong.setVisibility(View.VISIBLE);
                        }
                    }
                    else {
                        Toasty.error(getContext(), "כל השדות חייבים להיות מלאים").show();
                    }
                }
                else {
                    Toasty.info(context, "עלייך להתחבר בכדי לבקש שיר", Toast.LENGTH_SHORT).show();
                    login();
                }
            }
            else if (btnBack == v) {
                getFragmentManager().popBackStack();
            }
        }
    }

    /* AdMob - RewardAd */
    private boolean rewardAdFailedToLoad = false;
    private void loadRewardAd() {
        rewardedAd = new RewardedAd(context, getString(R.string.admob_reward_unit_id));
        RewardedAdLoadCallback callback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
                super.onRewardedAdLoaded();
            }

            @Override
            public void onRewardedAdFailedToLoad(LoadAdError loadAdError) {
                super.onRewardedAdFailedToLoad(loadAdError);
                rewardAdFailedToLoad = true;
                Log.e(TAG, "Reward Ad failed to load. <RequestSongFragment>");
            }
        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), callback);
    }
    private void showRewardAd() {
        if (rewardedAd.isLoaded()) {
            RewardedAdCallback callback = new RewardedAdCallback() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    sendRequestToDB();
                    btnOK.setVisibility(View.GONE);
                    progressBarUploadSong.setVisibility(View.VISIBLE);
                    userHaveSeenRewardAd = true;
                }
                @Override
                public void onRewardedAdClosed() {
                    super.onRewardedAdClosed();
                    loadRewardAd();
                    progressBarUploadSong.setVisibility(View.GONE);
                    btnOK.setVisibility(View.VISIBLE);
                }
                @Override
                public void onRewardedAdFailedToShow(AdError adError) {
                    super.onRewardedAdFailedToShow(adError);
                    sendRequestToDB();
                    btnOK.setVisibility(View.GONE);
                    progressBarUploadSong.setVisibility(View.VISIBLE);
                }
            };
            rewardedAd.show(getActivity(), callback);
        }
        else if (rewardAdFailedToLoad) {
            sendRequestToDB();
            btnOK.setVisibility(View.GONE);
            progressBarUploadSong.setVisibility(View.VISIBLE);
        }
        else {
            Toasty.error(context, "נסה שוב בעוד מספר רגעים").show();
            progressBarUploadSong.setVisibility(View.GONE);
            btnOK.setVisibility(View.VISIBLE);
        }
    }
    /* AdMob - RewardAd - END*/

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

    private void createSong() {
        song = new Song(
                txtSongName.getText().toString().trim(),
                txtSingerName.getText().toString().trim(),
                "",
                "",
                "",
                0,
                -1,
                0,
                "",
                "",
                ""
        );
        Functions.getYouTubeURL(context, song);
    }
    private void sendRequestToDB() {
        if (Functions.isNetworkAvailable(context)) {
            DocumentReference df;
            df = db.collection(FIRESTORE_SONG_REQUESTS_PATH).document();
            Map<String, Object> songMap = song.getSongMap();
            songMap.put("user_uploader_uid", user.getUid());
            songMap.put("user_uploader_name", user.getDisplayName());
            songMap.put("views_day", 0);
            songMap.put("views_array", new ArrayList<Integer>(Collections.nCopies(30, 0)));
            df.set(songMap).addOnSuccessListener(getActivity(), new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Intent intent = new Intent(context, SuccessfullyUploadedActivity.class);
                    intent.putExtra(TITLE_EXTRA_TAG, "בקשת השיר נשלחה בהצלחה!");
                    intent.putExtra(INFO_EXTRA_TAG, "נעשה את מירב המאמצים בכדי להעלות את השיר");
                    intent.putExtra(SONG_NAME_EXTRA_TAG, song.getName());
                    intent.putExtra(SINGER_NAME_EXTRA_TAG, song.getSinger());
                    startActivity(intent);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toasty.error(context, "שגיאה בשליחת בקשת השיר").show();
                    btnOK.setVisibility(View.VISIBLE);
                    progressBarUploadSong.setVisibility(View.GONE);
                }
            });
        }
        else
            Toasty.error(context, "נראה לנו שאין חיבור לאינטרנט").show();
    }
}
