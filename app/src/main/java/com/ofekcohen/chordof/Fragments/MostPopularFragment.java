package com.ofekcohen.chordof.Fragments;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ofekcohen.chordof.Adapters.SongsRecyclerViewAdapter;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Classes.ViewedSong;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.Core.NativeAds.NativeAds;
import com.ofekcohen.chordof.R;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_SONG_PATH;
import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_SORT_BY_ALLTIME;
import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_SORT_BY_MONTH;
import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_SORT_BY_WEEK;
import static com.ofekcohen.chordof.Core.Constants.SHARED_PREFERENCES_MOST_POPULAR_SORT_BY_LAST_SELECTION;
import static com.ofekcohen.chordof.Core.Constants.SHARED_PREFERENCES_VIEWED_SONGS_UID;
import static com.ofekcohen.chordof.Core.Constants.TAG;

public class MostPopularFragment extends Fragment {

    // Firebase
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference collectionReference = firestore.collection(FIRESTORE_SONG_PATH);

    private Context context;
    private RecyclerView rvMostPopular;
    private ArrayList<Object> songs;
    private ArrayList[] loadedSongs = new ArrayList[3]; // Sort by (all time / week / month)
    private ProgressBar progressBar;
    private TextView tvNoResult, tvTitle, tvOrderByEver, tvOrderByWeek, tvOrderByMonth;
    private LinearLayout linearLayoutOrderBy;
    private Typeface normalFont, boldFont;
    private String sortResultsBy;
    private Button btnBack;

