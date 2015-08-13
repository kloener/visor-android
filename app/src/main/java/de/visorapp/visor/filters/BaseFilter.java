package de.visorapp.visor.filters;

import android.graphics.ColorMatrix;

/**
 * Created by Christian Illies on 10.08.15.
 *
 * BaseFilter class to give subclasses some pre-defined color setups
 * like greyscale or high contrast.
 */
public abstract class BaseFilter implements ColorFilter {


    /**
     * returns a float array with increased contrast for further use in
     * a {@link ColorMatrix} object.
     *
     * -1f no visible contrast (grey layer)
     * -0.5f low contrast
     * 0f no changes made
     * 1f high contrast
     * 2f very high contrast
     * ...
     *
     * @param contrast the contrast value you wanna increase (or decrease) -1f to +1f
     * @return
     */
    public float[] getContrastMatrix(float contrast) {
        float scale = contrast + 1.f;
        float translate = (-.5f * scale + .5f) * 255.f;
        //cm.set(new float[] {
        return new float[] {
                scale, 0, 0, 0, translate,
                0, scale, 0, 0, translate,
                0, 0, scale, 0, translate,
                0, 0, 0, 1, 0
        };
    }

    /**
     * inverts the colors of the {@link ColorMatrix}.
     * @return
     */
    public float[] getInvertMatrix() {
        return new float[] {
                -1,  0,  0,  0, 255,
                0,  -1,  0,  0, 255,
                0,   0, -1,  0, 255,
                0,   0,  0,  1,   0
        };
    }

    /**
     * inverts the colors of the {@link ColorMatrix}.
     * @return
     */
    public float[] getInvertedGreyscaledMatrix() {
        return new float[] {
                -0.5f,  -0.5f,  -0.5f,  0, 255,
                -0.5f,  -0.5f,  -0.5f,  0, 255,
                -0.5f,  -0.5f,  -0.5f,  0, 255,
                0,       0,      0,     1,   0
        };
    }

    /**
     * grey scale the colors of the {@link ColorMatrix}.
     * @return
     */
    public float[] getGreyscaleMatrix() {
        return new float[] {
                0.5f, 0.5f, 0.5f,  0, 0,
                0.5f, 0.5f, 0.5f,  0, 0,
                0.5f, 0.5f, 0.5f,  0, 0,
                   0,    0,    0,  1, 0
        };
    }
}
