package de.visorapp.visor

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.OutputStream


class Util {

    companion object {
        // https://stackoverflow.com/a/57265702/6585346 and
        // https://stackoverflow.com/a/38858040/6585346
        @JvmStatic
        // @param folderName can be your app's {name}
        fun saveImageOnAllAPIs(bitmap: Bitmap, context: Context, folderName: String, fileName: String, quality: Int) : Uri? {
            var uri: Uri? = null
            val values = ContentValues()
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            if (Build.VERSION.SDK_INT >= 29) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName)
                values.put(MediaStore.Images.Media.IS_PENDING, true)
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                // RELATIVE_PATH and IS_PENDING are introduced in API 29.

                uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri), quality)
                    values.put(MediaStore.Images.Media.IS_PENDING, false)
                    context.contentResolver.update(uri, values, null, null)
                }
            } else {
                val directory = File(Environment.getExternalStorageDirectory().toString() + separator + folderName)
                // getExternalStorageDirectory is deprecated in API 29

                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, fileName)
                saveImageToStream(bitmap, FileOutputStream(file), quality)
                values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                // .DATA is deprecated in API 29
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (Build.VERSION.SDK_INT < 24) {
                    uri = Uri.fromFile(file);
                } else {
                    uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file)
                }
            }
            return uri
        }

        private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?, quality: Int) {
            if (outputStream != null) {
                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    outputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }
}