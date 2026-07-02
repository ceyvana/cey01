package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import com.example.ui.AIScreen
import com.example.ui.CoverPageScreen
import com.example.ui.CRMScreen
import com.example.ui.DashboardScreen
import com.example.ui.EcommerceScreen
import com.example.ui.InventoryScreen
import com.example.ui.POSScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BusinessViewModel

enum class Screen {
    DASHBOARD,
    POS,
    SHOP,
    INVENTORY,
    CRM,
    AI
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: BusinessViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                // Collect state flows reactively
                val products by viewModel.products.collectAsState()
                val categories by viewModel.categories.collectAsState()
                val customers by viewModel.customers.collectAsState()
                val suppliers by viewModel.suppliers.collectAsState()
                val cart by viewModel.cart.collectAsState()
                val selectedCustomer by viewModel.selectedCustomer.collectAsState()
                val discountPercentage by viewModel.discountPercentage.collectAsState()
                val chatHistory by viewModel.chatHistory.collectAsState()
                val isAiLoading by viewModel.isAiLoading.collectAsState()
                val businessStats by viewModel.businessStats.collectAsState()
                val transactions by viewModel.transactions.collectAsState()
                val lowStockProducts by viewModel.lowStockProducts.collectAsState()
                val selectedCurrency by viewModel.selectedCurrency.collectAsState()
                val usdExchangeRate by viewModel.usdExchangeRate.collectAsState()
                val currencySyncState by viewModel.currencySyncState.collectAsState()
                val invoices by viewModel.invoices.collectAsState()
                val whatsappLogs by viewModel.whatsappLogs.collectAsState()


                var showCoverPage by rememberSaveable { mutableStateOf(true) }

