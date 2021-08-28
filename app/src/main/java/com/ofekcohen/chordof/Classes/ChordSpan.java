package com.ofekcohen.chordof.Classes;

import android.content.Context;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

import com.ofekcohen.chordof.Core.Constants;


/**
 * Chords highlight in textView
 */
public class ChordSpan extends ClickableSpan {

    private Context context;
    private String chord;

    public ChordSpan(Context context, String chord) {
        this.context = context;
        this.chord = chord;
    }

    @Override
    public void onClick(@NonNull View widget) {
        // Not Clickable
    }
    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        super.updateDrawState(ds);
        ds.setColor(Constants.CHORD_COLOR);
        ds.setUnderlineText(false);
    }
}
