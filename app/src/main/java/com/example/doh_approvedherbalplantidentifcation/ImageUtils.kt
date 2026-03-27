package com.example.doh_approvedherbalplantidentifcation

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        return stream.toByteArray()
    }
}