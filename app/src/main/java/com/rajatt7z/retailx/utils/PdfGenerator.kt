package com.rajatt7z.retailx.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.models.Bill
import com.rajatt7z.retailx.models.Order
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateBillPdf(context: Context, bill: Bill): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        drawBillContent(canvas, paint, pageInfo.pageWidth, bill)

        pdfDocument.finishPage(page)

        val fileName = "Bill_${bill.id}_${System.currentTimeMillis()}.pdf"
        val uri = savePdfToDownloads(context, pdfDocument, fileName)
        
        pdfDocument.close()
        
        if (uri != null) {
            showDownloadNotification(context, uri, fileName)
            Toast.makeText(context, "Bill saved to Downloads", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save Bill", Toast.LENGTH_SHORT).show()
        }
        
        return uri
    }

    fun generateOrderPdf(context: Context, order: Order, employeeName: String = "System"): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        drawOrderContent(canvas, paint, pageInfo.pageWidth, order, employeeName)

        pdfDocument.finishPage(page)

        val fileName = "Order_${order.id}_${System.currentTimeMillis()}.pdf"
        val uri = savePdfToDownloads(context, pdfDocument, fileName)
        
        pdfDocument.close()
        
        if (uri != null) {
            showDownloadNotification(context, uri, fileName)
            Toast.makeText(context, "Order Receipt saved to Downloads", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save Receipt", Toast.LENGTH_SHORT).show()
        }
        
        return uri
    }

    private fun savePdfToDownloads(context: Context, pdfDocument: PdfDocument, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ReTailX")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream)
                    outputStream.close()
                    return uri
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun showDownloadNotification(context: Context, uri: Uri, fileName: String) {
        val channelId = "downloads_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Downloads", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done) // Use generic or app icon
            .setContentTitle("Download Complete")
            .setContentText(fileName)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun drawBillContent(canvas: Canvas, paint: Paint, pageWidth: Int, bill: Bill) {
    // Header
    paint.textSize = 24f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("ReTailX Invoice", (pageWidth / 2).toFloat(), 50f, paint)

    paint.textSize = 14f
    paint.isFakeBoldText = false
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("Bill ID: ${bill.id.takeLast(8).uppercase()}", 20f, 90f, paint)
    canvas.drawText("Date: ${formatDate(bill.timestamp)}", 20f, 110f, paint)
    
    canvas.drawText("Customer: ${bill.customerName}", 20f, 140f, paint)
    canvas.drawText("Phone: ${bill.customerPhone}", 20f, 160f, paint)
    canvas.drawText("Generated By: ${bill.generatedByName}", 20f, 180f, paint)

    // Table Header
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawLine(20f, 200f, pageWidth - 20f, 200f, paint)
    
    paint.style = Paint.Style.FILL
    paint.textSize = 14f
    paint.isFakeBoldText = true
    canvas.drawText("Product", 20f, 220f, paint)
    canvas.drawText("Qty", 250f, 220f, paint)
    canvas.drawText("Price", 320f, 220f, paint)
    canvas.drawText("Disc", 410f, 220f, paint)
    canvas.drawText("Total", 500f, 220f, paint)
    
    paint.style = Paint.Style.STROKE
    canvas.drawLine(20f, 230f, pageWidth - 20f, 230f, paint)

    // Items
    var yPos = 250f
    paint.style = Paint.Style.FILL
    paint.isFakeBoldText = false
    
    for (item in bill.products) {
        canvas.drawText(item.productName.take(20), 20f, yPos, paint)
        canvas.drawText(item.quantity.toString(), 250f, yPos, paint)
        canvas.drawText(String.format("%.2f", item.unitPrice), 320f, yPos, paint)
        
        val discText = when (item.discountType) {
            "percentage" -> String.format("-%.0f%%", item.discountValue)
            "fixed" -> String.format("-%.2f", item.discountValue)
            else -> "-"
        }
        canvas.drawText(discText, 410f, yPos, paint)
        canvas.drawText(String.format("%.2f", item.totalPrice), 500f, yPos, paint)
        yPos += 25f
    }

    // Totals Section
    paint.style = Paint.Style.STROKE
    canvas.drawLine(20f, yPos + 10f, pageWidth - 20f, yPos + 10f, paint)
    
    paint.style = Paint.Style.FILL
    paint.textSize = 14f
    yPos += 30f

    // Subtotal
    canvas.drawText("Subtotal:", 350f, yPos, paint)
    canvas.drawText(String.format("%.2f", bill.subtotal), 500f, yPos, paint)
    yPos += 20f

    // Discount
    if (bill.discountAmount > 0) {
        paint.color = android.graphics.Color.rgb(229, 57, 53)
        canvas.drawText("Discount:", 350f, yPos, paint)
        canvas.drawText(String.format("-%.2f", bill.discountAmount), 500f, yPos, paint)
        paint.color = android.graphics.Color.BLACK
        yPos += 20f
    }

    // Tax
    if (bill.taxRate > 0 && bill.taxAmount > 0) {
        canvas.drawText(String.format("Tax (%.1f%%):", bill.taxRate), 350f, yPos, paint)
        canvas.drawText(String.format("%.2f", bill.taxAmount), 500f, yPos, paint)
        yPos += 20f
    }

    // Grand Total
    paint.style = Paint.Style.STROKE
    canvas.drawLine(350f, yPos, pageWidth - 20f, yPos, paint)
    yPos += 5f
    
    paint.style = Paint.Style.FILL
    paint.textSize = 16f
    paint.isFakeBoldText = true
    canvas.drawText(String.format("Grand Total: %.2f", bill.totalAmount), 380f, yPos + 20f, paint)
    }

    private fun drawOrderContent(canvas: Canvas, paint: Paint, pageWidth: Int, order: Order, employeeName: String) {
        // Header
        paint.textSize = 24f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ReTailX Order Receipt", (pageWidth / 2).toFloat(), 50f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Order ID: ${order.id.takeLast(8).uppercase()}", 20f, 90f, paint)
        canvas.drawText("Date: ${formatDate(order.timestamp)}", 20f, 110f, paint)
        
        canvas.drawText("Customer: ${order.customerName}", 20f, 140f, paint)
        canvas.drawText("Phone: ${order.customerPhone}", 20f, 160f, paint)
        canvas.drawText("Sold By: $employeeName", 20f, 180f, paint)

        // Table Header
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawLine(20f, 200f, pageWidth - 20f, 200f, paint)
        
        paint.style = Paint.Style.FILL
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Product", 20f, 220f, paint)
        canvas.drawText("Qty", 300f, 220f, paint)
        canvas.drawText("Price", 400f, 220f, paint)
        canvas.drawText("Total", 500f, 220f, paint)
        
        paint.style = Paint.Style.STROKE
        canvas.drawLine(20f, 230f, pageWidth - 20f, 230f, paint)

        // Item
        var yPos = 250f
        paint.style = Paint.Style.FILL
        paint.isFakeBoldText = false
        
        canvas.drawText(order.productName.take(25), 20f, yPos, paint)
        canvas.drawText(order.quantity.toString(), 300f, yPos, paint)
        val unitPrice = if(order.quantity > 0) order.totalPrice / order.quantity else 0.0
        canvas.drawText(String.format("%.2f", unitPrice), 400f, yPos, paint)
        canvas.drawText(String.format("%.2f", order.totalPrice), 500f, yPos, paint)
        
        // Total
        paint.style = Paint.Style.STROKE
        canvas.drawLine(20f, yPos + 30f, pageWidth - 20f, yPos + 30f, paint)
        
        paint.style = Paint.Style.FILL
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Grand Total: $${String.format("%.2f", order.totalPrice)}", 400f, yPos + 60f, paint)
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "Unknown Date"
        }
    }

    fun generateRefundPdf(context: Context, refund: com.rajatt7z.retailx.models.Refund, originalBill: com.rajatt7z.retailx.models.Bill): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        drawRefundContent(canvas, paint, pageInfo.pageWidth, refund, originalBill)

        pdfDocument.finishPage(page)

        val fileName = "Refund_${refund.id.takeLast(6)}_${System.currentTimeMillis()}.pdf"
        val uri = savePdfToDownloads(context, pdfDocument, fileName)

        pdfDocument.close()

        if (uri != null) {
            showDownloadNotification(context, uri, fileName)
            Toast.makeText(context, "Refund receipt saved to Downloads", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save refund receipt", Toast.LENGTH_SHORT).show()
        }

        return uri
    }

    private fun drawRefundContent(canvas: Canvas, paint: Paint, pageWidth: Int, refund: com.rajatt7z.retailx.models.Refund, bill: com.rajatt7z.retailx.models.Bill) {
        // Header
        paint.textSize = 24f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ReTailX Refund Receipt", (pageWidth / 2).toFloat(), 50f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Refund ID: REF-${refund.id.takeLast(6).uppercase()}", 20f, 90f, paint)
        canvas.drawText("Date: ${formatDate(refund.timestamp)}", 20f, 110f, paint)
        canvas.drawText("Original Bill: ${bill.id.takeLast(8).uppercase()}", 20f, 130f, paint)

        canvas.drawText("Customer: ${bill.customerName}", 20f, 160f, paint)
        canvas.drawText("Phone: ${bill.customerPhone}", 20f, 180f, paint)
        canvas.drawText("Processed By: ${refund.processedByName}", 20f, 200f, paint)
        canvas.drawText("Reason: ${refund.reason}", 20f, 220f, paint)

        // Table Header
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawLine(20f, 240f, pageWidth - 20f, 240f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Product", 20f, 260f, paint)
        canvas.drawText("Qty", 300f, 260f, paint)
        canvas.drawText("Price", 380f, 260f, paint)
        canvas.drawText("Refund", 480f, 260f, paint)

        paint.style = Paint.Style.STROKE
        canvas.drawLine(20f, 270f, pageWidth - 20f, 270f, paint)

        // Returned Items
        var yPos = 290f
        paint.style = Paint.Style.FILL
        paint.isFakeBoldText = false

        for (item in refund.returnedItems) {
            canvas.drawText(item.productName.take(25), 20f, yPos, paint)
            canvas.drawText(item.quantity.toString(), 300f, yPos, paint)
            canvas.drawText(String.format("%.2f", item.unitPrice), 380f, yPos, paint)
            canvas.drawText(String.format("%.2f", item.refundAmount), 480f, yPos, paint)
            yPos += 25f
        }

        // Total Section
        paint.style = Paint.Style.STROKE
        canvas.drawLine(20f, yPos + 10f, pageWidth - 20f, yPos + 10f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.color = Color.rgb(211, 47, 47) // Red for refund
        canvas.drawText(String.format("Total Refund: %.2f", refund.refundAmount), 350f, yPos + 40f, paint)

        paint.color = Color.BLACK
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Status: ${refund.status}", 20f, yPos + 80f, paint)
    }
}
