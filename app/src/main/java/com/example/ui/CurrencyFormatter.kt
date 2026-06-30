package com.example.ui

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double, currency: String, exchangeRate: Double): String {
        val rate = if (exchangeRate <= 0.0) 300.0 else exchangeRate
        return if (currency == "USD") {
            val usdVal = amount / rate
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            format.format(usdVal)
        } else {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            formatter.minimumFractionDigits = 2
            formatter.maximumFractionDigits = 2
            "LKR ${formatter.format(amount)}"
        }
    }
}
