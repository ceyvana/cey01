package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.Category
import com.example.data.database.Customer
import com.example.data.database.Product
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    products: List<Product>,
    categories: List<Category>,
    customers: List<Customer>,
    cart: Map<Product, Int>,
    selectedCustomer: Customer?,
    discountPercentage: Double,
    selectedCurrency: String,
    usdExchangeRate: Double,
    onAddToCart: (Product) -> Unit,
    onRemoveFromCart: (Product) -> Unit,
    onUpdateCartQty: (Product, Int) -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onSetDiscount: (Double) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (String, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var barcodeInput by remember { mutableStateOf("") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var activePaymentMethod by remember { mutableStateOf("Cash") }
    var showCustomerSelector by remember { mutableStateOf(false) }

    // Weight modal state variables
    var weightProductToAdd by remember { mutableStateOf<Product?>(null) }
    var weightInput by remember { mutableStateOf("") }
    var weightUnit by remember { mutableStateOf("kg") }
    var weightUnitMenuExpanded by remember { mutableStateOf(false) }

    // Packet modal state variables
    var packetProductToAdd by remember { mutableStateOf<Product?>(null) }
    var sellByOption by remember { mutableStateOf("Packet") } // "Packet" or "Weight"
    var packetQtyInput by remember { mutableStateOf("1") }
    var packetWeightInputForSale by remember { mutableStateOf("") }
    var packetWeightUnitForSale by remember { mutableStateOf("g") }
    var packetWeightUnitForSaleExpanded by remember { mutableStateOf(false) }

    // Cash Tendered calculator state
    var cashTendered by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Filter products based on search and category
    val filteredProducts = products.filter { prod ->
        val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || prod.category.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    // Calculations
    val subtotal = cart.entries.sumOf { entry ->
        val prod = entry.key
        val qty = entry.value
        when {
            prod.isWeightBased -> prod.price * (qty / 1000.0)
            prod.unitType == "Packet" -> {
                val packWeightG = prod.getPacketWeightInGrams()
                val packets = if (packWeightG > 0) qty / packWeightG else 0.0
                prod.price * packets
            }
            else -> prod.price * qty
        }
    }
    val discountAmt = subtotal * (discountPercentage / 100.0)
    val taxAmt = (subtotal - discountAmt) * 0.08 // 8% sales tax
    val finalTotal = subtotal - discountAmt + taxAmt

    // Simulation of barcode scan
    fun handleBarcodeScan() {
        val found = products.find { it.sku.equals(barcodeInput.trim(), ignoreCase = true) }
        if (found != null) {
            if (found.isWeightBased) {
                weightProductToAdd = found
                weightInput = ""
                weightUnit = "kg"
            } else if (found.unitType == "Packet") {
                packetProductToAdd = found
                sellByOption = "Packet"
                packetQtyInput = "1"
                packetWeightInputForSale = ""
                packetWeightUnitForSale = "g"
            } else {
                onAddToCart(found)
                Toast.makeText(context, "Added ${found.name} to cart", Toast.LENGTH_SHORT).show()
            }
            barcodeInput = ""
        } else {
            Toast.makeText(context, "SKU/Barcode not found!", Toast.LENGTH_SHORT).show()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWide = maxWidth > 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT COLUMN (or Main area for compact): Catalog
            Column(
                modifier = Modifier
                    .weight(if (isWide) 1.2f else 1f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                // Search & Scanner Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name/SKU...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pos_search_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )

                    // Barcode Simulator
                    TextField(
                        value = barcodeInput,
                        onValueChange = { barcodeInput = it },
                        placeholder = { Text("Barcode lookup") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        trailingIcon = {
                            IconButton(onClick = { handleBarcodeScan() }) {
                                Icon(Icons.Default.Add, contentDescription = "Lookup")
                            }
                        },
                        modifier = Modifier
                            .width(160.dp)
                            .testTag("pos_barcode_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category filter row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allCats = listOf("All") + categories.map { it.name }
                    allCats.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("category_chip_$cat")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Catalog Grid
                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching products found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProducts) { product ->
                            ProductGridItem(
                                product = product,
                                currency = selectedCurrency,
                                exchangeRate = usdExchangeRate,
                                onAdd = {
                                    if (product.isWeightBased) {
                                        weightProductToAdd = product
                                        weightInput = ""
                                        weightUnit = "kg"
                                    } else if (product.unitType == "Packet") {
                                        packetProductToAdd = product
                                        sellByOption = "Packet"
                                        packetQtyInput = "1"
                                        packetWeightInputForSale = ""
                                        packetWeightUnitForSale = "g"
                                    } else {
                                        onAddToCart(product)
                                    }
                                }
                            )
                        }
                    }
                }

                // If not wide, show Cart drawer/floating indicator at bottom
                if (!isWide && cart.isNotEmpty()) {
                    Button(
                        onClick = { showCheckoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("mobile_cart_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Cart (${cart.values.sum()} items) • ${CurrencyFormatter.format(finalTotal, selectedCurrency, usdExchangeRate)}")
                    }
                }
            }

            // RIGHT COLUMN (Only if wide display): Live Cart Panel
            if (isWide) {
                Card(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Current Basket",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Customer Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                .clickable { showCustomerSelector = true }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = "Customer", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedCustomer?.name ?: "Select Customer (CRM)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            if (selectedCustomer != null) {
                                IconButton(
                                    onClick = { onSelectCustomer(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Select", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Cart item list
                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Basket is empty",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(cart.entries.toList()) { entry ->
                                    CartListItem(
                                        product = entry.key,
                                        quantity = entry.value,
                                        currency = selectedCurrency,
                                        exchangeRate = usdExchangeRate,
                                        onIncrease = {
                                            val step = if (entry.key.unitType == "Packet") {
                                                entry.key.getPacketWeightInGrams().toInt().coerceAtLeast(1)
                                            } else if (entry.key.isWeightBased) {
                                                250
                                            } else {
                                                1
                                            }
                                            val newQty = entry.value + step
                                            if (newQty <= entry.key.stock) {
                                                onUpdateCartQty(entry.key, newQty)
                                            } else {
                                                Toast.makeText(context, "Insufficient stock!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onDecrease = {
                                            val step = if (entry.key.unitType == "Packet") {
                                                entry.key.getPacketWeightInGrams().toInt().coerceAtLeast(1)
                                            } else if (entry.key.isWeightBased) {
                                                250
                                            } else {
                                                1
                                            }
                                            val newQty = entry.value - step
                                            if (newQty <= 0) {
                                                onRemoveFromCart(entry.key)
                                            } else {
                                                onUpdateCartQty(entry.key, newQty)
                                            }
                                        },
                                        onRemove = { onRemoveFromCart(entry.key) }
                                    )
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Discount and pricing inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Discount:", style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(0.0, 5.0, 10.0, 15.0).forEach { disc ->
                                    FilterChip(
                                        selected = discountPercentage == disc,
                                        onClick = { onSetDiscount(disc) },
                                        label = { Text("${disc.toInt()}%") }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Summaries
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(CurrencyFormatter.format(subtotal, selectedCurrency, usdExchangeRate), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (discountPercentage > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount (${discountPercentage}%):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                Text("-${CurrencyFormatter.format(discountAmt, selectedCurrency, usdExchangeRate)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sales Tax (8%):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(CurrencyFormatter.format(taxAmt, selectedCurrency, usdExchangeRate), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(CurrencyFormatter.format(finalTotal, selectedCurrency, usdExchangeRate), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Complete buttons
                        Button(
                            onClick = { showCheckoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("checkout_btn"),
                            enabled = cart.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Complete")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay & Complete", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // CUSTOMER SELECTOR DIALOG
    if (showCustomerSelector) {
        Dialog(onDismissRequest = { showCustomerSelector = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Attach CRM Customer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .height(260.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(customers) { cust ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectCustomer(cust)
                                        showCustomerSelector = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.Bold)
                                    Text("Points: ${cust.loyaltyPoints} | ${cust.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Icon(Icons.Default.Check, contentDescription = "Select", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showCustomerSelector = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // WEIGHT-BASED PRODUCT SELECTION DIALOG
    if (weightProductToAdd != null) {
        val prod = weightProductToAdd!!
        val maxStockGrams = prod.stock
        val parsedGrams = parseWeightToGrams(weightInput, weightUnit) ?: 0
        val isValid = parsedGrams > 0 && parsedGrams <= maxStockGrams
        val calculatedPrice = prod.price * (parsedGrams / 1000.0)

        Dialog(onDismissRequest = { weightProductToAdd = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sell Weight-Based Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = prod.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Stock display
                    val stockStr = if (prod.stock >= 1000) "${prod.stock / 1000.0} kg" else "${prod.stock} g"
                    Text(
                        text = "Available Stock: $stockStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Unit Price: ${CurrencyFormatter.format(prod.price, selectedCurrency, usdExchangeRate)} / kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Input fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Enter Weight (e.g. 1.5, 500)") },
                            modifier = Modifier.weight(1f).testTag("weight_qty_input"),
                            singleLine = true
                        )

                        // Unit Selector Dropdown
                        Box {
                            OutlinedButton(
                                onClick = { weightUnitMenuExpanded = true },
                                modifier = Modifier.testTag("pos_unit_selector_btn")
                            ) {
                                Text(weightUnit.uppercase())
                            }
                            DropdownMenu(
                                expanded = weightUnitMenuExpanded,
                                onDismissRequest = { weightUnitMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Kilogram (kg)") },
                                    onClick = {
                                        weightUnit = "kg"
                                        weightUnitMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gram (g)") },
                                    onClick = {
                                        weightUnit = "g"
                                        weightUnitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real-time conversion display
                    if (weightInput.isNotEmpty()) {
                        val displayWeightStr = if (parsedGrams >= 1000) "${parsedGrams / 1000.0} kg" else "$parsedGrams g"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "REAL-TIME CONVERSION & PRICING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Weight to sell: $parsedGrams grams ($displayWeightStr)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Item Total: ${CurrencyFormatter.format(calculatedPrice, selectedCurrency, usdExchangeRate)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (parsedGrams > maxStockGrams) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Insufficient stock! Only $stockStr available.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { weightProductToAdd = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (isValid) {
                                    onUpdateCartQty(prod, (cart[prod] ?: 0) + parsedGrams)
                                    weightProductToAdd = null
                                } else {
                                    Toast.makeText(context, "Please enter a valid weight within available stock!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isValid,
                            modifier = Modifier.weight(1f).testTag("add_weight_to_cart_btn")
                        ) {
                            Text("Add to Basket")
                        }
                    }
                }
            }
        }
    }

    // PACKET-BASED PRODUCT SELECTION DIALOG
    if (packetProductToAdd != null) {
        val prod = packetProductToAdd!!
        val maxStockGrams = prod.stock
        val packWeightG = prod.getPacketWeightInGrams()
        
        val parsedGramsToSell = if (sellByOption == "Packet") {
            val qtyPackets = packetQtyInput.toDoubleOrNull() ?: 0.0
            (qtyPackets * packWeightG).toInt()
        } else {
            parseWeightToGrams(packetWeightInputForSale, packetWeightUnitForSale) ?: 0
        }
        
        val isValid = parsedGramsToSell > 0 && parsedGramsToSell <= maxStockGrams
        
        // Calculate price
        val pricePerGram = if (packWeightG > 0) prod.price / packWeightG else 0.0
        val calculatedPrice = pricePerGram * parsedGramsToSell
        
        Dialog(onDismissRequest = { packetProductToAdd = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sell Packet-Based Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = prod.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Display current stock
                    val availablePackets = if (packWeightG > 0) prod.stock / packWeightG else 0.0
                    val availPacketsStr = if (availablePackets % 1.0 == 0.0) "${availablePackets.toInt()}" else String.format(java.util.Locale.US, "%.1f", availablePackets)
                    val availKg = prod.stock / 1000.0
                    Text(
                        text = "Available Stock: $availPacketsStr Packets ($availKg kg)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Price per Packet: ${CurrencyFormatter.format(prod.price, selectedCurrency, usdExchangeRate)} (${prod.packetWeight} ${prod.packetWeightUnit} per pkt)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Selling Option Selection Tabs (Packet vs Weight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Packet", "Weight").forEach { option ->
                            val isSel = sellByOption == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { sellByOption = option }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (option == "Packet") "Packets Quantity" else "Grams / Weight",
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Input fields based on option
                    if (sellByOption == "Packet") {
                        OutlinedTextField(
                            value = packetQtyInput,
                            onValueChange = { packetQtyInput = it },
                            label = { Text("Specify Packet Quantity") },
                            modifier = Modifier.fillMaxWidth().testTag("packet_qty_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = packetWeightInputForSale,
                                onValueChange = { packetWeightInputForSale = it },
                                label = { Text(if (packetWeightUnitForSale == "g") "Specify Gram Quantity (g)" else "Specify Kilogram Quantity (kg)") },
                                modifier = Modifier.weight(1f).testTag("packet_weight_qty_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Box {
                                OutlinedButton(
                                    onClick = { packetWeightUnitForSaleExpanded = true },
                                    modifier = Modifier.testTag("packet_sale_unit_btn")
                                ) {
                                    Text(packetWeightUnitForSale.uppercase())
                                }
                                DropdownMenu(
                                    expanded = packetWeightUnitForSaleExpanded,
                                    onDismissRequest = { packetWeightUnitForSaleExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Grams (g)") },
                                        onClick = {
                                            packetWeightUnitForSale = "g"
                                            packetWeightUnitForSaleExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Kilograms (kg)") },
                                        onClick = {
                                            packetWeightUnitForSale = "kg"
                                            packetWeightUnitForSaleExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Live calculation display
                    if (parsedGramsToSell > 0) {
                        val soldPackets = parsedGramsToSell / packWeightG
                        val soldPacketsStr = if (soldPackets % 1.0 == 0.0) "${soldPackets.toInt()}" else String.format(java.util.Locale.US, "%.2f", soldPackets)
                        val soldKgStr = String.format(java.util.Locale.US, "%.3f", parsedGramsToSell / 1000.0)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "LIVE QUANTITY & PRICING PREVIEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Selling weight: $parsedGramsToSell g ($soldKgStr kg)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Equivalent Packets: $soldPacketsStr packets",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Total Price: ${CurrencyFormatter.format(calculatedPrice, selectedCurrency, usdExchangeRate)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                if (parsedGramsToSell > maxStockGrams) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Insufficient stock! Only $availPacketsStr Packets available.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { packetProductToAdd = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (isValid) {
                                    onUpdateCartQty(prod, (cart[prod] ?: 0) + parsedGramsToSell)
                                    packetProductToAdd = null
                                } else {
                                    Toast.makeText(context, "Please enter a valid quantity within available stock!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isValid,
                            modifier = Modifier.weight(1f).testTag("add_packet_to_cart_btn")
                        ) {
                            Text("Add to Basket")
                        }
                    }
                }
            }
        }
    }

    // CHECKOUT DRAWER / MODAL DIALOG
    if (showCheckoutDialog) {
        Dialog(onDismissRequest = { showCheckoutDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "POS Quick Billing Checkout",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Payment method grid
                    Text("Select Payment Method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "Card", "QR", "Bank Transfer").forEach { method ->
                            Button(
                                onClick = { activePaymentMethod = method },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activePaymentMethod == method) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (activePaymentMethod == method) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(method, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // If Cash is selected, show calculator
                    if (activePaymentMethod == "Cash") {
                        Text("Cash Calculator ($selectedCurrency)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextField(
                                value = cashTendered,
                                onValueChange = { cashTendered = it },
                                placeholder = { Text("Tendered Cash amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            val tenderedVal = cashTendered.toDoubleOrNull() ?: 0.0
                            val finalTotalConverted = if (selectedCurrency == "USD") finalTotal / usdExchangeRate else finalTotal
                            val change = if (tenderedVal >= finalTotalConverted) tenderedVal - finalTotalConverted else 0.0
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = "Change: ${CurrencyFormatter.format(if (selectedCurrency == "USD") change * usdExchangeRate else change, selectedCurrency, usdExchangeRate)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Total summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Amount Due:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(CurrencyFormatter.format(finalTotal, selectedCurrency, usdExchangeRate), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCheckoutDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onCheckout(activePaymentMethod) { success ->
                                    if (success) {
                                        showCheckoutDialog = false
                                        showReceiptDialog = true
                                    } else {
                                        Toast.makeText(context, "Error saving sale!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Print & Complete")
                        }
                    }
                }
            }
        }
    }

    // 58MM / 80MM RECEIPT VIEW SIMULATION DIALOG
    if (showReceiptDialog) {
        Dialog(onDismissRequest = { showReceiptDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Thermal printer receipt header
                    Icon(Icons.Default.Check, contentDescription = "Check", tint = Color.Green, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "GLOBAL EXPORT POS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Colombo Spice & Gemstone Center",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Colombo 03, Sri Lanka",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "-".repeat(32),
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )

                    // Receipt Metadata
                    val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("DATE: $nowStr", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                        Text("TXN: #${(10000..99999).random()}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                        Text("CUST: ${selectedCustomer?.name ?: "Walk-in Customer"}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                        Text("METHOD: $activePaymentMethod", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                    }

                    Text(
                        text = "-".repeat(32),
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )

                    // Items list
                    cart.forEach { (prod, qty) ->
                        val qtyDisplay = when {
                            prod.isWeightBased -> {
                                if (qty >= 1000) "${qty / 1000.0} kg" else "${qty} g"
                            }
                            prod.unitType == "Packet" -> {
                                val packWeightG = prod.getPacketWeightInGrams()
                                val packets = if (packWeightG > 0) qty / packWeightG else 0.0
                                val packetsStr = if (packets % 1.0 == 0.0) "${packets.toInt()}" else String.format(java.util.Locale.US, "%.1f", packets)
                                "$packetsStr pkt ($qty g)"
                            }
                            else -> "$qty"
                        }
                        val itemTotal = when {
                            prod.isWeightBased -> prod.price * (qty / 1000.0)
                            prod.unitType == "Packet" -> {
                                val packWeightG = prod.getPacketWeightInGrams()
                                val packets = if (packWeightG > 0) qty / packWeightG else 0.0
                                prod.price * packets
                            }
                            else -> prod.price * qty
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${prod.name.take(16)} x$qtyDisplay",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.Black
                            )
                            Text(
                                text = CurrencyFormatter.format(itemTotal, selectedCurrency, usdExchangeRate),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.Black
                            )
                        }
                    }

                    Text(
                        text = "-".repeat(32),
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )

                    // Totals
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SUBTOTAL:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                        Text(CurrencyFormatter.format(subtotal, selectedCurrency, usdExchangeRate), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                    }
                    if (discountPercentage > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DISC (${discountPercentage}%):", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                            Text("-${CurrencyFormatter.format(discountAmt, selectedCurrency, usdExchangeRate)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TAX (8%):", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                        Text(CurrencyFormatter.format(taxAmt, selectedCurrency, usdExchangeRate), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                        Text(CurrencyFormatter.format(finalTotal, selectedCurrency, usdExchangeRate), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                    }

                    Text(
                        text = "=".repeat(32),
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )

                    // Printed QR Code indicator for e-invoicing compliance
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .border(1.dp, Color.Black)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing mock QR Code lines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(color = Color.Black, size = size, alpha = 0.8f)
                            drawRect(color = Color.White, topLeft = Offset(10f, 10f), size = size * 0.7f)
                            drawRect(color = Color.Black, topLeft = Offset(18f, 18f), size = size * 0.4f)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SCAN FOR FISCAL VERIFICATION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showReceiptDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridItem(product: Product, currency: String, exchangeRate: Double, onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (product.stock > 0) onAdd() }
            .testTag("product_item_${product.sku}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = product.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Low Stock Marker
                if (product.stock <= product.lowStockThreshold) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFE6E6), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LOW",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "SKU: ${product.sku}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val priceLabel = when {
                    product.unitType == "Packet" -> "${CurrencyFormatter.format(product.price, currency, exchangeRate)}/pkt"
                    product.isWeightBased -> "${CurrencyFormatter.format(product.price, currency, exchangeRate)}/kg"
                    else -> {
                        val formattedUnit = product.unit.lowercase().replace(" (pcs)", "").replace("piece", "pc")
                        "${CurrencyFormatter.format(product.price, currency, exchangeRate)}/$formattedUnit"
                    }
                }
                Text(
                    text = priceLabel,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )

                val stockStr = product.getStockDisplay()
                Text(
                    text = "Stock: $stockStr",
                    fontSize = 11.sp,
                    color = if (product.stock > 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else Color.Red,
                    fontWeight = if (product.stock == 0) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun CartListItem(
    product: Product,
    quantity: Int,
    currency: String,
    exchangeRate: Double,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val unitLabel = when {
                product.unitType == "Packet" -> "per pkt"
                product.isWeightBased -> "per kg"
                else -> "per ${product.unit.lowercase().replace(" (pcs)", "")}"
            }
            Text(
                text = "${CurrencyFormatter.format(product.price, currency, exchangeRate)} $unitLabel",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp)) {
                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            val qtyStr = when {
                product.unitType == "Packet" -> {
                    val packWeightG = product.getPacketWeightInGrams()
                    val packets = if (packWeightG > 0) quantity / packWeightG else 0.0
                    val packetsStr = if (packets % 1.0 == 0.0) "${packets.toInt()}" else String.format(java.util.Locale.US, "%.1f", packets)
                    "$packetsStr pkt ($quantity g)"
                }
                product.isWeightBased -> {
                    if (quantity >= 1000) "${quantity / 1000.0} kg" else "${quantity} g"
                }
                else -> {
                    val formattedUnit = product.unit.lowercase().replace(" (pcs)", "").replace("piece", "pcs")
                    "$quantity $formattedUnit"
                }
            }
            Text(
                text = qtyStr,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.widthIn(min = 45.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onIncrease,
                modifier = Modifier.size(28.dp),
                enabled = quantity < product.stock
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
        }
    }
}
