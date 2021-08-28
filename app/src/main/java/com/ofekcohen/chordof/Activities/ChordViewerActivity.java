package com.ofekcohen.chordof.Activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.os.Handler;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.ofekcohen.chordof.Classes.ChordClickableSpan;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Classes.ViewedSong;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.Fragments.SearchResultFragment;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.*;

public class ChordViewerActivity extends AppCompatActivity {

    // AdMob
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    // Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference mDocRef;
    private String docRefString;
    private FirebaseUser user;
    private Map<String, Object> songSnapshot;
    private String songUid;

    private Activity activity;
    private Song song;
    private TextView tvChords, tvTranspose, tvSize, tvSongName, tvSinger;
    private Button btnTransposeUp, btnTransposeDown, btnSizeUp, btnSizeDown, btnEasyTone, btnFavorite, btnSongInfo, btnPlay, btnEditMode, btnKeyboard, btnUpdate, btnDelete, btnBack, btnFasterAutoScroll, btnSlowerAutoScroll, btnStopAutoScroll;
    private ProgressBar progressBarChords;
    private ArrayList<String> chordsOnSong;
    private Context context;
    private HorizontalScrollView horizontalScrollView;
    private NestedScrollView scrollView;
    private CoordinatorLayout coordinatorLayout;
    private int autoScrollSpeed;
    private ScheduledExecutorService autoScrollScheduledExecutorService;
    private LinearLayout linearLayoutAutoScroll;
    private AppBarLayout appBarLayout;
    private boolean isKeyboardVisible;
    private CountDownTimer timerViewsInc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein_500, R.anim.fadeout_500);
        setContentView(R.layout.activity_chord_viewer);
        activity = this;
        context = this;

        /* AdMob */
        // Banner (Bottom of the screen)
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Interstitial (Fullscreen Ad)
        mInterstitialAd = new InterstitialAd(context);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.admob_interstitial_unit_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        /**/

        // Extra
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                docRefString = extras.getString(SONG_REF_EXTRA_TAG);
                songUid = docRefString.replace(FIRESTORE_SONG_PATH, ""); // Ex. "songs/kiSj0cnH2dv.."
            }
            else {
                Toasty.error(this, "שגיאה בשיר", Toast.LENGTH_LONG).show();
                onBackPressed();
            }
        }

        progressBarChords = findViewById(R.id.progressBarChords);

        // Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();
        fetchSongFromDB();

        tvChords = findViewById(R.id.tvChords);
        tvTranspose = findViewById(R.id.tvTranspose);
        tvSize = findViewById(R.id.tvSize);
        tvSongName = findViewById(R.id.tvSongName);
        tvSinger = findViewById(R.id.tvSingerName);
        btnTransposeUp = findViewById(R.id.btnTransposeUp);
        btnTransposeDown = findViewById(R.id.btnTransposeDown);
        btnSizeUp = findViewById(R.id.btnSizeUp);
        btnSizeDown = findViewById(R.id.btnSizeDown);
        btnEasyTone = findViewById(R.id.btnEasyTone);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnSongInfo = findViewById(R.id.btnSongInfo);
        btnPlay = findViewById(R.id.btnPlay);
        btnEditMode = findViewById(R.id.btnEditMode);
        btnKeyboard = findViewById(R.id.btnKeyboard);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);
        btnFasterAutoScroll = findViewById(R.id.btnFasterAutoScroll);
        btnSlowerAutoScroll = findViewById(R.id.btnSlowerAutoScroll);
        btnStopAutoScroll = findViewById(R.id.btnStopAutoScroll);
        horizontalScrollView = findViewById(R.id.horizontalScrollView);
        scrollView = findViewById(R.id.scrollView);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        appBarLayout = findViewById(R.id.appBarLayout);
        linearLayoutAutoScroll = findViewById(R.id.linearLayoutAutoScroll);

        MyOnClickListener myOnClickListener = new MyOnClickListener();
        btnTransposeUp.setOnClickListener(myOnClickListener);
        btnTransposeDown.setOnClickListener(myOnClickListener);
        btnSizeUp.setOnClickListener(myOnClickListener);
        btnSizeDown.setOnClickListener(myOnClickListener);
        btnEasyTone.setOnClickListener(myOnClickListener);
        btnFavorite.setOnClickListener(myOnClickListener);
        btnSongInfo.setOnClickListener(myOnClickListener);
        btnPlay.setOnClickListener(myOnClickListener);
        btnEditMode.setOnClickListener(myOnClickListener);
        btnKeyboard.setOnClickListener(myOnClickListener);
        btnUpdate.setOnClickListener(myOnClickListener);
        btnDelete.setOnClickListener(myOnClickListener);
        tvSinger.setOnClickListener(myOnClickListener);
        btnBack.setOnClickListener(myOnClickListener);
        btnFasterAutoScroll.setOnClickListener(myOnClickListener);
        btnSlowerAutoScroll.setOnClickListener(myOnClickListener);
        btnStopAutoScroll.setOnClickListener(myOnClickListener);

        MyOnLongClickListener myOnLongClickListener = new MyOnLongClickListener();
        btnTransposeUp.setOnLongClickListener(myOnLongClickListener);
        btnTransposeDown.setOnLongClickListener(myOnLongClickListener);
        btnSizeUp.setOnLongClickListener(myOnLongClickListener);
        btnSizeDown.setOnLongClickListener(myOnLongClickListener);

        MyOnTouchListener myOnTouchListener = new MyOnTouchListener();
        linearLayoutAutoScroll.setOnTouchListener(myOnTouchListener);

        btnEditMode.setTag(false);
        btnEditMode.setVisibility(View.INVISIBLE);

        autoScrollSpeed = 1;

        if (user != null && Functions.isAdmin(user.getUid())) {
            btnEditMode.setTag(false);
            btnEditMode.setVisibility(View.VISIBLE);
            isKeyboardVisible = true;
        }
    }

    @Override
    public void onBackPressed() {
        // AdMob
        if (mInterstitialAd.isLoaded())
            mInterstitialAd.show();
        else if (mInterstitialAd.isLoading())
            Log.d(TAG, "Interstitial Ads wasn't loaded yet.");

        cancelTimerViewsInc();
        overridePendingTransition(R.anim.fadein_500, R.anim.fadeout_500);
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        // AdMob
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        // AdMob
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (song != null)
            {
                if (btnTransposeUp == v) {
                    // If something was edited, save it and only then transpose
                    song.setChords(tvChords.getText().toString());
                    makeChordsInteractive();

                    song.transpose(1);
                    updateUI();
                }
                else if (btnTransposeDown == v) {
                    // If something was edited, save it and only then transpose
                    song.setChords(tvChords.getText().toString());
                    makeChordsInteractive();

                    song.transpose(-1);
                    updateUI();
                }
                else if (btnFavorite == v) {
                    if (user == null) {
                        Toasty.info(context, "עלייך להתחבר בכדי לשמור שיר במועדפים", Toast.LENGTH_SHORT).show();
                        login();
                    } else {
                        if (btnFavorite.getTag() == null || (Boolean) btnFavorite.getTag() == FAVORITE_HEART_DISABLED) {
                            // Make this song Favorite
                            btnFavorite.setBackgroundResource(FAVORITE_HEART_ENABLED_ICON);
                            btnFavorite.setTag(FAVORITE_HEART_ENABLED);
                        } else {
                            // Remove from Favorite
                            DocumentReference df = db.document(FIRESTORE_USERS_PATH + user.getUid() + "/favorites/" + songUid);
                            df.delete();
                            btnFavorite.setBackgroundResource(FAVORITE_HEART_DISABLED_ICON);
                            btnFavorite.setTag(FAVORITE_HEART_DISABLED);
                        }
                    }
                }
                else if (btnSizeUp == v || btnSizeDown == v) {
                    int sizeDirection;
                    if (btnSizeUp == v)
                        sizeDirection = 1;
                    else
                        sizeDirection = -1;

                    float px = tvChords.getTextSize();
                    float sp = px / getResources().getDisplayMetrics().scaledDensity;
                    tvChords.setTextSize(Dimension.SP, sp + sizeDirection);
                    if (tvChords.getTag() == null)
                        tvChords.setTag(sp);

                    // Update UI
                    int prevSize = Integer.valueOf(tvSize.getText().toString());
                    tvSize.setText(prevSize + sizeDirection + "");
                }
                else if (btnEasyTone == v) {
                    if (song.getTranspose() != song.getEasyTone()) {
                        song.transpose(-1 * song.getTranspose()); // Reset to 0
                        song.transpose(song.getEasyTone());
                        updateUI();
                    }
                }
                else if (btnSongInfo == v) {
                    dialogSongInfo();
                }
                else if (btnEditMode == v) {
                    if ((boolean) btnEditMode.getTag())
                        toggelEditMode(false);
                    else
                        toggelEditMode(true);
                }
                else if (btnKeyboard == v) {
                    toggleKeyboardVisibility(isKeyboardVisible);
                }
                else if (btnUpdate == v) {
                    // Update the song on Firebase
                    confirmUpdateSong(); // Update if YES pressed
                }
                else if (btnDelete == v) {
                    // Delete the song from Firebase
                    confirmDeleteSong(); // Delete if YES pressed
                }
                else if (tvSinger == v) {
                    // Open SearchResultFragment to search for singer's song
                    Fragment searchResultFragment = new SearchResultFragment();
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.setCustomAnimations(R.anim.fadein_300, R.anim.fadeout_300);
                    fragmentTransaction.replace(R.id.frmContainerChordViewer, searchResultFragment);
                    fragmentTransaction.addToBackStack(null);

                    // Extras
                    Bundle bundle = new Bundle();
                    bundle.putString(SEARCH_EXTRA_TAG, tvSinger.getText().toString());
                    searchResultFragment.setArguments(bundle);

                    fragmentTransaction.commit();
                }
                else if (btnBack == v) {
                    onBackPressed();
                }

                // Auto-Scroll
                else if (btnPlay == v) {
                    startAutoScroll();
                    linearLayoutAutoScroll.setVisibility(View.VISIBLE);
                }
                else if (btnFasterAutoScroll == v) {
                    long vibrationDuration = VIBRATION_DURATION_ON_CLICK;
                    if (autoScrollSpeed + 1 == 0)
                        autoScrollSpeed = 1;
                    else if (autoScrollSpeed + 1 <= 8)
                        autoScrollSpeed++;
                    else // Too fast
                        vibrationDuration = VIBRATION_DURATION_ON_CLICK * 10;
                    Functions.vibrateDevice(context, vibrationDuration);
                }
                else if (btnSlowerAutoScroll == v) {
                    long vibrationDuration = VIBRATION_DURATION_ON_CLICK;
                    if (autoScrollSpeed - 1 == 0)
                        autoScrollSpeed = -1;
                    else if (autoScrollSpeed - 1 >= -8)
                        autoScrollSpeed--;
                    else // Too slow
                        vibrationDuration = VIBRATION_DURATION_ON_CLICK * 10;
                    Functions.vibrateDevice(context, vibrationDuration);
                }
                else if (btnStopAutoScroll == v) {
                    autoScrollScheduledExecutorService.shutdown();
                    linearLayoutAutoScroll.setVisibility(View.INVISIBLE);
                }
            }
            else
                Toasty.info(context, "השיר עדיין לא נטען").show();
        }
    }
    private class MyOnLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            if (song != null)
            {
                if (btnTransposeUp == v || btnTransposeDown == v) {
                    song.transpose(-1 * song.getTranspose());
                    updateUI();
                }
                else if (btnSizeUp == v || btnSizeDown == v) {
                    float px = tvChords.getTextSize();
                    float sp = px / getResources().getDisplayMetrics().scaledDensity;
                    if (tvChords.getTag() == null)
                        tvChords.setTag(sp);
                    tvChords.setTextSize(Dimension.SP, (float) tvChords.getTag());
                    tvSize.setText("0");
                }
                return true;
            }
            else
                Toasty.info(context, "השיר עדיין לא נטען").show();
            return false;
        }
    }

    float dX, dY; // used in 'MyOnTouchListener'
    private class MyOnTouchListener implements View.OnTouchListener {
        private Point screenSize;

        public MyOnTouchListener() {
            // Calculate Screen Size in Pixels
            Display display = getWindowManager().getDefaultDisplay();
            screenSize = new Point();
            display.getSize(screenSize);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    ViewPropertyAnimator animation = v.animate();
                    if (event.getRawX() + dX >= 0 && event.getRawX() + dX + v.getWidth() <= screenSize.x) // Make sure view is inside the screen dimensions
                        animation.x(event.getRawX() + dX);
                    if (event.getRawY() + dY >= 0 && event.getRawY() + dY + v.getHeight() <= screenSize.y) // Make sure view is inside the screen dimensions
                        animation.y(event.getRawY() + dY);

                    animation
                            .setDuration(0)
                            .start();

                    break;
            }
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        // Save favorite and transpose status before exit
        if (user != null
                && btnFavorite.getTag() != null && (boolean) btnFavorite.getTag() == FAVORITE_HEART_ENABLED) {

            // Save song chords in original tone at the database
            int backupTranspose = song.getTranspose();
            song.transpose(-1 * backupTranspose);

            DocumentReference df = db.document(FIRESTORE_USERS_PATH + user.getUid() + "/favorites/" + songUid);
            Map<String, Object> favoriteSong = new HashMap<>();
            favoriteSong.put("name", song.getName());
            favoriteSong.put("singer", song.getSinger());
            favoriteSong.put("rating", song.getRating());
            favoriteSong.put("reference", docRefString);
            favoriteSong.put("transpose", backupTranspose);
            favoriteSong.put("favorite_time_milisecond", java.util.Calendar.getInstance().getTime().getTime());
            favoriteSong.put("youtube", song.getYoutube());
            df.set(favoriteSong).addOnSuccessListener(this, new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Song has added to favorite");
                }
            });
        }

        // AdMob
        if (mAdView != null) {
            mAdView.removeAllViews();
            mAdView.destroy();
        }

        super.onDestroy();
    }

    /**
     * Make chords clickable and highlight (Ab, C#m ...)
     * <p>Make sections words bold and underline (פיזמון, פתיחה ...)
     */
    private void makeChordsInteractive() {
        SpannableString ss = new SpannableString(song.getChords());
        makeChordsClickableAndHighlight(ss, song.getChords());
        makeSectionWordsBold(ss, song.getChords());
        tvChords.setText(ss);
    }
    /**
     * Find sections words within the text and <p>
     * Make sections words bold and underline. (פיזמון, פתיחה ...)
     */
    private void makeSectionWordsBold(SpannableString ss, String chords) {
        for (int i = 0; i < CHORDS_SECTIONS.length; i++) {
            int startIndex = chords.indexOf(CHORDS_SECTIONS[i]);
            while (startIndex != -1) {
                ss.setSpan(new UnderlineSpan(), startIndex, startIndex + CHORDS_SECTIONS[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new StyleSpan(Typeface.BOLD), startIndex, startIndex + CHORDS_SECTIONS[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        Pattern p;
        Matcher m;
        chordsOnSong = song.getChordsListOnSong();
        for (int i = 0; i < chordsOnSong.size(); i++) {
            p = Pattern.compile(regexSingelChord.replace("CHORD", chordsOnSong.get(i)));
            m = p.matcher(chords);
            while (m.find()) {
                ChordClickableSpan chordClickableSpan = new ChordClickableSpan(this, chordsOnSong.get(i));
                ss.setSpan(chordClickableSpan, m.start(), m.start() + chordsOnSong.get(i).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        tvChords.setMovementMethod(LinkMovementMethod.getInstance()); // Chords are clickable
    }

    private void updateUI()
    {
        if (song != null) {
            tvSongName.setText(song.getName());
            tvSinger.setText(song.getSinger());
            tvTranspose.setText(song.getTranspose() + "");

            makeChordsInteractive();
        }
    }

    private void scrollToRight() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                horizontalScrollView.smoothScrollTo(tvChords.getWidth(), 0);
            }
        }, 500);
    }
    private void startAutoScroll(){
        appBarLayout.setExpanded(false); // Collapse 'appBarLayout' (Song info section)

        autoScrollScheduledExecutorService = Executors.newScheduledThreadPool(5);
        autoScrollScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                scrollView.smoothScrollBy(0, autoScrollSpeed);
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void fetchSongFromDB()
    {
        if (!docRefString.contains(FIRESTORE_USERS_PATH)) {
            mDocRef = db.document(docRefString);
            mDocRef.get().addOnSuccessListener(this, new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        songSnapshot = documentSnapshot.getData();
                        try {
                            song = new Song(
                                (String) songSnapshot.get("name"),
                                (String) songSnapshot.get("singer"),
                                (String) songSnapshot.get("author"),
                                (String) songSnapshot.get("composer"),
                                (String) songSnapshot.get("youtube"),
                                ((Long) songSnapshot.get("rating")).intValue(),
                                (Long) songSnapshot.get("upload_time_milisecond"),
                                ((Long) songSnapshot.get("easy_tone")).intValue(),
                                (String) songSnapshot.get("user_uploader_uid"),
                                (String) songSnapshot.get("user_uploader_name"),
                                (String) songSnapshot.get("chords"),
                                ((Long) songSnapshot.get("views")).intValue(),
                                ((Long) songSnapshot.get("views_month")).intValue() + ((Long) songSnapshot.get("views_day")).intValue()
                            );
                            if (songSnapshot.get("is_english") != null && (Boolean) songSnapshot.get("is_english")) {
//                                tvChords.setGravity(Gravity.LEFT);
                            }

                            startTimerViewsInc(); // Increase 'views_day' & 'views' after 5 seconds
                        }
                        catch (Exception e) { Log.d(TAG, "ChordViewerActivity:fetchSongFromDB() - " + e.getMessage()); }

                        // Hide ProgressBar (Loading)
                        progressBarChords.setVisibility(View.GONE);

                        updateUI();
                        isFavorite();
                        scrollToRight();
                    }
                }
            });
        }
    }

    /**
     * Start a timer to 'increaseSongViews()' only after 20 seconds.
     */
    private void startTimerViewsInc() {
        timerViewsInc = new CountDownTimer(20000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "'timerViewsInc' seconds remaining: " + millisUntilFinished / 1000);
            }
            @Override
            public void onFinish() {
                increaseSongViews();
            }
        }.start();
    }
    /**
     * Cancel the timer which 'increaseSongViews()' after 5 seconds.
     */
    private void cancelTimerViewsInc() {
        if (timerViewsInc != null)
            timerViewsInc.cancel();
    }
    /**
     * Increase 'views' & 'views_day' to the song (popularity)
     */
    private void increaseSongViews() {
        // Increase 'views' & 'views_day' to the song (popularity)
        // Admin doesn't see ads so doesn't affect views
        List<ViewedSong> viewedSongs = ViewedSong.readViewedSongsToPref(activity);
        if (user == null || !Functions.isAdmin(user.getUid())) {
            if (!ViewedSong.isSongViewedToday(activity, viewedSongs, songUid)) { // Increase view only if the user have not viewed this song in the last 24 hours
                Map<String, Object> viewsMap = new HashMap<>();
                int views = 1, viewsDay = 1;
                if (songSnapshot.get("views") != null)
                    views = ((Long) songSnapshot.get("views")).intValue() + 1;
                if (songSnapshot.get("views_day") != null)
                    viewsDay = ((Long) songSnapshot.get("views_day")).intValue() + 1;
                viewsMap.put("views", views);
                viewsMap.put("views_day", viewsDay);
                mDocRef.set(viewsMap, SetOptions.merge());

                // Save to SharedPreferences that this song is viewed in the last 24 hours
                // so we want increase his views for today
                viewedSongs.add(new ViewedSong(Calendar.getInstance(), songUid));
                ViewedSong.writeViewedSongsToPref(activity, viewedSongs);
            }
        }
    }

    /**
     * Check if song is favorite by this user <p>
     * And update the UI if needed
     */
    private void isFavorite() {
        if (user != null && docRefString.contains(FIRESTORE_SONG_PATH)) {
            DocumentReference df = db.document(FIRESTORE_USERS_PATH + user.getUid());
            df.collection("favorites").document(songUid).get().addOnSuccessListener(this, new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        Object o = documentSnapshot.get("transpose");
                        if (o != null) {
                            song.transpose(((Number) o).intValue()); // Long to Integer
                            updateUI();
                        }
                        btnFavorite.setBackgroundResource(FAVORITE_HEART_ENABLED_ICON);
                        btnFavorite.setTag(FAVORITE_HEART_ENABLED);
                    }
                }
            });
        }
    }

    private void login() {
        Intent intent = Functions.login(activity);
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

                btnFavorite.performClick();
            }
            else if (response != null) { // If response is null the user canceled the
                Toasty.error(context, response.getError().getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void dialogSongInfo() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(context);
        View mView = getLayoutInflater().inflate(R.layout.dialog_chord_viewer_info, null);
        mBuilder.setView(mView);
        final Dialog dialog = mBuilder.create();

        final TextView tvAuthor = mView.findViewById(R.id.tvAuthor);
        final TextView tvComposer = mView.findViewById(R.id.tvComposer);
        final TextView tvEasyTone = mView.findViewById(R.id.tvEasyTone);
        final TextView tvYouTube = mView.findViewById(R.id.tvYouTube);
        final TextView tvYouTubeTitle = mView.findViewById(R.id.tvYouTubeTitle);
        final TextView tvUploader = mView.findViewById(R.id.tvUploader);
        final TextView tvUploaderTitle = mView.findViewById(R.id.tvUploaderTitle);
        final TextView tvViewsMonth = mView.findViewById(R.id.tvViewsMonth);
        final TextView tvViewsMonthTitle = mView.findViewById(R.id.tvViewsMonthTitle);
        final TextView tvViewsTotal = mView.findViewById(R.id.tvViewsTotal);
        final TextView tvViewsTotalTitle = mView.findViewById(R.id.tvViewsTotalTitle);

        tvAuthor.setText(song.getAuthor());
        tvComposer.setText(song.getComposer());
        tvEasyTone.setText(song.getEasyTone() + "");
        tvUploader.setText(song.getUploaderName());
        tvViewsMonth.setText(song.getViewsMonth() + "");
        tvViewsTotal.setText(song.getViewsTotal() + "");
        if (Functions.isAdmin(song.getUploaderUid()))
            tvUploader.setText("Chordof");

        if (!song.getYoutube().isEmpty()) {
            tvYouTube.setText(Html.fromHtml("<a href=\"" + song.getYoutube() + "\">לחץ כאן" + "</a>"));
            tvYouTube.setClickable(true);
            tvYouTube.setMovementMethod(LinkMovementMethod.getInstance());
        }
        else {
            tvYouTube.setVisibility(View.GONE);
            tvYouTubeTitle.setVisibility(View.GONE);
        }

        tvUploader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((boolean) btnEditMode.getTag() || !Functions.isAdmin(song.getUploaderUid())) { // If the uploader is an admin - Don't show his profile. If EditMode is enabled - Show his profile
                    Intent intent = new Intent(context, UserActivity.class);
                    intent.putExtra(USER_ACTIVITY_UID_EXTRA_TAG, song.getUploaderUid());
                    startActivity(intent);
                }
            }
        });

        if ((boolean) btnEditMode.getTag() && Functions.isAdmin(user)) {
            tvYouTube.setText(song.getYoutube()); // See the link and not href
            tvAuthor.setFocusableInTouchMode(true);
            tvComposer.setFocusableInTouchMode(true);
            tvEasyTone.setFocusableInTouchMode(true);
            tvYouTube.setFocusableInTouchMode(true);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    song = new Song(
                            song.getName(),
                            song.getSinger(),
                            tvAuthor.getText().toString(),
                            tvComposer.getText().toString(),
                            tvYouTube.getText().toString(),
                            song.getRating(),
                            song.getUploadTimeMiliSec(),
                            Integer.valueOf(tvEasyTone.getText().toString()),
                            song.getUploaderUid(),
                            song.getUploaderName(),
                            song.getChords(),
                            song.getViewsTotal(),
                            song.getViewsMonth()
                    );
                }
            });
            tvYouTube.setVisibility(View.VISIBLE);
            tvYouTubeTitle.setVisibility(View.VISIBLE);
            tvViewsMonth.setVisibility(View.VISIBLE);
            tvViewsMonthTitle.setVisibility(View.VISIBLE);
            tvViewsTotal.setVisibility(View.VISIBLE);
            tvViewsTotalTitle.setVisibility(View.VISIBLE);
        }
        dialog.show();
    }

    /**
     * Send email to the user who uploaded the song
     */
    private void sendEmailToSongUploader() {
        // If it's uploaded by admin we won't send an email:
        // When this function is called - 'user' must be an admin,
        // So we will compare it with the uploaderUID to check if we need to send an email.
        if (!user.getUid().equals(song.getUploaderUid())) {
            String subject = "", body = "";
            if (mDocRef.getPath().contains(FIRESTORE_SONG_PENDING_PATH)) {
                subject = EMAIL_SUBJECT_USER_UPLOADED_SONG;
                body = EMAIL_BODY_USER_UPLOADED_SONG.replace("user", song.getUploaderName())
                        .replace("songName", song.getName());
            }
            else if (mDocRef.getPath().contains(FIRESTORE_SONG_REQUESTS_PATH)) {
                subject = EMAIL_SUBJECT_USER_REQUEST_SONG_APPROVED;
                body = EMAIL_BODY_USER_REQUEST_SONG_APPROVED.replace("user", song.getUploaderName())
                        .replace("songName", song.getName());
            }

            Functions.sendEmail(
                    context,
                    song,
                    subject,
                    body);
        }
    }




    /* Administrator Addons */
    private void toggelEditMode(boolean isEnabled) {
        tvChords.setFocusable(isEnabled);
        tvSongName.setFocusable(isEnabled);
        tvSinger.setFocusable(isEnabled);
        tvChords.setFocusableInTouchMode(isEnabled);
        tvSongName.setFocusableInTouchMode(isEnabled);
        tvSinger.setFocusableInTouchMode(isEnabled);
        btnEditMode.setTag(isEnabled);

        if (isEnabled) {
            btnKeyboard.setVisibility(View.VISIBLE);
            btnUpdate.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            mAdView.setVisibility(View.INVISIBLE);
            tvSinger.setOnClickListener(null); // disable Search for artist onClick
            tvChords.setMovementMethod(ArrowKeyMovementMethod.getInstance()); // Chords are not clickable
            Toasty.info(context, "מצב עריכה הופעל").show();
        }
        else {
            btnKeyboard.setVisibility(View.GONE);
            btnUpdate.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            mAdView.setVisibility(View.VISIBLE);
            tvSinger.setOnClickListener(new MyOnClickListener()); // enable Search for artist onClick
            tvChords.setMovementMethod(LinkMovementMethod.getInstance()); // Chords are clickable
            Toasty.info(context, "מצב עריכה הופסק").show();
        }
    }
    /**
     * @param isKeyboardVisible TRUE - Keyboard is visible
     */
    private void toggleKeyboardVisibility(boolean isKeyboardVisible) {
        if (isKeyboardVisible) {
            this.isKeyboardVisible = false;
            tvChords.setShowSoftInputOnFocus(true); // Show keyboard on press
            Toasty.info(context, "המקלדת תוצג כעת").show();
        }
        else
        {
            this.isKeyboardVisible = true;
            tvChords.setShowSoftInputOnFocus(false); // Don't show keyboard on press
            Toasty.info(context, "המקלדת תוסתר כעת").show();
        }
    }

    /**
     * Check if the song is already exist in Firebase.<p>
     * If it is exist, a Dialog will appear to insure the upload process.<p>
     * If it is not exist, the song will be uploaded to Firebase.
     * @param songName
     */
    private void checkIfSongExistInFirebase(String songName) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        uploadSongToDB();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // No button clicked
                        break;
                }
            }
        };

        db.collection(FIRESTORE_SONG_PATH).whereEqualTo("name", songName).get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful())
                        if (task.getResult().size() > 0) {
                            // Song was found in Firebase - We will alert before uploading it
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setMessage("השיר כבר קיים, האם להעלות בכל זאת?").setPositiveButton("כן", dialogClickListener)
                                    .setNegativeButton("לא", dialogClickListener).show();
                            return;
                        }
                    // Song was not found in Firebase - We can upload it
                    uploadSongToDB();
                }
            });
    }

    /**
     * Upload a new song
     */
    private void uploadSongToDB() {
        if (Functions.isAdmin(user)) {
            if (!Functions.isFetchingYoutube) {
                saveChanges();
                DocumentReference df = db.collection(FIRESTORE_SONG_PATH).document();
                final Map<String, Object> songMap = song.getSongMap();
                if (mDocRef != null) {
                    songMap.put("user_uploader_uid", songSnapshot.get("user_uploader_uid"));
                    songMap.put("user_uploader_name", songSnapshot.get("user_uploader_name"));
                    songMap.put("upload_time_milisecond", java.util.Calendar.getInstance().getTime().getTime()); // Replace the user upload date with the current time (To be shown first at 'Recent Songs')
                }
                songMap.put("views_day", 0);
                songMap.put("views_array", new ArrayList<Integer>(Collections.nCopies(30, 0)));
                df.set(songMap).addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (mDocRef != null)
                            mDocRef.delete();
                        sendEmailToSongUploader();
                        Intent intent = new Intent(context, SuccessfullyUploadedActivity.class);
                        intent.putExtra(SONG_NAME_EXTRA_TAG, song.getName());
                        intent.putExtra(SINGER_NAME_EXTRA_TAG, song.getSinger());
                        startActivity(intent);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toasty.error(context, "שגיאה בהעלאת השיר").show();
                    }
                });
            }
            else
                Toasty.info(context, "מאתר לינק לYouTube").show();
        }
    }

    /**
     * Update an exisiting song
     */
    private void updateSongOnDB() {
        if (Functions.isAdmin(user)) {
            saveChanges();
            Map<String, Object> songMap = song.getSongMap();
            songMap.remove("rating");
            songMap.remove("views");
            songMap.remove("views_month");
            songMap.remove("upload_time_milisecond");
            mDocRef.update(songMap).addOnSuccessListener(this, new OnSuccessListener<Void>() {
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
                    Toasty.error(context, "שגיאה בעדכון השיר").show();
                }
            });
        }
    }
    private void confirmUpdateSong() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if (mDocRef == null || mDocRef.getPath().contains(FIRESTORE_SONG_PENDING_PATH))
                            //uploadSongToDB();
                            checkIfSongExistInFirebase(song.getName());
                        else if (mDocRef.getPath().contains(FIRESTORE_SONG_REQUESTS_PATH)) {
                            sendEmailToSongUploader();
                            mDocRef.delete();
                            onBackPressed();
                        }
                        else
                            updateSongOnDB();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("להעלות?").setPositiveButton("כן", dialogClickListener)
                .setNegativeButton("לא", dialogClickListener).show();
    }
    private void saveChanges() {
        song = new Song(
                tvSongName.getText().toString().trim(),
                tvSinger.getText().toString().trim(),
                song.getAuthor().trim(),
                song.getComposer().trim(),
                song.getYoutube().trim(),
                song.getRating(),
                song.getUploadTimeMiliSec(),
                song.getEasyTone(),
                song.getUploaderUid(),
                song.getUploaderName(),
                tvChords.getText().toString()
        );
    }

    // Delete Song
    private void deleteSong() {
        if (Functions.isAdmin(user)) {
            mDocRef.delete();
            Toasty.info(context, "השיר נמחק").show();
            onBackPressed();
        }
    }
    private void confirmDeleteSong() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        deleteSong();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("למחוק?").setPositiveButton("כן", dialogClickListener)
                .setNegativeButton("לא", dialogClickListener).show();
    }
    /* Administrators Addons - END */
}
