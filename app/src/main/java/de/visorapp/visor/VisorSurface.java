package de.visorapp.visor;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.visorapp.visor.filters.BlackWhiteColorFilter;
import de.visorapp.visor.filters.BlueYellowColorFilter;
import de.visorapp.visor.filters.ColorFilter;
import de.visorapp.visor.filters.NoColorFilter;
import de.visorapp.visor.filters.WhiteBlackColorFilter;
import de.visorapp.visor.filters.YellowBlueColorFilter;

/**
 * Created by root on 29.07.15.
 */
public class VisorSurface extends SurfaceView implements SurfaceHolder.Callback {

    /**
     * The debug Tag identifier for the whole class.
     */
    private static final String TAG = "VisorSurface";

    /**
     * The maximum of steps until we will reach the maximum zoom level.
     */
    private static final int mCameraZoomSteps = 4;

    /**
     * TODO: I don't know that ...
     */
    private static final boolean ALG = true;

    /**
     * TODO: I don't know if we really need that, because it obviously doesn't work.
     */
    private static final int JPEG_QUALITY = 70;

    /**
     * Camera state: Device is closed.
     */
    private static final int STATE_CLOSED = 0;

    /**
     * Camera state: Device is opened, but is not capturing.
     */
    private static final int STATE_OPENED = 1;

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 2;

    /**
     * thread description and identifier.
     */
    private static final String CAMERA_PREVIEW_THREAD = "visorSurfaceCameraThread";

    /**
     * the currently used camera id.
     * Only available if mState is Opened or Preview.
     */
    private static int mCameraId;

    /**
     * contains the current color effect index of the supported
     * color effects from the camera.
     * TODO: should be deleted, if we're using our custom filters.
     */
    private int mCameraColorEffectIndex;

    /**
     *
     */
    private SurfaceHolder mHolder;

    /**
     * The camera device reference.
     * An instance will be created if the surface is created.
     * We'll close the camera reference if the surface gets destroyed.
     */
    private Camera mCamera;

    /**
     * defines the current zoom level of the camera.
     */
    private int mCameraCurrentZoomLevel;

    /**
     * if true the flashlight should be on.
     */
    private boolean mCameraFlashMode;
    /**
     * stores the value of the devices max zoom level of the camera.
     */
    private int mCameraMaxZoomLevel;

    /**
     * @deprecated use the mState instead.
     */
    private boolean mCameraPreviewIsRunning;

    /**
     * TODO: make it work.
     * The current filter for the camera.
     * The filter is an interface which takes some bytes as the param and
     * converts the bits to make several different color effects.
     */
    private ColorFilter mCameraColorFilter;

    /**
     * the width of the view and camera preview.
     */
    private int width;

    /**
     * the height of the view and the camera preview
     */
    private int height;

    /**
     * the maximum possible width of the camera preview that we'll use..
     */
    private int mCameraPreviewWidth;

    /**
     * the maximum possible height of the camera preview that we'll use.
     */
    private int mCameraPreviewHeight;

    /**
     * position fix if the camera preview is smaller than the device's screen.
     */
    private int mCameraPositionMoveX;

    /**
     * position fix if the camera preview is smaller than the device's screen.
     */
    private int mCameraPositionMoveY;

    /**
     * TODO: rename to something more expressive
     * TODO: do we need it?
     */
    private int[] rgba;
    /**
     * TODO: rename to something more expressive
     * TODO: do we need it?
     */
    private Paint paint;

    /**
     * A {@link Handler} for running tasks in the background. We will use a seperate thread
     * for the camera preview to have a smooth preview.
     */
    private Handler mBackgroundHandler;

    /**
     * An additional thread for running tasks that shouldn't block the UI.  This is used for all
     * callbacks from the {@link Camera}.
     */
    private HandlerThread mBackgroundThread;

    /**
     * Monitor Object for synchronzied accesses.
     */
    final private Object mCameraStateLock = new Object();;

