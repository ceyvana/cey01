package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.Product
import com.example.data.database.Supplier
import com.example.data.database.Transaction
import com.example.viewmodel.BusinessStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    stats: BusinessStats,
    recentTransactions: List<Transaction>,
    lowStockProducts: List<Product>,
    products: List<Product>,
    suppliers: List<Supplier>,
    onAddPurchase: (Int, Int, Double, Int?, String, String) -> Unit,
    onAddExpense: (String, Double, String, String) -> Unit,
    onAddPayment: (Int, String, Double, String) -> Unit,
    onReturnTransaction: (Int) -> Unit,
    onSeedDemo: () -> Unit,
    onNavigateToPOS: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dialog Triggers
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showPayoutDialog by remember { mutableStateOf(false) }

    // Dialog Fields
    // Purchase fields
    var selectedProdId by remember { mutableStateOf<Int?>(null) }
    var purchaseQty by remember { mutableStateOf("") }
    var purchaseCost by remember { mutableStateOf("") }
    var selectedSuppId by remember { mutableStateOf<Int?>(null) }
    var purchasePaymentMethod by remember { mutableStateOf("Cash") } // "Cash", "Bank", "Credit"

    // Expense fields
    var expenseTitle by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("Rent") }
    var expensePaymentMethod by remember { mutableStateOf("Cash") }

    // Supplier Payment fields
    var payoutSuppId by remember { mutableStateOf<Int?>(null) }
    var payoutAmount by remember { mutableStateOf("") }
    var payoutPaymentMethod by remember { mutableStateOf("Cash") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBFF))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HEADER SECTION (Bento styling)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ENTERPRISE POS SYSTEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Live Intelligence",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A1C1E)
                    )
                }

                // Initial Avatar Circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "HQ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 2. PRIMARY HERO CARD - TOTAL REVENUE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("revenue_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "All-Time Sales Revenue",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$${String.format("%,.2f", stats.totalSales)}",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Transaction counter badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${stats.transactionCount} Sales",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sparkline Column chart visualizer (drawn live!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barCount = 7
                            val barSpacing = 16.dp.toPx()
                            val totalSpacing = barSpacing * (barCount - 1)
                            val barWidth = (size.width - totalSpacing) / barCount
                            val barHeights = listOf(0.2f, 0.45f, 0.3f, 0.75f, 0.5f, 0.95f, 0.8f)

                            for (i in 0 until barCount) {
                                val h = barHeights[i] * size.height
                                val x = i * (barWidth + barSpacing)
                                val y = size.height - h

                                drawRoundRect(
                                    color = Color(0xFF0061A4).copy(alpha = if (i == 5) 1.0f else 0.4f),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, h),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. THE MANDATED DASHBOARD KPI BENTO GRID (REAL-TIME, LIVE CALCULATED)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Core Performance KPIs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A1C1E)
                )

                // Row 1: Today's Sales & Monthly Sales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoKpiCard(
                        title = "Today's Sales",
                        value = stats.todaySales,
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFF0061A4),
                        containerColor = Color(0xFFE3F2FD),
                        modifier = Modifier.weight(1f)
                    )
                    BentoKpiCard(
                        title = "Monthly Sales",
                        value = stats.monthlySales,
                        icon = Icons.Default.DateRange,
                        iconColor = Color(0xFF00796B),
                        containerColor = Color(0xFFE0F2F1),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Gross Profit & Net Profit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoKpiCard(
                        title = "Gross Profit",
                        value = stats.grossProfit,
                        icon = Icons.Default.Check,
                        iconColor = Color(0xFF388E3C),
                        containerColor = Color(0xFFE8F5E9),
                        modifier = Modifier.weight(1f)
                    )
                    BentoKpiCard(
                        title = "Net Profit",
                        value = stats.netProfit,
                        icon = Icons.Default.Check,
                        iconColor = Color(0xFF2E7D32),
                        containerColor = Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Inventory Value & Outstanding Payments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoKpiCard(
                        title = "Inventory Value",
                        value = stats.inventoryValue,
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFF4F46E5),
                        containerColor = Color(0xFFEEF2F6),
                        modifier = Modifier.weight(1f)
                    )
                    BentoKpiCard(
                        title = "Outstanding Payments",
                        value = stats.outstandingPayments,
                        icon = Icons.Default.Warning,
                        iconColor = if (stats.outstandingPayments > 0.0) Color(0xFFD32F2F) else Color(0xFF555555),
                        containerColor = if (stats.outstandingPayments > 0.0) Color(0xFFFFEBEE) else Color(0xFFF5F5F5),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. CASH AND BANK ACCOUNT BALANCES AND LEDGERS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Financial Treasury Accounts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A1C1E)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cash Balance Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE1E2EC))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(16.dp))
                                Text("Cash Balance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$${String.format("%,.2f", stats.cashBalance)}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                        }
                    }

                    // Bank Balance Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE1E2EC))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
                                Text("Bank Balance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$${String.format("%,.2f", stats.bankBalance)}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D47A1))
                        }
                    }
                }

                // Purchases, Expenses & Tax Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Purchases
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Purchases", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("$${String.format("%,.2f", stats.totalPurchases)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                    }
                    // Expenses
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Expenses", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("$${String.format("%,.2f", stats.totalExpenses)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                    }
                    // Tax
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Accumulated Tax", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("$${String.format("%,.2f", stats.tax)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                    }
                }
            }
        }

        // 5. INTERACTIVE TRANSACTION QUICK ACTIONS BAR
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Treasury Management Actions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A1C1E)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPurchaseDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Purchase", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showExpenseDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showPayoutDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Payout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Demo Reset/Seed Button (Optional helper for users to instantly test UI fidelity)
                OutlinedButton(
                    onClick = { onSeedDemo() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pre-populate Demo Ledger (Simulate Business)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 6. OPERATIONAL LEDGERS METADATA SUMMARY
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F6FA)),
                border = BorderStroke(1.dp, Color(0xFFE1E2EC))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetadataCounter(label = "Products", value = stats.productsCount)
                    VerticalDivider(modifier = Modifier.height(30.dp), color = Color(0xFFC4C6D0))
                    MetadataCounter(label = "Customers", value = stats.customersCount)
                    VerticalDivider(modifier = Modifier.height(30.dp), color = Color(0xFFC4C6D0))
                    MetadataCounter(label = "Suppliers", value = stats.suppliersCount)
                }
            }
        }

        // 7. LIVE LEDGER TRANSACTION REGISTER
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Transaction Ledger",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A1C1E)
                    )

                    Text(
                        text = "New Sale",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToPOS() }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        if (recentTransactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No completed transactions recorded yet.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF44474E).copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            recentTransactions.forEachIndexed { index, tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.customerName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A1C1E)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        val dateStr = SimpleDateFormat("HH:mm • MMM dd, yyyy", Locale.getDefault()).format(Date(tx.timestamp))
                                        Text(
                                            text = "$dateStr • ${tx.paymentMethod} • Status: ${tx.status}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = if (tx.status == "Returned") "-$${String.format("%,.2f", tx.total)}" else "+$${String.format("%,.2f", tx.total)}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (tx.status == "Returned") Color(0xFFBA1A1A) else Color(0xFF2E7D32)
                                        )

                                        if (tx.status != "Returned") {
                                            IconButton(
                                                onClick = { onReturnTransaction(tx.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Return Sale",
                                                    tint = Color(0xFFBA1A1A),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (index < recentTransactions.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = Color(0xFFE1E2EC)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ================== DIALOGS ==================

    // 1. ADD PURCHASE (SUPPLY STOCK) DIALOG
    if (showPurchaseDialog) {
        Dialog(onDismissRequest = { showPurchaseDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Record Supply Purchase",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (products.isEmpty()) {
                        Text("No products registered. Register a product in the Inventory screen first.", color = Color.Red, fontSize = 13.sp)
                    } else {
                        Text("Select Product:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        var expandedProds by remember { mutableStateOf(false) }
                        val currProd = products.find { it.id == selectedProdId }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { expandedProds = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE), contentColor = Color.Black)
                            ) {
                                Text(currProd?.name ?: "Click to Select Product", fontSize = 13.sp)
                            }
                            DropdownMenu(
                                expanded = expandedProds,
                                onDismissRequest = { expandedProds = false }
                            ) {
                                products.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} (SKU: ${p.sku})") },
                                        onClick = {
                                            selectedProdId = p.id
                                            purchaseCost = p.costPrice.toString()
                                            expandedProds = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = purchaseQty,
                            onValueChange = { purchaseQty = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = purchaseCost,
                            onValueChange = { purchaseCost = it },
                            label = { Text("Unit Cost Price ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Supplier (Optional):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        var expandedSupps by remember { mutableStateOf(false) }
                        val currSupp = suppliers.find { it.id == selectedSuppId }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { expandedSupps = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE), contentColor = Color.Black)
                            ) {
                                Text(currSupp?.name ?: "Click to Select Supplier", fontSize = 13.sp)
                            }
                            DropdownMenu(
                                expanded = expandedSupps,
                                onDismissRequest = {expandedSupps = false}
                            ) {
                                suppliers.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = {
                                            selectedSuppId = s.id
                                            expandedSupps = false
                                        }
                                    )
                                }
                            }
                        }

                        Text("Payment Type:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Cash", "Bank", "Credit").forEach { type ->
                                FilterChip(
                                    selected = purchasePaymentMethod == type,
                                    onClick = { purchasePaymentMethod = type },
                                    label = { Text(type) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPurchaseDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    val prodId = selectedProdId
                                    val qty = purchaseQty.toIntOrNull() ?: 0
                                    val cost = purchaseCost.toDoubleOrNull() ?: 0.0
                                    if (prodId != null && qty > 0 && cost > 0.0) {
                                        val suppName = suppliers.find { it.id == selectedSuppId }?.name ?: "Generic Supplier"
                                        onAddPurchase(prodId, qty, cost, selectedSuppId, suppName, purchasePaymentMethod)
                                        showPurchaseDialog = false
                                        // Reset fields
                                        purchaseQty = ""
                                        purchaseCost = ""
                                        selectedProdId = null
                                        selectedSuppId = null
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Submit")
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. ADD EXPENSE DIALOG
    if (showExpenseDialog) {
        Dialog(onDismissRequest = { showExpenseDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Record Business Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = { expenseTitle = it },
                        label = { Text("Expense Title (e.g., Rent, Bills)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = expenseAmount,
                        onValueChange = { expenseAmount = it },
                        label = { Text("Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    var expandedCats by remember { mutableStateOf(false) }
                    val categoriesList = listOf("Rent", "Utilities", "Salaries", "Marketing", "Other")

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expandedCats = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE), contentColor = Color.Black)
                        ) {
                            Text(expenseCategory)
                        }
                        DropdownMenu(
                            expanded = expandedCats,
                            onDismissRequest = { expandedCats = false }
                        ) {
                            categoriesList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        expenseCategory = cat
                                        expandedCats = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Payment Method:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "Bank").forEach { method ->
                            FilterChip(
                                selected = expensePaymentMethod == method,
                                onClick = { expensePaymentMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExpenseDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val amt = expenseAmount.toDoubleOrNull() ?: 0.0
                                if (expenseTitle.isNotBlank() && amt > 0.0) {
                                    onAddExpense(expenseTitle, amt, expenseCategory, expensePaymentMethod)
                                    showExpenseDialog = false
                                    expenseTitle = ""
                                    expenseAmount = ""
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }

    // 3. RECORD SUPPLIER PAYOUT (CREDIT PAYMENT) DIALOG
    if (showPayoutDialog) {
        Dialog(onDismissRequest = { showPayoutDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Supplier Credit Payout",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (suppliers.isEmpty()) {
                        Text("No suppliers registered. Create suppliers in CRM first.", color = Color.Red, fontSize = 13.sp)
                    } else {
                        Text("Select Supplier:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        var expandedSupps by remember { mutableStateOf(false) }
                        val currSupp = suppliers.find { it.id == payoutSuppId }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { expandedSupps = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE), contentColor = Color.Black)
                            ) {
                                Text(
                                    text = if (currSupp != null) "${currSupp.name} (Credit Owed: $${String.format("%.2f", currSupp.balance)})" else "Click to Select Supplier",
                                    fontSize = 13.sp
                                )
                            }
                            DropdownMenu(
                                expanded = expandedSupps,
                                onDismissRequest = { expandedSupps = false }
                            ) {
                                suppliers.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.name} (Owed: $${String.format("%.2f", s.balance)})") },
                                        onClick = {
                                            payoutSuppId = s.id
                                            payoutAmount = s.balance.toString()
                                            expandedSupps = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = payoutAmount,
                            onValueChange = { payoutAmount = it },
                            label = { Text("Payout Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Payment Method:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Cash", "Bank").forEach { method ->
                                FilterChip(
                                    selected = payoutPaymentMethod == method,
                                    onClick = { payoutPaymentMethod = method },
                                    label = { Text(method) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPayoutDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    val suppId = payoutSuppId
                                    val amt = payoutAmount.toDoubleOrNull() ?: 0.0
                                    if (suppId != null && amt > 0.0) {
                                        onAddPayment(suppId, "SupplierPayout", amt, payoutPaymentMethod)
                                        // Also record it as an operating expense payout to make cash balance matching perfect!
                                        val suppName = suppliers.find { it.id == suppId }?.name ?: "Supplier"
                                        onAddExpense("Supplier Credit Paid to $suppName", amt, "Other", payoutPaymentMethod)
                                        showPayoutDialog = false
                                        payoutSuppId = null
                                        payoutAmount = ""
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Submit")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoKpiCard(
    title: String,
    value: Double,
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    border: BorderStroke? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF44474E).copy(alpha = 0.8f)
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "$${String.format("%,.2f", value)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A1C1E)
            )
        }
    }
}

@Composable
fun MetadataCounter(label: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$value",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}
