package com.ofekcohen.chordof.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.Core.GuitarChordHelper;
import com.ofekcohen.chordof.Core.PianoChordHelper;
import com.ofekcohen.chordof.R;

import es.dmoral.toasty.Toasty;

public class ChordHelperDialog extends Dialog {

    private FrameLayout frmLayoutChordHelper;
    private TextView tvChordName;
    private Button btnNextOption, btnPrevOption;

    private String chord;
    private View chordHelper;
    private int chordHelperType;
    private int options, curOption;

    public ChordHelperDialog(@NonNull Context context, String chord) {
        super(context);
        this.chord = chord;
        this.chordHelperType = Constants.GUITAR;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chord_helper_dialog);

        if (GuitarChordHelper.isChordExist(chord)) {
            tvChordName = findViewById(R.id.tvChordName);
            frmLayoutChordHelper = findViewById(R.id.frmLayoutChordHelper);
            tvChordName.setText(chord);

            if (chordHelperType == Constants.GUITAR) {
                btnNextOption = findViewById(R.id.btnNextOption);
                btnPrevOption = findViewById(R.id.btnPrevOption);

                curOption = 1;
                chordHelper = new GuitarChordHelper(getContext(), chord, curOption);
                options = GuitarChordHelper.optionsOfChord(chord);

                if (options > 1) {
                    MyOnClickListener listener = new MyOnClickListener();
                    btnNextOption.setOnClickListener(listener);
                    btnPrevOption.setOnClickListener(listener);

                    btnNextOption.setVisibility(View.VISIBLE);
                    btnPrevOption.setVisibility(View.INVISIBLE);

                }
            }

            else if (chordHelperType == Constants.PIANO) {
                chordHelper = new PianoChordHelper(getContext(), chord);
            }

            frmLayoutChordHelper.addView(chordHelper, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
        else {
            Toasty.error(getContext(), "האקורד לא קיים במערכת שלנו").show();
        }
    }
    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (chordHelperType == Constants.GUITAR
                    && (btnNextOption == v || btnPrevOption == v)) {
                if (btnNextOption == v && curOption + 1 <= options) {
                    curOption++;
                    chordHelper = new GuitarChordHelper(getContext(), chord, curOption);
                    btnPrevOption.setVisibility(View.VISIBLE);
                    if (curOption == options) {
                        btnNextOption.setVisibility(View.INVISIBLE);
                    }
                }
                else if (btnPrevOption == v && curOption - 1 >= 1) {
                    curOption--;
                    chordHelper = new GuitarChordHelper(getContext(), chord, curOption);
                    btnNextOption.setVisibility(View.VISIBLE);
                    if (curOption == 1) {
                        btnPrevOption.setVisibility(View.INVISIBLE);
                    }
                }

                frmLayoutChordHelper.removeAllViews();
                frmLayoutChordHelper.addView(chordHelper);
            }
        }
    }
}
