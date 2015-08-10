package de.visorapp.visor.filters;

import android.graphics.ColorMatrix;

/**
 * Created by Christian Illies on 02.08.15.
 */
public class BlackWhiteColorFilter extends BaseFilter {
    @Override
    public void filter(ColorMatrix colorMatrix) {
        float[] contrast = getContrastMatrix(1f);
        float[] greyscale = getContrastMatrix(1f);

        colorMatrix.postConcat(new ColorMatrix(greyscale));
        colorMatrix.postConcat(new ColorMatrix(contrast));
    }
}
