package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BusinessRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val categoryDao = database.categoryDao()
    private val customerDao = database.customerDao()
    private val supplierDao = database.supplierDao()
    private val transactionDao = database.transactionDao()
    private val expenseDao = database.expenseDao()
    private val employeeDao = database.employeeDao()
    private val purchaseDao = database.purchaseDao()
    private val paymentDao = database.paymentDao()
    private val invoiceDao = database.invoiceDao()
    private val whatsAppMessageLogDao = database.whatsAppMessageLogDao()
    private val exchangeRateDao = database.exchangeRateDao()

    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val productsCount: Flow<Int> = productDao.getProductsCountFlow()
    val inventoryValue: Flow<Double> = productDao.getInventoryValueFlow()

    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts("%$query%")

    suspend fun getProductById(id: Int): Product? = productDao.getProductById(id)
    suspend fun getProductBySku(sku: String): Product? = productDao.getProductBySku(sku)

    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProductById(id: Int) = productDao.deleteProductById(id)

    // Invoices & WhatsApp logs
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()
    val allLogs: Flow<List<WhatsAppMessageLog>> = whatsAppMessageLogDao.getAllLogs()

    suspend fun getMaxInvoiceId(): Int = invoiceDao.getMaxInvoiceId()
    suspend fun insertInvoice(invoice: Invoice): Long = invoiceDao.insertInvoice(invoice)
    suspend fun updateInvoice(invoice: Invoice) = invoiceDao.updateInvoice(invoice)
    suspend fun getInvoiceByTransactionId(transactionId: Int): Invoice? = invoiceDao.getInvoiceByTransactionId(transactionId)
    suspend fun getInvoiceById(id: Int): Invoice? = invoiceDao.getInvoiceById(id)
    fun getInvoicesForCustomer(customerId: Int): Flow<List<Invoice>> = invoiceDao.getInvoicesForCustomer(customerId)

    suspend fun insertWhatsAppMessageLog(log: WhatsAppMessageLog): Long = whatsAppMessageLogDao.insertLog(log)


    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun deleteCategoryById(id: Int) = categoryDao.deleteCategoryById(id)

    // Customers
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val customersCount: Flow<Int> = customerDao.getCustomersCountFlow()
    suspend fun getCustomerById(id: Int): Customer? = customerDao.getCustomerById(id)
    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomerById(id: Int) = customerDao.deleteCustomerById(id)

    // Suppliers
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    val suppliersCount: Flow<Int> = supplierDao.getSuppliersCountFlow()
    val outstandingPayments: Flow<Double> = supplierDao.getOutstandingPaymentsFlow()
    suspend fun insertSupplier(supplier: Supplier): Long = supplierDao.insertSupplier(supplier)
    suspend fun updateSupplier(supplier: Supplier) = database.supplierDao().updateSupplier(supplier)
    suspend fun deleteSupplierById(id: Int) = supplierDao.deleteSupplierById(id)

    // Expenses
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val totalExpenses: Flow<Double> = expenseDao.getTotalExpensesFlow()
    val cashExpenses: Flow<Double> = expenseDao.getCashExpensesFlow()
    val bankExpenses: Flow<Double> = expenseDao.getBankExpensesFlow()
    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun deleteExpenseById(id: Int) = expenseDao.deleteExpenseById(id)

    // Employees
    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()
    suspend fun insertEmployee(employee: Employee): Long = employeeDao.insertEmployee(employee)
    suspend fun updateEmployee(employee: Employee) = employeeDao.updateEmployee(employee)
    suspend fun deleteEmployeeById(id: Int) = employeeDao.deleteEmployeeById(id)

    // Purchases
    val allPurchases: Flow<List<Purchase>> = purchaseDao.getAllPurchases()
    val totalPurchases: Flow<Double> = purchaseDao.getTotalPurchasesFlow()
    val cashPurchases: Flow<Double> = purchaseDao.getCashPurchasesFlow()
    val bankPurchases: Flow<Double> = purchaseDao.getBankPurchasesFlow()
    
    suspend fun insertPurchase(purchase: Purchase): Long {
        val purchaseId = purchaseDao.insertPurchase(purchase)
        
        // Add purchased items to inventory stock!
        val product = productDao.getProductById(purchase.productId)
        if (product != null) {
            val newStock = product.stock + purchase.quantity
            productDao.updateStock(product.id, newStock)
        }

        // If unpaid / credit purchase, add to Supplier balance (outstanding payments)
        if (purchase.paymentMethod == "Credit" && purchase.supplierId != null) {
            val supplier = supplierDao.getAllSuppliers().first { true }.find { it.id == purchase.supplierId }
            if (supplier != null) {
                supplierDao.updateSupplier(supplier.copy(balance = supplier.balance + purchase.total))
            }
        }
        return purchaseId
    }

    suspend fun deletePurchaseById(id: Int) = purchaseDao.deletePurchaseById(id)

    // Payments
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments()
    suspend fun insertPayment(payment: Payment): Long {
        val paymentId = paymentDao.insertPayment(payment)

        // If it's a Supplier Payout, decrease Supplier balance (Outstanding Payments)
        if (payment.type == "SupplierPayout") {
            val supplier = supplierDao.getAllSuppliers().first().find { it.id == payment.referenceId }
            if (supplier != null) {
                // Deduct payment amount from supplier balance
                val newBalance = (supplier.balance - payment.amount).coerceAtLeast(0.0)
                supplierDao.updateSupplier(supplier.copy(balance = newBalance))
            }
        } else if (payment.type == "CustomerReceipt") {
            val customer = customerDao.getAllCustomers().first().find { it.id == payment.referenceId }
            if (customer != null) {
                // Deduct payment from customer balance (money they owe us)
                val newBalance = (customer.balance - payment.amount).coerceAtLeast(0.0)
                customerDao.updateCustomer(customer.copy(balance = newBalance))
            }
        }
        return paymentId
    }

    suspend fun deletePaymentById(id: Int) = paymentDao.deletePaymentById(id)

    // Transactions (Sales)
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val totalSales: Flow<Double> = transactionDao.getTotalSalesFlow()
    val totalTax: Flow<Double> = transactionDao.getTotalTaxFlow()
    val salesCount: Flow<Int> = transactionDao.getSalesCountFlow()
    val costOfGoodsSold: Flow<Double> = transactionDao.getCostOfGoodsSoldFlow()

    fun getTodaySales(todayStart: Long): Flow<Double> = transactionDao.getTodaySalesFlow(todayStart)
    fun getMonthSales(monthStart: Long): Flow<Double> = transactionDao.getMonthSalesFlow(monthStart)

    suspend fun getItemsForTransaction(transactionId: Int): List<TransactionItem> =
        transactionDao.getItemsForTransaction(transactionId)

    suspend fun completeTransaction(
        transaction: Transaction,
        items: List<TransactionItem>
    ): Long {
        val transactionId = transactionDao.insertTransaction(transaction).toInt()

        for (item in items) {
            val dbItem = item.copy(transactionId = transactionId)
            transactionDao.insertTransactionItem(dbItem)

            // Deduct inventory stock
            val product = productDao.getProductById(item.productId)
            if (product != null) {
                val newStock = (product.stock - item.quantity).coerceAtLeast(0)
                productDao.updateStock(product.id, newStock)
            }
        }

        // Add Loyalty Points (e.g., 1 point per $10 spent)
        if (transaction.customerId != null) {
            val pointsEarned = (transaction.total / 10).toInt()
            if (pointsEarned > 0) {
                customerDao.addLoyaltyPoints(transaction.customerId, pointsEarned)
            }
            // If credit sale / customer owes us, update customer balance
            if (transaction.paymentMethod == "Credit") {
                val customer = customerDao.getCustomerById(transaction.customerId)
                if (customer != null) {
                    customerDao.updateCustomer(customer.copy(balance = customer.balance + transaction.total))
                }
            }
        }

        return transactionId.toLong()
    }

    suspend fun returnTransaction(transactionId: Int) {
        val transaction = transactionDao.getTransactionById(transactionId)
        if (transaction != null && transaction.status != "Returned") {
            // Set status to Returned
            transactionDao.updateTransaction(transaction.copy(status = "Returned"))
            
            // Restore inventory stock
            val items = transactionDao.getItemsForTransaction(transactionId)
            for (item in items) {
                val product = productDao.getProductById(item.productId)
                if (product != null) {
                    productDao.updateStock(product.id, product.stock + item.quantity)
                }
            }

            // If credit transaction, deduct from customer balance
            if (transaction.paymentMethod == "Credit" && transaction.customerId != null) {
                val customer = customerDao.getCustomerById(transaction.customerId)
                if (customer != null) {
                    val newBalance = (customer.balance - transaction.total).coerceAtLeast(0.0)
                    customerDao.updateCustomer(customer.copy(balance = newBalance))
                }
            }
        }
    }

    suspend fun deleteTransactionById(id: Int) = transactionDao.deleteTransactionById(id)

    // Exchange Rates
    fun getExchangeRateFlow(currencyCode: String): Flow<ExchangeRateEntity?> = exchangeRateDao.getExchangeRateFlow(currencyCode)
    suspend fun getExchangeRate(currencyCode: String): ExchangeRateEntity? = exchangeRateDao.getExchangeRate(currencyCode)
    suspend fun insertExchangeRate(exchangeRate: ExchangeRateEntity) = exchangeRateDao.insertExchangeRate(exchangeRate)
}
