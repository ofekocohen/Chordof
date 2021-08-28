package com.ofekcohen.chordof.Classes;

import android.content.Context;

import androidx.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Toast;

import com.ofekcohen.chordof.Core.GuitarChordHelper;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Dialogs.ChordHelperDialog;

import es.dmoral.toasty.Toasty;


/**
 * Chords highlight and clickable in textView
 */
public class ChordClickableSpan extends ClickableSpan {

    private Context context;
    private String chord;

    public ChordClickableSpan(Context context, String chord) {
        this.context = context;
        this.chord = chord;
    }

    @Override
    public void onClick(@NonNull View widget) {
        if (GuitarChordHelper.isChordExist(chord)) {
            ChordHelperDialog chordHelperDialog = new ChordHelperDialog(context, chord);
            chordHelperDialog.show();
        }
        else
            Toasty.info(context, "האקורד " + chord + " לא קיים במערכת שלנו", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        super.updateDrawState(ds);
        ds.setColor(Constants.CHORD_COLOR);
        ds.setUnderlineText(false);
    }
}
