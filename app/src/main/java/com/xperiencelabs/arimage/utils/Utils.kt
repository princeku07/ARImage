package com.xperiencelabs.arimage.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

fun makeToast(message:String,context:Context){
    Toast.makeText(context,message,Toast.LENGTH_SHORT).show()
}

fun uriToBitmap(uri: Uri, context: Context,compress:Boolean): Bitmap? {
    try {
        val parcelFileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor
        if (fileDescriptor != null) {
            val options = Options()
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Use RGB_565 for reduced memory usage
            if (compress){
                options.inSampleSize = 3 // Reduce image dimensions to further save memory
            }

            val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)

            parcelFileDescriptor.close()

            return bitmap
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}

 fun compressBitmap(bitmap: Bitmap):Bitmap {
     val options = Options()
     options.inSampleSize = 3
     options.inPreferredConfig = Bitmap.Config.RGB_565
     val outputStream = ByteArrayOutputStream()
     bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream)
     val byteArray = outputStream.toByteArray()
     return BitmapFactory.decodeByteArray(byteArray,0,byteArray.size,options)
 }

 fun deleteImageFromInternalStorage(fileName: String,context: Context): Boolean {
     val directory = File(context.filesDir,"Images")
    val file = File(directory,fileName)

    // Check if the file exists
    if (file.exists()) {
        // Attempt to delete the file
        return file.delete()
    }
    // If the file doesn't exist, return false to indicate that it wasn't deleted
    return false
}


@SuppressLint("Range")
fun getFileName(uri: Uri,context: Context): String {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val cursor = context.contentResolver.query(uri,null,null,null,null)
        return if(cursor != null && cursor.moveToFirst()){
            val displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            cursor.close()
            val name = extractName(displayName)
            name
        } else {
            "Unknown"
        }
    }catch (e:SecurityException){
        e.printStackTrace()
        return "Unknown"
    }
}

fun extractName(fileName:String):String {
    val dotIndex = fileName.lastIndexOf(".")
    if(dotIndex != -1){
        return fileName.substring(0,dotIndex)
    }
    return fileName
}

fun getRecentImageUri(context: Context): Uri? {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_TAKEN
    )

    val contentResolver: ContentResolver = context.contentResolver
    val cursor = contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    )

    cursor?.use {
        if (it.moveToFirst()) {
            val imageIdColumnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val imageId = it.getLong(imageIdColumnIndex)
            return ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageId
            )
        }
    }

    return null
}


 fun hideSystemUI(activity: Activity){
    val window = activity.window
    val windowInsetsController = WindowCompat.getInsetsController(window,window.decorView)
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
        window.insetsController?.hide(WindowInsets.Type.navigationBars())
    }else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}

 fun showSystemUI(activity: Activity){
    val window = activity.window
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.show(WindowInsets.Type.navigationBars())
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}
