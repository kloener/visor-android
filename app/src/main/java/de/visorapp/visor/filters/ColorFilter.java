package de.visorapp.visor.filters;

import android.graphics.ColorMatrix;

/**
 * Created by Christian Illies on 02.08.15.
 */
public interface ColorFilter {
    /**
     * Filters the given matrix by using {@link ColorMatrix}.postConcat.
     * The given colorMatrix is handled as a reference so we don't need return value.
     * @param colorMatrix the color matrix you wanna change
     */
    void filter(ColorMatrix colorMatrix);
}
