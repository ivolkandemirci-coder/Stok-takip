package com.apparel.stok

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/** Ürün fotoğraflarını uygulamanın kendi klasöründe küçültüp saklar. */
object ImageStore {
    private const val MAX_SIZE = 1280

    private fun imagesDir(ctx: Context) = File(ctx.filesDir, "images").apply { mkdirs() }

    private fun cameraFile(ctx: Context) =
        File(File(ctx.cacheDir, "camera").apply { mkdirs() }, "capture.jpg")

    /** Kamera uygulamasının fotoğrafı yazacağı geçici adres. */
    fun cameraUri(ctx: Context): Uri =
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", cameraFile(ctx))

    /** Fotoğrafı okur, döndürmeyi düzeltir, küçültür ve kalıcı olarak kaydeder. */
    suspend fun import(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val cr = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE) sample *= 2
            val bmp = cr.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null

            val orientation = cr.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val scale = MAX_SIZE.toFloat() / max(bmp.width, bmp.height)
            val matrix = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                if (degrees != 0f) postRotate(degrees)
            }
            val out = if (matrix.isIdentity) bmp
            else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)

            val file = File(imagesDir(ctx), "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            cameraFile(ctx).delete()
            file.absolutePath
        }.getOrNull()
    }

    fun delete(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }
}
