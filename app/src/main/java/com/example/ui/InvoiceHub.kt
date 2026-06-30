package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.Customer
import com.example.data.database.Invoice
import com.example.data.database.Transaction
import com.example.data.database.TransactionItem
import com.example.data.database.WhatsAppMessageLog
import com.example.data.database.Product
import com.example.data.database.AppDatabase
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHub(
    invoices: List<Invoice>,
    whatsappLogs: List<WhatsAppMessageLog>,
    transactions: List<Transaction>,
    customers: List<Customer>,
    selectedCurrency: String,
    usdExchangeRate: Double,
    onSendWhatsAppCloudApi: (String, String, String, Double, String, Double, String, (Boolean, String) -> Unit) -> Unit,
    onLogWhatsAppClickToChat: (String, String, Double) -> Unit,
    onGetTransactionItems: suspend (Int) -> List<TransactionItem>,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Invoices, 1 = Delivery Logs
    var selectedCustomerIdFilter by remember { mutableStateOf<Int?>(null) }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var selectedInvoiceForDetail by remember { mutableStateOf<Invoice?>(null) }

    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        // Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sub-tabs: Invoices / Logs
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 }
                ) {
                    Text("Invoices", style = MaterialTheme.typography.labelMedium)
                }
                SegmentedButton(
                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 }
                ) {
                    Text("Message Logs", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Customer Filter Dropdown
            Box {
                Button(
                    onClick = { showCustomerDropdown = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Filter", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedCustomerIdFilter == null) "All Customers" else customers.find { it.id == selectedCustomerIdFilter }?.name ?: "Selected Customer",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                DropdownMenu(
                    expanded = showCustomerDropdown,
                    onDismissRequest = { showCustomerDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Customers") },
                        onClick = {
                            selectedCustomerIdFilter = null
                            showCustomerDropdown = false
                        }
                    )
                    customers.forEach { cust ->
                        DropdownMenuItem(
                            text = { Text(cust.name) },
                            onClick = {
                                selectedCustomerIdFilter = cust.id
                                showCustomerDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // Sub Tab Content
        if (activeSubTab == 0) {
            // Invoices view
            val filteredInvoices = if (selectedCustomerIdFilter == null) {
                invoices
            } else {
                invoices.filter { it.customerId == selectedCustomerIdFilter }
            }

            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = "No Invoices", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedCustomerIdFilter == null) "No invoices generated yet.\nComplete a checkout in the POS terminal to generate one!" else "No invoices found for this customer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredInvoices) { invoice ->
                        val tx = transactions.find { it.id == invoice.transactionId }
                        InvoiceCard(
                            invoice = invoice,
                            transaction = tx,
                            selectedCurrency = selectedCurrency,
                            usdExchangeRate = usdExchangeRate,
                            onClick = { selectedInvoiceForDetail = invoice }
                        )
                    }
                }
            }
        } else {
            // Delivery Logs view
            val filteredLogs = if (selectedCustomerIdFilter == null) {
                whatsappLogs
            } else {
                // Filter logs matching invoices belonging to selected customer
                val customerInvoiceNumbers = invoices.filter { it.customerId == selectedCustomerIdFilter }.map { it.invoiceNumber }
                whatsappLogs.filter { it.invoiceNumber in customerInvoiceNumbers }
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MailOutline, contentDescription = "No Logs", tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No WhatsApp messaging delivery logs found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredLogs) { log ->
                        WhatsAppLogCard(log = log)
                    }
                }
            }
        }
    }

    // Invoice Detail Dialog
    if (selectedInvoiceForDetail != null) {
        val invoice = selectedInvoiceForDetail!!
        val tx = transactions.find { it.id == invoice.transactionId }
        val cust = customers.find { it.id == invoice.customerId }

        InvoiceDetailDialog(
            invoice = invoice,
            transaction = tx,
            customer = cust,
            selectedCurrency = selectedCurrency,
            usdExchangeRate = usdExchangeRate,
            onDismiss = { selectedInvoiceForDetail = null },
            onSendWhatsAppCloudApi = onSendWhatsAppCloudApi,
            onLogWhatsAppClickToChat = onLogWhatsAppClickToChat,
            onGetTransactionItems = onGetTransactionItems
        )
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    transaction: Transaction?,
    selectedCurrency: String,
    usdExchangeRate: Double,
    onClick: () -> Unit
) {
    val date = Date(invoice.timestamp)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(date)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = transaction?.customerName ?: "Walk-in Customer",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Payment: ${transaction?.paymentMethod ?: "Cash"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.format(invoice.amountLkr, selectedCurrency, usdExchangeRate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Payment Status badge
                        val payPending = invoice.paymentStatus == "Pending"
                        Box(
                            modifier = Modifier
                                .background(
                                    if (payPending) Color(0xFFFDF2E9) else Color(0xFFE8F8F5),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = invoice.paymentStatus.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (payPending) Color(0xFFD35400) else Color(0xFF117A65)
                            )
                        }

                        // WhatsApp Delivery badge
                        Box(
                            modifier = Modifier
                                .background(
                                    if (invoice.whatsappSent) Color(0xFFEAF2F8) else Color(0xFFF2F4F4),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (invoice.whatsappSent) "SENT" else "NOT SENT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (invoice.whatsappSent) Color(0xFF2471A3) else Color(0xFF7F8C8D)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppLogCard(log: WhatsAppMessageLog) {
    val date = Date(log.timestamp)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val formattedDate = sdf.format(date)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Sent",
                        modifier = Modifier.size(14.dp),
                        tint = if (log.status == "Sent") Color(0xFF27AE60) else Color(0xFFC0392B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Inv: ${log.invoiceNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (log.status == "Sent") Color(0xFFE8F8F5) else Color(0xFFFADBD8),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.status.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (log.status == "Sent") Color(0xFF117A65) else Color(0xFF922B21)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "To Phone: ${log.recipientPhone}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.messageContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Log Time: $formattedDate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailDialog(
    invoice: Invoice,
    transaction: Transaction?,
    customer: Customer?,
    selectedCurrency: String,
    usdExchangeRate: Double,
    onDismiss: () -> Unit,
    onSendWhatsAppCloudApi: (String, String, String, Double, String, Double, String, (Boolean, String) -> Unit) -> Unit,
    onLogWhatsAppClickToChat: (String, String, Double) -> Unit,
    onGetTransactionItems: suspend (Int) -> List<TransactionItem>
) {
    var items by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    var productsMap by remember { mutableStateOf<Map<Int, Product>>(emptyMap()) }
    var isLoadingItems by remember { mutableStateOf(true) }
    var localCurrencyMode by remember { mutableStateOf("LKR") } // "LKR" or "USD"
    var isSendingWhatsAppCloud by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(invoice) {
        isLoadingItems = true
        val fetchedItems = onGetTransactionItems(invoice.transactionId)
        items = fetchedItems
        
        val db = AppDatabase.getDatabase(context)
        val pMap = mutableMapOf<Int, Product>()
        fetchedItems.forEach { item ->
            try {
                val prod = db.productDao().getProductById(item.productId)
                if (prod != null) {
                    pMap[item.productId] = prod
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        productsMap = pMap
        isLoadingItems = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of Modal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invoice Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // Currency Toggle Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Display Currency (USD Conversion)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LKR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (localCurrencyMode == "LKR") FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (localCurrencyMode == "LKR") MaterialTheme.colorScheme.primary else Color.Gray
                            )
                            Switch(
                                checked = localCurrencyMode == "USD",
                                onCheckedChange = { localCurrencyMode = if (it) "USD" else "LKR" },
                                modifier = Modifier
                                    .scale(0.8f)
                                    .padding(horizontal = 4.dp)
                            )
                            Text(
                                text = "USD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (localCurrencyMode == "USD") FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (localCurrencyMode == "USD") MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Business Receipt Banner Header
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "CEYVANA PROFESSIONAL POS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Premium Retail Solution | Colombo, Sri Lanka",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    "Contact: +94 11 234 5678",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Invoice & Customer Info Grid
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("INVOICE METADATA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Invoice Number: ${invoice.invoiceNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(invoice.timestamp))
                                    Text("Date & Time: $formattedTime", style = MaterialTheme.typography.bodySmall)
                                    Text("Payment Method: ${transaction?.paymentMethod ?: "Cash"}", style = MaterialTheme.typography.bodySmall)
                                    Text("Payment Status: ${invoice.paymentStatus}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("CUSTOMER DIRECTORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Name: ${customer?.name ?: "Walk-in Customer"}", style = MaterialTheme.typography.bodySmall)
                                    Text("Phone: ${customer?.phone?.ifBlank { "N/A" } ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                    Text("Email: ${customer?.email?.ifBlank { "N/A" } ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                    if (customer != null) {
                                        Text("Loyalty Points: ${customer.loyaltyPoints}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        // Items Header
                        item {
                            Text(
                                text = "Billed Items",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Items list loading or display
                        if (isLoadingItems) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        } else {
                            items(items) { item ->
                                val product = productsMap[item.productId]
                                val qtyDisplay: String
                                val totalVal: Double
                                val unitLabel: String
                                
                                if (product != null) {
                                    when {
                                        product.unitType == "Packet" -> {
                                            val packWeightG = product.getPacketWeightInGrams()
                                            val packets = if (packWeightG > 0) item.quantity / packWeightG else 0.0
                                            val packetsStr = if (packets % 1.0 == 0.0) "${packets.toInt()}" else String.format(java.util.Locale.US, "%.1f", packets)
                                            qtyDisplay = "$packetsStr pkt (${item.quantity} g)"
                                            totalVal = packets * item.price
                                            unitLabel = "pkt"
                                        }
                                        product.isWeightBased -> {
                                            qtyDisplay = if (item.quantity >= 1000) {
                                                val kg = item.quantity / 1000.0
                                                if (kg % 1.0 == 0.0) "${kg.toInt()} kg" else "$kg kg"
                                            } else {
                                                "${item.quantity} g"
                                            }
                                            totalVal = (item.quantity / 1000.0) * item.price
                                            unitLabel = "kg"
                                        }
                                        else -> {
                                            qtyDisplay = item.quantity.toString()
                                            totalVal = item.price * item.quantity
                                            unitLabel = product.unit.lowercase().replace(" (pcs)", "").replace("piece", "pc")
                                        }
                                    }
                                } else {
                                    qtyDisplay = item.quantity.toString()
                                    totalVal = item.price * item.quantity
                                    unitLabel = "unit"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "$qtyDisplay x ${CurrencyFormatter.format(item.price, localCurrencyMode, usdExchangeRate)} per $unitLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.format(totalVal, localCurrencyMode, usdExchangeRate),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }

                        // Summary pricing card
                        item {
                            if (transaction != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Subtotal:", style = MaterialTheme.typography.bodySmall)
                                            Text(CurrencyFormatter.format(transaction.subtotal, localCurrencyMode, usdExchangeRate), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Discount Applied:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                            Text("-" + CurrencyFormatter.format(transaction.discount, localCurrencyMode, usdExchangeRate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("VAT (8% Sales Tax):", style = MaterialTheme.typography.bodySmall)
                                            Text(CurrencyFormatter.format(transaction.tax, localCurrencyMode, usdExchangeRate), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("GRAND TOTAL:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
                                            Text(
                                                text = CurrencyFormatter.format(transaction.total, localCurrencyMode, usdExchangeRate),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Footer with multiple buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: PDF share / Email
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                invoice.pdfPath?.let {
                                    sharePdfFile(context, it, invoice.invoiceNumber)
                                } ?: Toast.makeText(context, "PDF Invoice not found.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val emailAddress = customer?.email ?: ""
                                invoice.pdfPath?.let {
                                    emailPdfFile(context, emailAddress, it, invoice.invoiceNumber)
                                } ?: Toast.makeText(context, "PDF Invoice not found.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Email", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email PDF", fontSize = 11.sp)
                        }
                    }

                    // Row 2: WhatsApp Click-to-Chat (Option A) & WhatsApp Cloud API (Option B)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // WhatsApp Click-to-Chat button
                        Button(
                            onClick = {
                                if (customer?.phone.isNullOrBlank()) {
                                    Toast.makeText(context, "Customer phone number is missing.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val link = WhatsAppHelper.getClickToChatLink(
                                        phone = customer!!.phone,
                                        invoiceNumber = invoice.invoiceNumber,
                                        customerName = customer.name,
                                        totalLkr = invoice.amountLkr,
                                        currency = localCurrencyMode,
                                        exchangeRate = usdExchangeRate,
                                        paymentStatus = invoice.paymentStatus
                                    )
                                    onLogWhatsAppClickToChat(invoice.invoiceNumber, customer.phone, invoice.amountLkr)
                                    
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open WhatsApp. Log saved.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "WhatsApp Chat", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp Link", fontSize = 11.sp)
                        }

                        // WhatsApp Cloud API button
                        Button(
                            onClick = {
                                if (customer?.phone.isNullOrBlank()) {
                                    Toast.makeText(context, "Customer phone number is missing.", Toast.LENGTH_SHORT).show()
                                } else {
                                    isSendingWhatsAppCloud = true
                                    onSendWhatsAppCloudApi(
                                        customer!!.phone,
                                        invoice.invoiceNumber,
                                        customer.name,
                                        invoice.amountLkr,
                                        localCurrencyMode,
                                        usdExchangeRate,
                                        invoice.paymentStatus
                                    ) { success, msg ->
                                        isSendingWhatsAppCloud = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isSendingWhatsAppCloud,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isSendingWhatsAppCloud) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = "Send API", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp API", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun sharePdfFile(context: Context, pdfPath: String, invoiceNumber: String) {
    val file = File(pdfPath)
    if (!file.exists()) {
        Toast.makeText(context, "Invoice PDF file not found.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ceyvana POS Invoice: $invoiceNumber")
            putExtra(Intent.EXTRA_TEXT, "Hello,\n\nPlease find attached the invoice $invoiceNumber from Ceyvana POS.\n\nThank you!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Invoice PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun emailPdfFile(context: Context, emailAddress: String, pdfPath: String, invoiceNumber: String) {
    val file = File(pdfPath)
    if (!file.exists()) {
        Toast.makeText(context, "Invoice PDF file not found.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            putExtra(Intent.EXTRA_SUBJECT, "Invoice $invoiceNumber - Ceyvana Professional POS")
            putExtra(Intent.EXTRA_TEXT, "Hello,\n\nPlease find attached your PDF invoice ($invoiceNumber) for your recent purchase at Ceyvana.\n\nThank you for your business!\nCeyvana POS Team.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Send Invoice via Email"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error emailing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

