package de.visorapp.visor.threads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import java.io.ByteArrayOutputStream;

import de.visorapp.visor.BitmapRenderer;

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
     * It seems that it would cause unexpected behaviour if we disable the max
     * instances, because the rendered images get drawed unordered.
     * If your device is slow, it is better to have only one single thread rendering the image
     * in the background, wait for it and then render the next one.
     */
    private static final int MAX_INSTANCES = 1;

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

    /**
     * returns an instance of the task
     * @param yuvDataArray
     * @param renderer
     * @return
     */
    public static BitmapCreateThread getInstance(byte[] yuvDataArray, BitmapRenderer renderer, int previewWidth, int previewHeight, int targetWidth, int targetHeight, int jpegQuality) {

        if(MAX_INSTANCES > 0 && instanceCounter >= MAX_INSTANCES) {
            Log.d("BitmapCreateThread", "Thread Creation blocked, because we reached our MAX_INSTANCES.");
            return null;
        }

        BitmapCreateThread instance = new BitmapCreateThread();
        instanceCounter++;

        instance.setYuvDataArray(yuvDataArray);

        instance.setPreviewWidth(previewWidth);
        instance.setPreviewHeight(previewHeight);

        instance.setTargetWidth(targetWidth);
        instance.setTargetHeight(targetHeight);

        instance.setJpegQuality(jpegQuality);
        instance.setRenderer(renderer);

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
     * @return the resulting bitmap.
     */
    protected Bitmap createBitmap(byte[] yuvData) {
        long startTimeComplete = System.currentTimeMillis();

        YuvImage yuvImage = new YuvImage(yuvData, ImageFormat.NV21, previewWidth, previewHeight, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // here start the time-consuming functions:
        //long startTimeCreateJpeg = System.currentTimeMillis();
        yuvImage.compressToJpeg(new Rect(0, 0, previewWidth, previewHeight), jpegQuality, byteArrayOutputStream);
        //Log.d("BitmapCreateThread", "YUV compressToJpeg in " + Long.toString(System.currentTimeMillis() - startTimeCreateJpeg) + "ms");

        // this is your rendered Bitmap which has the same size as the camera preview,
        // but we want it to have the full screen size.
        //long startTimeDecode = System.currentTimeMillis();
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size());
        //Log.d("BitmapCreateThread", "BitmapFactory.decodeByteArray in "+Long.toString(System.currentTimeMillis()-startTimeDecode)+"ms");

        // so we have to convert it again:
        //long startTimeScale = System.currentTimeMillis();
        bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        //Log.d("BitmapCreateThread", "Bitmap scaled in "+Long.toString(System.currentTimeMillis()-startTimeScale)+"ms");

        Log.d("BitmapCreateThread", "Bitmap completely created - "+Long.toString(1000/(System.currentTimeMillis()-startTimeComplete))+" FPS");
        return bitmap;
    }

    /**
     * do the hard stuff.
     * @param yuvDataArray
     * @return
     */
    protected Bitmap doInBackground(byte[] yuvDataArray) {
        return this.createBitmap(yuvDataArray);
    }

    /**
     * after the hard stuff is done.
     * @param bitmap
     */
    protected void onPostExecute(Bitmap bitmap) {
        renderer.renderBitmap(bitmap);
        instanceCounter--;
    }

    @Override
    public void run() {
        onPostExecute(doInBackground(yuvDataArray));
    }

    public void setTargetWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    public void setTargetHeight(int targetHeight) {
        this.targetHeight = targetHeight;
    }
}
