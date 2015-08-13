package de.visorapp.visor.filters;

import android.graphics.ColorMatrix;

/**
 * Created by Christian Illies on 02.08.15.
 */
public class WhiteBlackColorFilter extends BlackWhiteColorFilter {
    @Override
    public void filter(ColorMatrix colorMatrix) {
        super.filter(colorMatrix);

        float[] inverted = getInvertMatrix();
        colorMatrix.postConcat(new ColorMatrix(inverted));
    }
}