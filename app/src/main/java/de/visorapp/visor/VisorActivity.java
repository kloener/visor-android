package de.visorapp.visor;

import de.visorapp.visor.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class VisorActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 1000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = false;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
    private static final String TAG = "VisorActivity";
    private static final int MEDIA_TYPE_IMAGE = 1;

    private Camera mCamera;
    private VisorSurface mVisorView;

    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;

        final int numOfCameras = Camera.getNumberOfCameras();
        Log.d(TAG, "There're "+Integer.toString(numOfCameras)+" cameras on your device. You want camera "+Integer.toString(cameraId));

        if(!(cameraId < numOfCameras)) {
            Log.d(TAG, "The requested cameraId is too damn high.");
            return null;
        }

        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
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

    protected void abortAppWithMessage(CharSequence text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toasty = Toast.makeText(context, text, duration);
        toasty.show();

        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_visor);

        mCamera = getCameraInstance();
        if(mCamera == null) {
            Log.d(TAG, "Camera could not be opened! We have to abort.");
            abortAppWithMessage("Camera could not be opened! Please ensure to quit all other apps accessing the camera on your device.");
            return;
        }
        mVisorView = new VisorSurface(this, mCamera);

        FrameLayout previewLayout = (FrameLayout) findViewById(R.id.camera_preview);
        previewLayout.addView(mVisorView);

        // Add a listener to the Zoom button
        Button zoomButton = (Button) findViewById(R.id.button_zoom);
        zoomButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mVisorView.nextZoomLevel();
                    }
                }
        );

        // Add a listener to the Flash button
        Button flashButton = (Button) findViewById(R.id.button_flash);
        flashButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mVisorView.nextFlashlightMode(getApplicationContext());
                    }
                }
        );

        // Add a listener to the Flash button
        Button colorButton = (Button) findViewById(R.id.button_color);
        colorButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mVisorView.toggleColorMode();
                    }
                }
        );

        // Add a listener to the Preview button
        mVisorView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mVisorView.autoFocusCamera();
                    }
                }
        );
    }
}
