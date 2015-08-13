package de.visorapp.visor;

import android.graphics.Bitmap;

/**
 * Created by handspiel on 11.08.15.
 */
public interface BitmapRenderer {
    /**
     * renders bitmaps.
     * You can also use it as a setter method.
     * @param bitmap
     */
    void renderBitmap(Bitmap bitmap);
}
