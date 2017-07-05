package de.visorapp.visor.threads;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import de.visorapp.visor.BitmapRenderer;
import de.visorapp.visor.NativeYuvDecoder;

/**
 * An external thread to render the bitmap out of the raw yuvData from the legacy camera preview API.
 *
 * Created by Christian Illies on 11.08.15.
 */
public class BitmapCreateThread implements Runnable {

    /**
     * too many simultan instances will end up in a huge memory leak
     * set to 0 to disable limitations.
     *
     * Old Info:
     * It seems that it would cause unexpected behaviour if we disable the max
     * instances, because the rendered images get drawed unordered.
     * If your device is slow, it is better to have only one single thread rendering the image
     * in the background, wait for it and then render the next one.
     *
     * New Info:
     * On my new telephone LG G4 it works much better with a higher instances value.
     */
    private static final int MAX_INSTANCES = 3;

    /**
     * count all instances.
     */
    private static int instanceCounter = 0;

    /**
     *
     */
    private int previewWidth;
    private int previewHeight;
    private int targetWidth;
    private int targetHeight;
    private int jpegQuality;
    private BitmapRenderer renderer;
    private byte[] yuvDataArray;
    private boolean useRgb;
    private int[] rgbArray;

    private Bitmap renderedBitmap;

    /**
     * returns an instance of the task
     *
     * @param yuvDataArray
     * @param renderer
     * @return
     */
    public static BitmapCreateThread getInstance(int[] rgb, byte[] yuvDataArray, BitmapRenderer renderer, int previewWidth, int previewHeight, int targetWidth, int targetHeight, int jpegQuality, boolean useRgb) {

        if (instanceCounter >= MAX_INSTANCES) {
            Log.d("BitmapCreateThread", "Thread Creation blocked, because we reached our MAX_INSTANCES.");
            return null;
        }


        BitmapCreateThread instance = new BitmapCreateThread();
        instanceCounter++;
        // Log.d("BitmapCreateThread", "BitmapCreateThreads: " + instanceCounter);

        instance.setYuvDataArray(yuvDataArray);

        instance.setPreviewWidth(previewWidth);
        instance.setPreviewHeight(previewHeight);

        instance.setTargetWidth(targetWidth);
        instance.setTargetHeight(targetHeight);

        instance.setJpegQuality(jpegQuality);
        instance.setRenderer(renderer);
        instance.setUseRgb(useRgb);
        instance.setRgbArray(rgb);

        return instance;
    }

    public void setJpegQuality(int jpegQuality) {
        this.jpegQuality = jpegQuality;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    public void setRenderer(BitmapRenderer renderer) {
        this.renderer = renderer;
    }

    public void setYuvDataArray(byte[] yuvDataArray) {
        this.yuvDataArray = yuvDataArray;
    }

    /**
     * the actual hard work.
     * @param yuvData
     */
    protected void createBitmap(byte[] yuvData) {
        // YuvImage yuvImage = new YuvImage(yuvData, ImageFormat.NV21, previewWidth, previewHeight, null);


        // greyscale bitmap rendering is a bit faster than yuv-to-rgb convert.
        //int[] rgbData;

        // different strategies (for performance): use greyscale in preview mode and rgb in picture mode.
        //if(!useRgb) rgbData = this.decodeYuvWithNativeYuvToGreyScale(yuvData, previewWidth, previewHeight);
        if(!useRgb) this.decodeYuvWithNativeYuvToGreyScale(rgbArray, yuvData, previewWidth, previewHeight);
        else this.decodeYuvToRgb(rgbArray, yuvData, previewWidth, previewHeight);

        if(renderedBitmap == null) {
            renderedBitmap = Bitmap.createBitmap(previewWidth, previewHeight, android.graphics.Bitmap.Config.ARGB_8888);
        }
        renderedBitmap.setPixels(rgbArray, 0, previewWidth, 0, 0, previewWidth, previewHeight);

        // scaling (costs a lot of memory)
        // renderedBitmap = Bitmap.createScaledBitmap(renderedBitmap, targetWidth, targetHeight, true);
    }

    /**
     * custom scaling function. replaces createScaledBitmap.
     *
     * DO NOT USE. the bitmap gets scaled in "onDraw" via a Matrix.
     *
     * @param bitmap
     * @param newWidth
     * @param newHeight
     * @return
     */
    public static Bitmap scaleBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, android.graphics.Bitmap.Config.ARGB_8888);

        float scaleX = newWidth / (float) bitmap.getWidth();
        float scaleY = newHeight / (float) bitmap.getHeight();
        float pivotX = 0;
        float pivotY = 0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    private void decodeYuvWithNativeYuvToGreyScale(int[] rgb, byte[] yuvData, int width, int height) {
        //int pixelCount = width * height;
        //)//int[] out = new int[pixelCount];
        NativeYuvDecoder.YUVtoRGBGreyscale(yuvData, width, height, rgb);
        //return out;
    }

    // NOTE does change the colors
    private void decodeYuvWithNativeYuvToRgb(int[] rgb, byte[] yuvData, int width, int height) {
        //int pixelCount = width * height;
        // int[] out = new int[pixelCount];
        NativeYuvDecoder.YUVtoRBGA(yuvData, width, height, rgb);
        //return out;
    }

    /**
     * decodes YUV to RGB
     * @source https://stackoverflow.com/questions/8350230/android-how-to-display-camera-preview-with-callback
     * @param nv21
     * @param width
     * @param height
     * @return
     */
    private void decodeYuvToRgb(int[] rgb, byte[] nv21, int width, int height) {
        int frameSize = width * height;
        //int[] rgba = new int[frameSize + 1];

        // Convert YUV to RGB
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) nv21[i * width + j]));
                int u = (0xff & ((int) nv21[frameSize + (i >> 1) * width + (j & ~1) + 0]));
                int v = (0xff & ((int) nv21[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;

                // @source http://www.wordsaretoys.com/2013/10/18/making-yuv-conversion-a-little-faster/
                // @thanks John Jared (https://codetracer.co/profile/109)
                int a0 = 1192 * (y - 16);
                int a1 = 1634 * (v - 128);
                int a2 = 832 * (v - 128);
                int a3 = 400 * (u - 128);
                int a4 = 2066 * (u - 128);

                int r = (a0 + a1) >> 10;
                int g = (a0 - a2 - a3) >> 10;
                int b = (a0 + a4) >> 10;
                /*int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));*/

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                rgb[i * width + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }

        //return rgba;
    }

    /**
     * do the hard stuff.
     * @param yuvDataArray
     * @return
     */
    protected void doInBackground(byte[] yuvDataArray) {
        this.createBitmap(yuvDataArray);
    }

    /**
     * after the hard stuff is done.
     */
    protected void onPostExecute() {
        renderer.renderBitmap(renderedBitmap);
        instanceCounter--;
    }

    @Override
    public void run() {
        doInBackground(yuvDataArray);
        onPostExecute();
    }

    public void setTargetWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    public void setTargetHeight(int targetHeight) {
        this.targetHeight = targetHeight;
    }

    public void setUseRgb(boolean useRgb) {
        this.useRgb = useRgb;
    }

    public void setRgbArray(int[] rgbArray) {
        this.rgbArray = rgbArray;
    }
}
