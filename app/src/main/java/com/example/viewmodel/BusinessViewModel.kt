package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.*
import com.example.data.repository.BusinessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Chat Message Data Class
data class ChatMessage(
    val sender: String, // "User" or "AI"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Business Statistics Summary for UI & AI Context
data class BusinessStats(
    val productsCount: Int = 0,
    val customersCount: Int = 0,
    val suppliersCount: Int = 0,
    val totalSales: Double = 0.0,
    val todaySales: Double = 0.0,
    val monthlySales: Double = 0.0,
    val grossProfit: Double = 0.0,
    val netProfit: Double = 0.0,
    val inventoryValue: Double = 0.0,
    val outstandingPayments: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val tax: Double = 0.0,
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0,
    val transactionCount: Int = 0,
    val averageOrderValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val topProducts: List<Pair<String, Int>> = emptyList()
)

sealed class CurrencySyncState {
    object Idle : CurrencySyncState()
    object Syncing : CurrencySyncState()
    data class Success(val rate: Double, val lastUpdated: String) : CurrencySyncState()
    data class Error(val message: String) : CurrencySyncState()
}

class BusinessViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = BusinessRepository(database)

    // Data streams
    val products = repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = repository.allCustomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = repository.allSuppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val employees = repository.allEmployees.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val purchases = repository.allPurchases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val payments = repository.allPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lowStockProducts = repository.lowStockProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val invoices = repository.allInvoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val whatsappLogs = repository.allLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // Cart state
    private val _cart = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val cart: StateFlow<Map<Product, Int>> = _cart.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _discountPercentage = MutableStateFlow(0.0)
    val discountPercentage: StateFlow<Double> = _discountPercentage.asStateFlow()

    // AI Chat History
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("AI", "Greetings! I am your AI Business Intelligence Advisor. Ask me anything about your sales, inventory valuation, profit margins, expense reports, or ask for demand forecasting and dynamic pricing strategies!")
        )
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("LKR")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _usdExchangeRate = MutableStateFlow(300.0)
    val usdExchangeRate: StateFlow<Double> = _usdExchangeRate.asStateFlow()

    private val _currencySyncState = MutableStateFlow<CurrencySyncState>(CurrencySyncState.Idle)
    val currencySyncState: StateFlow<CurrencySyncState> = _currencySyncState.asStateFlow()

    fun setCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    fun setExchangeRate(rate: Double) {
        if (rate > 0.0) {
            viewModelScope.launch {
                val exchangeRate = com.example.data.database.ExchangeRateEntity(
                    currencyCode = "USD",
                    rate = rate,
                    lastUpdated = System.currentTimeMillis()
                )
                repository.insertExchangeRate(exchangeRate)
            }
        }
    }

    fun syncExchangeRate() {
        viewModelScope.launch {
            _currencySyncState.value = CurrencySyncState.Syncing
            val context = getApplication<Application>()
            var isConnected = false
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                if (connectivityManager != null) {
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    isConnected = capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
                }
            } catch (e: Exception) {
                // If checking connectivity fails for any reason (e.g. security permissions, testing framework),
                // fall back to assuming we are connected so the API call is still attempted.
                isConnected = true
            }

            if (!isConnected) {
                _currencySyncState.value = CurrencySyncState.Error("No phone internet connection detected.")
                return@launch
            }

            try {
                val response = com.example.data.api.CurrencyRetrofitClient.service.getLatestRates()
                if (response.result == "success") {
                    val lkrRate = response.rates["LKR"]
                    if (lkrRate != null && lkrRate > 0.0) {
                        val exchangeRate = com.example.data.database.ExchangeRateEntity(
                            currencyCode = "USD",
                            rate = lkrRate,
                            lastUpdated = System.currentTimeMillis()
                        )
                        repository.insertExchangeRate(exchangeRate)
                    } else {
                        _currencySyncState.value = CurrencySyncState.Error("LKR rate not found in API response.")
                    }
                } else {
                    _currencySyncState.value = CurrencySyncState.Error("API error: ${response.result}")
                }
            } catch (e: Exception) {
                _currencySyncState.value = CurrencySyncState.Error(e.localizedMessage ?: "Network request failed.")
            }
        }
    }

    // Combined Business Statistics Flow using standard flow combinations
    val businessStats: StateFlow<BusinessStats> = combine(
        products,
        customers,
        suppliers,
        expenses,
        purchases,
        transactions
    ) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        val prods = args[0] as List<Product>
        @Suppress("UNCHECKED_CAST")
        val custs = args[1] as List<Customer>
        @Suppress("UNCHECKED_CAST")
        val supps = args[2] as List<Supplier>
        @Suppress("UNCHECKED_CAST")
        val exps = args[3] as List<Expense>
        @Suppress("UNCHECKED_CAST")
        val purchs = args[4] as List<Purchase>
        @Suppress("UNCHECKED_CAST")
        val txs = args[5] as List<Transaction>

        val pCount = prods.size
        val cCount = custs.size
        val sCount = supps.size

        val totalSales = txs.filter { it.status == "Completed" }.sumOf { it.total }
        val totalTax = txs.filter { it.status == "Completed" }.sumOf { it.tax }
        val salesCount = txs.filter { it.status == "Completed" }.size

        val inventoryValue = prods.sumOf { 
            when {
                it.isWeightBased -> (it.stock / 1000.0) * it.costPrice
                it.unitType == "Packet" -> {
                    val packWeightG = it.getPacketWeightInGrams()
                    val packets = if (packWeightG > 0) it.stock / packWeightG else 0.0
                    packets * it.costPrice
                }
                else -> it.stock * it.costPrice
            }
        }
        val outstandingPayments = supps.sumOf { it.balance }
        val totalPurchases = purchs.sumOf { it.total }
        val totalExpenses = exps.sumOf { it.amount }

        val todayStart = getTodayStartTimestamp()
        val monthStart = getMonthStartTimestamp()

        val todaySales = txs.filter { it.status == "Completed" && it.timestamp >= todayStart }.sumOf { it.total }
        val monthlySales = txs.filter { it.status == "Completed" && it.timestamp >= monthStart }.sumOf { it.total }

        // Dynamic Cash Balance & Bank Balance Calculations:
        val cashSales = txs.filter { it.paymentMethod == "Cash" && it.status == "Completed" }.sumOf { it.total }
        val bankSales = txs.filter { it.paymentMethod != "Cash" && it.status == "Completed" }.sumOf { it.total }

        val cashExpenses = exps.filter { it.paymentMethod == "Cash" }.sumOf { it.amount }
        val bankExpenses = exps.filter { it.paymentMethod != "Cash" }.sumOf { it.amount }

        val cashPurchases = purchs.filter { it.paymentMethod == "Cash" }.sumOf { it.total }
        val bankPurchases = purchs.filter { it.paymentMethod != "Cash" }.sumOf { it.total }

        val cashBalance = 0.0 + cashSales - cashExpenses - cashPurchases
        val bankBalance = 0.0 + bankSales - bankExpenses - bankPurchases

        // Cost of Goods Sold = Total Purchases - Ending Inventory Value
        val costOfGoodsSold = (totalPurchases - inventoryValue).coerceAtLeast(0.0)
        
        // Profit formulas requested by user
        val grossProfit = totalSales - costOfGoodsSold
        val netProfit = grossProfit - totalExpenses

        val avgOrderValue = if (salesCount > 0) totalSales / salesCount else 0.0
        val lowStockCount = prods.filter { it.stock <= it.lowStockThreshold }.size

        BusinessStats(
            productsCount = pCount,
            customersCount = cCount,
            suppliersCount = sCount,
            totalSales = totalSales,
            todaySales = todaySales,
            monthlySales = monthlySales,
            grossProfit = grossProfit,
            netProfit = netProfit,
            inventoryValue = inventoryValue,
            outstandingPayments = outstandingPayments,
            totalPurchases = totalPurchases,
            totalExpenses = totalExpenses,
            tax = totalTax,
            cashBalance = cashBalance,
            bankBalance = bankBalance,
            transactionCount = salesCount,
            averageOrderValue = avgOrderValue,
            lowStockCount = lowStockCount,
            topProducts = emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessStats())

    init {
        // Absolutely EMPTY on startup - No automatic database seeding
        viewModelScope.launch {
            repository.getExchangeRateFlow("USD").collect { rateEntity ->
                if (rateEntity != null && rateEntity.rate > 0.0) {
                    _usdExchangeRate.value = rateEntity.rate
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(rateEntity.lastUpdated))
                    _currencySyncState.value = CurrencySyncState.Success(rateEntity.rate, timeStr)
                }
            }
        }
        syncExchangeRate()
    }

    private fun getTodayStartTimestamp(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getMonthStartTimestamp(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // POS Cart Operations
    fun addToCart(product: Product) {
        val current = _cart.value.toMutableMap()
        val currentQty = current[product] ?: 0
        if (currentQty < product.stock) {
            current[product] = currentQty + 1
            _cart.value = current
        }
    }

    fun removeFromCart(product: Product) {
        val current = _cart.value.toMutableMap()
        current.remove(product)
        _cart.value = current
    }

    fun updateCartQty(product: Product, quantity: Int) {
        val current = _cart.value.toMutableMap()
        if (quantity <= 0) {
            current.remove(product)
        } else if (quantity <= product.stock) {
            current[product] = quantity
        }
        _cart.value = current
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
    }

    fun setDiscount(percentage: Double) {
        _discountPercentage.value = percentage.coerceIn(0.0, 100.0)
    }

    fun clearCart() {
        _cart.value = emptyMap()
        _selectedCustomer.value = null
        _discountPercentage.value = 0.0
    }

    fun checkout(paymentMethod: String, onComplete: (Boolean, String?) -> Unit) {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) {
            onComplete(false, null)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val subtotal = cartItems.entries.sumOf { entry ->
                val prod = entry.key
                val qty = entry.value
                when {
                    prod.isWeightBased -> prod.price * (qty / 1000.0)
                    prod.unitType == "Packet" -> {
                        val packWeightG = prod.getPacketWeightInGrams()
                        val packets = if (packWeightG > 0) qty / packWeightG else 0.0
                        packets * prod.price
                    }
                    else -> prod.price * qty
                }
            }
            val discountAmount = subtotal * (_discountPercentage.value / 100.0)
            val taxAmount = (subtotal - discountAmount) * 0.08 // 8% sales tax
            val finalTotal = subtotal - discountAmount + taxAmount

            val customer = _selectedCustomer.value

            val transaction = Transaction(
                customerId = customer?.id,
                customerName = customer?.name ?: "Walk-in Customer",
                subtotal = subtotal,
                discount = discountAmount,
                tax = taxAmount,
                total = finalTotal,
                paymentMethod = paymentMethod,
                status = "Completed"
            )

            val itemsToSave = cartItems.map { entry ->
                TransactionItem(
                    transactionId = 0, // will be overwritten in repository
                    productId = entry.key.id,
                    productName = entry.key.name,
                    quantity = entry.value,
                    price = entry.key.price
                )
            }

            try {
                // 1. Complete Sale Transaction
                val transactionId = repository.completeTransaction(transaction, itemsToSave)

                // 2. Generate Invoice Number (auto-incrementing)
                val maxInvoiceId = repository.getMaxInvoiceId()
                val nextInvoiceId = maxInvoiceId + 1
                val invoiceNumber = "INV-${String.format("%06d", nextInvoiceId)}"

                // 3. Generate PDF Automatically
                val pdfFile = com.example.ui.InvoicePdfGenerator.generateInvoicePdf(
                    context = getApplication(),
                    invoiceNumber = invoiceNumber,
                    transaction = transaction.copy(id = transactionId.toInt()),
                    items = itemsToSave,
                    customer = customer,
                    currency = _selectedCurrency.value,
                    exchangeRate = _usdExchangeRate.value
                )

                // 4. Save Invoice to Database
                val invoice = Invoice(
                    invoiceNumber = invoiceNumber,
                    transactionId = transactionId.toInt(),
                    pdfPath = pdfFile?.absolutePath,
                    amountLkr = finalTotal,
                    customerId = customer?.id,
                    paymentStatus = if (paymentMethod == "Credit") "Pending" else "Paid"
                )
                repository.insertInvoice(invoice)

                // 5. WhatsApp Auto-Send (Option B) if phone exists and credentials configured
                if (customer != null && customer.phone.isNotBlank()) {
                    val apiToken = BuildConfig.WHATSAPP_API_TOKEN
                    val phoneNumberId = BuildConfig.WHATSAPP_PHONE_NUMBER_ID
                    val isCloudApiConfigured = apiToken.isNotBlank() && apiToken != "YOUR_WHATSAPP_API_TOKEN" &&
                            phoneNumberId.isNotBlank() && phoneNumberId != "YOUR_WHATSAPP_PHONE_NUMBER_ID"

                    if (isCloudApiConfigured) {
                        val result = com.example.ui.WhatsAppHelper.sendCloudApiMessage(
                            phone = customer.phone,
                            invoiceNumber = invoiceNumber,
                            customerName = customer.name,
                            totalLkr = finalTotal,
                            currency = _selectedCurrency.value,
                            exchangeRate = _usdExchangeRate.value,
                            paymentStatus = invoice.paymentStatus
                        )

                        repository.insertWhatsAppMessageLog(
                            WhatsAppMessageLog(
                                invoiceNumber = invoiceNumber,
                                recipientPhone = customer.phone,
                                messageContent = "Automatic Cloud API Send for invoice $invoiceNumber. Status: ${result.second}",
                                status = if (result.first) "Sent" else "Failed"
                            )
                        )

                        if (result.first) {
                            repository.updateInvoice(invoice.copy(whatsappSent = true))
                        }
                    }
                }

                val pdfPathResult = pdfFile?.absolutePath

                withContext(Dispatchers.Main) {
                    clearCart()
                    onComplete(true, pdfPathResult)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, null)
                }
            }
        }
    }

    suspend fun getItemsForTransaction(transactionId: Int) = repository.getItemsForTransaction(transactionId)

    // Manual/Re-send WhatsApp Message via Cloud API
    fun sendInvoiceWhatsAppCloudApi(
        invoiceNumber: String,
        phone: String,
        customerName: String,
        totalLkr: Double,
        currency: String,
        exchangeRate: Double,
        paymentStatus: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.ui.WhatsAppHelper.sendCloudApiMessage(
                phone = phone,
                invoiceNumber = invoiceNumber,
                customerName = customerName,
                totalLkr = totalLkr,
                currency = currency,
                exchangeRate = exchangeRate,
                paymentStatus = paymentStatus
            )

            repository.insertWhatsAppMessageLog(
                WhatsAppMessageLog(
                    invoiceNumber = invoiceNumber,
                    recipientPhone = phone,
                    messageContent = "Manual Cloud API Send. Response: ${result.second}",
                    status = if (result.first) "Sent" else "Failed"
                )
            )

            if (result.first) {
                val allInvs = repository.allInvoices.first()
                val existingInvoice = allInvs.find { it.invoiceNumber == invoiceNumber }
                if (existingInvoice != null) {
                    repository.updateInvoice(existingInvoice.copy(whatsappSent = true))
                }
            }

            withContext(Dispatchers.Main) {
                onResult(result.first, result.second)
            }
        }
    }

    // Logging for Client-side Deep-linking Click-To-Chat (Option A)
    fun logWhatsAppClickToChat(invoiceNumber: String, phone: String, totalLkr: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertWhatsAppMessageLog(
                WhatsAppMessageLog(
                    invoiceNumber = invoiceNumber,
                    recipientPhone = phone,
                    messageContent = "Click-to-chat URL triggered manually for invoice $invoiceNumber (Total: $totalLkr).",
                    status = "Sent"
                )
            )
            val allInvs = repository.allInvoices.first()
            val existingInvoice = allInvs.find { it.invoiceNumber == invoiceNumber }
            if (existingInvoice != null) {
                repository.updateInvoice(existingInvoice.copy(whatsappSent = true))
            }
        }
    }


    // Product Management
    fun addProduct(
        name: String,
        sku: String,
        price: Double,
        costPrice: Double,
        stock: Int,
        threshold: Int,
        category: String,
        brand: String,
        isWeightBased: Boolean,
        unit: String,
        unitType: String,
        packetWeight: Double,
        packetWeightUnit: String,
        openingStock: Int,
        totalWeightInGrams: Int,
        description: String = "Premium quality product with exceptional flavor, sourcing, and texture.",
        imageUrl: String = "",
        specifications: String = "",
        features: String = "",
        ingredients: String = "",
        warranty: String = "",
        returnPolicy: String = "",
        shippingInfo: String = "",
        careInstructions: String = "",
        countryOfOrigin: String = "",
        shortDescription: String = "",
        longDescription: String = "",
        wholesalePrice: Double = 0.0,
        dealerPrice: Double = 0.0,
        vipPrice: Double = 0.0,
        bulkPrice: Double = 0.0,
        minimumOrderPrice: Double = 0.0,
        salePrice: Double = 0.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProduct(
                Product(
                    name = name,
                    sku = sku,
                    price = price,
                    costPrice = costPrice,
                    stock = stock,
                    lowStockThreshold = threshold,
                    category = category,
                    brand = brand,
                    isWeightBased = isWeightBased,
                    unit = unit,
                    unitType = unitType,
                    packetWeight = packetWeight,
                    packetWeightUnit = packetWeightUnit,
                    openingStock = openingStock,
                    totalWeightInGrams = totalWeightInGrams,
                    description = description,
                    imageUrl = imageUrl,
                    specifications = specifications,
                    features = features,
                    ingredients = ingredients,
                    warranty = warranty,
                    returnPolicy = returnPolicy,
                    shippingInfo = shippingInfo,
                    careInstructions = careInstructions,
                    countryOfOrigin = countryOfOrigin,
                    shortDescription = shortDescription,
                    longDescription = longDescription,
                    wholesalePrice = wholesalePrice,
                    dealerPrice = dealerPrice,
                    vipPrice = vipPrice,
                    bulkPrice = bulkPrice,
                    minimumOrderPrice = minimumOrderPrice,
                    salePrice = salePrice
                )
            )
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProductById(id)
        }
    }

    fun updateStock(id: Int, newStock: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val product = repository.getProductById(id)
            if (product != null) {
                repository.updateProduct(product.copy(stock = newStock))
            }
        }
    }

    fun breakBulkProduct(sourceProductId: Int, destProductId: Int, qtyToBreak: Int, conversionFactor: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val sourceProduct = repository.getProductById(sourceProductId) ?: return@launch
            val destProduct = repository.getProductById(destProductId) ?: return@launch
            
            val sourceDeduction = if (sourceProduct.unitType == "Packet") {
                val packWeightG = sourceProduct.getPacketWeightInGrams()
                (qtyToBreak * packWeightG).toInt()
            } else {
                qtyToBreak
            }
            
            if (sourceProduct.stock >= sourceDeduction) {
                val updatedSource = sourceProduct.copy(stock = (sourceProduct.stock - sourceDeduction).coerceAtLeast(0))
                repository.updateProduct(updatedSource)
                
                val destAddition = if (destProduct.unitType == "Packet") {
                    val destPackWeightG = destProduct.getPacketWeightInGrams()
                    if (destPackWeightG > 0) {
                        ((qtyToBreak * conversionFactor) * destPackWeightG).toInt()
                    } else {
                        (qtyToBreak * conversionFactor).toInt()
                    }
                } else {
                    (qtyToBreak * conversionFactor).toInt()
                }
                
                val updatedDest = destProduct.copy(stock = destProduct.stock + destAddition)
                repository.updateProduct(updatedDest)
            }
        }
    }

    // Purchase Management
    fun addPurchase(productId: Int, quantity: Int, costPrice: Double, supplierId: Int?, supplierName: String, paymentMethod: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val product = repository.getProductById(productId)
            val isWeight = product?.isWeightBased == true
            val total = if (isWeight) (quantity / 1000.0) * costPrice else quantity * costPrice
            repository.insertPurchase(
                Purchase(
                    productId = productId,
                    productName = product?.name ?: "Product",
                    quantity = quantity,
                    costPrice = costPrice,
                    total = total,
                    supplierId = supplierId,
                    supplierName = supplierName,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    fun deletePurchase(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePurchaseById(id)
        }
    }

    // Payment Management
    fun addPayment(referenceId: Int, type: String, amount: Double, paymentMethod: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPayment(
                Payment(
                    referenceId = referenceId,
                    type = type,
                    amount = amount,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    fun deletePayment(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePaymentById(id)
        }
    }

    // Transaction Return Management
    fun returnTransaction(transactionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.returnTransaction(transactionId)
        }
    }

    // Expense Management
    fun addExpense(title: String, amount: Double, category: String, paymentMethod: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExpense(
                Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    paymentMethod = paymentMethod
                )
            )
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpenseById(id)
        }
    }

    // Customer Management
    fun addCustomer(name: String, phone: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCustomer(Customer(name = name, phone = phone, email = email))
        }
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomerById(id)
        }
    }

    // Supplier Management
    fun addSupplier(name: String, phone: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSupplier(Supplier(name = name, phone = phone, email = email))
        }
    }

    fun deleteSupplier(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSupplierById(id)
        }
    }

    // Employee Management
    fun addEmployee(name: String, role: String, salary: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertEmployee(Employee(name = name, role = role, salary = salary))
        }
    }

    fun deleteEmployee(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEmployeeById(id)
        }
    }

    fun toggleEmployeeShift(employee: Employee) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = employee.copy(
                shiftActive = !employee.shiftActive,
                attendanceTime = if (!employee.shiftActive) System.currentTimeMillis() else employee.attendanceTime
            )
            repository.updateEmployee(updated)
        }
    }

    // Seeding mock database if requested
    fun seedDemoDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            seedInitialDatabase()
        }
    }

    // AI Assistant Integration with Context Loading
    fun sendChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // Add user message to history
        val updatedHistory = _chatHistory.value.toMutableList()
        updatedHistory.add(ChatMessage("User", userMessage))
        _chatHistory.value = updatedHistory

        _isAiLoading.value = true

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    executeGeminiQuery(userMessage)
                }
                val finalHistory = _chatHistory.value.toMutableList()
                finalHistory.add(ChatMessage("AI", response))
                _chatHistory.value = finalHistory
            } catch (e: Exception) {
                val finalHistory = _chatHistory.value.toMutableList()
                finalHistory.add(ChatMessage("AI", "Apologies, I encountered an issue retrieving business insights. Please ensure your Gemini API key is configured correctly in the Secrets panel."))
                _chatHistory.value = finalHistory
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearChat() {
        _chatHistory.value = listOf(
            ChatMessage("AI", "Greetings! I am your AI Business Intelligence Advisor. Ask me anything about your sales, inventory valuation, profit margins, expense reports, or ask for demand forecasting and pricing strategies!")
        )
    }

    private suspend fun executeGeminiQuery(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Gemini API Key is not configured yet. Please input your Gemini API Key in the AI Studio Secrets panel to enable real-time interactive business intelligence reports."
        }

        val stats = businessStats.value
        val prodList = products.value
        val expList = expenses.value
        val txList = transactions.value

        val productSummary = prodList.joinToString("\n") { 
            "- ${it.name} (SKU: ${it.sku}, Category: ${it.category}, Price: $${it.price}, Cost: $${it.costPrice}, Stock: ${it.stock}, Min Threshold: ${it.lowStockThreshold})" 
        }
        val expenseSummary = expList.joinToString("\n") { 
            "- ${it.title} (Category: ${it.category}, Amount: $${it.amount})" 
        }
        val recentSales = txList.take(5).joinToString("\n") { 
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.timestamp))
            "- Sale #${it.id} on $dateStr: Total $${it.total} via ${it.paymentMethod} to ${it.customerName}" 
        }

        val systemContext = """
            You are "Enterprise POS AI Advisor", an advanced Business Intelligence & Analytics LLM.
            You have real-time read-only access to the business's database. Here is the exact current state of the business:
            
            == FINANCIAL METRICS ==
            - Total Sales Revenue: $${String.format("%.2f", stats.totalSales)}
            - Today's Sales: $${String.format("%.2f", stats.todaySales)}
            - Monthly Sales: $${String.format("%.2f", stats.monthlySales)}
            - Gross Profit: $${String.format("%.2f", stats.grossProfit)}
            - Total Operating Expenses: $${String.format("%.2f", stats.totalExpenses)}
            - Net Profit margin: $${String.format("%.2f", stats.netProfit)}
            - Inventory Valuation: $${String.format("%.2f", stats.inventoryValue)}
            - Outstanding Payments (Accounts Payable): $${String.format("%.2f", stats.outstandingPayments)}
            - Cash Account Balance: $${String.format("%.2f", stats.cashBalance)}
            - Bank Account Balance: $${String.format("%.2f", stats.bankBalance)}
            - Total Transaction Orders Count: ${stats.transactionCount}
            - Average Transaction Basket Value: $${String.format("%.2f", stats.averageOrderValue)}
            
            == INVENTORY REGISTER (${prodList.size} items, ${stats.lowStockCount} low stock alerts) ==
            $productSummary
            
            == RECENT OPERATING EXPENSES ==
            $expenseSummary
            
            == RECENT SALES TRANSACTIONS ==
            $recentSales
            
            == ADVISOR DIRECTIVE ==
            - Formulate highly analytical, professional, and business-focused responses.
            - When asked to show profit, analyze margins, predict sales, find slow items, or calculate metrics, look at the exact values above and compute the correct mathematical answer.
            - Format answers with elegant bold headings, Bullet lists, and currency symbols. Keep explanations concise, professional, and action-oriented.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemContext)))
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Received an empty analytics output."
        } catch (e: Exception) {
            "API Query failed: ${e.localizedMessage ?: "Unknown Error"}"
        }
    }

    private suspend fun seedInitialDatabase() {
        // Categories
        val cats = listOf("Spices", "Tea", "Gemstones", "General")
        cats.forEach { repository.insertCategory(Category(name = it)) }

        // Products
        val mockProducts = listOf(
            Product(
                name = "Premium Ceylon Cinnamon",
                sku = "SP-CIN-01",
                price = 1500.0,
                costPrice = 700.0,
                stock = 50000,
                lowStockThreshold = 10000,
                category = "Spices",
                brand = "Ceylon Spice Farms",
                isWeightBased = true,
                unit = "Kilogram (kg)",
                shortDescription = "100% Pure Sri Lankan Cinnamon.",
                description = "Harvested from the finest plantations of Sri Lanka.\nRich aroma.\nNatural.\nNo additives.\nExport quality.",
                currency = "LKR",
                salePrice = 1200.0,
                wholesalePrice = 950.0,
                dealerPrice = 900.0,
                vipPrice = 850.0,
                bulkPrice = 800.0,
                minimumOrderPrice = 1000.0
            ),
            Product(name = "Green Cardamom Pods", sku = "SP-CAR-03", price = 25.0, costPrice = 14.0, stock = 35000, lowStockThreshold = 5000, category = "Spices", brand = "Ceylon Spice Farms", isWeightBased = true, unit = "Kilogram (kg)"),
            Product(name = "Whole Black Pepper", sku = "SP-PEP-02", price = 12.0, costPrice = 6.5, stock = 8000, lowStockThreshold = 10000, category = "Spices", brand = "Ceylon Spice Farms", isWeightBased = true, unit = "Kilogram (kg)"),
            Product(name = "Blue Sapphire (GIA Certified 1.8ct)", sku = "GM-SAP-01", price = 1200.0, costPrice = 750.0, stock = 2, lowStockThreshold = 1, category = "Gemstones", brand = "Rathnapura Gems", isWeightBased = false, unit = "Piece (pcs)"),
            Product(name = "Imperial Star Ruby (1.2ct)", sku = "GM-RUB-02", price = 1800.0, costPrice = 1100.0, stock = 1, lowStockThreshold = 1, category = "Gemstones", brand = "Rathnapura Gems", isWeightBased = false, unit = "Piece (pcs)"),
            Product(name = "Uva Highland Pekoe Tea", sku = "TE-UHP-02", price = 14.5, costPrice = 7.0, stock = 4000, lowStockThreshold = 10000, category = "Tea", brand = "Highland Tea Ltd", isWeightBased = true, unit = "Kilogram (kg)"),
            Product(name = "Matcha Ceremonial Grade", sku = "TE-MAT-03", price = 32.0, costPrice = 18.0, stock = 15000, lowStockThreshold = 5000, category = "Tea", brand = "Highland Tea Ltd", isWeightBased = true, unit = "Kilogram (kg)"),
            Product(name = "English Breakfast Tea", sku = "TE-EBT-01", price = 8.5, costPrice = 4.0, stock = 120000, lowStockThreshold = 20000, category = "Tea", brand = "Highland Tea Ltd", isWeightBased = true, unit = "Kilogram (kg)")
        )
        val addedProducts = mockProducts.map { prod ->
            val id = repository.insertProduct(prod)
            prod.copy(id = id.toInt())
        }

        // Customers
        val mockCustomers = listOf(
            Customer(name = "Helena Visser", phone = "+31 6 1234 5678", email = "helena@gemcollector.nl", loyaltyPoints = 120, balance = 0.0),
            Customer(name = "Johnathan Doe", phone = "+1 555-0199", email = "john.doe@gmail.com", loyaltyPoints = 15, balance = 45.0),
            Customer(name = "Amara Wijesinghe", phone = "+94 77 123 4567", email = "amara@srilankatea.com", loyaltyPoints = 250, balance = 0.0)
        )
        mockCustomers.forEach { repository.insertCustomer(it) }

        // Suppliers
        val mockSuppliers = listOf(
            Supplier(name = "Highland Tea Growers Ltd", phone = "+94 51 223 4455", email = "orders@highlandteagrowers.lk", balance = 450.0),
            Supplier(name = "Ceylon Spice Farms", phone = "+94 11 223 3445", email = "sales@ceylonspices.lk", balance = 0.0),
            Supplier(name = "Rathnapura Gem Mines Corp", phone = "+94 45 222 1122", email = "contracts@gemsrathnapura.com", balance = 12000.0)
        )
        mockSuppliers.forEach { repository.insertSupplier(it) }

        // Expenses
        val mockExpenses = listOf(
            Expense(title = "Office & Warehouse Rent", amount = 1200.0, category = "Rent", paymentMethod = "Bank", timestamp = System.currentTimeMillis() - 86400000 * 5),
            Expense(title = "Freight & Export Shipping", amount = 350.0, category = "Utilities", paymentMethod = "Bank", timestamp = System.currentTimeMillis() - 86400000 * 3),
            Expense(title = "Electricity & Cold Storage Bill", amount = 180.0, category = "Utilities", paymentMethod = "Cash", timestamp = System.currentTimeMillis() - 86400000 * 1)
        )
        mockExpenses.forEach { repository.insertExpense(it) }

        // Employees
        val mockEmployees = listOf(
            Employee(name = "Amir Khan", role = "Manager", salary = 3200.0),
            Employee(name = "Sarah Connor", role = "Cashier", salary = 2000.0)
        )
        mockEmployees.forEach { repository.insertEmployee(it) }

        // Transactions (Pre-populate with 3 interesting sales)
        val sales = listOf(
            Pair(addedProducts[3], 1), // Blue Sapphire
            Pair(addedProducts[0], 2000), // Ceylon Cinnamon x 2kg
            Pair(addedProducts[6], 1000)  // Matcha Ceremonial Grade x 1kg
        )

        // Purchase 1: Gemstones transaction to Helena Visser
        val gemTx = Transaction(
            timestamp = System.currentTimeMillis() - 86400000 * 2,
            customerId = 1,
            customerName = "Helena Visser",
            subtotal = 1200.0,
            discount = 50.0,
            tax = 92.0,
            total = 1242.0,
            paymentMethod = "Bank Transfer",
            status = "Completed"
        )
        repository.completeTransaction(
            gemTx,
            listOf(
                TransactionItem(
                    transactionId = 0,
                    productId = addedProducts[3].id,
                    productName = addedProducts[3].name,
                    quantity = 1,
                    price = 1200.0
                )
            )
        )

        // Purchase 2: Cash Spices & Tea sale
        val spiceTx = Transaction(
            timestamp = System.currentTimeMillis() - 86400000,
            customerId = null,
            customerName = "Walk-in Customer",
            subtotal = 62.0,
            discount = 0.0,
            tax = 4.96,
            total = 66.96,
            paymentMethod = "Cash",
            status = "Completed"
        )
        repository.completeTransaction(
            spiceTx,
            listOf(
                TransactionItem(transactionId = 0, productId = addedProducts[0].id, productName = addedProducts[0].name, quantity = 2000, price = 0.015),
                TransactionItem(transactionId = 0, productId = addedProducts[6].id, productName = addedProducts[6].name, quantity = 1000, price = 0.032)
            )
        )
    }
}
