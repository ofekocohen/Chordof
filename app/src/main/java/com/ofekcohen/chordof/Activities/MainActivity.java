package com.ofekcohen.chordof.Activities;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.testing.FakeReviewManager;
import com.google.android.play.core.tasks.Task;
import com.ofekcohen.chordof.BuildConfig;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.Fragments.HomeFragment;
import com.ofekcohen.chordof.Fragments.MySongsFragment;
import com.ofekcohen.chordof.Fragments.SearchResultFragment;
import com.ofekcohen.chordof.Fragments.UploadSongFragment;
import com.ofekcohen.chordof.R;
import com.ofekcohen.chordof.Services.MyAlarmBroadcastReceiver;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.EMAIL_BODY_USER_UPLOADED_SONG;
import static com.ofekcohen.chordof.Core.Constants.EMAIL_SUBJECT_USER_UPLOADED_SONG;
import static com.ofekcohen.chordof.Core.Constants.SEARCH_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.SHARED_PREFERENCES_UPDATE_VERSION_CODE;
import static com.ofekcohen.chordof.Core.Constants.VOICE_SEARCH;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private ConstraintLayout clContainer;
    private HomeFragment homeFragment;
    private MySongsFragment mySongsFragment;
    private UploadSongFragment uploadSongFragment;
    private TextView tvHome, tvMySongs, tvUploadSong;
    private ImageView imgInfo, imgVoiceSearch;
    private ConstraintLayout clHome, clMySongs, clUploadSong;
    private View underLineHome, underLineMySongs, underLineUploadSong;
    private EditText txtSearchBar;
    private ReviewInfo reviewInfo;

    // Fonts
    private Typeface normalFont;
    private Typeface boldFont;

    private class MenuClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE); // Empty all fragment stack
            switch (v.getId()) {
                case R.id.clHome:
                    setFragment(homeFragment, false);
                    return;
                case R.id.clMySongs:
                    setFragment(mySongsFragment, false);
                    return;
                case R.id.clUploadSong:
                    setFragment(uploadSongFragment, false);
                    return;
                case R.id.imgSettings:
                    //Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://chordof.co.nf/privacy_policy.html"));
                    startActivity(new Intent(context, SettingsActivity.class));
                    return;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein_500, R.anim.fadeout_500);
        setContentView(R.layout.activity_main);
        context = this;

        // Initiate Fragments
        homeFragment = new HomeFragment();
        mySongsFragment = new MySongsFragment();
        uploadSongFragment = new UploadSongFragment();

        // findViewById
        View menuNavigator = findViewById(R.id.menu_navigator);
        tvHome = menuNavigator.findViewById(R.id.tvHome);
        tvMySongs = menuNavigator.findViewById(R.id.tvMySongs);
        tvUploadSong = menuNavigator.findViewById(R.id.tvUploadSong);
        imgInfo = menuNavigator.findViewById(R.id.imgSettings);
        clHome = menuNavigator.findViewById(R.id.clHome);
        clMySongs = menuNavigator.findViewById(R.id.clMySongs);
        clUploadSong = menuNavigator.findViewById(R.id.clUploadSong);
        underLineHome = menuNavigator.findViewById(R.id.underLineHome);
        underLineMySongs = menuNavigator.findViewById(R.id.underLineMySongs);
        underLineUploadSong = menuNavigator.findViewById(R.id.underLineUploadSong);
        txtSearchBar = findViewById(R.id.searchBar).findViewById(R.id.txtSearchBar);
        imgVoiceSearch = findViewById(R.id.imgVoiceSearch);
        clContainer = findViewById(R.id.clContainer);

        // Listener
        MenuClickListener menuClickListener = new MenuClickListener();
        clHome.setOnClickListener(menuClickListener);
        clMySongs.setOnClickListener(menuClickListener);
        clUploadSong.setOnClickListener(menuClickListener);
        imgInfo.setOnClickListener(menuClickListener);

        MyOnClickListener myOnClickListener = new MyOnClickListener();
        txtSearchBar.setOnClickListener(myOnClickListener);
        clContainer.setOnClickListener(myOnClickListener);
        imgVoiceSearch.setOnClickListener(myOnClickListener);

        txtSearchBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                txtSearchBar.setText("");
                return false;
            }
        });

        // Fonts
        normalFont = ResourcesCompat.getFont(this, R.font.segoeui);
        boldFont = ResourcesCompat.getFont(this, R.font.segoeuib);

        // Set Fragment
        setFragment(homeFragment, false);

        // Check for updates
        checkForUpdates();
    }

    @Override
    protected void onStart() {
        super.onStart();
        txtSearchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                performSearch();
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        // אם פתחנו פראגמנט בתוך פראמנט - קודם כל נחזיר את הפראגמנט שהיה פתוח
        // לא לשכוח להשתמש בaddToBackStack לפני המעבר לפראגמנט
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frmContainer);
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count == 0) { // Fragment Stack is empty
            if (!(currentFragment instanceof HomeFragment)) // We will exit app only after visit HomeFragment
                setFragment(homeFragment, false);
            else
                super.onBackPressed();
        } else {
            // Fix highlight at the bottom (If we press 'back' on either of those fragments we sure arrived from HomeFragment so we will highlight it)
            if (currentFragment instanceof UploadSongFragment || currentFragment instanceof MySongsFragment) {
                clearSelection();
                underLineHome.setVisibility(View.VISIBLE);
                tvHome.setTypeface(boldFont);
            }

            // Pop previous fragment
            getSupportFragmentManager().popBackStack();
        }
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (imgVoiceSearch == v) {
                startVoiceSearch();
            }
            else if (clContainer == v) {
                txtSearchBar.clearFocus();
                txtSearchBar.setText("חיפוש");
            }
        }
    }

    /**
     * Set fragment to 'searchResultFragment' and start search by 'txtSearchBar.getText()'
     */
    private void performSearch() {
        Fragment searchResultFragment = new SearchResultFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.fadein_300, R.anim.fadeout_300);
        fragmentTransaction.replace(R.id.frmContainer, searchResultFragment);
        fragmentTransaction.addToBackStack(null);

        // Extras
        Bundle bundle = new Bundle();
        bundle.putString(SEARCH_EXTRA_TAG, txtSearchBar.getText().toString());
        searchResultFragment.setArguments(bundle);

        fragmentTransaction.commit();
    }

    public void setFragment(Fragment fragment, boolean addToBackStack)
    {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.fadein_300, R.anim.fadeout_300);
        fragmentTransaction.replace(R.id.frmContainer, fragment);
        if (addToBackStack)
            fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

        // Highlight Section
        clearSelection();
        if (fragment instanceof HomeFragment) {
            underLineHome.setVisibility(View.VISIBLE);
            tvHome.setTypeface(boldFont);
        }
        else if (fragment instanceof MySongsFragment) {
            underLineMySongs.setVisibility(View.VISIBLE);
            tvMySongs.setTypeface(boldFont);
        }
        else if (fragment instanceof UploadSongFragment) {
            underLineUploadSong.setVisibility(View.VISIBLE);
            tvUploadSong.setTypeface(boldFont);
        }
    }

    private void clearSelection()
    {
        // Clear Under Lines
        underLineHome.setVisibility(View.INVISIBLE);
        underLineMySongs.setVisibility(View.INVISIBLE);
        underLineUploadSong.setVisibility(View.INVISIBLE);

        // Clear Fonts
        tvHome.setTypeface(normalFont);
        tvMySongs.setTypeface(normalFont);
        tvUploadSong.setTypeface(normalFont);
    }

    /**
     * Check in SharedPreferences if there is a newer version to the app.
     * <p>If there is one -> start 'UpdateActivity'.
     */
    private void checkForUpdates() {
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_UPDATE_VERSION_CODE, Context.MODE_PRIVATE);
        int versionCode = sharedPref.getInt(SHARED_PREFERENCES_UPDATE_VERSION_CODE, BuildConfig.VERSION_CODE);
        if (BuildConfig.VERSION_CODE < versionCode)
            startActivity(new Intent(context, UpdateActivity.class));
        else
            createAlarmForCheckingUpdates();
    }
    /**
     * Create AlarmManager (schedule task) to check if there is a newer app version.
     * <p>It triggers every 7 days.
     * <p>If the alarm is already activated, nothing happens.
     */
    private void createAlarmForCheckingUpdates() {
        boolean alarmExist = (PendingIntent.getBroadcast(context, 0,
                new Intent(getApplicationContext().getPackageName()),
                PendingIntent.FLAG_NO_CREATE) != null);
        if (!alarmExist) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, MyAlarmBroadcastReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            if (alarmManager != null) {
                alarmManager.setRepeating(AlarmManager.RTC, Calendar.getInstance().getTimeInMillis(), AlarmManager.INTERVAL_DAY * 7, pendingIntent);
            }
        }
    }

    /**
     * start Google Voice search
     */
    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL");

        try {
            startActivityForResult(intent, VOICE_SEARCH);
        } catch (ActivityNotFoundException a) {
            Toasty.error(context, "המכשיר שלך לא תומך בחיפוש קולי", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle Google Voice search
        if (requestCode == VOICE_SEARCH && resultCode == Activity.RESULT_OK && null != data) {
            txtSearchBar.setText(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0));
            performSearch();
        }
    }
}
