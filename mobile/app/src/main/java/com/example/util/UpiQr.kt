package com.example.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UpiQr {

    /**
     * Builds a standard UPI payment deep-link URI string.
     */
    fun buildUpiUri(
        payeeVpa: String,
        payeeName: String,
        amount: Int,
        bookingCode: String
    ): String {
        val cleanVpa = payeeVpa.trim()
        val cleanName = payeeName.trim()
        val amountStr = "$amount.00"
        val note = "Servora booking $bookingCode"
        val encodedVpa = URLEncoder.encode(cleanVpa, StandardCharsets.UTF_8.name())
        val encodedName = URLEncoder.encode(cleanName, StandardCharsets.UTF_8.name())
        val encodedNote = URLEncoder.encode(note, StandardCharsets.UTF_8.name())
        val encodedRef = URLEncoder.encode(bookingCode, StandardCharsets.UTF_8.name())

        return "upi://pay?pa=$encodedVpa&pn=$encodedName&am=$amountStr&cu=INR&tn=$encodedNote&tr=$encodedRef"
    }

    /**
     * Generates a QR code [Bitmap] for the given text content.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val writer = QRCodeWriter()
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a QR code [ImageBitmap] ready for Compose [Image].
     */
    fun generateQrImageBitmap(content: String, sizePx: Int = 512): ImageBitmap? {
        return generateQrBitmap(content, sizePx)?.asImageBitmap()
    }
}
