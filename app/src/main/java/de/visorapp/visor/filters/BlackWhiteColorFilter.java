package de.visorapp.visor.filters;

import android.graphics.ColorMatrix;

/**
 * Created by Christian Illies on 02.08.15.
 */
public class BlackWhiteColorFilter extends BaseFilter {
    /**
     * our default contrast level
     */
    protected static final float CONTRAST_LEVEL = 0.66f;

    @Override
    public void filter(ColorMatrix colorMatrix) {
        float[] contrast = getContrastMatrix(CONTRAST_LEVEL);
        float[] greyscale = getGreyscaleMatrix();

        colorMatrix.postConcat(new ColorMatrix(greyscale));
        colorMatrix.postConcat(new ColorMatrix(contrast));
    }
}
