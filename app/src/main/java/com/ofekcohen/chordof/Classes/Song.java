package com.ofekcohen.chordof.Classes;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ofekcohen.chordof.Core.Functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ofekcohen.chordof.Core.Constants.FIRESTORE_USERS_PATH;
import static com.ofekcohen.chordof.Core.Constants.TAG;
import static com.ofekcohen.chordof.Core.Constants.possiblesRoots;
import static com.ofekcohen.chordof.Core.Constants.regexChords;
import static com.ofekcohen.chordof.Core.Constants.regexSingelChord;

public class Song {
    private String name;
    private String singer;
    private String author;
    private String composer;
    private int rating;
    private String chords;
    private String youtube;
    private long uploadTimeMiliSec;
    private int easyTone;
    private String uploaderUid;
    private String uploaderName;
    private int viewsTotal;
    private int viewsMonth;

    private ArrayList<String> chordsOnSong;
    private int transpose;

    private String firestoreReference; // Firestore

    public Song(String name, String singer, String author, String composer, String youtube, int rating, long uploadTimeMiliSec, int easyTone, String uploaderUid, String uploaderName, String chords) {
        this.name = name.trim();
        this.singer = singer.trim();
        this.author = author.trim();
        this.composer = composer.trim();
        this.youtube = youtube.trim();
        this.rating = rating;
        this.uploadTimeMiliSec = uploadTimeMiliSec;
        this.easyTone = easyTone;
        this.uploaderUid = uploaderUid;
        this.uploaderName = uploaderName;
        this.chords = chords;
        calChordsListOnSong();
        this.transpose = 0;
    }
    public Song(String name, String singer, String author, String composer, String youtube, int rating, long uploadTimeMiliSec, int easyTone, String uploaderUid, String uploaderName, String chords, int viewsTotal, int viewsMonth) {
        this.name = name.trim();
        this.singer = singer.trim();
        this.author = author.trim();
        this.composer = composer.trim();
        this.youtube = youtube.trim();
        this.rating = rating;
        this.uploadTimeMiliSec = uploadTimeMiliSec;
        this.easyTone = easyTone;
        this.uploaderUid = uploaderUid;
        this.uploaderName = uploaderName;
        this.chords = chords;
        calChordsListOnSong();
        this.transpose = 0;

        this.viewsTotal = viewsTotal; // (That's the different)
        this.viewsMonth = viewsMonth; // (That's the different)
    }

    public String getName() {
        return name;
    }
    public String getSinger() {
        return singer;
    }
    public String getAuthor() {
        return author;
    }
    public String getComposer() {
        return composer;
    }
    public String getYoutube() {
        return youtube != null ? youtube : "";
    }
    public void setYoutube(String youtube) { this.youtube = youtube; }
    public int getRating() {
        return rating;
    }
    public long getUploadTimeMiliSec() {
        return uploadTimeMiliSec;
    }
    public int getEasyTone() {
        return easyTone;
    }
    public String getUploaderUid() {
        return uploaderUid;
    }
    public String getUploaderName() {
        return uploaderName;
    }
    public void setEasyTone(int easyTone) {
        this.easyTone = easyTone;
    }
    public String getChords() {
        return chords;
    }
    public ArrayList<String> getChordsListOnSong()
    {
        return chordsOnSong;
    }
    public int getTranspose() {
        return transpose;
    }
    public void setChords(String chords) {
        this.chords = chords;
        calChordsListOnSong();
    }
    public String getFirestoreReference() {
        return firestoreReference;
    }
    public void setFirestoreReference(String firestoreReference) { this.firestoreReference = firestoreReference; }
    public int getViewsTotal() { return viewsTotal; }
    public void setViewsTotal(int viewsTotal) { this.viewsTotal = viewsTotal; }
    public int getViewsMonth() { return viewsMonth; }
    public void setViewsMonth(int viewsTotal) { this.viewsMonth = viewsMonth; }
    public Map<String, Object> getSongMap()
    {
        Map<String, Object> songMap = new HashMap<>();
        songMap.put("name", name);
        songMap.put("singer", singer);
        songMap.put("author", author);
        songMap.put("composer", composer);
        songMap.put("youtube", youtube);
        songMap.put("rating", rating);
        songMap.put("chords", chords);
        songMap.put("views", 0);
        songMap.put("views_month", 0);
        songMap.put("easy_tone", easyTone);
        if (uploadTimeMiliSec == -1)
            songMap.put("upload_time_milisecond", java.util.Calendar.getInstance().getTime().getTime());
        else
            songMap.put("upload_time_milisecond", uploadTimeMiliSec);
        return songMap;
    }

