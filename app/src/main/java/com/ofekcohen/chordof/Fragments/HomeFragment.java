package com.ofekcohen.chordof.Fragments;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ofekcohen.chordof.Activities.PendingSongActivity;
import com.ofekcohen.chordof.Activities.SongsRequestsActivity;
import com.ofekcohen.chordof.Activities.UserActivity;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.R;

import java.util.Calendar;

import es.dmoral.toasty.Toasty;

import static android.app.Activity.RESULT_OK;
import static com.ofekcohen.chordof.Core.Constants.RC_SIGN_IN;

public class HomeFragment extends Fragment{

    private Context context;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private Button btnLogin, btnMostPopular, btnMostRecent, btnPending, btnRequests;
    private CardView cardViewUser;
    private ImageView imgUser;
    private TextView tvGreeting, tvWelcomeUser;
    private LinearLayout linearLayoutAdminControllers;

    public HomeFragment() { }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin = view.findViewById(R.id.btnLogIn);
        btnMostPopular = view.findViewById(R.id.btnMostPopular);
        btnMostRecent = view.findViewById(R.id.btnMostRecent);
        linearLayoutAdminControllers = view.findViewById(R.id.linearLayoutAdminControllers);
        btnPending = view.findViewById(R.id.btnPending);
        btnRequests = view.findViewById(R.id.btnRequests);
        cardViewUser = view.findViewById(R.id.cardViewUserAvatar);
        imgUser = view.findViewById(R.id.imgUserAvatar);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvWelcomeUser = view.findViewById(R.id.tvWelcome);

        MyOnClickListener listener = new MyOnClickListener();
        btnLogin.setOnClickListener(listener);
        btnMostPopular.setOnClickListener(listener);
        btnMostRecent.setOnClickListener(listener);
        btnPending.setOnClickListener(listener);
        btnRequests.setOnClickListener(listener);
        imgUser.setOnClickListener(listener);
        tvWelcomeUser.setOnClickListener(listener);

        greetingMessage();

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getContext();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        user = mAuth.getCurrentUser();
        if (Functions.isAdmin(user))
            linearLayoutAdminControllers.setVisibility(View.VISIBLE);
        updateUI();
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (btnLogin == v) {
                login();
            }
            else if (imgUser == v || tvWelcomeUser == v) {
                startActivity(new Intent(context, UserActivity.class));
//                setFragment(new UserProfileFragment());
            }
            else if (btnMostPopular == v) {
                setFragment(new MostPopularFragment());
            }
            else if (btnMostRecent == v) {
                setFragment(new MostRecentFragment());
            }
            else if (btnPending == v) {
                startActivity(new Intent(context, PendingSongActivity.class));
            }
            else if (btnRequests == v) {
                startActivity(new Intent(context, SongsRequestsActivity.class));
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

                // Update UI
                updateUI();
            }
            else if (response != null) { // If response is null the user canceled the
                Toasty.error(context, response.getError().getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        /*else if (requestCode == USER_ACTIVITY)
        {
            if (resultCode == RESULT_OK) {
                // User logged out
                user = null;
                updateUI();
            }
        }*/
    }

    public void updateUI() {
        if (user == null) {
            // User logged out
            btnLogin.setVisibility(View.VISIBLE);
            cardViewUser.setVisibility(View.INVISIBLE);
            tvWelcomeUser.setVisibility(View.INVISIBLE);
        }
        else
        {
            // User logged in
            btnLogin.setVisibility(View.INVISIBLE);

            Glide.with(this).load(user.getPhotoUrl()).into(imgUser);
            if (user.getDisplayName() != null && !user.getDisplayName().equals(""))
                tvWelcomeUser.setText("שלום " + user.getDisplayName() + "!");
            else
                tvWelcomeUser.setText("ברוך הבא!");
            cardViewUser.setVisibility(View.VISIBLE);
            tvWelcomeUser.setVisibility(View.VISIBLE);

            if (Functions.isAdmin(user)) {
                linearLayoutAdminControllers.setVisibility(View.VISIBLE);
            }
        }
    }

    private void greetingMessage()
    {
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (currentHour >= 6 && currentHour <= 11)
            tvGreeting.setText("בוקר טוב,");
        else if (currentHour >= 12 && currentHour <= 15)
            tvGreeting.setText("צהריים טובים,");
        else if (currentHour >= 16 && currentHour <= 19)
            tvGreeting.setText("ערב טוב,");
        else if (currentHour >= 20 || currentHour <= 5)
            tvGreeting.setText("לילה טוב,");
    }
}