    /**
     * the current state of the camera device.
     * i.e. open, closed or preview.
     */
    private int mState;

    /*
        some pre-defined color filters:
     */

    /**
     * const for the blue yellow color filter {@link BlueYellowColorFilter}
     */
    public final static ColorFilter BLUE_YELLOW_COLOR_FILTER = new BlueYellowColorFilter();

    /**
     * const for the yellow blue color filter {@link YellowBlueColorFilter}
     */
    public final static ColorFilter YELLOW_BLUE_COLOR_FILTER = new YellowBlueColorFilter();

    /**
     * const for the b/w color filter {@link BlackWhiteColorFilter}
     */
    public final static ColorFilter BLACK_WHITE_COLOR_FILTER = new BlackWhiteColorFilter();

    /**
     * const for the w/b color filter {@link WhiteBlackColorFilter}
     */
    public final static ColorFilter WHITE_BLACK_COLOR_FILTER = new WhiteBlackColorFilter();

    /**
     * const for the no color filter {@link NoColorFilter}
     */
    public final static ColorFilter NO_FILTER = new NoColorFilter();

    /**
     * the preview format
     */
    private int mCameraPreviewFormat;

    /**
     * stores the YUV image (format NV21) when onPreviewFrame was called
     */
    private byte[] mCameraPreviewBufferData;

    /**
     * after the onPreviewFrame was called we'll generate a
     * bitmap for usage in onDraw.
     */
    private Bitmap mCameraPreviewBitmapBuffer;
    /**
     * callback for camera previews
     */
    private Camera.PreviewCallback mCameraPreviewCallbackHandler = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // Log.d(TAG, "catched preview frame");
            Camera.Parameters parameters = mCamera.getParameters();
            int imageFormat = parameters.getPreviewFormat();

            if (imageFormat == ImageFormat.NV21)
            {
                mCameraPreviewBufferData = data;
                createBitmap();
                invalidate();
                /*
                Rect rect = new Rect(mCameraPositionMoveX, mCameraPositionMoveY, mCameraPreviewWidth, mCameraPreviewHeight);
                YuvImage img = new YuvImage(data, ImageFormat.NV21, mCameraPreviewWidth, mCameraPreviewHeight, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try
                {
                    img.compressToJpeg(rect, 10, baos);
                    mCameraPreviewBufferData = baos.toByteArray();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                */
            }



