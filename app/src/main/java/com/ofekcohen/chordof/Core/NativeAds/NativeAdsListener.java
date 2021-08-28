package com.ofekcohen.chordof.Core.NativeAds;

import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.formats.UnifiedNativeAd;

import java.util.ArrayList;
import java.util.List;

public interface NativeAdsListener {
    void onSuccess(ArrayList<Object> songsWithAds, RecyclerView rvSongsWithAds, List<UnifiedNativeAd> mNativeAds, ProgressBar progressBar);
}
