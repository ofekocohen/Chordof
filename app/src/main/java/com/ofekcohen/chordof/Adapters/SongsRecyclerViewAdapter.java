package com.ofekcohen.chordof.Adapters;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.ofekcohen.chordof.Activities.ChordViewerActivity;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;

import static com.ofekcohen.chordof.Core.Constants.ITEM_PER_AD;
import static com.ofekcohen.chordof.Core.Constants.SONG_REF_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.TAG;

public class SongsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Object> songs;
    private Context context;

    private static final int ITEM_SONG = 0;
    private static final int ITEM_NATIVE_AD = 1;

    public SongsRecyclerViewAdapter(ArrayList<Object> songs, Context context) {
        this.songs = songs;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case ITEM_NATIVE_AD:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.native_ad_layout, parent, false);
                return new ViewHolderNativeAd(view);
            case ITEM_SONG:
            default:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_song_item, parent, false);
                return new ViewHolderSong(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        switch (viewType) {
            case ITEM_NATIVE_AD:
                UnifiedNativeAd nativeAd = (UnifiedNativeAd) songs.get(position);
                mapUnifiedNativeAdToLayout(nativeAd, ((ViewHolderNativeAd) holder).getAdView());
                break;
            case ITEM_SONG:
            default:
                ViewHolderSong viewHolderSong = (ViewHolderSong) holder;
                Song song = (Song) songs.get(position);
                viewHolderSong.tvSongName.setText(song.getName());
                viewHolderSong.tvSingerName.setText(song.getSinger());
                viewHolderSong.tvRating.setText(song.getRating() + "");

                loadYouTubeThumbnail(viewHolderSong, position);

        }
    }

    @Override
    public int getItemCount() {
        if (songs == null)
            return 0;
        return songs.size();
    }

    @Override
    public int getItemViewType(int position) {
        Object recyclerViewItem = songs.get(position);
        if (recyclerViewItem instanceof UnifiedNativeAd) {
            return ITEM_NATIVE_AD;
        }
        return ITEM_SONG;
    }

    public class ViewHolderSong extends RecyclerView.ViewHolder {
        TextView tvSongName, tvSingerName, tvRating;
        ImageView imgBackground;

        public ViewHolderSong(View itemView) {
            super(itemView);
            tvSongName = itemView.findViewById(R.id.tvSongName);
            tvSingerName = itemView.findViewById(R.id.tvSingerName);
            tvRating = itemView.findViewById(R.id.tvAuthorTitle);
            imgBackground = itemView.findViewById(R.id.imgBackground);

            // On song click
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, ChordViewerActivity.class);
                    intent.putExtra(SONG_REF_EXTRA_TAG, ((Song) songs.get(getAdapterPosition())).getFirestoreReference());
                    context.startActivity(intent);
                }
            });
        }
    }

    public class ViewHolderNativeAd extends RecyclerView.ViewHolder {
        private UnifiedNativeAdView adView;

        public UnifiedNativeAdView getAdView() {
            return adView;
        }

        public ViewHolderNativeAd(View itemView) {
            super(itemView);
            adView = (UnifiedNativeAdView) itemView.findViewById(R.id.ad_view);

            // The MediaView will display a video asset if one is present in the ad, and the
            // first image asset otherwise.
            adView.setMediaView((MediaView) adView.findViewById(R.id.ad_media));

            // Register the view used for each individual asset.
            adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
            adView.setBodyView(adView.findViewById(R.id.ad_body));
            adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
            adView.setIconView(adView.findViewById(R.id.ad_icon));
            adView.setPriceView(adView.findViewById(R.id.ad_price));
            adView.setStarRatingView(adView.findViewById(R.id.ad_rating));
            adView.setStoreView(adView.findViewById(R.id.ad_store));
            adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        }
    }

    private void loadYouTubeThumbnail(@NonNull final ViewHolderSong holder, int position) {
        String youtubeURL = ((Song) songs.get(position)).getYoutube();
        if (youtubeURL != null && !youtubeURL.isEmpty()) {
            String youtubeVideoID = "";
            try {
                if (youtubeURL.contains("?v="))
                    youtubeVideoID = youtubeURL.substring(youtubeURL.indexOf("?v=") + 3, youtubeURL.indexOf("?v=") + 3 + 11); // 11 - YouTube ID length
                else
                    youtubeVideoID = youtubeURL.substring(youtubeURL.lastIndexOf('/') + 1, youtubeURL.lastIndexOf('/') + 1 + 11); // 11 - YouTube ID length

                final String youtubeThumnail = Constants.YOUTUBE_THUMBNAIL.replace("CHORDOF", youtubeVideoID);
                Glide.with(context).load(youtubeThumnail)
                        .error(Glide.with(context).load(youtubeThumnail.replace("mqdefault", "0")))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.imgBackground);
            } catch (Exception e) {
                Log.d(TAG, "ERROR");
                Log.d(TAG, youtubeURL);
                Log.d(TAG, "ID - " + youtubeVideoID);
                Log.d(TAG, "//////////////");
            }
        }
        else
            holder.imgBackground.setVisibility(View.GONE);
    }

    public void mapUnifiedNativeAdToLayout(UnifiedNativeAd adFromGoogle, UnifiedNativeAdView myAdView) {
        MediaView mediaView = myAdView.findViewById(R.id.ad_media);
        myAdView.setMediaView(mediaView);

        ((TextView) myAdView.getHeadlineView()).setText(adFromGoogle.getHeadline());

        if (adFromGoogle.getBody() == null) {
            myAdView.getBodyView().setVisibility(View.GONE);
        } else {
            ((TextView) myAdView.getBodyView()).setText(adFromGoogle.getBody());
        }

        if (adFromGoogle.getCallToAction() == null) {
            myAdView.getCallToActionView().setVisibility(View.GONE);
        } else {
            ((Button) myAdView.getCallToActionView()).setText(adFromGoogle.getCallToAction());
        }

        if (adFromGoogle.getIcon() == null) {
            myAdView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) myAdView.getIconView()).setImageDrawable(adFromGoogle.getIcon().getDrawable());
        }

        if (adFromGoogle.getPrice() == null) {
            myAdView.getPriceView().setVisibility(View.GONE);
        } else {
            ((TextView) myAdView.getPriceView()).setText(adFromGoogle.getPrice());
        }

        if (adFromGoogle.getStarRating() == null) {
            myAdView.getStarRatingView().setVisibility(View.GONE);
        } else {
            ((RatingBar) myAdView.getStarRatingView()).setRating(adFromGoogle.getStarRating().floatValue());
        }

        if (adFromGoogle.getStore() == null) {
            myAdView.getStoreView().setVisibility(View.GONE);
        } else {
            ((TextView) myAdView.getStoreView()).setText(adFromGoogle.getStore());
        }

        if (adFromGoogle.getAdvertiser() == null) {
            myAdView.getAdvertiserView().setVisibility(View.GONE);
        } else {
            ((TextView) myAdView.getAdvertiserView()).setText(adFromGoogle.getAdvertiser());
        }

        myAdView.setNativeAd(adFromGoogle);
    }
}