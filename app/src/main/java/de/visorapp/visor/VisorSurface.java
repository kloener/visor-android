package de.visorapp.visor;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by root on 29.07.15.
 */
public class VisorSurface extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "VisorSurface";
    private static final int mCameraZoomSteps = 4;

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int mCameraCurrentZoomLevel;
    private boolean mCameraFlashMode;
    private int mCameraMaxZoomLevel;
    private boolean mCameraPreviewIsRunning;

    public VisorSurface(Context context, Camera camera) {
        super(context);

        Log.d(TAG, "VisorSurface instantiated");

        mCameraCurrentZoomLevel = 0;
        mCameraMaxZoomLevel     = 0;
        mCameraFlashMode        = false;
        mCameraPreviewIsRunning = false;

        mCamera = camera;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "called surfaceCreated");
        if(mCameraPreviewIsRunning == true) return;

        Camera.Parameters parameters = mCamera.getParameters();
        if(parameters.isZoomSupported()) mCameraMaxZoomLevel = parameters.getMaxZoom();

        // init zoom level
        mCameraCurrentZoomLevel = mCameraMaxZoomLevel;
        nextZoomLevel();

        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);

            // mCamera.setOneShotPreviewCallback(this);
            // mCamera.setPreviewCallbackWithBuffer(this);

            mCamera.startPreview();
            mCamera.setPreviewCallback(this);

            mCameraPreviewIsRunning = true;
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            mCameraPreviewIsRunning = false;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "called surfaceDestroyed");
        synchronized (this) {
            try {
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.setPreviewCallback(null);
                    mCameraPreviewIsRunning = false;
                    mCamera.release();
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
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
        if(mCameraPreviewIsRunning == false) return;
        mCamera.cancelAutoFocus();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                              @Override
                              public void onAutoFocus(boolean success, Camera camera) {
                                  Log.d(TAG, "autofocussing");
                              }
                          }
        );
    }

    /**
     * starts or stops the preview mode of the camera to hold still the current
     * picture. We don't need to store it at the moment.
     */
    public void toggleCameraPreview() {
        mCameraPreviewIsRunning = !mCameraPreviewIsRunning;
        if(mCameraPreviewIsRunning) {
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
        if(mCameraPreviewIsRunning == false) return;

        boolean hasFlash = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if(hasFlash == false) {
            return;
        }

        Camera.Parameters parameters = mCamera.getParameters();
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
     * Returns immediately if the preview is not running.
     */
    public void nextZoomLevel() {
        if(mCameraPreviewIsRunning == false) return;

        final int steps  = (mCameraMaxZoomLevel/mCameraZoomSteps);
        final int modulo = (mCameraMaxZoomLevel%mCameraZoomSteps);

        int nextLevel = mCameraCurrentZoomLevel+steps;

        if(mCameraCurrentZoomLevel == mCameraMaxZoomLevel) {
            nextLevel = modulo;
        }
        setCameraZoomLevel(nextLevel);
    }

    /**
     * change color modes if the camera preview if supported.
     */
    public void toggleColorMode() {
        if(mCameraPreviewIsRunning == false) return;
        Camera.Parameters parameters = mCamera.getParameters();
        final String currentEffect = parameters.getColorEffect();

        if(currentEffect == null) {
            Log.d(TAG, "Warning! Could not receive current color effect. Cannot change the color effect.");
            return;
        }

        Log.d(TAG, parameters.getSupportedColorEffects().toString());

        switch(currentEffect) {
            case Camera.Parameters.EFFECT_MONO: parameters.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE); break;
            case Camera.Parameters.EFFECT_NEGATIVE: parameters.setColorEffect(Camera.Parameters.EFFECT_AQUA); break;
            case Camera.Parameters.EFFECT_AQUA: parameters.setColorEffect(Camera.Parameters.EFFECT_BLACKBOARD); break;
            case Camera.Parameters.EFFECT_BLACKBOARD: parameters.setColorEffect(Camera.Parameters.EFFECT_WHITEBOARD); break;
            case Camera.Parameters.EFFECT_WHITEBOARD: parameters.setColorEffect(Camera.Parameters.EFFECT_POSTERIZE); break;
            case Camera.Parameters.EFFECT_POSTERIZE: parameters.setColorEffect(Camera.Parameters.EFFECT_NONE); break;
            default: parameters.setColorEffect(Camera.Parameters.EFFECT_MONO);
        }
        try {
            mCamera.setParameters(parameters);
        } catch(RuntimeException exception) {
            Log.d(TAG, "could not apply the color effect. Your device does not support it.");
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(mCameraPreviewIsRunning == false) return;
        Log.d(TAG, "onPreviewFrame called");
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

}
