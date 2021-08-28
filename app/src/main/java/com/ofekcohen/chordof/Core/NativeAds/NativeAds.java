package com.ofekcohen.chordof.Core.NativeAds;

import android.content.Context;
import android.util.Log;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.ofekcohen.chordof.Adapters.SongsRecyclerViewAdapter;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.Functions;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Add 'native-ads' to recycler view of songs.
 */
public class NativeAds {

    public int ADS_OFFSET_MIN = 8;
    public int ADS_OFFSET_MAX = 12;
    public AdLoader adLoader;
    private NativeAdsListener nativeAdsListener;
    private RecyclerView rvSongsResult;

    /**
     * Create a recycler view with both songs and native-ads.
     * @param context
     * @param songsResult list of songs
     * @param rvSongsResult recycler view of songs list which ads are going to be imported to
     * @param progressBar 'View' of loading progress bar
     */
    public NativeAds(Context context, ArrayList<Object> songsResult, RecyclerView rvSongsResult, ProgressBar progressBar)
    {
        this.rvSongsResult = rvSongsResult;
        if (Functions.isNetworkAvailable(context))
            loadNativeAds(context, songsResult, rvSongsResult, progressBar);
    }

    /**
     * Triggers when a recycler view with both songs and ads has been created successfully.
     * @param nativeAdsListener
     */
    public void setOnAdLoadedListener(NativeAdsListener nativeAdsListener) {
        this.nativeAdsListener = nativeAdsListener;
    }


    /**
     * Loads all ads required (Acc. OFFSET) and add them to songs' list.
     * <p>Then it creates a recycler view with both songs and ads.
     * @param context
     * @param songsResult list of songs
     * @param rvSongsResult recycler view of songs list which ads are going to be imported to
     * @param progressBar 'View' of loading progress bar
     */
    public void loadNativeAds(Context context, ArrayList<Object> songsResult, RecyclerView rvSongsResult, ProgressBar progressBar) {
        if (!calAdsOffset(songsResult.size())) {
            Functions.createRecyclerView(context, songsResult, rvSongsResult, progressBar);
            return;
        }

        List<UnifiedNativeAd> mNativeAds = new ArrayList<>(); // Reset in each call

        AdLoader.Builder builder = new AdLoader.Builder(context, context.getString(R.string.admob_native_unit_id));
        adLoader = builder.forUnifiedNativeAd(
                new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        // A native ad loaded successfully, check if the ad loader has finished loading
                        // and if so, insert the ads into the list.
                        mNativeAds.add(unifiedNativeAd);
                        if (!adLoader.isLoading()) {
                            insertAdsInBetweenSongsItems(songsResult, mNativeAds);
                            //Functions.createRecyclerView(context, songsResult, rvSongsResult, progressBar);
                            if (nativeAdsListener != null)
                                nativeAdsListener.onSuccess(songsResult, rvSongsResult, mNativeAds, progressBar);
                        }
                    }
                }).withAdListener(
                new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // A native ad failed to load, check if the ad loader has finished loading
                        // and if so, insert the ads into the list.
                        Log.e(Constants.TAG, "The previous native ad failed to load. Attempting to"
                                + " load another.");
                        if (!adLoader.isLoading()) {
                            insertAdsInBetweenSongsItems(songsResult, mNativeAds);
                            //Functions.createRecyclerView(context, songsResult, rvSongsResult, progressBar);
                            if (nativeAdsListener != null)
                                nativeAdsListener.onSuccess(songsResult, rvSongsResult, mNativeAds, progressBar);
                        }
                    }
                }).build();

        // Load the Native Express ad.
        int numberOfAdsToLoad = (songsResult.size() / ADS_OFFSET_MIN);
        adLoader.loadAds(new AdRequest.Builder().build(), numberOfAdsToLoad);
    }

    /**
     * Insert 'native-ads' in-between songs list according to constant OFFSET.
     * @param songsResult list of songs
     * @param mNativeAds list of native ads that were loaded successfully
     */
    private void insertAdsInBetweenSongsItems(ArrayList<Object> songsResult, List<UnifiedNativeAd> mNativeAds) {
        if (mNativeAds.size() <= 0)
            return;
        if (songsResult.size() % 2 == 0 && mNativeAds.size() % 2 != 0) // MostRecent & MostPopular will end list with 2 songs
            mNativeAds.remove(0);

        Random r = new Random();
        int index = ADS_OFFSET_MIN != ADS_OFFSET_MAX ? r.nextInt(ADS_OFFSET_MAX - ADS_OFFSET_MIN) + ADS_OFFSET_MIN : ADS_OFFSET_MIN; // If MIN == MAX there are few result only, so we will use MIN offset to make sure ads are shown
        for (int i = 0; i < mNativeAds.size() && index < songsResult.size(); i++) {
            songsResult.add(index, mNativeAds.get(i));
            rvSongsResult.getAdapter().notifyItemInserted(index);
            index += ADS_OFFSET_MIN != ADS_OFFSET_MAX ? r.nextInt(ADS_OFFSET_MAX - ADS_OFFSET_MIN) + ADS_OFFSET_MIN : ADS_OFFSET_MIN; // If MIN == MAX there are few result only, so we will use MIN offset to make sure ads are shown
        }
    }

    /**
     * Check if ads can be shown (If there are enough results).
     * <p> If there are few results, it changes the OFFSET accordingly.
     * @param listSize songsResult.size()
     * @return TRUE - Ads can be shown
     *      <p> FALSE - Not enough results to show ads
     */
    private boolean calAdsOffset(int listSize) {
        if (listSize >= ADS_OFFSET_MAX)
            return true;
        else if (listSize >= ADS_OFFSET_MIN) { // listSize is between MIN to MAX
            ADS_OFFSET_MAX = ADS_OFFSET_MIN;
            return true;
        }
        return false;// Not enough results to show ads
    }
}

