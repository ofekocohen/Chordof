package com.ofekcohen.chordof.Fragments;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.ofekcohen.chordof.Adapters.SongsRecyclerViewAdapter;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;
import java.util.Map;

import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_USERS_PATH;

public class MySongsFragment extends Fragment {

    // Firebase
    private FirebaseUser user;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private Context context;
    private RecyclerView rvMySongs;
    private ArrayList<Object> mySongs;
    private ProgressBar progressBar;
    private TextView tvResultSize;
    private TextView tvNoResult;

    public MySongsFragment() { }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_songs, container, false);
        context = getContext();
        user = FirebaseAuth.getInstance().getCurrentUser();

        rvMySongs = view.findViewById(R.id.rvMySongs);
        progressBar = view.findViewById(R.id.progressBar);
        tvResultSize = view.findViewById(R.id.tvResultSize);
        tvNoResult = view.findViewById(R.id.tvNoResult);

        fetchResult();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            progressBar.setVisibility(View.INVISIBLE);
            tvNoResult.setVisibility(View.VISIBLE);
        }
        else if (mySongs == null || mySongs.isEmpty()) {
            tvNoResult.setVisibility(View.GONE);
            user = FirebaseAuth.getInstance().getCurrentUser();
            fetchResult();
        }
    }

    private void fetchResult()
    {
        if (user != null) {
            CollectionReference collectionReference = firestore.collection(FIRESTORE_USERS_PATH + user.getUid() + "/favorites/");
            Query songsQuery = collectionReference.orderBy("favorite_time_milisecond", Query.Direction.DESCENDING);
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
                                        "",
                                        "",
                                        map.get("youtube") != null ? (String) map.get("youtube") : "",
                                        0,
                                        ((Long) map.get("favorite_time_milisecond")).intValue(),
                                        0,
                                        null,
                                        null,
                                        ""
                                ));
                                ((Song) mySongs.get(mySongs.size() - 1)).setFirestoreReference(documentSnapshot.get("reference").toString());
                            }
                            Functions.createRecyclerView(context, mySongs, rvMySongs, progressBar);
                            tvResultSize.setText(mySongs.size() + " שירים");
                            tvResultSize.setVisibility(View.VISIBLE);
                        } else {
                            progressBar.setVisibility(View.INVISIBLE);
                            tvNoResult.setVisibility(View.VISIBLE);
                        }
                    } else {
//                        Toasty.error(context, task.getException().toString(), Toast.LENGTH_LONG).show();
                        Log.d(Constants.TAG, task.getException().toString());
                    }
                }
            });
        }
        else {
            progressBar.setVisibility(View.INVISIBLE);
            tvNoResult.setVisibility(View.VISIBLE);
        }
    }
}