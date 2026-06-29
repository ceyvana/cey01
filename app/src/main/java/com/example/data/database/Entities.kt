package com.example.data.database

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
    val brand: String = "Generic"
)

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