    public MostPopularFragment() { }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_most_list, container, false);
        context = getContext();

        rvMostPopular = view.findViewById(R.id.rvMostPopular);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoResult = view.findViewById(R.id.tvNoResult);
        tvTitle = view.findViewById(R.id.tvTitle);
        linearLayoutOrderBy = view.findViewById(R.id.linearLayoutOrderBy);
        tvOrderByEver = view.findViewById(R.id.tvOrderByEver);
        tvOrderByWeek = view.findViewById(R.id.tvOrderByWeek);
        tvOrderByMonth = view.findViewById(R.id.tvOrderByMonth);
        btnBack = view.findViewById(R.id.btnBack);

        MyOnClickListener myOnClickListener = new MyOnClickListener();
        tvOrderByEver.setOnClickListener(myOnClickListener);
        tvOrderByWeek.setOnClickListener(myOnClickListener);
        tvOrderByMonth.setOnClickListener(myOnClickListener);
        btnBack.setOnClickListener(myOnClickListener);

        rvMostPopular.addOnScrollListener(new MyOnScrollListener());

        tvTitle.setText("הנצפים ביותר");
        linearLayoutOrderBy.setAlpha(0.0f);
        linearLayoutOrderBy.setVisibility(View.VISIBLE);

        // Fonts
        normalFont = ResourcesCompat.getFont(context, R.font.segoeui);
        boldFont = ResourcesCompat.getFont(context, R.font.segoeuib);

        readSortResultsByToPref();
        fetchResult(this.sortResultsBy);

        return view;
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (btnBack == v)
                getFragmentManager().popBackStack();
            else if (tvOrderByEver == v)
                fetchResult(FIRESTORE_SORT_BY_ALLTIME);
            else if (tvOrderByMonth == v)
                fetchResult(FIRESTORE_SORT_BY_MONTH);
            else if (tvOrderByWeek == v)
                fetchResult(FIRESTORE_SORT_BY_WEEK);
        }
    }
    private class MyOnScrollListener extends RecyclerView.OnScrollListener {
        private int y;
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (y > 0)
                linearLayoutOrderBy.animate().alpha(0.0f); // Fade-out
            else
                linearLayoutOrderBy.animate().alpha(1.0f); // Fade-in
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            y = dy;
        }
    }

    /**
     * @param sortResultsBy { FIRESTORE_SORT_BY_ALLTIME, FIRESTORE_SORT_BY_MONTH, FIRESTORE_SORT_BY_WEEK }
     */
    private void fetchResult(String sortResultsBy)
    {
        selectSortSelection(sortResultsBy);

        if (loadedSongs[indexSortResultsBy(sortResultsBy)] == null) {
            Query songsQuery = collectionReference.orderBy(sortResultsBy, Query.Direction.DESCENDING).limit(100);
            songsQuery.get().addOnCompleteListener(getActivity(), new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            songs = new ArrayList<>();
                            for (int i = 0; i < task.getResult().size(); i++) {
                                DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(i);
                                Map<String, Object> map = documentSnapshot.getData();
                                songs.add(new Song(
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
                                ((Song) songs.get(songs.size() - 1)).setFirestoreReference(documentSnapshot.getReference().getPath());
                            }
                            NativeAds nativeAds = new NativeAds(context, songs, rvMostPopular, progressBar);
                            SongsRecyclerViewAdapter songsRecyclerViewAdapter = Functions.createRecyclerView(context, songs, rvMostPopular, progressBar);
                            saveLoadedSongs(sortResultsBy);
                            rvMostPopular.setVisibility(View.VISIBLE);
                            linearLayoutOrderBy.animate().alpha(1.0f);
                        } else {
                            rvMostPopular.setVisibility(View.INVISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);
                            tvNoResult.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toasty.error(context, task.getException().toString(), Toast.LENGTH_LONG).show();
                        Log.d(TAG, task.getException().toString());
                    }
                }
            });
        }
        else { // Already loaded
            Functions.createRecyclerView(context, loadedSongs[indexSortResultsBy(sortResultsBy)], rvMostPopular, progressBar);
            rvMostPopular.invalidate();
            rvMostPopular.setVisibility(View.VISIBLE);
        }
    }

    private int indexSortResultsBy(String sortResultsBy) {
        switch (sortResultsBy) {
            case FIRESTORE_SORT_BY_ALLTIME:
                return 0;
            case FIRESTORE_SORT_BY_MONTH:
                return 1;
            case FIRESTORE_SORT_BY_WEEK:
                return 2;
        }
        return -1;
    }
    private void saveLoadedSongs(String sortResultsBy) {
        loadedSongs[indexSortResultsBy(sortResultsBy)] = songs;
    }
    private void clearSortSelection()
    {
        // Clear Fonts
        tvOrderByEver.setTypeface(normalFont);
        tvOrderByMonth.setTypeface(normalFont);
        tvOrderByWeek.setTypeface(normalFont);

        // UI - recyclerview
        rvMostPopular.removeAllViews();
        rvMostPopular.setVisibility(View.INVISIBLE);
        tvNoResult.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }
    private void selectSortSelection(String sortResultsBy) {
        clearSortSelection();

        switch (sortResultsBy) {
            case FIRESTORE_SORT_BY_ALLTIME:
                tvOrderByEver.setTypeface(boldFont);
                break;
            case FIRESTORE_SORT_BY_MONTH:
                tvOrderByMonth.setTypeface(boldFont);
                break;
            case FIRESTORE_SORT_BY_WEEK:
                tvOrderByWeek.setTypeface(boldFont);
                break;
        }

        // Save last selection of sortResultsBy
        this.sortResultsBy = sortResultsBy;
        writeSortResultsByToPref();
    }

    /**
     * Write to SharedPreferences last 'sortResultsBy' selection
     */
    private void writeSortResultsByToPref() {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SHARED_PREFERENCES_MOST_POPULAR_SORT_BY_LAST_SELECTION, this.sortResultsBy);
        editor.apply();
    }
    /**
     * Read from SharedPreferences last 'sortResultsBy' selection.
     * <p>If null, 'FIRESTORE_SORT_BY_ALLTIME' is set.
     */
    private void readSortResultsByToPref() {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        this.sortResultsBy = sharedPref.getString(SHARED_PREFERENCES_MOST_POPULAR_SORT_BY_LAST_SELECTION, FIRESTORE_SORT_BY_ALLTIME);
    }
}