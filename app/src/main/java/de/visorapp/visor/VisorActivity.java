package de.visorapp.visor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.visorapp.visor.filters.ColorFilter;

/**
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
     * Tag name for the Log message.
     */
    private static final String TAG = "VisorActivity";

    /**
     * our surface view containing the camera preview image.
     */
    private VisorSurface mVisorView;

    /**
     * Is the preview running? > Pause Btn + Zoom Btn
     * If not > Play Btn + Photo Share Btn
     */
    private boolean cameraPreviewState = true;

    /**
     * stores the brightness level of the screen to restore it after the
     * app gets paused or destroyed.
     */
    private float prevScreenBrightnewss;

    private View.OnClickListener autoFocusClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mVisorView.autoFocusCamera();
        }
    };
    private View.OnClickListener colorModeClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mVisorView.toggleColorMode();
        }
    };
    private View.OnClickListener pauseClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mVisorView.toggleCameraPreview();
            ImageButton btn = (ImageButton) v;
            if(cameraPreviewState) {
                btn.setImageResource(R.drawable.ic_play_arrow_black_48dp);
                mZoomButton.setImageResource(R.drawable.ic_photo_camera_black_48dp);
                mFlashButton.setAlpha(64);
                mFlashButton.getBackground().setAlpha(64);
            } else {
                btn.setImageResource(R.drawable.ic_pause_black_48dp);
                mZoomButton.setImageResource(R.drawable.ic_add_black_48dp);
                mFlashButton.setAlpha(255);
                mFlashButton.getBackground().setAlpha(255);
            }

            cameraPreviewState = !cameraPreviewState;
        }
    };
    private View.OnClickListener flashLightClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mVisorView.nextFlashlightMode(getApplicationContext());
        }
    };
    private View.OnClickListener zoomClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(cameraPreviewState){
                mVisorView.nextZoomLevel();
                return;
            }

            takeScreenshot();
        }
    };
    private View.OnLongClickListener tapAndHoldListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            mVisorView.toggleAutoFocusMode();
            return true;
        }
    };

    /**
     * Store the reference to swap the icon on it if we pause the preview.
     */
    private ImageButton mZoomButton;
    private ImageButton mPauseButton;
    private ImageButton mFlashButton;

    /**
     * sends a {@link Toast} message to the user and quits the app immediately.
     *
     * @param text
     */
    protected void abortAppWithMessage(CharSequence text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toasty = Toast.makeText(context, text, duration);
        toasty.show();

        finish();
    }

    /**
     * sets the brightness value of the screen to 1F
     */
    protected void setBrightnessToMaximum() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        prevScreenBrightnewss = layout.screenBrightness;
        layout.screenBrightness = 1F;
        getWindow().setAttributes(layout);
    }

    /**
     * resets the brightness value to the previous screen value.
     */
    protected void resetBrightnessToPreviousValue() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = prevScreenBrightnewss;
        getWindow().setAttributes(layout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_visor);

        // mVisorView = new VisorSurface(this);
        mVisorView = new VisorSurface(this);

        List<ColorFilter> filterList = new ArrayList<ColorFilter>();
        filterList.add(VisorSurface.NO_FILTER);
        filterList.add(VisorSurface.BLACK_WHITE_COLOR_FILTER);
        filterList.add(VisorSurface.WHITE_BLACK_COLOR_FILTER);
        filterList.add(VisorSurface.BLUE_YELLOW_COLOR_FILTER);
        filterList.add(VisorSurface.YELLOW_BLUE_COLOR_FILTER);

        mVisorView.setCameraColorFilters(filterList);
        FrameLayout previewLayout = (FrameLayout) findViewById(R.id.camera_preview);
        previewLayout.addView(mVisorView);

        setButtonListeners();

        // Add a listener to the Preview button
        mVisorView.setOnClickListener(autoFocusClickHandler);/**/
        mVisorView.setOnLongClickListener(tapAndHoldListener);

    }

    /**
     *
     */
    private void setButtonListeners() {
        // Add a listener to the Zoom button
        ImageButton zoomButton = (ImageButton) findViewById(R.id.button_zoom);
        zoomButton.setOnClickListener(zoomClickHandler);

        // Add a listener to the Flash button
        ImageButton flashButton = (ImageButton) findViewById(R.id.button_flash);
        flashButton.setOnClickListener(flashLightClickHandler);

        // Add a listener to the Flash button
        ImageButton colorButton = (ImageButton) findViewById(R.id.button_color);
        colorButton.setOnClickListener(colorModeClickHandler);

        ImageButton pauseButton = (ImageButton) findViewById(R.id.button_pause);
        pauseButton.setOnClickListener(pauseClickHandler);

        mVisorView.setZoomButton(zoomButton);
        mVisorView.setFlashButton(flashButton);

        mZoomButton = zoomButton;
        mPauseButton = pauseButton;
        mFlashButton = flashButton;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 2015-10-19 ChangeRequest: Some users have problems with the high brightness value.
        //                           So the user now has to activly adjust the brightness.
        // resetBrightnessToPreviousValue();
        Log.d(TAG, "onPause called!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called!");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!cameraPreviewState) {
            cameraPreviewState = true;
            mZoomButton.setImageResource(R.drawable.ic_add_black_48dp);
            mPauseButton.setImageResource(R.drawable.ic_pause_black_48dp);
            mFlashButton.setAlpha(255);
            mFlashButton.getBackground().setAlpha(255);
        }

        // 2015-10-19 ChangeRequest: Some users have problems with the high brightness value.
        //                           So the user now has to activly adjust the brightness.
        // setBrightnessToMaximum();
        Log.d(TAG, "onResume called!");
    }

    /**
     * @source https://stackoverflow.com/questions/2661536/how-to-programmatically-take-a-screenshot-in-android#5651242
     */
    private void takeScreenshot() {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPath = Environment.getExternalStorageDirectory().toString() + "/visor-android.app_" + now + ".jpg";

            // create bitmap screen capture
            /*
            View v1 = mVisorView;
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);
            */

            Bitmap bitmap = mVisorView.getBitmap();

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            final int quality = 90;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            int duration = Toast.LENGTH_SHORT;
            Toast toasty = Toast.makeText(this, R.string.text_image_stored+mPath, duration);
            toasty.show();

            openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }
    }

    /**
     * @source https://stackoverflow.com/questions/2661536/how-to-programmatically-take-a-screenshot-in-android#5651242
     */
    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }
}
