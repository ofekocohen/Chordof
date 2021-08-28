package com.ofekcohen.chordof.Core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.ofekcohen.chordof.R;

import static com.ofekcohen.chordof.Core.Constants.ALL_CHORDS;
import static com.ofekcohen.chordof.Core.Constants.possiblesRoots;
import static com.ofekcohen.chordof.Core.Constants.possiblesRootsMol;

public class GuitarChordHelper extends View {

    private String chord;
    private char[] xo; // Sign for if you need to play the string or not (X, O)
    private boolean[][] places; // Sign for fingers on the strings
    private int fret;

    // onDraw Constants
    private Rect[] rect;
    private char[] notes;

    public GuitarChordHelper(Context context, String chord, int option) {
        super(context);

        this.chord = rootAlwaysWithSharp(chord);

        this.rect = new Rect[6];
        this.notes = new char[] {'E', 'A', 'D', 'G', 'B', 'E'};

        this.xo = new char[] {'N', 'N', 'N', 'N', 'N', 'N'}; // new char[] {'X', 'N', 'O', 'X', 'N', 'X'}
        this.places = new boolean[5][6];
        this.fret = 0; // 10 (fr)

        calChordProperties(this.chord, option);
        createPaints();
    }
    public GuitarChordHelper(Context context, char[] xo, boolean[][] places, int fret) {
        super(context);

        this.xo = xo; // new char[] {'X', 'N', 'O', 'X', 'N', 'X'}
        this.places = places; // new boolean[5][6]
        this.fret = fret; // 10 (fr)
        this.rect = new Rect[6];
        notes = new char[] {'E', 'A', 'D', 'G', 'B', 'E'};

        this.xo = new char[] {'X', 'N', 'O', 'X', 'N', 'X'};
        this.places = new boolean[5][6];
        this.places[1][1] = true;
        this.places[2][0] = true;
        this.places[2][4] = true;
        this.places[2][5] = true;
        this.fret = 10;

        createPaints();
    }

