package com.ofekcohen.chordof.Activities;

import android.content.Context;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;
import java.util.Map;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_SONG_PENDING_PATH;
import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_SONG_REQUESTS_PATH;

public class SongsRequestsActivity extends AppCompatActivity {

    // Firebase
    private FirebaseUser user;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private Context context;
    private RecyclerView rvMySongs;
    private ArrayList<Object> mySongs;
    private ProgressBar progressBar;
    private TextView tvResultSize;
    private TextView tvNoResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_my_songs);
        context = this;
        user = FirebaseAuth.getInstance().getCurrentUser();

        rvMySongs = findViewById(R.id.rvMySongs);
        progressBar = findViewById(R.id.progressBar);
        tvResultSize = findViewById(R.id.tvResultSize);
        tvNoResult = findViewById(R.id.tvNoResult);

        fetchResult();
    }

    private void createRecyclerView()
    {
        Functions.createRecyclerView(context, mySongs, rvMySongs,progressBar);

        tvResultSize.setText(mySongs.size() + " שירים");
        tvResultSize.setVisibility(View.VISIBLE);
    }

    private void fetchResult()
    {
        if (user != null) {
            CollectionReference collectionReference = firestore.collection(FIRESTORE_SONG_REQUESTS_PATH);
            collectionReference.orderBy("upload_time_milisecond").get().addOnCompleteListener(this, new OnCompleteListener<QuerySnapshot>() {
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
                            createRecyclerView();
                        }
                        else
                        {
                            progressBar.setVisibility(View.INVISIBLE);
                            tvNoResult.setVisibility(View.VISIBLE);
                            tvNoResult.setText("אין בקשות שירים חדשות");
                        }
                    } else {
                        Toasty.error(context, task.getException().toString(), Toast.LENGTH_LONG).show();
                        Log.d(Constants.TAG, task.getException().toString());
                    }
                }
            });
        }
    }
}
