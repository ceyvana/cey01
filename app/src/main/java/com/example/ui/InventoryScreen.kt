package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.Category
import com.example.data.database.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    products: List<Product>,
    categories: List<Category>,
    selectedCurrency: String,
    usdExchangeRate: Double,
    onAddProduct: (
        name: String,
        sku: String,
        price: Double,
        costPrice: Double,
        stock: Int,
        lowStockThreshold: Int,
        category: String,
        brand: String,
        isWeightBased: Boolean,
        unit: String,
        unitType: String,
        packetWeight: Double,
        packetWeightUnit: String,
        openingStock: Int,
        totalWeightInGrams: Int
    ) -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Int) -> Unit,
    onUpdateStock: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog state
    var newName by remember { mutableStateOf("") }
    var newSku by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newCost by remember { mutableStateOf("") }
    var newStock by remember { mutableStateOf("") }
    var newThreshold by remember { mutableStateOf("5") }
    var newCategory by remember { mutableStateOf("General") }
    var newBrand by remember { mutableStateOf("Generic") }
    var selectedUnit by remember { mutableStateOf("Piece") }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    // Packet fields
    var newPacketWeight by remember { mutableStateOf("") }
    var newPacketWeightUnit by remember { mutableStateOf("g") }
    var packetWeightUnitExpanded by remember { mutableStateOf(false) }

    // Edit state
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    val context = LocalContext.current

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.sku.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }

    val isWeightBased = (selectedUnit == "Gram (g)" || selectedUnit == "Kilogram (kg)")
    val isPacket = (selectedUnit == "Packet")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter products by name/SKU/category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_search"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Inventory List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No products found in database.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts) { product ->
                        InventoryRowItem(
                            product = product,
                            currency = selectedCurrency,
                            exchangeRate = usdExchangeRate,
                            onUpdateStock = { qty -> onUpdateStock(product.id, qty) },
                            onEdit = { editingProduct = product },
                            onDelete = { onDeleteProduct(product.id) }
                        )
                    }
                }
            }
        }

        // Add Product FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_product_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }

        // ADD PRODUCT DIALOG
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Add Enterprise Product",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Product Name") },
                            modifier = Modifier.fillMaxWidth().testTag("add_prod_name"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = newSku,
                                onValueChange = { newSku = it },
                                label = { Text("SKU / Barcode") },
                                modifier = Modifier.weight(1f).testTag("add_prod_sku"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newBrand,
                                onValueChange = { newBrand = it },
                                label = { Text("Brand") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = newPrice,
                                onValueChange = { newPrice = it },
                                label = { Text("Price ($selectedCurrency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("add_prod_price"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newCost,
                                onValueChange = { newCost = it },
                                label = { Text("Cost ($selectedCurrency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Unit Selection Dropdown (ALWAYS visible)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Unit of Measurement", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Box {
                                OutlinedButton(
                                    onClick = { unitMenuExpanded = true },
                                    modifier = Modifier.testTag("unit_selector_dropdown_btn")
                                ) {
                                    Text(selectedUnit)
                                }
                                DropdownMenu(
                                    expanded = unitMenuExpanded,
                                    onDismissRequest = { unitMenuExpanded = false }
                                ) {
                                    listOf("Packet", "Gram (g)", "Kilogram (kg)", "Piece", "Bottle", "Box").forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit) },
                                            onClick = {
                                                selectedUnit = unit
                                                unitMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Conditional Packet-based input fields
                        if (isPacket) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPacketWeight,
                                    onValueChange = { newPacketWeight = it },
                                    label = { Text("Weight per Packet") },
                                    modifier = Modifier.weight(1f).testTag("add_prod_packet_weight"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { packetWeightUnitExpanded = true },
                                        modifier = Modifier.fillMaxWidth().testTag("add_prod_packet_weight_unit_btn")
                                    ) {
                                        Text("Unit: $newPacketWeightUnit")
                                    }
                                    DropdownMenu(
                                        expanded = packetWeightUnitExpanded,
                                        onDismissRequest = { packetWeightUnitExpanded = false }
                                    ) {
                                        listOf("g", "kg").forEach { unit ->
                                            DropdownMenuItem(
                                                text = { Text(unit) },
                                                onClick = {
                                                    newPacketWeightUnit = unit
                                                    packetWeightUnitExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val stockLabel = if (isPacket) {
                                "Opening Stock (Packets)"
                            } else if (isWeightBased) {
                                if (selectedUnit == "Kilogram (kg)") "Opening Stock (kg)" else "Opening Stock (g)"
                            } else {
                                "Opening Stock"
                            }
                            val limitLabel = if (isPacket) {
                                "Low Limit (Packets)"
                            } else if (isWeightBased) {
                                if (selectedUnit == "Kilogram (kg)") "Low Limit (kg)" else "Low Limit (g)"
                            } else {
                                "Low Limit"
                            }
                            OutlinedTextField(
                                value = newStock,
                                onValueChange = { newStock = it },
                                label = { Text(stockLabel) },
                                modifier = Modifier.weight(1f).testTag("add_prod_stock"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = newThreshold,
                                onValueChange = { newThreshold = it },
                                label = { Text(limitLabel) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-time weight calculations display
                        if (isPacket) {
                            val pWeight = newPacketWeight.toDoubleOrNull() ?: 0.0
                            val pQty = newStock.toIntOrNull() ?: 0
                            val totalGrams = if (newPacketWeightUnit == "kg") {
                                pQty * pWeight * 1000.0
                            } else {
                                pQty * pWeight
                            }
                            val totalKg = totalGrams / 1000.0
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "AUTOMATIC WEIGHT CALCULATION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Total Weight: ${totalGrams.toInt()} g ($totalKg kg)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (isWeightBased) {
                            val defaultUnit = if (selectedUnit == "Kilogram (kg)") "kg" else "g"
                            val parsedStockGrams = parseWeightToGrams(newStock, defaultUnit)
                            val parsedThresholdGrams = parseWeightToGrams(newThreshold, defaultUnit)

                            Spacer(modifier = Modifier.height(6.dp))
                            if (parsedStockGrams != null) {
                                Text(
                                    text = "Real-time Conversion: $parsedStockGrams grams (${parsedStockGrams / 1000.0} kg)",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("real_time_conversion_display")
                                )
                            }
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "STOCK PREVIEW BEFORE SAVING",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Stock to Save: ${parsedStockGrams ?: 0} g (${(parsedStockGrams ?: 0) / 1000.0} kg)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Low Limit: ${parsedThresholdGrams ?: 0} g (${(parsedThresholdGrams ?: 0) / 1000.0} kg)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Category dropdown simulator using chips
                        Text("Category Selection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val availableCats = listOf("Spices", "Tea", "Gemstones", "General")
                            availableCats.forEach { cat ->
                                FilterChip(
                                    selected = newCategory == cat,
                                    onClick = { newCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val priceVal = newPrice.toDoubleOrNull()
                                    val costVal = newCost.toDoubleOrNull()
                                    
                                    val stockVal: Int?
                                    val limitVal: Int
                                    val totalGrams: Int

                                    if (isPacket) {
                                        val pWeight = newPacketWeight.toDoubleOrNull() ?: 0.0
                                        val pQty = newStock.toIntOrNull() ?: 0
                                        totalGrams = if (newPacketWeightUnit == "kg") {
                                            (pQty * pWeight * 1000.0).toInt()
                                        } else {
                                            (pQty * pWeight).toInt()
                                        }
                                        stockVal = totalGrams
                                        val limitPackets = newThreshold.toIntOrNull() ?: 5
                                        limitVal = if (newPacketWeightUnit == "kg") {
                                            (limitPackets * pWeight * 1000.0).toInt()
                                        } else {
                                            (limitPackets * pWeight).toInt()
                                        }
                                    } else if (isWeightBased) {
                                        val defaultUnit = if (selectedUnit == "Kilogram (kg)") "kg" else "g"
                                        stockVal = parseWeightToGrams(newStock, defaultUnit)
                                        totalGrams = stockVal ?: 0
                                        limitVal = parseWeightToGrams(newThreshold, defaultUnit) ?: 5000
                                    } else {
                                        stockVal = newStock.toIntOrNull()
                                        totalGrams = 0
                                        limitVal = newThreshold.toIntOrNull() ?: 5
                                    }

                                    if (newName.isNotBlank() && newSku.isNotBlank() && priceVal != null && costVal != null && stockVal != null) {
                                        val lkrPrice = if (selectedCurrency == "USD") priceVal * usdExchangeRate else priceVal
                                        val lkrCost = if (selectedCurrency == "USD") costVal * usdExchangeRate else costVal
                                        onAddProduct(
                                            newName,
                                            newSku,
                                            lkrPrice,
                                            lkrCost,
                                            stockVal,
                                            limitVal,
                                            newCategory,
                                            newBrand,
                                            isWeightBased,
                                            selectedUnit,
                                            selectedUnit,
                                            if (isPacket) (newPacketWeight.toDoubleOrNull() ?: 0.0) else 0.0,
                                            if (isPacket) newPacketWeightUnit else "g",
                                            if (isPacket) (newStock.toIntOrNull() ?: 0) else 0,
                                            totalGrams
                                        )
                                        // Clear states
                                        newName = ""
                                        newSku = ""
                                        newPrice = ""
                                        newCost = ""
                                        newStock = ""
                                        newThreshold = "5"
                                        newPacketWeight = ""
                                        newPacketWeightUnit = "g"
                                        selectedUnit = "Piece"
                                        showAddDialog = false
                                        Toast.makeText(context, "Product saved successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please complete all fields with correct numbers or format!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("save_prod_btn")
                            ) {
                                Text("Save Product")
                            }
                        }
                    }
                }
            }
        }

        // EDIT PRODUCT DIALOG
        val editingProductState = editingProduct
        if (editingProductState != null) {
            val prod = editingProductState
            
            // Local state for edit dialog
            var editName by remember(prod) { mutableStateOf(prod.name) }
            var editSku by remember(prod) { mutableStateOf(prod.sku) }
            var editPrice by remember(prod) { mutableStateOf(if (selectedCurrency == "USD") (prod.price / usdExchangeRate).toString() else prod.price.toString()) }
            var editCost by remember(prod) { mutableStateOf(if (selectedCurrency == "USD") (prod.costPrice / usdExchangeRate).toString() else prod.costPrice.toString()) }
            var editUnit by remember(prod) { mutableStateOf(prod.unitType) }
            var editUnitMenuExpanded by remember { mutableStateOf(false) }
            
            val isEditPacket = (editUnit == "Packet")
            val isEditWeightBased = (editUnit == "Gram (g)" || editUnit == "Kilogram (kg)")

            val initialStockStr = if (prod.unitType == "Packet") {
                val pWeightG = prod.getPacketWeightInGrams()
                val packets = if (pWeightG > 0) prod.stock / pWeightG else 0.0
                if (packets % 1.0 == 0.0) packets.toInt().toString() else packets.toString()
            } else {
                getEditStockDisplay(prod.stock, prod.unit)
            }
            
            val initialThresholdStr = if (prod.unitType == "Packet") {
                val pWeightG = prod.getPacketWeightInGrams()
                val packets = if (pWeightG > 0) prod.lowStockThreshold / pWeightG else 0.0
                if (packets % 1.0 == 0.0) packets.toInt().toString() else packets.toString()
            } else {
                getEditThresholdDisplay(prod.lowStockThreshold, prod.unit)
            }
            
            var editStock by remember(prod) { mutableStateOf(initialStockStr) }
            var editThreshold by remember(prod) { mutableStateOf(initialThresholdStr) }
            var editCategory by remember(prod) { mutableStateOf(prod.category) }
            var editBrand by remember(prod) { mutableStateOf(prod.brand) }

            // Packet fields for edit
            var editPacketWeight by remember(prod) { mutableStateOf(if (prod.unitType == "Packet") prod.packetWeight.toString() else "") }
            var editPacketWeightUnit by remember(prod) { mutableStateOf(if (prod.unitType == "Packet") prod.packetWeightUnit else "g") }
            var editPacketWeightUnitExpanded by remember { mutableStateOf(false) }
            
            Dialog(onDismissRequest = { editingProduct = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Edit Product",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        item {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Product Name") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_prod_name"),
                                singleLine = true
                            )
                        }
                        
                        item {
                            OutlinedTextField(
                                value = editSku,
                                onValueChange = { editSku = it },
                                label = { Text("SKU / Barcode") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        
                        item {
                            OutlinedTextField(
                                value = editBrand,
                                onValueChange = { editBrand = it },
                                label = { Text("Brand") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = editPrice,
                                    onValueChange = { editPrice = it },
                                    label = { Text("Price ($selectedCurrency)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editCost,
                                    onValueChange = { editCost = it },
                                    label = { Text("Cost Price ($selectedCurrency)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                        
                        item {
                            // Unit dropdown (ALWAYS visible)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Unit of Measurement", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Box {
                                    OutlinedButton(
                                        onClick = { editUnitMenuExpanded = true },
                                        modifier = Modifier.testTag("edit_unit_selector_btn")
                                    ) {
                                        Text(editUnit)
                                    }
                                    DropdownMenu(
                                        expanded = editUnitMenuExpanded,
                                        onDismissRequest = { editUnitMenuExpanded = false }
                                    ) {
                                        listOf("Packet", "Gram (g)", "Kilogram (kg)", "Piece", "Bottle", "Box").forEach { unit ->
                                            DropdownMenuItem(
                                                text = { Text(unit) },
                                                onClick = {
                                                    editUnit = unit
                                                    editUnitMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Conditional Packet weight fields for edit dialog
                        if (isEditPacket) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = editPacketWeight,
                                        onValueChange = { editPacketWeight = it },
                                        label = { Text("Weight per Packet") },
                                        modifier = Modifier.weight(1f).testTag("edit_prod_packet_weight"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { editPacketWeightUnitExpanded = true },
                                            modifier = Modifier.fillMaxWidth().testTag("edit_prod_packet_weight_unit_btn")
                                        ) {
                                            Text("Unit: $editPacketWeightUnit")
                                        }
                                        DropdownMenu(
                                            expanded = editPacketWeightUnitExpanded,
                                            onDismissRequest = { editPacketWeightUnitExpanded = false }
                                        ) {
                                            listOf("g", "kg").forEach { unit ->
                                                DropdownMenuItem(
                                                    text = { Text(unit) },
                                                    onClick = {
                                                        editPacketWeightUnit = unit
                                                        editPacketWeightUnitExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val stockLabel = if (isEditPacket) {
                                    "Opening Stock (Packets)"
                                } else if (isEditWeightBased) {
                                    if (editUnit == "Kilogram (kg)") "Opening Stock (kg)" else "Opening Stock (g)"
                                } else {
                                    "Opening Stock"
                                }
                                val limitLabel = if (isEditPacket) {
                                    "Low Limit (Packets)"
                                } else if (isEditWeightBased) {
                                    if (editUnit == "Kilogram (kg)") "Low Limit (kg)" else "Low Limit (g)"
                                } else {
                                    "Low Limit"
                                }
                                OutlinedTextField(
                                    value = editStock,
                                    onValueChange = { editStock = it },
                                    label = { Text(stockLabel) },
                                    modifier = Modifier.weight(1f).testTag("edit_prod_stock"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = editThreshold,
                                    onValueChange = { editThreshold = it },
                                    label = { Text(limitLabel) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                        
                        item {
                            // Real-time weight preview for edit
                            if (isEditPacket) {
                                val pWeight = editPacketWeight.toDoubleOrNull() ?: 0.0
                                val pQty = editStock.toIntOrNull() ?: 0
                                val totalGrams = if (editPacketWeightUnit == "kg") {
                                    pQty * pWeight * 1000.0
                                } else {
                                    pQty * pWeight
                                }
                                val totalKg = totalGrams / 1000.0
                                Spacer(modifier = Modifier.height(4.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "AUTOMATIC WEIGHT CALCULATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Total Weight: ${totalGrams.toInt()} g ($totalKg kg)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else if (isEditWeightBased) {
                                val editDefaultUnit = if (editUnit == "Kilogram (kg)") "kg" else "g"
                                val parsedEditStockGrams = parseWeightToGrams(editStock, editDefaultUnit)
                                val parsedEditThresholdGrams = parseWeightToGrams(editThreshold, editDefaultUnit)
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                if (parsedEditStockGrams != null) {
                                    Text(
                                        text = "Real-time Conversion: $parsedEditStockGrams grams (${parsedEditStockGrams / 1000.0} kg)",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "STOCK PREVIEW BEFORE SAVING",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Stock to Save: ${parsedEditStockGrams ?: 0} g (${(parsedEditStockGrams ?: 0) / 1000.0} kg)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Low Limit: ${parsedEditThresholdGrams ?: 0} g (${(parsedEditThresholdGrams ?: 0) / 1000.0} kg)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        item {
                            Text("Category Selection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val editCats = listOf("Spices", "Tea", "Gemstones", "General")
                                editCats.forEach { cat ->
                                    val isSel = editCategory == cat
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { editCategory = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { editingProduct = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        val priceVal = editPrice.toDoubleOrNull()
                                        val costVal = editCost.toDoubleOrNull()
                                        
                                        val stockVal: Int?
                                        val limitVal: Int
                                        val totalGrams: Int

                                        if (isEditPacket) {
                                            val pWeight = editPacketWeight.toDoubleOrNull() ?: 0.0
                                            val pQty = editStock.toIntOrNull() ?: 0
                                            totalGrams = if (editPacketWeightUnit == "kg") {
                                                (pQty * pWeight * 1000.0).toInt()
                                            } else {
                                                (pQty * pWeight).toInt()
                                            }
                                            stockVal = totalGrams
                                            val limitPackets = editThreshold.toIntOrNull() ?: 5
                                            limitVal = if (editPacketWeightUnit == "kg") {
                                                (limitPackets * pWeight * 1000.0).toInt()
                                            } else {
                                                (limitPackets * pWeight).toInt()
                                            }
                                        } else if (isEditWeightBased) {
                                            val editDefaultUnit = if (editUnit == "Kilogram (kg)") "kg" else "g"
                                            stockVal = parseWeightToGrams(editStock, editDefaultUnit)
                                            totalGrams = stockVal ?: 0
                                            limitVal = parseWeightToGrams(editThreshold, editDefaultUnit) ?: 5000
                                        } else {
                                            stockVal = editStock.toIntOrNull()
                                            totalGrams = 0
                                            limitVal = editThreshold.toIntOrNull() ?: 5
                                        }
                                        
                                        if (editName.isNotBlank() && editSku.isNotBlank() && priceVal != null && costVal != null && stockVal != null) {
                                            val lkrPrice = if (selectedCurrency == "USD") priceVal * usdExchangeRate else priceVal
                                            val lkrCost = if (selectedCurrency == "USD") costVal * usdExchangeRate else costVal
                                            
                                            onEditProduct(
                                                prod.copy(
                                                    name = editName,
                                                    sku = editSku,
                                                    brand = editBrand,
                                                    price = lkrPrice,
                                                    costPrice = lkrCost,
                                                    unit = if (isEditPacket) "Packet" else editUnit,
                                                    unitType = editUnit,
                                                    isWeightBased = isEditWeightBased,
                                                    stock = stockVal,
                                                    lowStockThreshold = limitVal,
                                                    category = editCategory,
                                                    packetWeight = if (isEditPacket) (editPacketWeight.toDoubleOrNull() ?: 0.0) else 0.0,
                                                    packetWeightUnit = if (isEditPacket) editPacketWeightUnit else "g",
                                                    openingStock = if (isEditPacket) (editStock.toIntOrNull() ?: 0) else 0,
                                                    totalWeightInGrams = totalGrams
                                                )
                                            )
                                            editingProduct = null
                                            Toast.makeText(context, "Product updated successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Please complete all fields correctly!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("save_edit_prod_btn")
                                ) {
                                    Text("Save Changes")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryRowItem(
    product: Product,
    currency: String,
    exchangeRate: Double,
    onUpdateStock: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SKU: ${product.sku} | Brand: ${product.brand} | Category: ${product.category}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_product_btn_${product.id}")) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock health badge
                val isLow = product.stock <= product.lowStockThreshold
                val isEmpty = product.stock == 0
                val (badgeBg, badgeText, status) = when {
                    isEmpty -> Triple(Color(0xFFFDE8E8), Color(0xFF9B1C1C), "OUT OF STOCK")
                    isLow -> Triple(Color(0xFFFEF08A), Color(0xFF713F12), "LOW STOCK")
                    else -> Triple(Color(0xFFDEF7EC), Color(0xFF03543F), "HEALTHY STOCK")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeText
                    )
                }

                // Pricing
                Column(horizontalAlignment = Alignment.End) {
                    val unitSuffix = when (product.unitType) {
                        "Packet" -> " / pkt"
                        "Gram (g)" -> " / g"
                        "Kilogram (kg)" -> " / kg"
                        else -> " / ${product.unit.lowercase().replace(" (pcs)", "")}"
                    }
                    Text(text = "Price: ${CurrencyFormatter.format(product.price, currency, exchangeRate)}$unitSuffix", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "Cost: ${CurrencyFormatter.format(product.costPrice, currency, exchangeRate)}$unitSuffix", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                // Stock adjusting actions
                val step = if (product.unitType == "Packet") {
                    product.getPacketWeightInGrams().toInt().coerceAtLeast(1)
                } else if (product.isWeightBased) {
                    250
                } else {
                    1
                }
                val stockDisplay = product.getStockDisplay()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateStock((product.stock - step).coerceAtLeast(0)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = stockDisplay,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.widthIn(min = 80.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(
                        onClick = { onUpdateStock(product.stock + step) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Stock", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

fun parseWeightToGrams(input: String, defaultUnit: String): Int? {
    val clean = input.trim().lowercase()
    if (clean.isEmpty()) return null
    if (clean.endsWith("kg") || clean.endsWith("kilograms") || clean.endsWith("kilogram")) {
        val numberPart = clean.replace("kg", "").replace("kilograms", "").replace("kilogram", "").trim()
        val d = numberPart.toDoubleOrNull() ?: return null
        return (d * 1000).toInt()
    }
    if (clean.endsWith("g") || clean.endsWith("grams") || clean.endsWith("gram")) {
        val numberPart = clean.replace("grams", "").replace("gram", "").replace("g", "").trim()
        val d = numberPart.toDoubleOrNull() ?: return null
        return d.toInt()
    }
    val d = clean.toDoubleOrNull() ?: return null
    return if (defaultUnit == "kg") {
        (d * 1000).toInt()
    } else {
        d.toInt()
    }
}

fun formatStockDisplay(stock: Int, unit: String): String {
    return when (unit) {
        "Kilogram (kg)" -> {
            val kg = stock / 1000.0
            if (kg % 1.0 == 0.0) "${kg.toInt()} kg" else "$kg kg"
        }
        "Gram (g)" -> {
            "$stock g"
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

fun getEditStockDisplay(stock: Int, unit: String): String {
    return when (unit) {
        "Kilogram (kg)" -> {
            val kg = stock / 1000.0
            if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()
        }
        "Gram (g)" -> {
            stock.toString()
        }
        else -> {
            stock.toString()
        }
    }
}

fun getEditThresholdDisplay(threshold: Int, unit: String): String {
    return when (unit) {
        "Kilogram (kg)" -> {
            val kg = threshold / 1000.0
            if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()
        }
        "Gram (g)" -> {
            threshold.toString()
        }
        else -> {
            threshold.toString()
        }
    }
}
