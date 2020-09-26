package de.visorapp.visor

import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import kotlin.math.max


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            initPreviewResolutionWidth()
            initCameraChooser()
        }

        private fun initCameraChooser() {
            val visorSurface = VisorSurface.getInstance()
            val numberOfCameras = Camera.getNumberOfCameras()
            var entryValues: MutableList<CharSequence> = ArrayList()
            var entries: MutableList<CharSequence> = ArrayList()
            val manager: CameraManager
            var haveMain = false
            var haveSelfie = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                manager = context?.getSystemService(CAMERA_SERVICE) as CameraManager
                manager.cameraIdList.forEach {
                    val cameraCharacteristics = manager.getCameraCharacteristics(it)
                    entryValues.add(it)
                    if (!haveMain && cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        entries.add(resources.getString(R.string.main_camera))
                        haveMain = true
                    } else if (!haveSelfie && cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        entries.add(resources.getString(R.string.selfie_camera))
                        haveSelfie = true
                    } else
                        entries.add(resources.getString(R.string.wide_or_other_camera))
                }
            } else {
                entryValues = MutableList(numberOfCameras) { i -> i.toString() }
                entries = entryValues
            }
            val cameraIdPreference = findPreference<DropDownPreference>(resources.getString(R.string.key_preference_camera_id))
            if (cameraIdPreference != null) {
                cameraIdPreference.entries = entries.toTypedArray()
                cameraIdPreference.entryValues = entryValues.toTypedArray()
                cameraIdPreference.setValueIndex(visorSurface.preferredCameraId)
            }
        }

        private fun initPreviewResolutionWidth() {
            val visorSurface = VisorSurface.getInstance()
            val availablePreviewWidths: Array<CharSequence>? = visorSurface.availablePreviewWidths
            val previewResolutionPreference = findPreference<DropDownPreference>(resources.getString(R.string.key_preference_preview_resolution))
            if (previewResolutionPreference != null && availablePreviewWidths != null) {
                previewResolutionPreference.entries = availablePreviewWidths
                previewResolutionPreference.entryValues = availablePreviewWidths
                val currentPreviewWidth = visorSurface.cameraPreviewWidth
                val currentIndex = max(0, availablePreviewWidths.indexOf(currentPreviewWidth.toString()))
                previewResolutionPreference.setValueIndex(currentIndex)
            }
        }

    }
}