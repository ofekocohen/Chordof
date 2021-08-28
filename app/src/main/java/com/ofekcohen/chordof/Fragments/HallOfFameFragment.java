package com.ofekcohen.chordof.Fragments;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.ofekcohen.chordof.Adapters.HallOfFameRecyclerViewAdapter;
import com.ofekcohen.chordof.Classes.User;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;
import java.util.Map;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_USERS_PATH;
import static com.ofekcohen.chordof.Core.Constants.SHARED_PREFERENCES_IS_USER_UPLOADER;

public class HallOfFameFragment extends Fragment {

    // Firestore
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference dbUsersPath = db.collection(FIRESTORE_USERS_PATH);

    private Activity activity;
    private Context context;
    private RecyclerView rvUsersResult;
    private ArrayList<User> usersResult;
    private ProgressBar progressBar;
    private TextView tvResultFor, tvNoResult;

    public HallOfFameFragment() { }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_result, container, false);
        context = getContext();

        rvUsersResult = view.findViewById(R.id.rvFavoritesSongs);
        progressBar = view.findViewById(R.id.progressBar);
        tvResultFor = view.findViewById(R.id.tvResultFor);
        tvNoResult = view.findViewById(R.id.tvNoResult);

        tvResultFor.setText("המעלים המובילים שלנו");
        tvResultFor.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();

        fetchResult();
    }

    private void fetchResult()
    {
        dbUsersPath.whereGreaterThan("uploads_count", 0).orderBy("uploads_count", Query.Direction.DESCENDING)
                .get().addOnCompleteListener(activity, new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (!task.getResult().isEmpty()) {
                        usersResult = new ArrayList<>();
                        for (int i = 0; i < task.getResult().size(); i++) {
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(i);
                            Map<String, Object> map = documentSnapshot.getData();
                            usersResult.add(new User(
                                    documentSnapshot.getId(),
                                    documentSnapshot.getString("name"),
                                    documentSnapshot.getString("photo_url"),
                                    ((Long) map.get("uploads_count")).intValue())
                            );
                        }
                        removeAdminUsers(usersResult);
                        createRecyclerView(context, usersResult, rvUsersResult, progressBar);
                    }
                    else
                    {
                        progressBar.setVisibility(View.INVISIBLE);
                        tvNoResult.setText("לא נמצאו תוצאות");
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

    /**
     * Remove Admin users from the list, so they won't be in Hall of Fame.
     * @param usersResult
     */
    private void removeAdminUsers(ArrayList<User> usersResult) {
        for (int i = 0; i < usersResult.size(); i++) {
            if (Functions.isAdmin(usersResult.get(i).getUid())) {
                usersResult.remove(i);
                i--; // On 'usersResult.remove(i)' -> all list shifts left -> so we need to check the element that moved
            }
        }
    }

    private void createRecyclerView(Context context, ArrayList<User> usersResult, RecyclerView rv, ProgressBar progressBar) {
        rv.setHasFixedSize(true);
//        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
//        rv.setLayoutManager(layoutManager);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 2);
        rv.setLayoutManager(gridLayoutManager);
        rv.setItemViewCacheSize(15);
        rv.setAdapter(new HallOfFameRecyclerViewAdapter(usersResult, context));

        progressBar.setVisibility(View.INVISIBLE);
    }
}