    public GuitarChordHelper(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GuitarChordHelper(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GuitarChordHelper(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private Paint strings, grid, notesText, xoText, circle, fretBar, fretText;
    private void createPaints() {
        strings = new Paint();
        strings.setColor(Color.parseColor("#F2FFFFFF")); // 95% of Color.WHITE
        strings.setStrokeWidth(20);

        grid = new Paint();
        grid.setColor(Color.parseColor("#80FFFFFF")); // 50% of Color.WHITE
        grid.setStrokeWidth(strings.getStrokeWidth() * (float) 0.75);

        notesText = new Paint();
        notesText.setColor(Color.WHITE);
        notesText.setTextSize(dpToPx(getContext(), 20));
        Typeface tfNotesText = ResourcesCompat.getFont(getContext(), R.font.segoeuib);
        notesText.setTypeface(tfNotesText);

        xoText = new Paint();
        xoText.setColor(Color.WHITE);
        xoText.setTextSize(dpToPx(getContext(), 20));
        Typeface tfXoText = ResourcesCompat.getFont(getContext(), R.font.segoeui);
        xoText.setTypeface(tfXoText);

        circle = new Paint();
        circle.setColor(Color.WHITE);

        fretBar = new Paint();
        fretBar.setColor(Color.parseColor("#F2FFFFFF")); // 95% of Color.WHITE
        fretBar.setStrokeWidth(70);

        fretText = new Paint();
        fretText.setColor(getResources().getColor(R.color.colorPrimary));
        fretText.setTextSize(dpToPx(getContext(), 20));
        Typeface tfFretText = ResourcesCompat.getFont(getContext(), R.font.segoeui);
        fretText.setTypeface(tfFretText);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isChordExist(chord)) {
            float widthSpace = (float) getWidth() / 7;
            float heightSpace = (float) getHeight() / 7;

            for (int i = 0; i < rect.length; i++) {
                if (i == 0 || i == rect.length - 1)
                    grid.setColor(Color.WHITE);
                else
                    grid.setColor(Color.parseColor("#80FFFFFF")); // 50% of Color.WHITE

                // Vertical Lines (Strings)
                canvas.drawLine(
                        (i + 1) * widthSpace,
                        heightSpace,
                        (i + 1) * widthSpace,
                        getHeight() - heightSpace,
                        strings);

                // Horizontal Lines (Grid)
                canvas.drawLine(
                        widthSpace - grid.getStrokeWidth() / (float) 1.5,
                        (i + 1) * heightSpace,
                        getWidth() - widthSpace + grid.getStrokeWidth() / (float) 1.5,
                        (i + 1) * heightSpace,
                        grid);

                // Notes
                canvas.drawText(String.valueOf(notes[i]),
                        (i + 1) * widthSpace - grid.getStrokeWidth(),
                        getHeight() - heightSpace / 3,
                        notesText);

                // X / O
                if (xo[i] == 'X' || xo[i] == 'O')
                    canvas.drawText(String.valueOf(xo[i]),
                            (i + 1) * widthSpace - grid.getStrokeWidth(),
                            heightSpace / (float) 1.3,
                            xoText);
            }

            // Fret
            if (fret != 0) {
                // Draw a bar
                canvas.drawLine(
                        widthSpace - widthSpace / (float) 2.5,
                        heightSpace + (heightSpace / 2),
                        getWidth() - widthSpace / (float) 1.5,
                        heightSpace + (heightSpace / 2),
                        fretBar
                );

                canvas.drawText(fret + "fr",
                        getWidth() / (float) 2.2,
                        heightSpace + (heightSpace / (float) 1.65),
                        fretText);
            }

            // Places (Can't be on the same loop because the strings will appear above the circles
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 6; j++)
                    if (places[i][j])
                        canvas.drawCircle(
                                (j + 1) * widthSpace,
                                (i + 1) * heightSpace + (heightSpace / 2), // (heightSpace / 2) put circles in-between frets
                                (float) Math.min(getHeight(), getWidth()) / 21, // 21 is a ratio constant to make on 1050*1050(=getHeight()*getWidth) a 50 float radius
                                circle);
            }
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
     * @param option Chord variation/option (The first option is 1, not 0)
     */
    private void calChordProperties(String chord, int option) {
        // Example of chordProperties => "{define A.              base-fret 1 frets 0 0 2 2 2 0}"

        // Move to specific option
        int chordLineBeginIndex = ALL_CHORDS.indexOf("{define " + chord + ".");
        int chordLineEndIndex = ALL_CHORDS.indexOf("\n", chordLineBeginIndex);
        for (int i = 1; i < option; i++) {
            chordLineBeginIndex = ALL_CHORDS.indexOf("{define " + chord + ".", chordLineBeginIndex + 1);
            chordLineEndIndex = ALL_CHORDS.indexOf("\n", chordLineBeginIndex);
        }
        // Validate the option
        if (chordLineBeginIndex != -1 && chordLineEndIndex != -1) {
            String chordProperties = ALL_CHORDS.substring(chordLineBeginIndex, chordLineEndIndex);

            /* Fetch Data */
            // Find 'base-fret' value
            int fretsOffset = 0;
            int baseFret = Integer.parseInt(chordProperties.substring(34, chordProperties.indexOf(" ", 34)));
            if (baseFret >= 10) // if 'base-fret' is 2 digit we need to offset all the calculations
                fretsOffset++;

            // Find 'frets' values
            int[] strings = new int[6];
            int j = 0;
            for (int i = 0; i < strings.length; i++) {
                strings[i] = Character.getNumericValue(chordProperties.charAt((42 + fretsOffset) + j));
                if (strings[i] == Character.getNumericValue('x'))
                    strings[i] = -1;
                j += 2;
            }


            /* Put Data In Place */
            // Calculate fret calue
            fret = baseFret == 1 ? 0 : baseFret;

            // Calculate where to put circle and on which string put 'X'
            for (int i = 0; i < strings.length; i++) {
                if (strings[i] == -1) {
                    xo[i] = 'X';
                } else if (strings[i] != 0)
                    places[strings[i] - 1][i] = true;
            }
        }
    }

    /**
     * Calculate how many options the chord has
     * @param chord
     * @return options - number of chord's options
     */
    public static int optionsOfChord(String chord) {
        chord = rootAlwaysWithSharp(chord);
        int chordLineBeginIndex = ALL_CHORDS.indexOf("{define " + chord + ".");
        int chordLineEndIndex = ALL_CHORDS.indexOf("\n", chordLineBeginIndex);
        int options = 0;
        while (chordLineBeginIndex != -1 && chordLineEndIndex != -1) {
            options++;
            chordLineBeginIndex = ALL_CHORDS.indexOf("{define " + chord + ".", chordLineBeginIndex + 1);
            chordLineEndIndex = ALL_CHORDS.indexOf("\n", chordLineBeginIndex);
        }
        return options;
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