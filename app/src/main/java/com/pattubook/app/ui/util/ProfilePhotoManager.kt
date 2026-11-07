package com.pattubook.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min

object ProfilePhotoManager {

    private const val TAG = "PattubookPhoto"
    private const val PHOTO_DIR_NAME = "profile_photos"
    private const val MAX_IMAGE_DIMENSION = 512
    private const val JPEG_QUALITY = 85

    // LRU cache for decoded bitmaps to prevent repeated disk I/O and UI jank
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = maxMemoryKb / 8
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Returns the private directory used to store profile photos.
     */
    private fun getPhotoDirectory(context: Context): File {
        val dir = File(context.filesDir, PHOTO_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Returns the File for a relative photoPath, or null if photoPath is null/blank or doesn't exist.
     */
    fun getPhotoFile(context: Context, photoPath: String?): File? {
        if (photoPath.isNullOrBlank()) return null
        val file = File(context.filesDir, photoPath)
        return if (file.exists()) file else null
    }

    /**
     * Decodes and loads a Bitmap from internal storage off the main thread, downsampling to target size.
     */
    suspend fun loadBitmap(
        context: Context,
        photoPath: String?,
        targetSizePx: Int = 256
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (photoPath.isNullOrBlank()) return@withContext null

        memoryCache.get(photoPath)?.let { return@withContext it }

        val file = File(context.filesDir, photoPath)
        if (!file.exists()) return@withContext null

        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            options.inSampleSize = calculateInSampleSizeFromDimensions(
                options.outWidth,
                options.outHeight,
                targetSizePx,
                targetSizePx
            )
            options.inJustDecodeBounds = false

            val decodedBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            if (decodedBitmap != null) {
                memoryCache.put(photoPath, decodedBitmap)
            }
            decodedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cached bitmap for path: $photoPath", e)
            null
        }
    }

    /**
     * Decodes an image Uri (content:// or file://) off the main thread into a high-resolution software Bitmap,
     * downsampling large images (up to maxDimension = 2048) and applying EXIF orientation fixes.
     */
    suspend fun decodeFullBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        var decodedBitmap: Bitmap? = null

        // Method 1: Use ImageDecoder (available on API 28+, minSdk is 29)
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            decodedBitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // Force software allocator so bitmap can be cropped and compressed to JPEG without crashing
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE

                val width = info.size.width
                val height = info.size.height
                val sampleSize = calculateInSampleSizeFromDimensions(width, height, maxDimension, maxDimension)
                if (sampleSize > 1) {
                    decoder.setTargetSampleSize(sampleSize)
                }
            }
        } catch (e: Exception) {
            Log.w(
                TAG,
                "ImageDecoder failed for Uri (scheme=${uri.scheme}, auth=${uri.authority}), trying ParcelFileDescriptor fallback",
                e
            )
            decodedBitmap = null
        }

        // Method 2: Fallback via ParcelFileDescriptor + BitmapFactory if ImageDecoder failed or was unavailable
        if (decodedBitmap == null) {
            try {
                decodedBitmap = decodeBitmapViaFileDescriptor(context, uri, maxDimension)
            } catch (e: Exception) {
                Log.e(TAG, "BitmapFactory fallback failed for Uri (scheme=${uri.scheme}, auth=${uri.authority})", e)
            }
        }

        if (decodedBitmap == null) {
            Log.e(TAG, "Unable to decode image from Uri: scheme=${uri.scheme}, auth=${uri.authority}")
            return@withContext null
        }

        try {
            // Apply EXIF rotation if needed
            val rotationDegrees = getExifOrientationDegrees(context, uri)
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    decodedBitmap, 0, 0,
                    decodedBitmap.width, decodedBitmap.height,
                    matrix, true
                )
                if (rotated != decodedBitmap) {
                    decodedBitmap.recycle()
                    decodedBitmap = rotated
                }
            }
            decodedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error applying EXIF rotation for Uri: scheme=${uri.scheme}, auth=${uri.authority}", e)
            null
        }
    }

    /**
     * Crops a 1:1 square region from [sourceBitmap] based on interactive user viewport parameters
     * ([panX], [panY], [userScale], [viewportSizePx]) and rescales to [targetSizePx] (default 512x512).
     */
    suspend fun cropSquareBitmap(
        sourceBitmap: Bitmap,
        panX: Float,
        panY: Float,
        userScale: Float,
        viewportSizePx: Float,
        targetSizePx: Int = 512
    ): Bitmap = withContext(Dispatchers.IO) {
        val imgWidth = sourceBitmap.width.toFloat()
        val imgHeight = sourceBitmap.height.toFloat()

        val baseScale = viewportSizePx / min(imgWidth, imgHeight)
        val totalScale = baseScale * kotlin.math.max(1.0f, userScale)

        val dispWidth = imgWidth * totalScale
        val dispHeight = imgHeight * totalScale

        val dispCropCenterX = (dispWidth / 2f) - panX
        val dispCropCenterY = (dispHeight / 2f) - panY

        val dispCropLeft = dispCropCenterX - (viewportSizePx / 2f)
        val dispCropTop = dispCropCenterY - (viewportSizePx / 2f)

        val bitmapCropLeft = (dispCropLeft / totalScale).toInt().coerceIn(0, (imgWidth - 1).toInt())
        val bitmapCropTop = (dispCropTop / totalScale).toInt().coerceIn(0, (imgHeight - 1).toInt())

        val bitmapCropSize = (viewportSizePx / totalScale).toInt()
            .coerceIn(1, min((imgWidth - bitmapCropLeft).toInt(), (imgHeight - bitmapCropTop).toInt()))

        val cropped = Bitmap.createBitmap(
            sourceBitmap,
            bitmapCropLeft,
            bitmapCropTop,
            bitmapCropSize,
            bitmapCropSize
        )

        if (cropped.width != targetSizePx || cropped.height != targetSizePx) {
            val scaled = Bitmap.createScaledBitmap(cropped, targetSizePx, targetSizePx, true)
            if (scaled != cropped && !cropped.isRecycled) {
                cropped.recycle()
            }
            scaled
        } else {
            cropped
        }
    }

    /**
     * Safely decodes an image Uri (content:// or file://) off the main thread,
     * downsamples large images, applies EXIF orientation, and crops into a square Bitmap.
     */
    suspend fun decodeAndCropUriToSquareBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_IMAGE_DIMENSION
    ): Bitmap? = withContext(Dispatchers.IO) {
        val fullBitmap = decodeFullBitmapFromUri(context, uri, maxDimension) ?: return@withContext null
        val squareBitmap = cropToCenterSquare(fullBitmap)
        if (squareBitmap != fullBitmap && !fullBitmap.isRecycled) {
            fullBitmap.recycle()
        }
        if (squareBitmap.width > maxDimension || squareBitmap.height > maxDimension) {
            val scaled = Bitmap.createScaledBitmap(squareBitmap, maxDimension, maxDimension, true)
            if (scaled != squareBitmap) {
                squareBitmap.recycle()
                return@withContext scaled
            }
        }
        squareBitmap
    }

    private fun decodeBitmapViaFileDescriptor(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap? {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        return pfd.use { descriptor ->
            val fileDescriptor = descriptor.fileDescriptor

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, boundsOptions)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSizeFromDimensions(
                    boundsOptions.outWidth,
                    boundsOptions.outHeight,
                    maxDimension,
                    maxDimension
                )
                inJustDecodeBounds = false
            }

            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, decodeOptions)
        }
    }

    /**
     * Saves the provided square Bitmap to private internal storage and returns the relative photo path.
     */
    suspend fun saveSquareBitmap(
        context: Context,
        squareBitmap: Bitmap
    ): String? = withContext(Dispatchers.IO) {
        try {
            val photoDir = getPhotoDirectory(context)
            val fileName = "photo_${UUID.randomUUID()}.jpg"
            val targetFile = File(photoDir, fileName)

            FileOutputStream(targetFile).use { out ->
                squareBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }

            val relativePath = "$PHOTO_DIR_NAME/$fileName"
            memoryCache.put(relativePath, squareBitmap)
            relativePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save profile photo bitmap to internal storage", e)
            null
        }
    }

    /**
     * Safely deletes a photo file from internal storage and removes it from the memory cache.
     */
    suspend fun deletePhotoFile(
        context: Context,
        photoPath: String?
    ): Boolean = withContext(Dispatchers.IO) {
        if (photoPath.isNullOrBlank()) return@withContext false
        memoryCache.remove(photoPath)

        val file = File(context.filesDir, photoPath)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Deletes all profile photo files in internal storage and clears the memory cache.
     */
    suspend fun clearAllPhotos(context: Context) = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        val dir = File(context.filesDir, PHOTO_DIR_NAME)
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                file.delete()
            }
        }
    }

    private fun cropToCenterSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val squareSize = min(width, height)

        val x = (width - squareSize) / 2
        val y = (height - squareSize) / 2

        return Bitmap.createBitmap(bitmap, x, y, squareSize, squareSize)
    }

    private fun calculateInSampleSizeFromDimensions(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getExifOrientationDegrees(context: Context, uri: Uri): Int {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
            pfd.use { descriptor ->
                val exif = ExifInterface(descriptor.fileDescriptor)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {
            0
        }
    }
}
