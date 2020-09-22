package de.visorapp.visor;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.text.SimpleDateFormat;
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

    // Defining Permission codes.
    // We can give any value
    // but unique for each permission.
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

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
    private boolean cameraPreviewState;

    /**
     * stores the brightness level of the screen to restore it after the
     * app gets paused or destroyed.
     */
    private float prevScreenBrightnewss;
    public PhotoView mPhotoView;

    private int zoomButtonVisibility = View.VISIBLE;

    public void playClickSound(View view) {
        // TODO the user can disable this; if click-sounds are enable I hear a double click effect...
        // if(view == null) return;
        // view.playSoundEffect(android.view.SoundEffectConstants.CLICK);
    }

    private View.OnClickListener autoFocusClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mVisorView.autoFocusCamera();
        }
    };

    private View.OnClickListener colorModeClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.startAnimation(animScale);
            playClickSound(v);

            mVisorView.toggleColorMode();
        }
    };

    private View.OnLongClickListener colorModeLongClickHandler = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            v.startAnimation(animScale);
            playClickSound(v);

            mVisorView.setColorMode(0);
            return true;
        }
    };

    private View.OnClickListener pauseClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.startAnimation(animScale);
            playClickSound(v);

            mVisorView.toggleCameraPreview();
            ImageButton btn = (ImageButton) v;
            // FrameLayout previewLayout = getCameraPreviewFrame();

            if (cameraPreviewState) {
                cameraPreviewIsPaused(btn);
            } else {
                cameraPreviewIsActive(btn);
            }

            // btn.invalidateDrawable(null);
            cameraPreviewState = !cameraPreviewState;
        }
    };

    private void cameraPreviewIsPaused(ImageButton playOrPauseButton) {
        playOrPauseButton.setImageResource(R.drawable.ic_play);
        mZoomButton.setImageResource(R.drawable.ic_camera);
        zoomButtonVisibility = mZoomButton.getVisibility();
        mZoomButton.setVisibility(View.VISIBLE);
        mFlashButton.setAlpha(64);
        mFlashButton.getBackground().setAlpha(64);

        /** enable pinch to zoom via PhotoView from https://github.com/chrisbanes/PhotoView */
        mPhotoView.setImageBitmap(mVisorView.getBitmap());

        mVisorView.setAlpha(0);
        // mVisorView.setVisibility(View.GONE); // the change of visiblity would cause a surfaceDestroy!

        mPhotoView.setVisibility(View.VISIBLE);
        mPhotoView.setAlpha(255);
    }

    private void cameraPreviewIsActive(ImageButton playOrPauseButton) {
        playOrPauseButton.setImageResource(R.drawable.ic_pause);
        mZoomButton.setVisibility(zoomButtonVisibility);
        mZoomButton.setImageResource(R.drawable.ic_zoom);
        mFlashButton.setAlpha(255);
        mFlashButton.getBackground().setAlpha(255);

        // previewLayout.removeView(mPhotoView);
        mVisorView.setAlpha(1.0f);
        // mVisorView.setVisibility(View.VISIBLE); // the change of visiblity would cause a surfaceDestroy!

        if (mPhotoView != null) {
            mPhotoView.setVisibility(View.GONE);
            mPhotoView.setAlpha(0);
        }
    }

    private View.OnClickListener flashLightClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.startAnimation(animScale);
            playClickSound(v);

            mVisorView.nextFlashlightMode(getApplicationContext());
        }
    };
    private View.OnClickListener zoomClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.startAnimation(animScale);
            playClickSound(v);

            if (cameraPreviewState) {
                mVisorView.nextZoomLevel();
                return;
            }

            takeScreenshot();
        }
    };
    private View.OnLongClickListener zoomOutClickHandler = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            v.startAnimation(animScaleLongPress);

            if (cameraPreviewState) {
                mVisorView.prevZoomLevel();
                return true;
            }
            return true;
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
    private Animation animScale;
    private Animation animScaleLongPress;

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

    /**
     * When you use the SYSTEM_UI_FLAG_IMMERSIVE_STICKY flag, an inward swipe in the system bars
     * areas causes the bars to temporarily appear in a semi-transparent state, but no flags are
     * cleared, and your system UI visibility change listeners are not triggered. The bars
     * automatically hide again after a short delay, or if the user interacts with the
     * middle of the screen.
     *
     * Below is a simple approach to using this flag. Any time the window receives focus, simply set the IMMERSIVE_STICKY flag,
     * along with the other flags discussed in Use IMMERSIVE.
     *
     * @note https://developer.android.com/training/system-ui/immersive.html
     *
     * @param hasFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();

            // Api level 1
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

            // Jelly Bean to Kitkat-1
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                uiOptions = uiOptions
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
            }

            // Kitkat to Oreo
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                uiOptions = uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY; // 19
            }

            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    // This function is called when the user accepts or decline the permission.
    // Request Code is used to check which permission called this function.
    // This request code is provided when the user is prompt for permission.

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super
                .onRequestPermissionsResult(requestCode,
                        permissions,
                        grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                restartActvitiy();
            } else {
                Toast.makeText(this,
                        "Camera Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takeScreenshot();
            } else {
                Toast.makeText(this,
                        "Storage Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void restartActvitiy() {
        Intent intent = new Intent(getApplicationContext(), this.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_visor);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            return;
        }

        animScale = AnimationUtils.loadAnimation(this, R.anim.scale);
        animScaleLongPress = AnimationUtils.loadAnimation(this, R.anim.longpress);

        mVisorView = new VisorSurface(this);
        mPhotoView = new PhotoView(this);

        List<ColorFilter> filterList = new ArrayList<ColorFilter>();
        filterList.add(VisorSurface.NO_FILTER);
        filterList.add(VisorSurface.BLACK_WHITE_COLOR_FILTER);
        filterList.add(VisorSurface.WHITE_BLACK_COLOR_FILTER);
        filterList.add(VisorSurface.BLUE_YELLOW_COLOR_FILTER);
        filterList.add(VisorSurface.YELLOW_BLUE_COLOR_FILTER);

        mVisorView.setCameraColorFilters(filterList);
        FrameLayout previewLayout = getCameraPreviewFrame();
        previewLayout.setBackgroundColor(Color.BLACK);
        previewLayout.addView(mVisorView);
        previewLayout.addView(mPhotoView);

        mPhotoView.setAlpha(0);

        setButtonListeners();

        // Add a listener to the Preview button
        mVisorView.setOnClickListener(autoFocusClickHandler);/**/
        mVisorView.setOnLongClickListener(tapAndHoldListener);

        // set proper display orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    }

    private FrameLayout getCameraPreviewFrame() {
        return (FrameLayout) findViewById(R.id.camera_preview);
    }

    /**
     *
     */
    private void setButtonListeners() {
        // Add a listener to the Zoom button
        ImageButton zoomButton = (ImageButton) findViewById(R.id.button_zoom);
        zoomButton.setOnClickListener(zoomClickHandler);
        zoomButton.setOnLongClickListener(zoomOutClickHandler);

        // Add a listener to the Flash button
        ImageButton flashButton = (ImageButton) findViewById(R.id.button_flash);
        flashButton.setOnClickListener(flashLightClickHandler);

        // Add a listener to the Flash button
        ImageButton colorButton = (ImageButton) findViewById(R.id.button_color);
        colorButton.setOnClickListener(colorModeClickHandler);
        colorButton.setOnLongClickListener(colorModeLongClickHandler);

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

        if (cameraPreviewState != true && mPhotoView != null) {
            cameraPreviewState = true;
            cameraPreviewIsActive(mPauseButton);
        }

        Log.d(TAG, "onResume called!");
    }

    /**
     * @source https://stackoverflow.com/questions/2661536/how-to-programmatically-take-a-screenshot-in-android#5651242
     */
    private void takeScreenshot() {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            return;
        }
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", getResources().getConfiguration().locale).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + ".jpg";
            Bitmap bitmap = mVisorView.getBitmap();
            mVisorView.playActionSoundShutter();
            mVisorView.mState = VisorSurface.STATE_CLOSED;
            cameraPreviewIsActive(mZoomButton);
            Uri uri = Util.saveImageOnAllAPIs(bitmap, this, "", imageFileName, VisorSurface.JPEG_QUALITY);
            if (uri != null)
                openScreenshot(uri);
        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }
    }

    /**
     * @source https://stackoverflow.com/questions/2661536/how-to-programmatically-take-a-screenshot-in-android#5651242
     */
    private void openScreenshot(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
}
