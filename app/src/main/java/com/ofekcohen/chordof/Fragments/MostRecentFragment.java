package com.ofekcohen.chordof.Fragments;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class MostRecentFragment extends Fragment {

    // Firebase
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    CollectionReference collectionReference = firestore.collection(FIRESTORE_SONG_PATH);

    private Context context;
    private RecyclerView rvMostRecent;
    private ArrayList<Object> mySongs;
    private ProgressBar progressBar;
    private TextView tvNoResult, tvTitle;
    private Button btnBack;

    public MostRecentFragment() { }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_most_list, container, false);
        context = getContext();

        rvMostRecent = view.findViewById(R.id.rvMostPopular);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoResult = view.findViewById(R.id.tvNoResult);
        tvTitle = view.findViewById(R.id.tvTitle);
        btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(new MyOnClickListener());

        tvTitle.setText("האחרונים שנוספו");

        fetchResult();

        return view;
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            getFragmentManager().popBackStack();
        }
    }

    private void fetchResult()
    {
        Query songsQuery = collectionReference.orderBy("upload_time_milisecond", Query.Direction.DESCENDING).limit(100);
        songsQuery.get().addOnCompleteListener(getActivity(), new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (!task.getResult().isEmpty()) {
                        mySongs = new ArrayList<>();
                        for (int i = 0; i < task.getResult().size(); i++) {
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(i);
                            Map<String, Object> map = documentSnapshot.getData();
                            mySongs.add(new Song(
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
                            ((Song) mySongs.get(mySongs.size() - 1)).setFirestoreReference(documentSnapshot.getReference().getPath());
                        }
                        SongsRecyclerViewAdapter songsRecyclerViewAdapter = Functions.createRecyclerView(context, mySongs, rvMostRecent, progressBar);
                        NativeAds nativeAds = new NativeAds(context, mySongs, rvMostRecent, progressBar);
                    }
                    else
                    {
                        progressBar.setVisibility(View.INVISIBLE);
                        tvNoResult.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toasty.error(context, task.getException().toString(), Toast.LENGTH_LONG).show();
                    Log.d(Constants.TAG, task.getException().toString());
                }
            }
        });
    }
}