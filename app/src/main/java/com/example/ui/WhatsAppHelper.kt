package com.example.ui

import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WhatsAppHelper {
    private const val TAG = "WhatsAppHelper"

    /**
     * Option A: Click-to-Chat Link Generator
     * Pre-fills a message summarizing the invoice.
     */
    fun getClickToChatLink(
        phone: String,
        invoiceNumber: String,
        customerName: String,
        totalLkr: Double,
        currency: String,
        exchangeRate: Double,
        paymentStatus: String,
        downloadLink: String = ""
    ): String {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        
        // Format LKR
        val lkrStr = CurrencyFormatter.format(totalLkr, "LKR", exchangeRate)
        // Format USD if selected
        val usdStr = if (currency == "USD") " (${CurrencyFormatter.format(totalLkr, "USD", exchangeRate)})" else ""

        val message = """
🌟 *INVOICE FROM CEYVANA POS* 🌟

Hello $customerName,

Thank you for your purchase! Here is your invoice summary:

*Invoice No:* $invoiceNumber
*Total Amount:* $lkrStr$usdStr
*Payment Status:* ${paymentStatus.uppercase()}

${if (downloadLink.isNotEmpty()) "You can view and download your full PDF invoice here:\n🔗 $downloadLink\n" else ""}
Have a wonderful day!
Ceyvana POS Team.
        """.trimIndent()

        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            Uri.encode(message)
        }

        return "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
    }

    /**
     * Option B: Live Cloud API Message Send
     * Sends a direct text message containing invoice details using the Meta Cloud API.
     */
    suspend fun sendCloudApiMessage(
        phone: String,
        invoiceNumber: String,
        customerName: String,
        totalLkr: Double,
        currency: String,
        exchangeRate: Double,
        paymentStatus: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiToken = BuildConfig.WHATSAPP_API_TOKEN
        val phoneNumberId = BuildConfig.WHATSAPP_PHONE_NUMBER_ID

        if (apiToken.isBlank() || apiToken == "YOUR_WHATSAPP_API_TOKEN" ||
            phoneNumberId.isBlank() || phoneNumberId == "YOUR_WHATSAPP_PHONE_NUMBER_ID") {
            Log.w(TAG, "WhatsApp Cloud API credentials are not configured.")
            return@withContext Pair(false, "WhatsApp Cloud API credentials are not configured in Secrets panel.")
        }

        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.isBlank()) {
            return@withContext Pair(false, "Recipient phone number is invalid.")
        }

        val lkrStr = CurrencyFormatter.format(totalLkr, "LKR", exchangeRate)
        val usdStr = if (currency == "USD") " (${CurrencyFormatter.format(totalLkr, "USD", exchangeRate)})" else ""

        val message = """
🌟 *INVOICE FROM CEYVANA POS* 🌟

Hello $customerName,

Thank you for your purchase! Here is your invoice summary:

*Invoice No:* $invoiceNumber
*Total Amount:* $lkrStr$usdStr
*Payment Status:* ${paymentStatus.uppercase()}

Thank you for your business!
Ceyvana POS Team.
        """.trimIndent()

        // Construct standard WhatsApp Cloud API JSON payload
        val jsonPayload = """
        {
            "messaging_product": "whatsapp",
            "recipient_type": "individual",
            "to": "$cleanPhone",
            "type": "text",
            "text": {
                "preview_url": false,
                "body": "${message.replace("\n", "\\n").replace("\"", "\\\"")}"
            }
        }
        """.trimIndent()

        try {
            val url = URL("https://graph.facebook.com/v17.0/$phoneNumberId/messages")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $apiToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "WhatsApp API success: $responseBody")
                Pair(true, "Sent successfully via Cloud API. Response Code: $responseCode")
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "WhatsApp API error $responseCode: $errorBody")
                Pair(false, "WhatsApp Cloud API returned error $responseCode: $errorBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp network exception", e)
            Pair(false, "Network error: ${e.message}")
        }
    }
}
