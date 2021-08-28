package com.ofekcohen.chordof.Core;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.ofekcohen.chordof.Adapters.SongsRecyclerViewAdapter;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.R;
import com.ofekcohen.chordof.Services.Email.GMailSender;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.dmoral.toasty.Toasty;

import static com.ofekcohen.chordof.Core.Constants.*;

public class Functions {
    public static String transpose(String chord, int value)
    {
//        String[] possiblesRoots = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        value = value % possiblesRoots.length;

        // Check if the root has "#" or "b"
        String root = "", restChord = "";
        if (chord.length() > 1 && (chord.charAt(1) == '#' || chord.charAt(1) == 'b')) {
            root = chord.substring(0, 2);
            restChord = chord.substring(2);
        }
        else {
            root = Character.toString(chord.charAt(0));
            restChord = chord.substring(1);
        }
        //


        // Tranpose
        int currRoot = -1;
        // Find the index of currRoot in our 'possibleRoots' array
        for (int i = 0; i < possiblesRoots.length; i++)
        {
            if (possiblesRoots[i].equals(root) || possiblesRootsMol[i].equals(root))
                currRoot = i;
        }
        if (currRoot != -1) // There is an index!
        {
            int newRoot = currRoot + value;
            if (newRoot > possiblesRoots.length)
                newRoot = newRoot % possiblesRoots.length - 1;
            else if (newRoot < 0)
                newRoot = possiblesRoots.length + newRoot % possiblesRoots.length;
            else if (newRoot == possiblesRoots.length)
                newRoot = 0;

            // Support in complicated chords like: C#m/Gmaj7
            if (restChord.contains("/")) {
                String baseRootRest = restChord.substring(0, restChord.indexOf("/") + 1);
                String secondRoot = restChord.substring(restChord.indexOf("/") + 1);
                restChord = baseRootRest + transpose(secondRoot, value);
            }
            return possiblesRoots[newRoot] + restChord;
        }
        return null; // There was an error
    }

    /**
     * Check if the given line has only chords
     * @param line
     * @return true - This line is a chord line
     */
    public static boolean isChordLine(String line) {
        // Find all the chords in the line
        ArrayList<String> chordsOnLine = new ArrayList<>();
        Pattern p = Pattern.compile(regexChords);
        Matcher m = p.matcher(line);
        while (m.find())
            chordsOnLine.add(m.group());

        // Calculate length of all the chords
        int chordsOnLineLength = 0;
        for (int i = 0; i < chordsOnLine.size(); i++)
            chordsOnLineLength += chordsOnLine.get(i).length();

        // Calculate length of line without spaces
        String noSpaces = line.replaceAll("\\s", ""); // \s - white space
        int lineTrimLength = noSpaces.length();

        // Compare length of all chords with length of line without spaces
        if (chordsOnLineLength == lineTrimLength && chordsOnLineLength != 0)
            return true;
        return false;
    }

    /**
     * Check if the user logged in is an Administrator
     * @param user Current user
     * @return true - User is an Admin
     */
    public static boolean isAdmin(FirebaseUser user) {
        if (user != null) {
            String[] administratorsUID = Constants.administratorsUID;
            for (int i = 0; i < administratorsUID.length; i++) {
                if (administratorsUID[i].equals(user.getUid()))
                    return true;
            }
        }
        return false;
    }
    /**
     * Check if the given uid is an Administrator
     * @param uid user.getUid()
     * @return true - User is an Admin
     */
     public static boolean isAdmin(String uid) {
         if (uid != null) {
             String[] administratorsUID = Constants.administratorsUID;
             for (int i = 0; i < administratorsUID.length; i++) {
                 if (administratorsUID[i].equals(uid))
                     return true;
             }
         }
         return false;
    }

