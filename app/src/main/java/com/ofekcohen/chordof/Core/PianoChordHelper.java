package com.ofekcohen.chordof.Core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.ofekcohen.chordof.R;

import static com.ofekcohen.chordof.Core.Constants.ALL_CHORDS;
import static com.ofekcohen.chordof.Core.Constants.possiblesRoots;
import static com.ofekcohen.chordof.Core.Constants.possiblesRootsMol;

public class PianoChordHelper extends View {

    private static final int NUMBER_OF_NOTES = 15;
    private static final int NOTES_IN_OCTAVE = 12;
    private static final int WIDTH_NOTES_OFFSET = 2;


    private String chord;
    private boolean[] places; // Sign for fingers on the piano

    // onDraw Constants
    private Rect[] rect;
    private int[] notes;

    public PianoChordHelper(Context context, String chord) {
        super(context);

        this.chord = rootAlwaysWithSharp(chord);

        this.rect = new Rect[NUMBER_OF_NOTES];
        this.notes = new int[NUMBER_OF_NOTES];
        this.places = new boolean[NUMBER_OF_NOTES];

        calChordProperties(this.chord);
        createPaints();
    }
    public PianoChordHelper(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public PianoChordHelper(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public PianoChordHelper(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private Paint whiteKeys, blackKeys, circle;
    private void createPaints() {
        whiteKeys = new Paint();
        whiteKeys.setColor(Color.parseColor("#F2FFFFFF")); // 95% of Color.WHITE
        whiteKeys.setStrokeWidth(40);

        blackKeys = new Paint();
        blackKeys.setColor(Color.parseColor("#000000")); // 95% of Color.WHITE
        blackKeys.setStrokeWidth(30);

        circle = new Paint();
        circle.setColor(getResources().getColor(R.color.colorPrimary));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isChordExist(chord)) {
            float widthSpace = (float) getWidth() / NUMBER_OF_NOTES;
            float heightSpace = (float) getHeight() / 4;

            // White Notes
            canvas.drawLine(
                    whiteKeys.getStrokeWidth() / 2,
                    10,
                    whiteKeys.getStrokeWidth() / 2,
                    heightSpace,
                    whiteKeys);
            for (int i = 0; i < rect.length; i++) {
                canvas.drawLine(
                    (i + 1) * widthSpace + whiteKeys.getStrokeWidth() / 2,
                    10,
                    (i + 1) * widthSpace + whiteKeys.getStrokeWidth() / 2,
                    heightSpace,
                    whiteKeys);
            }
            // Black Notes
            canvas.drawLine(
                    widthSpace / 2,
                    10,
                    widthSpace / 2,
                    (float) (heightSpace * 0.75),
                    blackKeys);
            boolean switchCondition = false;
            for (int i = 1; i < rect.length; i++) {
                 if (i != 3 && i != 7 && i != 10 && i != 14 && i != 17 && i != 21 && i != 24 && i != 28)
                    canvas.drawLine(
                            i * widthSpace + whiteKeys.getStrokeWidth(),
                            10,
                            i * widthSpace + whiteKeys.getStrokeWidth(),
                            (float) (heightSpace * 0.75),
                            blackKeys);
                switchCondition = !switchCondition;
            }

            // Places (Can't be on the same loop because the strings will appear above the circles
            /*for (int i = 0; i < NUMBER_OF_NOTES; i++) {
                    if (places[i])
                        canvas.drawCircle(
                                (j + 1) * widthSpace,
                                (i + 1) * heightSpace + (heightSpace / 2), // (heightSpace / 2) put circles in-between frets
                                (float) Math.min(getHeight(), getWidth()) / 21, // 21 is a ratio constant to make on 1050*1050(=getHeight()*getWidth) a 50 float radius
                                circle);
            }*/
        }
    }

    public static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    /**
     * Fetch data from the constant 'ALL_CHORDS' and use those properties to populate the 'ChordHelper'.
     * <p>This function doesn't check if the 'option' parameter is valid, this can be done using 'optionsOfChord()'.
     * If the option is not valid, an empty grid will appear.
     * @param chord Chord name to present in ChordHelper (Ex. Am/Eb, G7)
     */
    private void calChordProperties(String chord) {
        // C Chord
        //places[NOTES_IN_OCTAVE + 1] = true;
        //places[NOTES_IN_OCTAVE + 4] = true;
        //places[NOTES_IN_OCTAVE + 3] = true;
    }

    /**
     * Check if chord exist in the constant 'ALL_CHORDS'
     * @param chord
     * @return true - chord exist in 'ALL_CHORDS'
     */
    public static boolean isChordExist(String chord) {
        chord = rootAlwaysWithSharp(chord);
        int chordLineBeginIndex = ALL_CHORDS.indexOf("{define " + chord + ".");
        int chordLineEndIndex = ALL_CHORDS.indexOf("\n", chordLineBeginIndex);
        if (chordLineBeginIndex != -1 && chordLineEndIndex != -1)
            return true;
        return false;
    }

    /**
     * Make sure the chord's root will only be '#' and no 'b'.
     * It is essential because 'ALL_CHORDS' has only '#' roots.
     */
    private static String rootAlwaysWithSharp(String chord) {
        // Check if the root has "#" or "b"
        String root = "", restChord = "";
        if (chord.length() > 1 && (chord.charAt(1) == '#' || chord.charAt(1) == 'b')) {
            root = chord.substring(0, 2);
            restChord = chord.substring(2);

            // Change 'b' to '#' if needed
            for (int i = 0; i < possiblesRootsMol.length; i++) {
                if (possiblesRootsMol[i].equals(root))
                    chord = possiblesRoots[i] + restChord;
            }
        }
        return chord;
    }
}