package com.ofekcohen.chordof.Classes;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.ofekcohen.chordof.Activities.ChordViewerActivity;
import com.ofekcohen.chordof.Activities.CreateSongActivity;
import com.ofekcohen.chordof.Core.Constants;

import org.jmusixmatch.MusixMatch;
import org.jmusixmatch.MusixMatchException;
import org.jmusixmatch.entity.lyrics.Lyrics;
import org.jmusixmatch.entity.track.Track;
import org.jmusixmatch.entity.track.TrackData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ofekcohen.chordof.Core.Constants.AUTHOR_NAME_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.CHORDS_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.COMPOSER_NAME_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.MUSIXMATCH_API_KEY;
import static com.ofekcohen.chordof.Core.Constants.SINGER_NAME_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.SONG_NAME_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.SPACE_RATIO;
import static com.ofekcohen.chordof.Core.Constants.TAG;
import static com.ofekcohen.chordof.Core.Constants.WHITE_SPACE;
import static com.ofekcohen.chordof.Core.Constants.YOUTUBE_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.regexChords;

public class FetchFromMusixmatch {

    public static class FetchLyricsFromMusixmatch extends AsyncTask<Void, Void, Void> {

        private Context context;
        private MusixMatch musixMatch;
        private Song song;
        private Document doc;

        public FetchLyricsFromMusixmatch(Context context, Song song) {
            this.context = context;
            this.song = song;
            this.musixMatch = new MusixMatch(MUSIXMATCH_API_KEY);
        }

        protected Void doInBackground(Void... params) {
            try {
                /* Search song in Musixmatch database */
                Track track = musixMatch.getMatchingTrack(song.getName(), song.getSinger());
                TrackData data = track.getTrack();
//                Lyrics lyrics = musixMatch.getLyrics(data.getTrackId()); // Gives only 30% of the lyrics

                /* Get song's Musixmatch URL and fetch lyrics using Jsoup */
                String urlMusixmatch = data.getTrackShareUrl();
                doc = Jsoup.connect(urlMusixmatch).get();
                StringBuilder lyrics = new StringBuilder();
                Elements spans = doc.getElementsByClass("lyrics__content__ok");
                if (spans.isEmpty())
                        spans = doc.getElementsByClass("lyrics__content__warning");
                for (Element span : spans)
                    lyrics.append(span.wholeText());
                if (lyrics.length() != 0)
                    song.setChords(lyrics.toString());
                else // // Song was found but their is an error inside Musixmatch - "Lyrics not available."
                    Log.d(TAG, "No lyrics available from MusixMatch");
            } catch (MusixMatchException e) {
                e.printStackTrace();
                Log.e(TAG, "Couldn't find lyrics from MusixMatch");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void aVoid) {
            CreateSongActivity.setLyricsFromMusixmatch(song.getChords());
        }
    }
}
