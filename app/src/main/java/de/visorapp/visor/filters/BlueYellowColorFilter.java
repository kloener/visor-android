package de.visorapp.visor.filters;

import android.graphics.ColorMatrix;

/**
 * Created by Christian Illies on 02.08.15.
 */
public class BlueYellowColorFilter extends BlackWhiteColorFilter {

    @Override
    public void filter(ColorMatrix colorMatrix) {
        float[] contrast = getContrastMatrix(CONTRAST_LEVEL);
        float[] blueYellowMatrix = getInvertedBlueYellowMatrix();
        colorMatrix.postConcat(new ColorMatrix(blueYellowMatrix));
    }

    /**
     * inverts the colors of the {@link ColorMatrix} by using blue as black and yellow as white.
     * @return
     */
    public float[] getInvertedBlueYellowMatrix() {
        return new float[] {
                 3,        3,       1,    0, -512,
                 3,        3,       1,    0, -512,
            -0.75f,     0.0f,    0.7f,    0,  128,
                 0,        0,       0,    1,    0
        };
    }
}