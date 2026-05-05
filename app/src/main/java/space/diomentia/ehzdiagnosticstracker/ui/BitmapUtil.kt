package space.diomentia.ehzdiagnosticstracker.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

private fun exifDegrees(exif: ExifInterface): Float =
    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap =
    if (degrees == 0f) bitmap
    else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)

private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var n = 1
    while (maxOf(width, height) / n > maxDimension) n *= 2
    return n
}

/** Decode from a content URI, applying EXIF rotation. */
fun decodeScaledBitmap(context: Context, uri: Uri, maxDimension: Int = 1920): Bitmap? {
    val degrees = context.contentResolver.openInputStream(uri)?.use { exifDegrees(ExifInterface(it)) } ?: 0f
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    opts.inSampleSize = sampleSize(opts.outWidth, opts.outHeight, maxDimension)
    opts.inJustDecodeBounds = false
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
    return rotate(bitmap, degrees)
}

/** Decode from a file, applying EXIF rotation. */
fun decodeScaledBitmap(file: File, maxDimension: Int = 1920): Bitmap? {
    val degrees = exifDegrees(ExifInterface(file.absolutePath))
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, opts)
    opts.inSampleSize = sampleSize(opts.outWidth, opts.outHeight, maxDimension)
    opts.inJustDecodeBounds = false
    val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
    return rotate(bitmap, degrees)
}

/**
 * Decode from a raw byte array — no EXIF rotation, since stored photos are
 * already correctly oriented before being encoded.
 */
fun decodeScaledBitmap(bytes: ByteArray, maxDimension: Int): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    opts.inSampleSize = sampleSize(opts.outWidth, opts.outHeight, maxDimension)
    opts.inJustDecodeBounds = false
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}