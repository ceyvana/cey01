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
    onAddProduct: (String, String, Double, Double, Int, Int, String, String) -> Unit,
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

    val context = LocalContext.current

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.sku.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }

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
                            onUpdateStock = { qty -> onUpdateStock(product.id, qty) },
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
                                label = { Text("Price ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("add_prod_price"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newCost,
                                onValueChange = { newCost = it },
                                label = { Text("Cost ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = newStock,
                                onValueChange = { newStock = it },
                                label = { Text("Initial Stock") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("add_prod_stock"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newThreshold,
                                onValueChange = { newThreshold = it },
                                label = { Text("Low Limit") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
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
                                    val stockVal = newStock.toIntOrNull()
                                    val limitVal = newThreshold.toIntOrNull() ?: 5

                                    if (newName.isNotBlank() && newSku.isNotBlank() && priceVal != null && costVal != null && stockVal != null) {
                                        onAddProduct(
                                            newName, newSku, priceVal, costVal, stockVal, limitVal, newCategory, newBrand
                                        )
                                        // Clear states
                                        newName = ""
                                        newSku = ""
                                        newPrice = ""
                                        newCost = ""
                                        newStock = ""
                                        newThreshold = "5"
                                        showAddDialog = false
                                        Toast.makeText(context, "Product saved successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please complete all fields with correct numbers!", Toast.LENGTH_SHORT).show()
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
    }
}

@Composable
fun InventoryRowItem(
    product: Product,
    onUpdateStock: (Int) -> Unit,
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

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
                    Text(text = "Price: $${String.format("%.2f", product.price)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "Cost: $${String.format("%.2f", product.costPrice)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                // Stock adjusting actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateStock((product.stock - 1).coerceAtLeast(0)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = product.stock.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        modifier = Modifier.width(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(
                        onClick = { onUpdateStock(product.stock + 1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Stock", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
