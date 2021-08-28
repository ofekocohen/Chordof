package com.ofekcohen.chordof.Fragments;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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
import com.ofekcohen.chordof.Activities.MainActivity;
import com.ofekcohen.chordof.Adapters.SongsRecyclerViewAdapter;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.Core.NativeAds.NativeAds;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;
import java.util.Map;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_SONG_PATH;
import static com.ofekcohen.chordof.Core.Constants.SEARCH_BY_SINGER_NAME;
import static com.ofekcohen.chordof.Core.Constants.SEARCH_BY_SONG_NAME;
import static com.ofekcohen.chordof.Core.Constants.SEARCH_BY_USER_UPLOADER_UID;
import static com.ofekcohen.chordof.Core.Constants.SEARCH_EXTRA_TAG;

public class SearchResultFragment extends Fragment {

    // Firestore
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    CollectionReference collectionReference = firestore.collection(FIRESTORE_SONG_PATH);

    private Activity activity;
    private Context context;
    private String searchQuery;
    private RecyclerView rvSongsResult;
    private ArrayList<Object> songsResult;
    private ProgressBar progressBar;
    private TextView tvResultFor, tvNoResult;
    private LinearLayout linearLayoutNoResultSection;
    private Button btnAskForSongUpload, btnUploadSong;
    private boolean searchByUidUploader = false;

    public SearchResultFragment() { }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_result, container, false);
        context = getContext();

        // Extras
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            searchQuery = bundle.getString(SEARCH_EXTRA_TAG);
            if (searchQuery != null) {
                searchQuery = searchQuery.trim();
                if (searchQuery.length() < 3) {
                    Toasty.info(context, "הקלד 3 תווים לפחות", Toast.LENGTH_LONG).show();
                    getFragmentManager().popBackStack();
                }
            }

            searchByUidUploader = bundle.getBoolean(SEARCH_BY_USER_UPLOADER_UID);
        }
        else {
            Toasty.error(context, "שגיאה בחיפוש", Toast.LENGTH_LONG).show();
            getFragmentManager().popBackStack();
        }

        rvSongsResult = view.findViewById(R.id.rvFavoritesSongs);
        progressBar = view.findViewById(R.id.progressBar);
        tvResultFor = view.findViewById(R.id.tvResultFor);
        tvNoResult = view.findViewById(R.id.tvNoResult);
        linearLayoutNoResultSection = view.findViewById(R.id.linearLayoutNoResultSection);
        btnAskForSongUpload = view.findViewById(R.id.btnAskForSongUpload);
        btnUploadSong = view.findViewById(R.id.btnUploadSong);

        MyOnClickListener myOnClickListener = new MyOnClickListener();
        btnAskForSongUpload.setOnClickListener(myOnClickListener);
        btnUploadSong.setOnClickListener(myOnClickListener);

        return view;
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (btnAskForSongUpload == v) {
                setFragment(new RequestSongFragment());
            }
            else if (btnUploadSong == v) {
                ((MainActivity) getActivity()).setFragment(new UploadSongFragment(), true);
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();

        // If we got searchByUidUploader in Extra we will search by that and show all the uploads of that user
        if (searchByUidUploader)
            fetchResult(SEARCH_BY_USER_UPLOADER_UID);
        else
            fetchResult();
    }

    private void setFragment(Fragment fragment)
    {
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.fadein_300, R.anim.fadeout_300);
        fragmentTransaction.replace(R.id.frmContainer, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
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
        Query songsQuery;
        if (searchBy.equals(SEARCH_BY_USER_UPLOADER_UID)) {
            songsQuery = collectionReference.whereEqualTo("user_uploader_uid", searchQuery).orderBy("name", Query.Direction.ASCENDING);
        }
        else {
            songsQuery = collectionReference.orderBy(searchBy).orderBy("views", Query.Direction.DESCENDING).orderBy("rating", Query.Direction.DESCENDING).startAt(searchQuery.toUpperCase()).endAt(searchQuery.toLowerCase() + "\uf8ff");
            if (searchBy.equals(SEARCH_BY_SONG_NAME)) // If we search for a specific song we limit to 10 results. If we search for an artist we would like to see all of his songs
                songsQuery = songsQuery.limit(10);
        }
        songsQuery.get().addOnCompleteListener(activity, new OnCompleteListener<QuerySnapshot>() {
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
                        }
                        SongsRecyclerViewAdapter songsRecyclerViewAdapter = Functions.createRecyclerView(context, songsResult, rvSongsResult, progressBar);
                        NativeAds nativeAds = new NativeAds(context, songsResult, rvSongsResult, progressBar);
                        showLabelResultFor();
                    }
                    else if (searchBy.equals(SEARCH_BY_SONG_NAME))
                        fetchResult(SEARCH_BY_SINGER_NAME);
                    else
                    {
                        progressBar.setVisibility(View.INVISIBLE);
                        if (searchBy.equals(SEARCH_BY_USER_UPLOADER_UID))
                            tvNoResult.setText("לא נמצאו תוצאות עבור המשתמש המבוקש");
                        else
                            tvNoResult.setText("לא מצאנו תוצאות עבור: " + searchQuery);
                        linearLayoutNoResultSection.setVisibility(View.VISIBLE);
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

    /**
     * When a RecyclerView is created, We should update the title <p>
     * (Ex. תוצאות עבור:  /  העלאת המשתמש)
     */
    private void showLabelResultFor()
    {
        if (searchByUidUploader)
            tvResultFor.setText("העלאות המשתמש");
        else
            tvResultFor.setText("תוצאות עבור: " + searchQuery);
        tvResultFor.setVisibility(View.VISIBLE);
    }
}