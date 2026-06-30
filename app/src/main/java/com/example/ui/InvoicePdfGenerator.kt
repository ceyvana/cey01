package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.database.Customer
import com.example.data.database.Transaction
import com.example.data.database.TransactionItem
import com.example.data.database.AppDatabase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {
    fun generateInvoicePdf(
        context: Context,
        invoiceNumber: String,
        transaction: Transaction,
        items: List<TransactionItem>,
        customer: Customer?,
        currency: String,
        exchangeRate: Double
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(26, 82, 118) // Slate Primary Blue
            isAntiAlias = true
        }

        // 1. Draw header background
        canvas.drawRect(0f, 0f, 595f, 120f, headerPaint)

        // Draw Business Name
        textPaint.color = Color.WHITE
        textPaint.textSize = 22f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CEYVANA PROFESSIONAL POS", 30f, 50f, textPaint)

        // Draw Business Details
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Premium Retail & Distribution Solution | Colombo, Sri Lanka", 30f, 75f, textPaint)
        canvas.drawText("Contact: +94 11 234 5678 | Email: billing@ceyvana.lk | Web: www.ceyvana.lk", 30f, 95f, textPaint)

        // Invoice title
        textPaint.textSize = 26f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("INVOICE", 420f, 65f, textPaint)

        // 2. Draw Metadata Section
        textPaint.color = Color.BLACK
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var yPos = 160f
        canvas.drawText("INVOICE DETAILS", 30f, yPos, textPaint)
        canvas.drawText("CUSTOMER DETAILS", 300f, yPos, textPaint)

        yPos += 20f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Invoice No: $invoiceNumber", 30f, yPos, textPaint)
        canvas.drawText("Customer: ${customer?.name ?: "Walk-in Customer"}", 300f, yPos, textPaint)

        yPos += 15f
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = sdf.format(Date(transaction.timestamp))
        canvas.drawText("Date & Time: $dateStr", 30f, yPos, textPaint)
        canvas.drawText("Phone: ${customer?.phone?.ifBlank { "N/A" } ?: "N/A"}", 300f, yPos, textPaint)

        yPos += 15f
        canvas.drawText("Payment Method: ${transaction.paymentMethod}", 30f, yPos, textPaint)
        canvas.drawText("Email: ${customer?.email?.ifBlank { "N/A" } ?: "N/A"}", 300f, yPos, textPaint)

        yPos += 15f
        val statusText = if (transaction.paymentMethod == "Credit") "PENDING / CREDIT" else "PAID"
        canvas.drawText("Status: $statusText", 30f, yPos, textPaint)

        // Line separator
        yPos += 25f
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(30f, yPos, 565f, yPos, paint)

        // 3. Draw Table Header
        yPos += 25f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Item Name", 30f, yPos, textPaint)
        canvas.drawText("Qty", 320f, yPos, textPaint)
        canvas.drawText("Unit Price", 380f, yPos, textPaint)
        canvas.drawText("Total Price", 480f, yPos, textPaint)

        yPos += 10f
        canvas.drawLine(30f, yPos, 565f, yPos, paint)

        // 4. Draw Table Items
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (item in items) {
            yPos += 22f
            if (yPos > 700f) {
                // simple layout constraint: don't overflow the page
                break
            }
            
            val product = try {
                val db = AppDatabase.getDatabase(context)
                kotlinx.coroutines.runBlocking {
                    db.productDao().getProductById(item.productId)
                }
            } catch (e: Exception) {
                null
            }

            val qtyStr: String
            val displayUnitPrice: Double
            val totalValue: Double

            if (product != null) {
                when {
                    product.unitType == "Packet" -> {
                        val packWeightG = product.getPacketWeightInGrams()
                        val packets = if (packWeightG > 0) item.quantity / packWeightG else 0.0
                        val packetsStr = if (packets % 1.0 == 0.0) "${packets.toInt()}" else String.format(java.util.Locale.US, "%.1f", packets)
                        qtyStr = "$packetsStr pkt (${item.quantity} g)"
                        displayUnitPrice = item.price
                        totalValue = packets * item.price
                    }
                    product.isWeightBased -> {
                        qtyStr = if (item.quantity >= 1000) {
                            val kg = item.quantity / 1000.0
                            if (kg % 1.0 == 0.0) "${kg.toInt()} kg" else "$kg kg"
                        } else {
                            "${item.quantity} g"
                        }
                        displayUnitPrice = item.price
                        totalValue = (item.quantity / 1000.0) * item.price
                    }
                    else -> {
                        qtyStr = item.quantity.toString()
                        displayUnitPrice = item.price
                        totalValue = item.price * item.quantity
                    }
                }
            } else {
                qtyStr = item.quantity.toString()
                displayUnitPrice = item.price
                totalValue = item.price * item.quantity
            }

            // If product name is too long, truncate
            val nameToDraw = if (item.productName.length > 32) item.productName.take(29) + "..." else item.productName
            canvas.drawText(nameToDraw, 30f, yPos, textPaint)
            canvas.drawText(qtyStr, 320f, yPos, textPaint)
            
            val uPrice = CurrencyFormatter.format(displayUnitPrice, "LKR", exchangeRate)
            canvas.drawText(uPrice, 380f, yPos, textPaint)
            
            val tPrice = CurrencyFormatter.format(totalValue, "LKR", exchangeRate)
            canvas.drawText(tPrice, 480f, yPos, textPaint)
        }

        // Line separator
        yPos += 20f
        canvas.drawLine(30f, yPos, 565f, yPos, paint)

        // 5. Draw Summary Section
        yPos += 25f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SUMMARY (LKR - Base)", 260f, yPos, textPaint)
        if (currency == "USD") {
            canvas.drawText("USD SUMMARY (Converted)", 430f, yPos, textPaint)
        }

        yPos += 20f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Subtotal:", 260f, yPos, textPaint)
        canvas.drawText(CurrencyFormatter.format(transaction.subtotal, "LKR", exchangeRate), 360f, yPos, textPaint)
        if (currency == "USD") {
            canvas.drawText(CurrencyFormatter.format(transaction.subtotal, "USD", exchangeRate), 480f, yPos, textPaint)
        }

        yPos += 15f
        canvas.drawText("Discount:", 260f, yPos, textPaint)
        canvas.drawText("-" + CurrencyFormatter.format(transaction.discount, "LKR", exchangeRate), 360f, yPos, textPaint)
        if (currency == "USD") {
            canvas.drawText("-" + CurrencyFormatter.format(transaction.discount, "USD", exchangeRate), 480f, yPos, textPaint)
        }

        yPos += 15f
        canvas.drawText("Tax (8%):", 260f, yPos, textPaint)
        canvas.drawText(CurrencyFormatter.format(transaction.tax, "LKR", exchangeRate), 360f, yPos, textPaint)
        if (currency == "USD") {
            canvas.drawText(CurrencyFormatter.format(transaction.tax, "USD", exchangeRate), 480f, yPos, textPaint)
        }

        yPos += 20f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GRAND TOTAL:", 260f, yPos, textPaint)
        canvas.drawText(CurrencyFormatter.format(transaction.total, "LKR", exchangeRate), 360f, yPos, textPaint)
        if (currency == "USD") {
            canvas.drawText(CurrencyFormatter.format(transaction.total, "USD", exchangeRate), 480f, yPos, textPaint)
        }

        // 6. Draw Footer
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawRect(30f, 750f, 565f, 810f, paint)

        textPaint.color = Color.DKGRAY
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Thank you for your business! This is a computer-generated invoice.", 50f, 775f, textPaint)
        canvas.drawText("Powered by Ceyvana Professional POS. Colombo, Sri Lanka.", 50f, 792f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF to local storage
        val invoicesDir = File(context.filesDir, "invoices")
        if (!invoicesDir.exists()) {
            invoicesDir.mkdirs()
        }
        val pdfFile = File(invoicesDir, "$invoiceNumber.pdf")

        try {
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }
}