            // cameraPreviewBuffer = yuvsSource;
            // createBitmap();
            // invalidate();
            // mCamera.addCallbackBuffer(yuvsSource);
        }
    };

    /**
     *
     * @param context activity
     */
    public VisorSurface(Context context) {
        super(context);
        Log.d(TAG, "VisorSurface instantiated");

        mCameraCurrentZoomLevel = 0;
        mCameraMaxZoomLevel     = 0;

        mCameraFlashMode        = false;
        mCameraPreviewIsRunning = false;

        mCameraColorEffectIndex = 0;

        mState = STATE_CLOSED;

        Display mDisplay = ((Activity) context).getWindowManager().getDefaultDisplay();

        Point sizePoint = new Point();
        mDisplay.getSize(sizePoint);
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // getting a preciser value of the screen size to be more accurate.
            mDisplay.getRealSize(sizePoint);
        }

        width = sizePoint.x;
        height = sizePoint.y;

        setCameraColorFilter(NO_FILTER);

        //we have to set this if we're using our own onDraw method
        setWillNotDraw(false);

        mCamera = null;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        synchronized(mCameraStateLock) {
            mState = STATE_CLOSED;
            if (null != mCamera) {
                // mCamera.close();
                mCamera= null;
            }
        }
    }
    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread(CAMERA_PREVIEW_THREAD);
        mBackgroundThread.start();
        synchronized(mCameraStateLock) {
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        // mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            synchronized (mCameraStateLock) {
                mBackgroundHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public ColorFilter getCameraColorFilter() {
        return mCameraColorFilter;
    }

    public void setCameraColorFilter(ColorFilter mCameraColorFilter) {
        this.mCameraColorFilter = mCameraColorFilter;
    }

    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;

        final int numOfCameras = Camera.getNumberOfCameras();
        Log.d(TAG, "There're " + Integer.toString(numOfCameras) + " cameras on your device. You want camera " + Integer.toString(cameraId));

        if(!(cameraId < numOfCameras)) {
            Log.e(TAG, "The requested cameraId is too high.");
            return null;
        }

        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
            // stores the used camera id in the static var.
            mCameraId = cameraId;
        }
        catch (Exception e) {
            // Camera is not available (in use or does not exist)
            // try another one.
            c = getCameraInstance(++cameraId);
        }
        return c; // returns null if camera is unavailable
    }

    public static Camera getCameraInstance(){
        return getCameraInstance(0);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "called surfaceCreated");
        enableCamera();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "called surfaceDestroyed");
        releaseCamera();
    }

    /**
     * returns the maximum possible camera preview size which is the same or less than you've
     * specified with the maxWidth and maxHeight.
     * If you don't wanna set a maxWidth and maxHeight just add a huge integer in it like 9000
     * or more.
     *
     * @param maxWidth  the maximum width we wanna receive
     * @param maxHeight the maximum height we wanna receive
     * @param parameters the camera parameters to receive all supported preview sizes.
     * @return Camera.Size or null if the parameters could not be accessed or some other issues occured.
     */
    private Camera.Size getBestPreviewSize(int maxWidth, int maxHeight, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            // We do this to receive the maximum possible size value.

            if (size.width <= maxWidth && size.height <= maxHeight) {
                if (result == null) {
                    result = size;
                    continue;
                }
                else
                {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        if(result != null)
            Log.d(TAG, "got maximum preview size of " + Integer.toString(result.width) + "*" + Integer.toString(result.height));

        return result;
    }

    /**
     * open and enable the camera.
     * If the preview is already running we'll immediately return.
     * If a camera is already open we won't open it again and just use it instead.
     * If it wasn't possible to open the camera we will throw CameraCouldNotOpenedException.
     *
     */
    // public void enableCamera() throws NoCameraSizesFoundException, CameraCouldNotOpenedException {
    public void enableCamera() {
        if(mState == STATE_PREVIEW) return;
        // startBackgroundThread();

        // mBackgroundHandler.post(new Runnable() {
        //     @Override
        //     public void run() {
            if(mCamera == null) {
                mCamera = getCameraInstance();
                mState = STATE_OPENED;
            }
            // camera is still null so abort further actions
            // if(mCamera == null) throw new CameraCouldNotOpenedException();
            if(mCamera == null) return;

            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.isZoomSupported()) mCameraMaxZoomLevel = parameters.getMaxZoom();
            Camera.Size size = getBestPreviewSize(width, height, parameters);

            // no sizes found? something went wrong
            // if(size == null) throw new NoCameraSizesFoundException();
            if(size == null) return;

            mCameraPreviewWidth = size.width;
            mCameraPreviewHeight = size.height;

            mCameraPositionMoveX = 0;
            mCameraPositionMoveY = 0;

            // for the image calculations
            mCameraPreviewFormat = parameters.getPreviewFormat();

            if(mCameraPreviewWidth != width) {
                mCameraPositionMoveX = (width - mCameraPreviewWidth) / 2;
            }
            if(mCameraPreviewHeight != height) {
                mCameraPositionMoveY = (height - mCameraPreviewHeight) / 2;
            }

            parameters.setPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
            mCamera.setParameters(parameters);

            // init zoom level member attr.
            mCameraCurrentZoomLevel = mCameraMaxZoomLevel;

            // TODO: here it get's complicated...
            rgba = new int[mCameraPreviewWidth * mCameraPreviewHeight +1];
            int bufferSize = mCameraPreviewWidth * mCameraPreviewHeight * 3 / 2;
            mCameraPreviewBufferData = new byte[bufferSize];

            // The Surface has been created, now tell the
            // camera where to draw the preview.
            /**/
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            /**/

            mCamera.addCallbackBuffer(new byte[bufferSize]);
            mCamera.setPreviewCallback(mCameraPreviewCallbackHandler);

            mCamera.startPreview();
            mState = STATE_PREVIEW;

            // start with the first zoom level.
            nextZoomLevel();

            Log.d(TAG, "Thread done. Camera successfully started");
        // }
        // });
    }

    public void releaseCamera() {
        Log.d(TAG, "releasing the camera.");

        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;

                // stopBackgroundThread();
                mState = STATE_CLOSED;

                Log.d(TAG, "camera released. Threads closed.");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "called surfaceChanged");
    }

    /**
     * enables autofocus for the preview.
     * It will autofocus just a single time.
     */
    public void autoFocusCamera() {
        if(mState != STATE_PREVIEW) return;
        mCamera.cancelAutoFocus();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                              @Override
                              public void onAutoFocus(boolean success, Camera camera) {
                                  Log.d(TAG, "autofocus done with " + (success ? "" : "no ") + "success");
                              }
                          }
        );
    }

    /**
     * starts or stops the preview mode of the camera to hold still the current
     * picture. We don't need to store it at the moment.
     */
    public void toggleCameraPreview() {
        mState = mState == STATE_PREVIEW ? STATE_OPENED : STATE_PREVIEW;
        if(mState == STATE_PREVIEW) {
            mCamera.startPreview();
            return;
        }
        mCamera.stopPreview();
    }

    /**
     * toggles flashlight on and off.
     *
     * @param context we need the application context to determine if the users device has flash support or not.
     */
    public void nextFlashlightMode(Context context) {
        if(mState != STATE_PREVIEW) return;
        boolean hasFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if(hasFlash == false) {
            Log.e(TAG, "the current device does not have a flashlight!");
            return;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if(!supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
            Log.e(TAG, "the current device does not support flashlight mode TORCH.");
            return;
        }
        mCameraFlashMode = !mCameraFlashMode;

        if(mCameraFlashMode == true) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }

        mCamera.setParameters(parameters);
    }

    /**
     * triggers the next zoom level for the camera.
     * We use a simple math calculation to calculate each
     * single step until we reach the maximum zoom level.
     * the first step will always be the module of:
     * `mCameraMaxZoomLevel % mCameraZoomSteps` to avoid
     * a fifth baby step for some pixels.
     *
     * On my Nexus 5 the max level is 99. We have 4 steps, so
     * each step will be 24. The first step will be 3.
     * We'll never reach the 0 zoom level, but that's okay.
     *
     * If the preview isn't ready it, the values will
     * nevertheless stored in the member variables.
     */
    public void nextZoomLevel() {
        final int steps  = (mCameraMaxZoomLevel/(mCameraZoomSteps-1));
        final int modulo = (mCameraMaxZoomLevel%(mCameraZoomSteps-1));

        int nextLevel = mCameraCurrentZoomLevel+steps;

        if(mCameraCurrentZoomLevel == mCameraMaxZoomLevel) {
            nextLevel = modulo;
        }

        if(mState == STATE_PREVIEW)
            setCameraZoomLevel(nextLevel);
    }

    /**
     * change color modes if the camera preview if supported.
     */
    public void toggleColorMode() {
        if(mState != STATE_PREVIEW) return;

        Camera.Parameters parameters = mCamera.getParameters();
        List<String> supportedEffects = parameters.getSupportedColorEffects();
        // List<String> supportedEffects = parameters.getSupportedSceneModes(); // just for testing

        final int effectsNum = supportedEffects.size();
        Log.d(TAG, "Supported effects by your device: " + supportedEffects.toString());

        mCameraColorEffectIndex++;
        if(mCameraColorEffectIndex >= effectsNum) {
            mCameraColorEffectIndex = 0;
        }

        String newColorEffectName = supportedEffects.get(mCameraColorEffectIndex);
        Log.d(TAG, "the current color effect is "+newColorEffectName);

        parameters.setColorEffect(newColorEffectName);
        // parameters.setSceneMode(newColorEffectName);

        try {
            mCamera.setParameters(parameters);
        } catch(RuntimeException exception) {
            Log.d(TAG, "could not apply the color effect. Your device does not support it.");
        }
    }

    /**
     */
    public void createBitmap() {

        Log.d(TAG, "creating bitmap frame");

        YuvImage yuvImage = new YuvImage(mCameraPreviewBufferData, ImageFormat.NV21, mCameraPreviewWidth, mCameraPreviewHeight, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, mCameraPreviewWidth, mCameraPreviewHeight), JPEG_QUALITY, byteArrayOutputStream);
        mCameraPreviewBitmapBuffer = BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size());
    }



    @Override //from SurfaceView
    public void onDraw(Canvas canvas) {
        Log.d(TAG, "called onDraw");

        // canvas.drawBitmap(bitmap, 0, 0, paint);
        // canvas.setBitmap(mCameraPreviewBitmapBuffer); // this is unsupported and causes a Exception

        // canvas.drawBitmap(rgba, 0, mCameraPreviewWidth, mCameraPositionMoveX, mCameraPositionMoveY, mCameraPreviewWidth, mCameraPreviewHeight, false, null);

        if(mState != STATE_PREVIEW) return;
        if(mCameraPreviewBitmapBuffer == null) return;

        int width = mCameraPreviewBitmapBuffer.getWidth();
        int height = mCameraPreviewBitmapBuffer.getHeight();
        float h= (float) height;
        float w= (float) width;
        Matrix mat=new Matrix();
        mat.setTranslate( 500, 500 );
        mat.setScale(800 / w, 800 / h);
        canvas.drawBitmap(mCameraPreviewBitmapBuffer, mat, null);

        invalidate();

        /*
        if((ALG && rgba == null) || (!ALG && bitmap == null)) {
            Log.d(TAG, "onDraw abort because if nulls");
            return;
        }

        Log.d(TAG, "draw canvas");
        canvas.drawBitmap(rgba, 0, mCameraPreviewWidth, mCameraPositionMoveX, mCameraPositionMoveY, mCameraPreviewWidth, mCameraPreviewHeight, false, null);

        /*
        byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage = new YuvImage(cameraPreviewBuffer, ImageFormat.NV21, mCameraPreviewWidth, mCameraPreviewHeight, null);

        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, byteArrayOutputStream); //width and height of the screen
        jData = byteArrayOutputStream.toByteArray();

        bitmap = BitmapFactory.decodeByteArray(jData, 0, jData.length);

        canvas.drawBitmap(bitmap , 0, 0, paint);

        invalidate(); //to call ondraw again
        */

    }

    /**
     * sets the camera level to the specified {zoomLevel}.
     * It dependes on a valid {mCamera} object to receive
     * the parameters and set it as well.
     *
     * @param zoomLevel the integer of the new zoomLevel you want to set. All integers above the maximum possible value will be set to maximum.
     */
    private void setCameraZoomLevel(int zoomLevel) {
        Camera.Parameters parameters = mCamera.getParameters();

        if(!parameters.isZoomSupported()) {
            Log.w(TAG, "Zoom is not supported on this device.");
            return;
        }

        if(zoomLevel > mCameraMaxZoomLevel) {
            zoomLevel = mCameraMaxZoomLevel;
        }
        mCameraCurrentZoomLevel = zoomLevel;

        Log.d(TAG, "Current zoom level is "+Integer.toString(zoomLevel));

        parameters.setZoom(mCameraCurrentZoomLevel);
        mCamera.setParameters(parameters);
    }

    private class NoCameraSizesFoundException extends Throwable {

    }

    private class CameraCouldNotOpenedException extends Throwable {
    }
}
