package com.ofekcohen.chordof.Classes;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.ofekcohen.chordof.Core.Constants.SHARED_PREFERENCES_VIEWED_SONGS_UID;

/**
 * Handles viewed songs by the user (device) in the last 24 hours
 */
public class ViewedSong {

    private Calendar timeStamp;
    private String songUID;

    public ViewedSong(Calendar timeStamp, String songUID) {
        this.timeStamp = timeStamp;
        this.songUID = songUID;
    }

    public Calendar getTimeStamp() { return timeStamp; }
    public String getSongUID() { return songUID; }
    public void setTimeStamp(Calendar timeStamp) { this.timeStamp = timeStamp; }

    /**
     * Check if this song has already been viewed today.
     * @param activity
     * @param viewedSongs should be calculated via 'ViewedSong.readViewedSongsToPref()'
     * @param songUID
     * @return True - This song has already been viewed today.
     */
    public static boolean isSongViewedToday(Activity activity, List<ViewedSong> viewedSongs, String songUID) {
        for (ViewedSong viewedSong : viewedSongs)
            if (viewedSong.getSongUID().equals(songUID))
                return true;
        return false;
    }

    /**
     * Write to SharedPreferences all the songs viewed by the user in the last 24 hours.
     * @param activity
     * @param viewedSongs should be calculated via 'ViewedSong.readViewedSongsToPref()'
     */
    public static void writeViewedSongsToPref(Activity activity, List<ViewedSong> viewedSongs) {
        // Convert list to json
        Gson gson = new Gson();
        String jsonString = gson.toJson(viewedSongs);

        // Save json to SharedPreferences
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SHARED_PREFERENCES_VIEWED_SONGS_UID, jsonString);
        editor.apply();
    }

    /**
     * Read from SharedPreferences all the songs viewed by the user in the last 24 hours.
     * @param activity
     * @return List of all viewedSongs in the last 24 hours
     */
    public static List<ViewedSong> readViewedSongsToPref(Activity activity) {
        // Read json from SharedPreferences
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        String jsonString = sharedPref.getString(SHARED_PREFERENCES_VIEWED_SONGS_UID, "");

        // Convert json to list
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<ViewedSong>>() {}.getType();
        List<ViewedSong> viewedSongs = gson.fromJson(jsonString, type);

        // Remove songs which where added before the last 24 hours
        if (viewedSongs != null) {
            boolean songHasBeenRemoved = false;
            for (int i = 0; i < viewedSongs.size(); i++) {
                Calendar yesterday = Calendar.getInstance(); // Current time
                yesterday.add(Calendar.DAY_OF_MONTH, -1); // Current time - 24 hours (1 day) = Yesterday
                if (viewedSongs.get(i).timeStamp.before(yesterday)) {
                    viewedSongs.remove(i);
                    i--;
                    songHasBeenRemoved = true;
                }
            }

            // Write to SharePreferences after removing songs expired (more than 24 hours)
            if (songHasBeenRemoved)
                ViewedSong.writeViewedSongsToPref(activity, viewedSongs);
        }
        else
            viewedSongs = new ArrayList<>();
        return viewedSongs;
    }
}
