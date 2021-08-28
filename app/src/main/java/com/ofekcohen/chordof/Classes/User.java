package com.ofekcohen.chordof.Classes;

import com.google.firebase.auth.FirebaseUser;

/**
 * Use in Hall of Fame
 */
public class User {
    private String uid;
    private String name;
    private String photoUrl;
    private int uploadsCount;

    public User(String uid, String name, String photoUrl, int uploadsCount) {
        this.uid = uid;
        this.name = name;
        this.photoUrl = photoUrl;
        this.uploadsCount = uploadsCount;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public int getUploadsCount() { return uploadsCount; }
    public void setUploadsCount(int uploadsCount) { this.uploadsCount = uploadsCount; }
}
