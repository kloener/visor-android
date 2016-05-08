package de.visorapp.visor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.visorapp.visor.filters.BlackWhiteColorFilter;
import de.visorapp.visor.filters.BlueYellowColorFilter;
import de.visorapp.visor.filters.ColorFilter;
import de.visorapp.visor.filters.NoColorFilter;
import de.visorapp.visor.filters.WhiteBlackColorFilter;
import de.visorapp.visor.filters.YellowBlueColorFilter;
import de.visorapp.visor.threads.BitmapCreateThread;

/**
 * Created by Christian Illies on 29.07.15.
 */
public class VisorSurface extends SurfaceView implements SurfaceHolder.Callback, BitmapRenderer {

    /**
     * The debug Tag identifier for the whole class.
     */
    private static final String TAG = "VisorSurface";

    /**
     * The maximum of steps until we will reach the maximum zoom level.
     */
    private static final int mCameraZoomSteps = 4;

    /**
     * The jpeg quality which will be rendered for each camera preview image.
     * If the value is too high to performance decreased drastically.
     */
    private static final int JPEG_QUALITY = 77;

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
     * Max width for the camera preview to avoid performance and ram/cache issues.
     */
    private static final int MAX_CAMERA_PREVIEW_RESOLUTION_WIDTH = 800;

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
     * the width of the view.
     */
    private int width;

    /**
     * the height of the view
     */
    private int height;

    /**
     * the maximum possible width of the camera preview that we'll use.
     */
    private int mCameraPreviewWidth;

    /**
     * the maximum possible height of the camera preview that we'll use.
     */
    private int mCameraPreviewHeight;

    /**
     * the paint object which has the colorFilter assigned. We will use it
     * to apply the different color modes to the rendered preview bitmap.
     */
    private Paint mColorFilterPaint;

    /**
     * the current state of the camera device.
     * i.e. open, closed or preview.
     */
    private int mState;


