package de.visorapp.visor.threads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;

import de.visorapp.visor.BitmapRenderer;

/**
 * Test to use a separate AsyncTask to handle the jpeg compression for the yuvData
 * to receive the Bitmap out of it.
 *
 * Test failed.
 *
 * @deprecated My internal test failed, so don't use this class. Instead use the {@link BitmapCreateThread}.
 *
 * Created by Christian Illies on 11.08.15.
 */
public class AsyncBitmapCreateTask extends AsyncTask<byte[], Void, Bitmap> {

    /**
     * too many simultan instances will end up in a huge memory leak
     */
    private static final int MAX_INSTANCES = 1;

    /**
     * count all instances.
     */
    private static int instanceCounter = 0;
    
    private int previewWidth;
    private int previewHeight;
    private int jpegQuality;
    private BitmapRenderer renderer;

    /**
     * returns an instance of the task
     * @param yuvDataArray
     * @param renderer
     * @return
     */
    public static AsyncTask<byte[], Void, Bitmap> getInstance(byte[] yuvDataArray, BitmapRenderer renderer, int previewWidth, int previewHeight, int jpegQuality) {
        
        if(instanceCounter > MAX_INSTANCES) {
            instanceCounter = 0;
            return null;
        }
        
        AsyncBitmapCreateTask instance = new AsyncBitmapCreateTask();
        instanceCounter++;

        instance.setPreviewWidth(previewWidth);
        instance.setPreviewHeight(previewHeight);
        instance.setJpegQuality(jpegQuality);
        instance.setRenderer(renderer);

        return instance.execute(yuvDataArray);
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

    @Override
    protected Bitmap doInBackground(byte[]... yuvDataArray) {
        int count = yuvDataArray.length;
        Bitmap[] bitmaps = new Bitmap[1];
        for (int i = 0; i < count; i++) {
            bitmaps[i] = this.createBitmap(yuvDataArray[i]);
            if (isCancelled()) break;
        }
        return bitmaps[0];
    }


    @Override
    protected void onPostExecute(Bitmap bitmap) {
        renderer.renderBitmap(bitmap);
    }
}
