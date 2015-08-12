package de.visorapp.visor.threads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;

import de.visorapp.visor.BitmapRenderer;

/**
 * Created by handspiel on 11.08.15.
 */
public class BitmapCreateThread implements Runnable {

    /**
     * too many simultan instances will end up in a huge memory leak
     * set to 0 to disable limitations.
     */
    private static final int MAX_INSTANCES = 0;

    /**
     * count all instances.
     */
    private static int instanceCounter = 0;
    private int previewWidth;

    private int previewHeight;
    private int jpegQuality;
    private BitmapRenderer renderer;
    private byte[] yuvDataArray;

    /**
     * returns an instance of the task
     * @param yuvDataArray
     * @param renderer
     * @return
     */
    public static BitmapCreateThread getInstance(byte[] yuvDataArray, BitmapRenderer renderer, int previewWidth, int previewHeight, int jpegQuality) {

        if(MAX_INSTANCES > 0 && instanceCounter > MAX_INSTANCES) {
            instanceCounter = 0;
            Log.d("BitmapCreateThread", "Instance blocked");
            return null;
        }

        BitmapCreateThread instance = new BitmapCreateThread();
        instanceCounter++;

        Log.d("BitmapCreateThread", "Instance created. Current Counter: "+Integer.toString(instanceCounter));

        instance.setYuvDataArray(yuvDataArray);
        instance.setPreviewWidth(previewWidth);
        instance.setPreviewHeight(previewHeight);
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
        YuvImage yuvImage = new YuvImage(yuvData, ImageFormat.NV21, previewWidth, previewHeight, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, previewWidth, previewHeight), jpegQuality, byteArrayOutputStream);
        return BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size());
    }

    protected Bitmap doInBackground(byte[] yuvDataArray) {
        return this.createBitmap(yuvDataArray);
    }

    protected void onPostExecute(Bitmap bitmap) {
        renderer.renderBitmap(bitmap);
        instanceCounter--;
    }

    @Override
    public void run() {
        onPostExecute(doInBackground(yuvDataArray));
    }
}
