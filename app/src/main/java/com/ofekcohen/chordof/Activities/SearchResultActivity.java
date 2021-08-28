package com.ofekcohen.chordof.Activities;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.ofekcohen.chordof.Adapters.SongsRecyclerViewAdapter;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.Core.NativeAds.NativeAds;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_SONG_PATH;
import static com.ofekcohen.chordof.Core.Constants.SEARCH_BY_SINGER_NAME;
import static com.ofekcohen.chordof.Core.Constants.SEARCH_BY_SONG_NAME;
import static com.ofekcohen.chordof.Core.Constants.SEARCH_EXTRA_TAG;

public class SearchResultActivity extends AppCompatActivity {

    private static final int NUMBER_OF_ADS = 5;
    // Firestore
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    CollectionReference collectionReference = firestore.collection(FIRESTORE_SONG_PATH);

    private Activity context;
    private String searchQuery;
    private RecyclerView rvResultsSongs;
    private ArrayList<Object> songsResult;
    private ProgressBar progressBar;
    private TextView tvResultFor, tvNoResult;

    // Native Ads
    private AdLoader adLoader;
    private List<UnifiedNativeAd> mNativeAds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein_500, R.anim.fadeout_500);
        setContentView(R.layout.activity_search_result);
        context = this;

        // Extra
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                searchQuery = extras.getString(SEARCH_EXTRA_TAG);
                if (searchQuery != null) {
                    searchQuery = searchQuery.trim();
                    if (searchQuery.length() < 3) {
                        Toasty.info(this, "הקלד 3 תווים לפחות", Toast.LENGTH_LONG).show();
                        onBackPressed();
                    }
                }
            }
            else {
                Toasty.error(this, "שגיאה בחיפוש", Toast.LENGTH_LONG).show();
                onBackPressed();
            }
        }

        rvResultsSongs = findViewById(R.id.rvFavoritesSongs);
        progressBar = findViewById(R.id.progressBar);
        tvResultFor = findViewById(R.id.tvResultFor);
        tvNoResult = findViewById(R.id.tvNoResult);

        fetchResult();
//        loadNativeAds();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fadein_500, R.anim.fadeout_500);
    }

    private void fetchResult()
    {
        fetchResult(SEARCH_BY_SONG_NAME);
    }
    /**
     * @param searchBy EARCH_BY_SONG_NAME / SEARCH_BY_SINGER_NAME
     */
    private void fetchResult(final String searchBy)
    {
//        Query songsQuery = collectionReference.orderBy(searchBy).orderBy("views").orderBy("rating").startAt(searchQuery).endAt(searchQuery + "\uf8ff").limit(10);
        Query songsQuery = collectionReference.orderBy(searchBy).orderBy("views", Query.Direction.DESCENDING).orderBy("rating", Query.Direction.DESCENDING).startAt(searchQuery.toUpperCase()).endAt(searchQuery.toLowerCase() + "\uf8ff");
        if (searchBy.equals(SEARCH_BY_SONG_NAME)) // If we search for a specific song we limit to 10 results. If we search for an artist we would like to see all of his songs
            songsQuery = songsQuery.limit(10);
        songsQuery.get().addOnCompleteListener(this, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (!task.getResult().isEmpty()) {
                        songsResult = new ArrayList<>();
                        for (int i = 0; i < task.getResult().size(); i++) {
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(i);
                            Map<String, Object> map = documentSnapshot.getData();
                            songsResult.add(new Song(
                                    (String) map.get("name"),
                                    (String) map.get("singer"),
                                    (String) map.get("author"),
                                    (String) map.get("composer"),
                                    (String) map.get("youtube"),
                                    ((Long) map.get("rating")).intValue(),
                                    ((Long) map.get("upload_time_milisecond")).intValue(),
                                    ((Long) map.get("easy_tone")).intValue(),
                                    (String) map.get("user_uploader_uid"),
                                    (String) map.get("user_uploader_name"),
                                    (String) map.get("chords")
                            ));
                            ((Song) songsResult.get(songsResult.size() - 1)).setFirestoreReference(documentSnapshot.getReference().getPath());

                            SongsRecyclerViewAdapter songsRecyclerViewAdapter = Functions.createRecyclerView(context, songsResult, rvResultsSongs, progressBar);
                            NativeAds nativeAds = new NativeAds(context, songsResult, rvResultsSongs, progressBar);
                        }
                    }
                    else if (searchBy.equals(SEARCH_BY_SONG_NAME))
                        fetchResult(SEARCH_BY_SINGER_NAME);
                    else
                    {
                        progressBar.setVisibility(View.INVISIBLE);
                        tvNoResult.setText("לא מצאנו תוצאות עבור: " + searchQuery);
                        tvNoResult.setVisibility(View.VISIBLE);
                    }
                }
                else
                {
                    Toasty.error(context, task.getException().toString(), Toast.LENGTH_LONG).show();
                    Log.d(Constants.TAG, task.getException().toString());
                }
            }
        });
    }
}