                AnimatedVisibility(
                    visible = showCoverPage,
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = tween(600)) + slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(600))
                ) {
                    CoverPageScreen(onDismiss = { showCoverPage = false })
                }

                AnimatedVisibility(
                    visible = !showCoverPage,
                    enter = fadeIn(animationSpec = tween(600)),
                    exit = fadeOut()
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (currentScreen != Screen.DASHBOARD) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = when (currentScreen) {
                                            Screen.DASHBOARD -> "Business Intelligence"
                                            Screen.POS -> "POS Billing Terminal"
                                            Screen.SHOP -> "E-Shop Terminal"
                                            Screen.INVENTORY -> "Stock Inventory Hub"
                                            Screen.CRM -> "CRM & B2B Directories"
                                            Screen.AI -> "AI Executive Assistant"
                                        },
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("app_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == Screen.DASHBOARD,
                                onClick = { currentScreen = Screen.DASHBOARD },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                modifier = Modifier.testTag("nav_btn_dashboard")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.POS,
                                onClick = { currentScreen = Screen.POS },
                                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "POS") },
                                label = { Text("POS") },
                                modifier = Modifier.testTag("nav_btn_pos")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.SHOP,
                                onClick = { currentScreen = Screen.SHOP },
                                icon = { Icon(Icons.Default.Star, contentDescription = "Shop") },
                                label = { Text("Shop") },
                                modifier = Modifier.testTag("nav_btn_shop")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.INVENTORY,
                                onClick = { currentScreen = Screen.INVENTORY },
                                icon = { Icon(Icons.Default.List, contentDescription = "Inventory") },
                                label = { Text("Inventory") },
                                modifier = Modifier.testTag("nav_btn_inventory")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.CRM,
                                onClick = { currentScreen = Screen.CRM },
                                icon = { Icon(Icons.Default.Person, contentDescription = "CRM") },
                                label = { Text("CRM") },
                                modifier = Modifier.testTag("nav_btn_crm")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.AI,
                                onClick = { currentScreen = Screen.AI },
                                icon = { Icon(Icons.Default.Face, contentDescription = "AI") },
                                label = { Text("AI Advisor") },
                                modifier = Modifier.testTag("nav_btn_ai")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.DASHBOARD -> {
                                DashboardScreen(
                                    stats = businessStats,
                                    recentTransactions = transactions,
                                    lowStockProducts = lowStockProducts,
                                    products = products,
                                    suppliers = suppliers,
                                    selectedCurrency = selectedCurrency,
                                    usdExchangeRate = usdExchangeRate,
                                    currencySyncState = currencySyncState,
                                    onCurrencyChange = { viewModel.setCurrency(it) },
                                    onExchangeRateChange = { viewModel.setExchangeRate(it) },
                                    onSyncCurrency = { viewModel.syncExchangeRate() },
                                    onAddPurchase = { prodId, qty, cost, suppId, suppName, method ->
                                        viewModel.addPurchase(prodId, qty, cost, suppId, suppName, method)
                                    },
                                    onAddExpense = { title, amt, cat, method ->
                                        viewModel.addExpense(title, amt, cat, method)
                                    },
                                    onAddPayment = { refId, type, amt, method ->
                                        viewModel.addPayment(refId, type, amt, method)
                                    },
                                    onReturnTransaction = { txId ->
                                        viewModel.returnTransaction(txId)
                                    },
                                    onSeedDemo = {
                                        viewModel.seedDemoDatabase()
                                    },
                                    onNavigateToPOS = { currentScreen = Screen.POS },
                                    onShowCoverPage = { showCoverPage = true }
                                )
                            }
                            Screen.POS -> {
                                POSScreen(
                                    products = products,
                                    categories = categories,
                                    customers = customers,
                                    cart = cart,
                                    selectedCustomer = selectedCustomer,
                                    discountPercentage = discountPercentage,
                                    selectedCurrency = selectedCurrency,
                                    usdExchangeRate = usdExchangeRate,
                                    onAddToCart = { viewModel.addToCart(it) },
                                    onRemoveFromCart = { viewModel.removeFromCart(it) },
                                    onUpdateCartQty = { prod, qty -> viewModel.updateCartQty(prod, qty) },
                                    onSelectCustomer = { viewModel.selectCustomer(it) },
                                    onSetDiscount = { viewModel.setDiscount(it) },
                                    onClearCart = { viewModel.clearCart() },
                                    onCheckout = { method, callback -> viewModel.checkout(method, callback) }
                                )
                            }
                            Screen.SHOP -> {
                                EcommerceScreen(
                                    products = products,
                                    categories = categories,
                                    cart = cart,
                                    selectedCurrency = selectedCurrency,
                                    usdExchangeRate = usdExchangeRate,
                                    onAddToCart = { viewModel.addToCart(it) },
                                    onRemoveFromCart = { viewModel.removeFromCart(it) },
                                    onUpdateCartQty = { prod, qty -> viewModel.updateCartQty(prod, qty) },
                                    onClearCart = { viewModel.clearCart() },
                                    onCheckout = { method, callback -> viewModel.checkout(method, callback) },
                                    onAddProduct = { name, sku, price, cost, stock, threshold, cat, brand, isWeight, unit, unitType, packWt, packWtUnit, openStock, totalWtGrams, desc, img,
                                                      sDesc, lDesc, specs, feat, ing, war, ret, ship, care, origin,
                                                      wholesale, dealer, vip, bulk, minOrder, salePrice ->
                                        viewModel.addProduct(
                                            name, sku, price, cost, stock, threshold, cat, brand, isWeight, unit, unitType, packWt, packWtUnit, openStock, totalWtGrams, desc, img,
                                            sDesc, lDesc, specs, feat, ing, war, ret, ship, care, origin,
                                            wholesale, dealer, vip, bulk, minOrder, salePrice
                                        )
                                    },
                                    onEditProduct = { viewModel.updateProduct(it) },
                                    onDeleteProduct = { viewModel.deleteProduct(it) }
                                )
                            }
                            Screen.INVENTORY -> {
                                InventoryScreen(
                                    products = products,
                                    categories = categories,
                                    selectedCurrency = selectedCurrency,
                                    usdExchangeRate = usdExchangeRate,
                                    onAddProduct = { name, sku, price, cost, stock, threshold, cat, brand, isWeightBased, unit, unitType, packWeight, packWeightUnit, openStock, totalWeightGrams ->
                                        viewModel.addProduct(name, sku, price, cost, stock, threshold, cat, brand, isWeightBased, unit, unitType, packWeight, packWeightUnit, openStock, totalWeightGrams)
                                    },
                                    onEditProduct = { product ->
                                        viewModel.updateProduct(product)
                                    },
                                    onDeleteProduct = { viewModel.deleteProduct(it) },
                                    onUpdateStock = { id, qty -> viewModel.updateStock(id, qty) },
                                    onBreakBulkProduct = { srcId, dstId, qty, factor ->
                                        viewModel.breakBulkProduct(srcId, dstId, qty, factor)
                                    }
                                )
                            }
                            Screen.CRM -> {
                                CRMScreen(
                                    customers = customers,
                                    suppliers = suppliers,
                                    invoices = invoices,
                                    whatsappLogs = whatsappLogs,
                                    transactions = transactions,
                                    selectedCurrency = selectedCurrency,
                                    usdExchangeRate = usdExchangeRate,
                                    onAddCustomer = { name, phone, email -> viewModel.addCustomer(name, phone, email) },
                                    onDeleteCustomer = { viewModel.deleteCustomer(it) },
                                    onAddSupplier = { name, phone, email -> viewModel.addSupplier(name, phone, email) },
                                    onDeleteSupplier = { viewModel.deleteSupplier(it) },
                                    onSendWhatsAppCloudApi = { phone, invNum, name, total, curr, rate, status, callback ->
                                        viewModel.sendInvoiceWhatsAppCloudApi(invNum, phone, name, total, curr, rate, status, callback)
                                    },
                                    onLogWhatsAppClickToChat = { invNum, phone, total ->
                                        viewModel.logWhatsAppClickToChat(invNum, phone, total)
                                    },
                                    onGetTransactionItems = { txId ->
                                        viewModel.getItemsForTransaction(txId)
                                    }
                                )
                            }

                            Screen.AI -> {
                                AIScreen(
                                    chatHistory = chatHistory,
                                    isLoading = isAiLoading,
                                    onSendMessage = { viewModel.sendChatMessage(it) },
                                    onClearChat = { viewModel.clearChat() },
                                    products = products,
                                    businessStats = businessStats,
                                    selectedCurrency = selectedCurrency,
                                    usdExchangeRate = usdExchangeRate
                                )
                            }
                        }
                    }
                }
                } // End of AnimatedVisibility
            }
        }
    }
}