    /**
     * The current filter for the camera.
     * The filter is an interface which takes some bytes as the param and
     * converts the bits to make several different color effects.
     */
    private List<ColorFilter> mCameraColorFilterList;
    private int mCurrentColorFilterIndex;

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
    protected Camera.PreviewCallback mCameraPreviewCallbackHandler = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {

            Log.d(TAG, "mCameraPreviewCallbackHandler Camera.PreviewCallback called");

            mCameraPreviewBufferData = data;
            if(!hasActiveFilterEnabled()) {
                invalidate();
                Log.d(TAG, "mCameraPreviewCallbackHandler Camera.PreviewCallback no active Filter found. Invalidate view.");
                return;
            }

            runBitmapCreateThread(false);
            // we're on the ui thread here:
            // invalidate();
        }
    };
    /**
     * reference to the zoom button.
     *
     * We hide the zoom Button if zoom is not supported
     * by the device camera.
     */
    private View zoomButtonView;
    /**
     * reference to the flash button.
     *
     * We hide the flash button if flashlight isn't supported.
     */
    private View flashButtonView;

    /**
     * auto focus mode which was stored in the shared preferences.
     */
    private String storedAutoFocusMode;
    private boolean mPauseOnReady = false;

    /**
     * @param context activity
     */
    public VisorSurface(Context context) {
        super(context);

        Log.d(TAG, "VisorSurface instantiated");

        mCameraCurrentZoomLevel = 0;
        mCameraMaxZoomLevel = 0;
        mCurrentColorFilterIndex = 0;

        SharedPreferences sharedPreferences = context.getSharedPreferences(String.valueOf(R.string.visor_shared_preference_name), Context.MODE_PRIVATE);
        mCameraCurrentZoomLevel = sharedPreferences.getInt(String.valueOf(R.string.key_preference_zoom_level), mCameraCurrentZoomLevel);
        mCurrentColorFilterIndex = sharedPreferences.getInt(String.valueOf(R.string.key_preference_color_mode), mCurrentColorFilterIndex);
        storedAutoFocusMode = sharedPreferences.getString(String.valueOf(R.string.key_preference_autofocus_mode), Camera.Parameters.FOCUS_MODE_AUTO);

        mCameraFlashMode = false;
        mColorFilterPaint = new Paint();

        mState = STATE_CLOSED;

        Display mDisplay = ((Activity) context).getWindowManager().getDefaultDisplay();

        Point sizePoint = new Point();

        mDisplay.getSize(sizePoint);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // getting a preciser value of the screen size to be more accurate.
            mDisplay.getRealSize(sizePoint);
        }

        width = sizePoint.x;
        height = sizePoint.y;

        //we have to set this if we're using our own onDraw method
        setWillNotDraw(false);
        setDrawingCacheEnabled(true);

        mCamera = null;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    /**
     * open and return a camera instance.
     *
     * @param cameraId
     * @return
     */
    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;

        final int numOfCameras = Camera.getNumberOfCameras();
        Log.d(TAG, "There're " + Integer.toString(numOfCameras) + " cameras on your device. You want camera " + Integer.toString(cameraId));

        if (!(cameraId < numOfCameras)) {
            Log.e(TAG, "The requested cameraId is too high.");
            return null;
        }

        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
            // stores the used camera id in the static var.
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            // try another one.
            c = getCameraInstance(++cameraId);
        }
        return c; // returns null if camera is unavailable
    }

    /**
     * return camera with id 0 (default: back camera)
     *
     * @return
     */
    public static Camera getCameraInstance() {
        return getCameraInstance(0);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "called surfaceCreated");
        enableCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "called surfaceDestroyed. Storing settings");

        SharedPreferences sharedPreferences = this.getContext().getSharedPreferences(String.valueOf(R.string.visor_shared_preference_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(String.valueOf(R.string.key_preference_zoom_level), mCameraCurrentZoomLevel);
        editor.putInt(String.valueOf(R.string.key_preference_color_mode), mCurrentColorFilterIndex);
        editor.putString(String.valueOf(R.string.key_preference_autofocus_mode), mCamera.getParameters().getFocusMode());

        editor.commit();

        releaseCamera();
    }

    /**
     * returns the maximum possible camera preview size which is the same or less than you've
     * specified with the {MAX_CAMERA_PREVIEW_RESOLUTION_WIDTH} const.
     *
     * @param parameters the camera parameters to receive all supported preview sizes.
     * @return Camera.Size or null if the parameters could not be accessed or some other issues occured.
     */
    private Camera.Size getBestPreviewSize(Camera.Parameters parameters) {
        Camera.Size result = null;

        List<Camera.Size> size = parameters.getSupportedPreviewSizes();
        Collections.sort(size, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                if (lhs.width < rhs.width) return -1;
                if (lhs.width > rhs.width) return 1;
                return 0;
            }
        });

        if (size.size() <= 0) return null;

        for (int i = (size.size() - 1); i >= 0; i--) {
            Log.d(TAG, "Size: " + Integer.toString(size.get(i).width) + " * " + Integer.toString(size.get(i).height));

            final int currentWidth = size.get(i).width;
            if (currentWidth <= MAX_CAMERA_PREVIEW_RESOLUTION_WIDTH) {
                result = size.get(i);
                break;
            }
        }

        // just use the last one, if there are only a few supported sizes.
        if (result == null) return size.get(size.size() - 1);

        Log.d(TAG, "got maximum preview size of " + Integer.toString(result.width) + "*" + Integer.toString(result.height));
        return result;
    }

    /**
     * open and enable the camera.
     * If the preview is already running we'll immediately return.
     * If a camera is already open we won't open it again and just use it instead.
     * If it wasn't possible to open the camera we will throw CameraCouldNotOpenedException.
     */
    // public void enableCamera() throws NoCameraSizesFoundException, CameraCouldNotOpenedException {
    public void enableCamera() {
        if (mState == STATE_PREVIEW) return;
        // startBackgroundThread();

        // mBackgroundHandler.post(new Runnable() {
        //     @Override
        //     public void run() {
        if (mCamera == null) {
            mCamera = getCameraInstance();
            mState = STATE_OPENED;
        }
        // camera is still null so abort further actions
        // if(mCamera == null) throw new CameraCouldNotOpenedException();
        if (mCamera == null) return;

        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters.isZoomSupported()) {
            mCameraMaxZoomLevel = parameters.getMaxZoom();
        } else {
            getZoomButtonView().setVisibility(View.INVISIBLE);
        }
        Camera.Size size = getBestPreviewSize(parameters);

        if(!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            getFlashButtonView().setVisibility(View.INVISIBLE);
        }

        int cameraPreviewFormat = parameters.getPreviewFormat();
        if (cameraPreviewFormat != ImageFormat.NV21) parameters.setPreviewFormat(ImageFormat.NV21);

        // no sizes found? something went wrong
        // if(size == null) throw new NoCameraSizesFoundException();
        if (size == null) return;

        mCameraPreviewWidth = size.width;
        mCameraPreviewHeight = size.height;

        parameters.setPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);


        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parameters.setRecordingHint(true);

        mCamera.setDisplayOrientation(0);

        mCamera.setParameters(parameters);

        // pre-define some variables for image processing.
        mCameraPreviewBufferData = new byte[mCameraPreviewWidth * mCameraPreviewHeight * 3 / 2];

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
        mCamera.setPreviewCallback(mCameraPreviewCallbackHandler);
        mCamera.startPreview();
        mState = STATE_PREVIEW;

        if(!storedAutoFocusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
            toggleAutoFocusMode();
        }

        // start with the first zoom level.
        // init zoom level member attr.
        if(mCameraCurrentZoomLevel == 0) {
            mCameraCurrentZoomLevel = mCameraMaxZoomLevel;
            nextZoomLevel();
        } else {
            setCameraZoomLevel(mCameraCurrentZoomLevel);
        }

        if(mCurrentColorFilterIndex > 0) {
            mCurrentColorFilterIndex--;
            // decrease index because
            // the toggle causes the increment
            toggleColorMode();
        }

        if(mPauseOnReady) {
            toggleCameraPreview();
        }

        Log.d(TAG, "Thread done. Camera successfully started");
    }

    /**
     *
     */
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

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "called surfaceChanged");
        enableCamera();
    }

    /**
     * enables autofocus for the preview.
     * It will autofocus just a single time.
     */
    public void autoFocusCamera() {
        if (mState != STATE_PREVIEW) return;
        mCamera.cancelAutoFocus();
        final long startAutoFocusing = System.currentTimeMillis();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                              @Override
                              public void onAutoFocus(boolean success, Camera camera) {
                                  Log.d(TAG, "autofocus done with " + (success ? "" : "no ") + "success in " + Long.toString(System.currentTimeMillis() - startAutoFocusing) + "ms");
                              }
                          }
        );
    }

    /**
     * starts or stops the preview mode of the camera to hold still the current
     * picture. We don't need to store it at the moment.
     */
    public void toggleCameraPreview() {
        mState = (mState == STATE_PREVIEW ? STATE_OPENED : STATE_PREVIEW);

        if (mState == STATE_PREVIEW) {

            // FIX: 20160508 On some devices it occured, that the callback handler wasn't called anymore.
            mCamera.setPreviewCallback(mCameraPreviewCallbackHandler);
            mCamera.startPreview();

            // if(mCameraFlashMode == true) turnFlashlightOn();
            return;
        }

        // turnFlashlightOff();
        mCamera.stopPreview();

        // run create thread otherwise we could see an old image.
        runBitmapCreateThread(true);
    }

    /**
     * enables or disables the autofocus mode.
     * We use the FOCUS_MODE_CONTINUOUS_PICTURE to enable the autofocus.
     *
     * Your camera has to support this method.
     */
    public void toggleAutoFocusMode() {
        if(mState != STATE_PREVIEW) return;

        Camera.Parameters cameraParameters = mCamera.getParameters();

        List<String> focusModes = cameraParameters.getSupportedFocusModes();
        if (!focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) { return; }
        if (!focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) { return; }

        String currentMode = cameraParameters.getFocusMode();
        if(currentMode.equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
            Toast.makeText(VisorSurface.this.getContext(), R.string.text_autofocus_enabled, Toast.LENGTH_SHORT).show();
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else {
            Toast.makeText(VisorSurface.this.getContext(), R.string.text_autofocus_disabled, Toast.LENGTH_SHORT).show();
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        mCamera.setParameters(cameraParameters);

    }

    /**
     * toggles flashlight on and off.
     *
     * @param context we need the application context to determine if the users device has flash support or not.
     */
    public void nextFlashlightMode(Context context) {
        if (mState != STATE_PREVIEW) return;

        mCameraFlashMode = !mCameraFlashMode;
        if (mCameraFlashMode == true) {
            turnFlashlightOn();
        } else {
            turnFlashlightOff();
        }
    }

    private void turnFlashlightOff() {
        if (mState != STATE_PREVIEW || !supportsFlashlight()) return;
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(parameters);
    }

    private void turnFlashlightOn() {
        if (mState != STATE_PREVIEW || !supportsFlashlight()) return;
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);
    }


    /**
     * true of the current devices has a flash.
     * @return true if flash is supported
     */
    private boolean supportsFlashlight() {
        boolean hasFlash = getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (hasFlash == false) {
            // Log.e(TAG, "the current device does not have a flashlight!");
            return false;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        // 2015-08-20 Fix: Some devices/android versions return NULL instead of an list object.
        if (supportedFlashModes == null || !supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
            // Log.e(TAG, "the current device does not support flashlight mode TORCH.");
            return false;
        }

        return true;
    }

    /**
     * triggers the next zoom level for the camera.
     * We use a simple math calculation to calculate each
     * single step until we reach the maximum zoom level.
     * the first step will always be the module of:
     * `mCameraMaxZoomLevel % mCameraZoomSteps` to avoid
     * a fifth baby step for some pixels.
     * <p/>
     * On my Nexus 5 the max level is 99. We have 4 steps, so
     * each step will be 24. The first step will be 3.
     * We'll never reach the 0 zoom level, but that's okay.
     * <p/>
     * If the preview isn't ready it, the values will
     * nevertheless stored in the member variables.
     */
    public void nextZoomLevel() {
        final int steps = (mCameraMaxZoomLevel / (mCameraZoomSteps - 1));
        final int modulo = (mCameraMaxZoomLevel % (mCameraZoomSteps - 1));

        int nextLevel = mCameraCurrentZoomLevel + steps;

        if (mCameraCurrentZoomLevel == mCameraMaxZoomLevel) {
            nextLevel = modulo;
        }

        if (mState == STATE_PREVIEW)
            setCameraZoomLevel(nextLevel);
    }

    /**
     * add several different color filters as a list, which we
     * then toggle each time the corrosponding button gets pressed.
     *
     * @param colorFilters
     */
    public void setCameraColorFilters(List<ColorFilter> colorFilters) {
        this.mCameraColorFilterList = colorFilters;
    }

    /**
     * change color modes if the camera preview if supported.
     */
    public void toggleColorMode() {
        if (mState == STATE_CLOSED) return;
        if(mCameraColorFilterList == null) return;

        mCurrentColorFilterIndex++;
        if(mCurrentColorFilterIndex >= mCameraColorFilterList.size()) {
            mCurrentColorFilterIndex = 0;
        }

        ColorFilter currentFilter = mCameraColorFilterList.get(mCurrentColorFilterIndex);
        ColorMatrix colorMatrix = new ColorMatrix();
        currentFilter.filter(colorMatrix);

        ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
        mColorFilterPaint.setColorFilter(colorFilter);

        if(mState == STATE_OPENED) {
            invalidate();
        }
    }

    /**
     * Runs a bitmap create thread with the current `mCameraPreviewBufferData`.
     * If finished, the thread calls `renderBitmap` with the final bitmap as the result.
     */
    protected void runBitmapCreateThread(boolean rgb) {
        final BitmapCreateThread bitmapCreateThread = BitmapCreateThread.getInstance(
                mCameraPreviewBufferData,
                VisorSurface.this,
                mCameraPreviewWidth,
                mCameraPreviewHeight,
                width,
                height,
                JPEG_QUALITY,
                rgb
        );
        if (bitmapCreateThread == null) return;
        new Thread(bitmapCreateThread).start();
    }

    /**
     * sets the bitmap.
     *
     * @param bitmap
     */
    public void renderBitmap(Bitmap bitmap) {
        mCameraPreviewBitmapBuffer = bitmap;

        ((Activity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mState == STATE_CLOSED) {
            Log.d(TAG, "onDraw called but the camera state seems to be closed.");
            return;
        }
        if (mCameraPreviewBitmapBuffer == null || mCameraPreviewBitmapBuffer.isRecycled()) {
            Log.d(TAG, "onDraw called but the Bitmap is null or recycled. Do nothing here.");
            return;
        }

        if( ! ((mState == STATE_PREVIEW && hasActiveFilterEnabled()) || mState == STATE_OPENED)) {
            Log.d(TAG, "onDraw called but the camera state is preview but no filter is enabled or the state is not open.");
            return;
        }

        /**
         * Description:
         * If the state is opened the preview is probably paused
         */

        canvas.drawBitmap(mCameraPreviewBitmapBuffer, 0, 0, mColorFilterPaint);
    }

    /**
     * determines if a filter is active. A filter is active if it is not "NO_FILTER".
     * Used to save performance while have normal (without color effects) camera preview enabled.
     *
     * @return true if the current color mode is not NO_FILTER
     */
    private boolean hasActiveFilterEnabled() {
        return (mCameraColorFilterList.get(mCurrentColorFilterIndex) != NO_FILTER);
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

        if (!parameters.isZoomSupported()) {
            Log.w(TAG, "Zoom is not supported on this device.");
            return;
        }

        if (zoomLevel > mCameraMaxZoomLevel) {
            zoomLevel = mCameraMaxZoomLevel;
        }
        mCameraCurrentZoomLevel = zoomLevel;

        Log.d(TAG, "Current zoom level is " + Integer.toString(zoomLevel));

        parameters.setZoom(mCameraCurrentZoomLevel);
        mCamera.setParameters(parameters);
    }

    public View getZoomButtonView() {
        return zoomButtonView;
    }

    public View getFlashButtonView() {
        return flashButtonView;
    }

    public void setZoomButton(View zoomButton) {
        this.zoomButtonView = zoomButton;
    }

    public void setFlashButton(View flashButton) {
        this.flashButtonView = flashButton;
    }

    public Bitmap getBitmap() {
        buildDrawingCache();
        final Bitmap bitmap = Bitmap.createBitmap( getDrawingCache() );
        destroyDrawingCache();
        return bitmap;
        // return mCameraPreviewBitmapBuffer;
    }

    public void pausePreviewIfReady() {
        mPauseOnReady = true;
    }
}
