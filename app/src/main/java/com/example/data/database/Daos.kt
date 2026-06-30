package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): Product?

    @Query("SELECT * FROM products WHERE stock <= lowStockThreshold")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE :query OR sku LIKE :query OR category LIKE :query")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET stock = :newStock WHERE id = :id")
    suspend fun updateStock(id: Int, newStock: Int)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)

    @Query("SELECT COUNT(id) FROM products")
    fun getProductsCountFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(stock * costPrice), 0.0) FROM products")
    fun getInventoryValueFlow(): Flow<Double>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Int)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET loyaltyPoints = loyaltyPoints + :points WHERE id = :id")
    suspend fun addLoyaltyPoints(id: Int, points: Int)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Int)

    @Query("SELECT COUNT(id) FROM customers")
    fun getCustomersCountFlow(): Flow<Int>
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteSupplierById(id: Int)

    @Query("SELECT COUNT(id) FROM suppliers")
    fun getSuppliersCountFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(balance), 0.0) FROM suppliers")
    fun getOutstandingPaymentsFlow(): Flow<Double>
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Int): List<TransactionItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItem(item: TransactionItem): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    // Aggregate SQL functions
    @Query("SELECT COALESCE(SUM(total), 0.0) FROM transactions WHERE status = 'Completed'")
    fun getTotalSalesFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(total), 0.0) FROM transactions WHERE status = 'Completed' AND timestamp >= :todayStart")
    fun getTodaySalesFlow(todayStart: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(total), 0.0) FROM transactions WHERE status = 'Completed' AND timestamp >= :monthStart")
    fun getMonthSalesFlow(monthStart: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(tax), 0.0) FROM transactions WHERE status = 'Completed'")
    fun getTotalTaxFlow(): Flow<Double>

    @Query("SELECT COUNT(id) FROM transactions WHERE status = 'Completed'")
    fun getSalesCountFlow(): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(
            CASE WHEN p.isWeightBased = 1 
                 THEN (ti.quantity * p.costPrice / 1000.0) 
                 WHEN p.unit_type = 'Packet'
                 THEN (ti.quantity * p.costPrice / (CASE WHEN p.packet_weight = 0 THEN 1.0 ELSE p.packet_weight * (CASE WHEN p.packet_weight_unit = 'kg' THEN 1000.0 ELSE 1.0 END) END))
                 ELSE ti.quantity * p.costPrice 
            END
        ), 0.0) 
        FROM transaction_items ti 
        INNER JOIN products p ON ti.productId = p.id 
        INNER JOIN transactions t ON ti.transactionId = t.id 
        WHERE t.status = 'Completed'
    """)
    fun getCostOfGoodsSoldFlow(): Flow<Double>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses")
    fun getTotalExpensesFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE paymentMethod = 'Cash'")
    fun getCashExpensesFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE paymentMethod = 'Bank'")
    fun getBankExpensesFlow(): Flow<Double>
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployeeById(id: Int)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase): Long

    @Query("SELECT COALESCE(SUM(total), 0.0) FROM purchases")
    fun getTotalPurchasesFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(total), 0.0) FROM purchases WHERE paymentMethod = 'Cash'")
    fun getCashPurchasesFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(total), 0.0) FROM purchases WHERE paymentMethod = 'Bank'")
    fun getBankPurchasesFlow(): Flow<Double>

    @Query("DELETE FROM purchases WHERE id = :id")
    suspend fun deletePurchaseById(id: Int)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Int)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Int): Invoice?

    @Query("SELECT * FROM invoices WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getInvoiceByTransactionId(transactionId: Int): Invoice?

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getInvoicesForCustomer(customerId: Int): Flow<List<Invoice>>

    @Query("SELECT COALESCE(MAX(id), 0) FROM invoices")
    suspend fun getMaxInvoiceId(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Int)
}

@Dao
interface WhatsAppMessageLogDao {
    @Query("SELECT * FROM whatsapp_messages_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WhatsAppMessageLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WhatsAppMessageLog): Long
}

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :currencyCode LIMIT 1")
    suspend fun getExchangeRate(currencyCode: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :currencyCode LIMIT 1")
    fun getExchangeRateFlow(currencyCode: String): Flow<ExchangeRateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRate(exchangeRate: ExchangeRateEntity)
}