    /**
     * Check if network is available
     * @param context Activity's context
     * @return true - Network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    /**
     * Perform a user login
     * @param activity Activity
     * @return intent - login ui because we must launch it from activity to get access to 'onActivityResult'
     */
    public static Intent login(Activity activity) {
        if (Functions.isNetworkAvailable(activity)) {
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.GoogleBuilder().build(),
                    new AuthUI.IdpConfig.FacebookBuilder().build());

            // Create and launch sign-in intent
            return AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setTheme(R.style.FirebaseLoginTheme)
                    .setLogo(R.mipmap.ic_launcher)
                    .setTosAndPrivacyPolicyUrls(PRIVACY_POLICY_URL, PRIVACY_POLICY_URL)
                    .build();
        }
        return null;
    }

    /**
     * Save user properties (Name, Email, Profile Picture) to use when he is not current user.
     * @param user Current user
     */
    public static void saveUserProperties(final FirebaseUser user) {
        // Save user properties in Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference df = db.document(FIRESTORE_USERS_PATH + user.getUid());
        df.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull final Task<DocumentSnapshot> task) {
                final Map<String, Object> userProperties = new HashMap<>();
                if (user.getDisplayName() != null)
                    userProperties.put("name", user.getDisplayName());
                if (user.getPhotoUrl() != null)
                    userProperties.put("photo_url", user.getPhotoUrl().toString());
                if (user.getEmail() != null)
                    userProperties.put("email", user.getEmail());

                // If the user already saved in firestore don't overwrite
                df.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot doc = task.getResult();

                            // If the user doesn't exist -> create it on firestore (save properties)
                            if (!doc.exists()) {
                                df.set(userProperties).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful())
                                            Log.d(TAG, "Successfully save user properties to firebase : saveUserProperties() - ");
                                        else
                                            Log.d(TAG, "Functions.saveUserProperties() : " + task.getException().getMessage());
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Load 'privacy_policy.html' from Firebase Storage and open it in Chrome
     */
    public static void openPrivacyPolicy(final Context context) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference filePrivacyPoilcy = storage.getReferenceFromUrl("gs://chordof-2d2d0.appspot.com/privacy_policy.html");
        filePrivacyPoilcy.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "text/html");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Intent newIntent = Intent.createChooser(intent, "פתח קובץ");
                try {
                    context.startActivity(newIntent);
                } catch (ActivityNotFoundException e) {
                    // Instruct the user to install a PDF reader here, or something
                }
            }
        });
        Toasty.normal(context, "אנא המתן", Toast.LENGTH_SHORT).show();
    }

    /**
     * Create a grid of songs managed by a RecyclerView
     * @param context
     * @param mySongs List of elements in RecyclerView
     * @param rv RecyclerView from xml
     * @param progressBar Loading progressBar (Until fetch data from Firebase)
     */
    public static SongsRecyclerViewAdapter createRecyclerView(Context context, ArrayList<Object> mySongs, RecyclerView rv, ProgressBar progressBar) {
        rv.setHasFixedSize(true);
//        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
//        rv.setLayoutManager(layoutManager);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, calculateNumOfColumns(context, 200));
        SongsRecyclerViewAdapter songsRecyclerViewAdapter = new SongsRecyclerViewAdapter(mySongs, context);
        rv.setLayoutManager(gridLayoutManager);
        rv.setItemViewCacheSize(15);
        rv.setAdapter(songsRecyclerViewAdapter);

        progressBar.setVisibility(View.INVISIBLE);

        return songsRecyclerViewAdapter;
    }
    public static int calculateNumOfColumns(Context context, float columnWidthDp) { // For example columnWidthdp=180
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int numOfColumns = (int) (screenWidthDp / columnWidthDp + 0.5); // +0.5 for correct rounding to int.
        return numOfColumns;
    }

    /**
     * Make the device vibrate.
     * <p>
     * @param context
     * @param vibrationDuration use 'Constants.VIBRATION_DURATION' - vibration duration in milliseconds
     */
    public static void vibrateDevice(Context context, long vibrationDuration) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(vibrationDuration);
        }
    }


    /**
     * True - 'Functions.getYouTubeURL' is still running
     */
    public static boolean isFetchingYoutube;
    /**
     * Get song's youtube url, and update song's parameters accordingly.
     * <p>You can use 'Functions.isFetchingYoutube' to check status.
     */
    public static void getYouTubeURL(Context context, Song song) {
        isFetchingYoutube = true;
        RequestQueue mQueue = Volley.newRequestQueue(context);

        String query = Constants.YOUTUBE_QUERY_URL.replace(Constants.YOUTUBE_QUERY_PARAMETER_TO_REPLACE, song.getName() + " " + song.getSinger());

        // JSON PARSE
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, query, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray jsonArray = response.getJSONArray("items");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject items = jsonArray.getJSONObject(i).getJSONObject("id");
                                String videoID = items.getString("videoId");
                                song.setYoutube(Constants.YOUTUBE_VIDEO_URL + videoID);
                            }
                        } catch (JSONException e) { e.printStackTrace(); }
                        isFetchingYoutube = false;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                isFetchingYoutube = false;
            }
        });

        mQueue.add(request);
    }

    /**
     * Send an email to a user by UID.
     * @param context context
     * @param song song to get UploaderUID and UploaderName
     * @param subject Email's subject (Ex. EMAIL_SUBJECT_USER_UPLOADED_SONG)
     * @param body Email's body (Ex. EMAIL_BODY_USER_UPLOADED_SONG)
     */
    public static void sendEmail(Context context, Song song, String subject, String body) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FIRESTORE_USERS_PATH).document(song.getUploaderUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                String recipients;
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        recipients = document.getString("email");
                        if (recipients != null) {
                            try {
                                GMailSender sender = new GMailSender(EMAIL_USERNAME, EMAIL_PASSWORD);
                                sender.sendMail(
                                        subject,
                                        body,
                                        "chordof.mail",
                                        recipients);
                                Log.i(TAG, "Email has been sent to the user.");
                                Toasty.info(context, "נשלח אימייל ל" + song.getUploaderName()).show();
                            } catch (Exception e) {
                                Log.e(TAG, "<GMailSender>" + e.getMessage());
                                Toasty.error(context, "לא נשלח אימייל ל" + song.getUploaderName()).show();
                            }
                        }
                    }
                    else {
                        Log.e(TAG, "Failed to send an email, We couldn't find user email via UID.");
                        Toasty.error(context, "לא נשלח אימייל ל" + song.getUploaderName()).show();
                    }
                }
            }
        });
    }
}