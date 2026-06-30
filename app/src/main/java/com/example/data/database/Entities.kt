package com.example.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sku: String,
    val price: Double,
    val costPrice: Double,
    var stock: Int,
    val lowStockThreshold: Int = 5,
    val category: String = "General",
    val brand: String = "Generic",
    val isWeightBased: Boolean = false,
    val unit: String = "Piece (pcs)",
    @ColumnInfo(name = "unit_type") val unitType: String = "Piece",
    @ColumnInfo(name = "packet_weight") val packetWeight: Double = 0.0,
    @ColumnInfo(name = "packet_weight_unit") val packetWeightUnit: String = "g",
    @ColumnInfo(name = "opening_stock") val openingStock: Int = 0,
    @ColumnInfo(name = "total_weight_in_grams") val totalWeightInGrams: Int = 0
) {
    fun getPacketWeightInGrams(): Double {
        return if (packetWeightUnit == "kg") packetWeight * 1000.0 else packetWeight
    }

    fun getStockDisplay(): String {
        return when (unitType) {
            "Packet" -> {
                val pWeightG = getPacketWeightInGrams()
                val packets = if (pWeightG > 0) stock / pWeightG else 0.0
                val totalKg = stock / 1000.0
                val packetStr = if (packets % 1.0 == 0.0) "${packets.toInt()}" else String.format(java.util.Locale.US, "%.1f", packets)
                val kgStr = if (totalKg % 1.0 == 0.0) "${totalKg.toInt()}" else String.format(java.util.Locale.US, "%.2f", totalKg)
                "$packetStr Packets ($kgStr kg)"
            }
            "Gram (g)" -> {
                "$stock g"
            }
            "Kilogram (kg)" -> {
                val kg = stock / 1000.0
                val kgStr = if (kg % 1.0 == 0.0) "${kg.toInt()}" else String.format(java.util.Locale.US, "%.2f", kg)
                "$kgStr kg"
            }
            else -> {
                val suffix = when (unit) {
                    "Piece (pcs)" -> "pcs"
                    "Pack" -> "packs"
                    "Bottle" -> "bottles"
                    "Box" -> "boxes"
                    else -> unit.lowercase()
                }
                "$stock $suffix"
            }
        }
    }
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val loyaltyPoints: Int = 0,
    val balance: Double = 0.0
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val balance: Double = 0.0
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val customerId: Int? = null,
    val customerName: String = "Walk-in Customer",
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double,
    val paymentMethod: String, // "Cash", "Card", "QR", "Bank Transfer"
    val status: String = "Completed" // "Completed", "Held", "Returned"
)

@Entity(tableName = "transaction_items")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionId: Int,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val price: Double
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String, // "Rent", "Utilities", "Salaries", "Marketing", "Other"
    val paymentMethod: String = "Cash", // "Cash", "Bank"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // "Owner", "Manager", "Cashier"
    val shiftActive: Boolean = false,
    val attendanceTime: Long = 0L,
    val salary: Double = 0.0
)

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val costPrice: Double,
    val total: Double,
    val supplierId: Int? = null,
    val supplierName: String = "Generic Supplier",
    val paymentMethod: String = "Cash", // "Cash", "Bank", "Credit"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val referenceId: Int, // e.g. Supplier ID (for payout) or Customer ID (for credit payment) or Transaction/Purchase ID
    val type: String, // "SupplierPayout", "CustomerReceipt"
    val amount: Double,
    val paymentMethod: String, // "Cash", "Bank"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String, // format "INV-000001"
    val transactionId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val pdfPath: String? = null,
    val whatsappSent: Boolean = false,
    val emailSent: Boolean = false,
    val amountLkr: Double,
    val customerId: Int? = null,
    val paymentStatus: String // "Paid", "Pending"
)

@Entity(tableName = "whatsapp_messages_logs")
data class WhatsAppMessageLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val recipientPhone: String,
    val messageContent: String,
    val status: String, // "Sent", "Failed", "Pending"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currencyCode: String, // e.g. "USD"
    val rate: Double,                    // rate in LKR (e.g. 300.0)
    val lastUpdated: Long = System.currentTimeMillis()
)