    /**
     * Calculate the chords inside this song and add all the chords to a list (song.getChordsListOnSong())
     */
    public void calChordsListOnSong()
    {
        this.chordsOnSong = new ArrayList<>();
        Pattern p = Pattern.compile(regexChords);
        Matcher m = p.matcher(this.chords);
        while (m.find())
            addChordToList(m.group());
    }
    /**
     * Adding a chord to 'chordsOnSong' list.
     * @param chord Chord to add
     */
    private void addChordToList(String chord)
    {
        if (!isChordExistOnList(chord))
            chordsOnSong.add(chord);
    }
    /**
     * Check if the chord is on 'chordsOnSong' list.
     * @param chord Chord to check
     * @return TRUE - exist
     */
    private boolean isChordExistOnList(String chord)
    {
        for (int i = 0; i < chordsOnSong.size(); i++)
            if (chord.equals(chordsOnSong.get(i)))
                return true;
        return false;
    }
    //

    public void transpose(int value)
    {
        int transposeDirection = 0;
        if (value > 0)
            transposeDirection = +1;
        else if (value < 0)
            transposeDirection = -1;

        // Every time of a loop the song transpose only 1 time (up/down by the transposeDirection)
        for (int j = 0; j < Math.abs(value); j++) {
            // Replace every chord to something like: "3CHORD_CHORDOF"
            for (int i = 0; i < chordsOnSong.size(); i++) {
                String regex = regexSingelChord.replace("CHORD", chordsOnSong.get(i)); // regex
                chords = chords.replaceAll(regex, i + "xXxXxXx");
            }
            // Replace every "*CHORD_CHORDOF" to a tuned chord
            for (int i = 0; i < chordsOnSong.size(); i++) {
                String newChord = Functions.transpose(chordsOnSong.get(i), transposeDirection);
                chordsOnSong.set(i, newChord);
                String regex = regexSingelChord.replace("CHORD", i + "xXxXxXx"); // regex
                chords = chords.replaceAll(regex, chordsOnSong.get(i));
            }
        }
        transpose = (transpose + value) % possiblesRoots.length;
    }
    //

    /**
     * Find username by uid from firebase
     */
    private void getUploaderNameByUid() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference df = db.document(FIRESTORE_USERS_PATH + getUploaderUid());
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                try {
                    Map<String, Object> map = documentSnapshot.getData();
                    String username = (String) map.get("name");
                    if (username != null)
                        uploaderName = username;
                    else
                        uploaderName = "Chordof";
                }
                catch (Exception e) {
                    Log.d(TAG, "Song.getUploaderNameByUid() : " + e.getMessage());
                    uploaderName = "Chordof";
                }
            }
        });
    }

    /**
     * Calculate the best easy tone of the song and return the tone value
     * @return easy tone value
     */
    public int calEasyTone() {
        ArrayList<String> chords = this.getChordsListOnSong();
        double[] countHardChords = new double[12];
        int minHardChordsIndex = 0;
        for (int i = 0; i < countHardChords.length; i++) {
            for (int j = 0; j < chords.size(); j++) {
                String chord = chords.get(j);
                if (chord != null) {
                    if (chord.contains("#") || chord.contains("b"))
                        countHardChords[i]++;
                    else if (chord.contains("Bm") || chord.equals("F"))
                        countHardChords[i] += 0.5;
                    else if (chord.contains("B") ||
                    chord.contains("Cm") ||
                    chord.contains("F") ||
                    chord.contains("Fm") ||
                    chord.contains("Gm"))
                        countHardChords[i]++;
                }
            }
            if (countHardChords[i] < countHardChords[minHardChordsIndex])
                minHardChordsIndex = i;
            this.transpose(-1);
        }
        return -1 * minHardChordsIndex;
    }
}