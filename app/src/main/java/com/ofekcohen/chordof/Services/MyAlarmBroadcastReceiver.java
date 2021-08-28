package com.ofekcohen.chordof.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ofekcohen.chordof.BuildConfig;
import com.ofekcohen.chordof.Core.Constants;

import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_APP_SETTINGS_PATH;
import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_VERSION_CODE;
import static com.ofekcohen.chordof.Core.Constants.SHARED_PREFERENCES_UPDATE_VERSION_CODE;

/**
 * Check in Firestore what is the 'version_code', update SharedPreferences accordingly.
 * <p>Triggers every 7 days.
 */
public class MyAlarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        /* Check For Updates */
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.document(FIRESTORE_APP_SETTINGS_PATH).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if (documentSnapshot.exists() && documentSnapshot.get(FIRESTORE_VERSION_CODE) != null) {
                        int versionCode = documentSnapshot.getLong(FIRESTORE_VERSION_CODE).intValue();
                        if (BuildConfig.VERSION_CODE < versionCode) {
                            // Need to update!
                            SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_UPDATE_VERSION_CODE, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putInt(SHARED_PREFERENCES_UPDATE_VERSION_CODE, versionCode);
                            editor.apply();
                        }
                    }
                }
                else {
                    String err = task.getException().getMessage();
                    Log.d(Constants.TAG, err);
                }
            }
        });
    }
}
