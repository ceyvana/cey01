package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.Category
import com.example.data.database.Product
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcommerceScreen(
    products: List<Product>,
    categories: List<Category>,
    cart: Map<Product, Int>,
    selectedCurrency: String,
    usdExchangeRate: Double,
    onAddToCart: (Product) -> Unit,
    onRemoveFromCart: (Product) -> Unit,
    onUpdateCartQty: (Product, Int) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (String, (Boolean, String?) -> Unit) -> Unit,
    onAddProduct: (
        name: String, sku: String, price: Double, costPrice: Double, stock: Int,
        threshold: Int, category: String, brand: String, isWeightBased: Boolean,
        unit: String, unitType: String, packetWeight: Double, packetWeightUnit: String,
        openingStock: Int, totalWeightInGrams: Int, description: String, imageUrl: String,
        shortDescription: String, longDescription: String, specifications: String, features: String,
        ingredients: String, warranty: String, returnPolicy: String, shippingInfo: String,
        careInstructions: String, countryOfOrigin: String,
        wholesalePrice: Double, dealerPrice: Double, vipPrice: Double, bulkPrice: Double, minimumOrderPrice: Double,
        salePrice: Double
    ) -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Navigation and Panels
    var showCartSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showCheckoutSuccessDialog by remember { mutableStateOf(false) }
    var lastGeneratedPdfPath by remember { mutableStateOf<String?>(null) }

    // Filter products
    val filteredProducts = products.filter { prod ->
        val matchesQuery = prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true) ||
                prod.brand.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || prod.category.equals(selectedCategory, ignoreCase = true)
        matchesQuery && matchesCategory
    }

    // Total Cart Count
    val totalCartCount = cart.values.sum()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // High-End Header Banner with subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "E-Shop Terminal",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Browse, manage catalog, and place orders directly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Shopping Bag Icon Button with Badge
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { showCartSheet = true }
                                .testTag("btn_eshop_open_cart"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (totalCartCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = Color.White,
                                            modifier = Modifier.offset(x = 2.dp, y = (-2).dp)
                                        ) {
                                            Text(totalCartCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Cart",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search and Filter Block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search products, SKUs, brands...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("eshop_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Add Product Quick Button
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("btn_eshop_add_product_trigger"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Categories Filter List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allCats = listOf("All") + categories.map { it.name }
                allCats.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("eshop_category_chip_$cat")
                    )
                }
            }

            // Product Grid Content
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No Products Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try clearing search queries or add a new premium product to start selling.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductEcomCard(
                            product = product,
                            selectedCurrency = selectedCurrency,
                            usdExchangeRate = usdExchangeRate,
                            onAddToCart = {
                                onAddToCart(product)
                                Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                            },
                            onClick = { selectedProductForDetail = product }
                        )
                    }
                }
            }
        }

        // Cart Drawer Side Sheet Overlay
        AnimatedVisibility(
            visible = showCartSheet,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
        ) {
            EShopCartPanel(
                cart = cart,
                selectedCurrency = selectedCurrency,
                usdExchangeRate = usdExchangeRate,
                onDismiss = { showCartSheet = false },
                onUpdateQty = onUpdateCartQty,
                onRemoveItem = onRemoveFromCart,
                onClearCart = onClearCart,
                onCheckout = { method ->
                    onCheckout(method) { success, pdfPath ->
                        if (success) {
                            lastGeneratedPdfPath = pdfPath
                            showCartSheet = false
                            showCheckoutSuccessDialog = true
                        } else {
                            Toast.makeText(context, "Failed to complete transaction", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        // Add Product Form Dialog
        if (showAddDialog) {
            ProductFormDialog(
                title = "Add Premium Product",
                categories = categories,
                existingProducts = products,
                onDismiss = { showAddDialog = false },
                onSave = { name, sku, price, cost, stock, threshold, cat, brand, desc, img,
                           sDesc, lDesc, specs, feat, ing, war, ret, ship, care, origin,
                           wholesale, dealer, vip, bulk, minOrder, salePrice ->
                    onAddProduct(
                        name, sku, price, cost, stock, threshold, cat, brand,
                        false, "Piece (pcs)", "Piece", 0.0, "g", stock, 0, desc, img,
                        sDesc, lDesc, specs, feat, ing, war, ret, ship, care, origin,
                        wholesale, dealer, vip, bulk, minOrder, salePrice
                    )
                    showAddDialog = false
                    Toast.makeText(context, "$name successfully created!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Edit Product Form Dialog
        productToEdit?.let { product ->
            ProductFormDialog(
                title = "Edit Product Details",
                categories = categories,
                productToEdit = product,
                existingProducts = products,
                onDismiss = { productToEdit = null },
                onSave = { name, sku, price, cost, stock, threshold, cat, brand, desc, img,
                           sDesc, lDesc, specs, feat, ing, war, ret, ship, care, origin,
                           wholesale, dealer, vip, bulk, minOrder, salePrice ->
                    val updatedProduct = product.copy(
                        name = name,
                        sku = sku,
                        price = price,
                        costPrice = cost,
                        stock = stock,
                        lowStockThreshold = threshold,
                        category = cat,
                        brand = brand,
                        description = desc,
                        imageUrl = img,
                        shortDescription = sDesc,
                        longDescription = lDesc,
                        specifications = specs,
                        features = feat,
                        ingredients = ing,
                        warranty = war,
                        returnPolicy = ret,
                        shippingInfo = ship,
                        careInstructions = care,
                        countryOfOrigin = origin,
                        wholesalePrice = wholesale,
                        dealerPrice = dealer,
                        vipPrice = vip,
                        bulkPrice = bulk,
                        minimumOrderPrice = minOrder,
                        salePrice = salePrice
                    )
                    onEditProduct(updatedProduct)
                    // If the detailed view is showing this product, update it too
                    if (selectedProductForDetail?.id == product.id) {
                        selectedProductForDetail = updatedProduct
                    }
                    productToEdit = null
                    Toast.makeText(context, "Product changes saved!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Product Detailed Showcase Dialog
        selectedProductForDetail?.let { product ->
            ProductDetailDialog(
                product = product,
                selectedCurrency = selectedCurrency,
                usdExchangeRate = usdExchangeRate,
                onDismiss = { selectedProductForDetail = null },
                onAddToCart = {
                    onAddToCart(product)
                    Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                },
                onEdit = {
                    productToEdit = product
                },
                onDelete = {
                    onDeleteProduct(product.id)
                    selectedProductForDetail = null
                    Toast.makeText(context, "Product permanently deleted.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Checkout Success Dialog
        if (showCheckoutSuccessDialog) {
            Dialog(onDismissRequest = { showCheckoutSuccessDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Text(
                            text = "Order Confirmed!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "The transaction has been successfully processed. The invoice has been generated automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Open Invoice PDF Button
                        lastGeneratedPdfPath?.let { path ->
                            Button(
                                onClick = { openPdfFile(context, path) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("eshop_btn_view_pdf"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open PDF Invoice", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { showCheckoutSuccessDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Product Grid Card Composable
@Composable
fun ProductEcomCard(
    product: Product,
    selectedCurrency: String,
    usdExchangeRate: Double,
    onAddToCart: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("eshop_product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // High-end Visual Gradient & Category Icon Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.sweepGradient(
                            colors = getCategoryGradient(product.category)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Large styled category/brand icon in center
                Icon(
                    imageVector = getCategoryIcon(product.category),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(48.dp)
                )

                // High contrast badge for stock
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (product.stock > product.lowStockThreshold)
                                Color(0xFF2E7D32).copy(alpha = 0.85f)
                            else if (product.stock > 0)
                                Color(0xFFEF6C00).copy(alpha = 0.85f)
                            else
                                Color(0xFFC62828).copy(alpha = 0.85f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (product.stock > 0) "${product.stock} left" else "Out of stock",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Category overlay chip at bottom left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = product.category,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = product.brand.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "SKU: ${product.sku}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = CurrencyFormatter.format(product.price, selectedCurrency, usdExchangeRate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Compact Add to Cart Button
                    IconButton(
                        onClick = onAddToCart,
                        enabled = product.stock > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (product.stock > 0) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("btn_add_to_cart_item_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Cart",
                            tint = if (product.stock > 0) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Side Cart Panel Panel
@Composable
fun EShopCartPanel(
    cart: Map<Product, Int>,
    selectedCurrency: String,
    usdExchangeRate: Double,
    onDismiss: () -> Unit,
    onUpdateQty: (Product, Int) -> Unit,
    onRemoveItem: (Product) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activePaymentMethod by remember { mutableStateOf("Cash") }

    val subtotal = cart.entries.sumOf { it.key.price * it.value }
    val tax = subtotal * 0.08
    val total = subtotal + tax

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        // Sliding Drawer Surface
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterEnd)
                .clickable(enabled = false) {}, // prevent click-through
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header of Cart drawer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shopping Bag",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Bag is Empty",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "Browse and add products.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    // Cart Item List
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${cart.size} unique items",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Clear All",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { onClearCart() }
                                    .testTag("eshop_cart_clear_all")
                            )
                        }

                        cart.forEach { (product, quantity) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Icon mini placeholder
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.radialGradient(getCategoryGradient(product.category))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(product.category),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Details column
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(product.price, selectedCurrency, usdExchangeRate),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Interactive Qty Adjuster
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (quantity > 1) onUpdateQty(product, quantity - 1)
                                            else onRemoveItem(product)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                    }

                                    Text(
                                        text = quantity.toString(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.testTag("eshop_cart_qty_label_${product.id}")
                                    )

                                    IconButton(
                                        onClick = {
                                            if (quantity < product.stock) onUpdateQty(product, quantity + 1)
                                        },
                                        enabled = quantity < product.stock,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Summary & Payment picker
                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("Payment Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Cash", "Card", "Bank").forEach { method ->
                            val isSelected = activePaymentMethod == method
                            Button(
                                onClick = { activePaymentMethod = method },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("eshop_pay_method_$method"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(method, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Totals
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.format(subtotal, selectedCurrency, usdExchangeRate), fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sales Tax (8%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.format(tax, selectedCurrency, usdExchangeRate), fontSize = 11.sp)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                CurrencyFormatter.format(total, selectedCurrency, usdExchangeRate),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onCheckout(activePaymentMethod) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("eshop_btn_checkout"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Place Order", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Product add/edit Form Dialog
@Composable
fun ProductFormDialog(
    title: String,
    categories: List<Category>,
    productToEdit: Product? = null,
    existingProducts: List<Product> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        name: String, sku: String, price: Double, costPrice: Double, stock: Int,
        threshold: Int, category: String, brand: String, description: String, imageUrl: String,
        shortDescription: String, longDescription: String, specifications: String, features: String,
        ingredients: String, warranty: String, returnPolicy: String, shippingInfo: String,
        careInstructions: String, countryOfOrigin: String,
        wholesalePrice: Double, dealerPrice: Double, vipPrice: Double, bulkPrice: Double, minimumOrderPrice: Double,
        salePrice: Double
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var sku by remember { mutableStateOf(productToEdit?.sku ?: "") }
    var priceStr by remember { mutableStateOf(productToEdit?.price?.toString() ?: "") }
    var costStr by remember { mutableStateOf(productToEdit?.costPrice?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(productToEdit?.stock?.toString() ?: "") }
    var thresholdStr by remember { mutableStateOf(productToEdit?.lowStockThreshold?.toString() ?: "5") }
    var selectedCat by remember { mutableStateOf(productToEdit?.category ?: if (categories.isNotEmpty()) categories.first().name else "General") }
    var brand by remember { mutableStateOf(productToEdit?.brand ?: "Generic") }
    var description by remember { mutableStateOf(productToEdit?.description ?: "") }
    var imageUrl by remember { mutableStateOf(productToEdit?.imageUrl ?: "") }

    var shortDescription by remember { mutableStateOf(productToEdit?.shortDescription ?: "") }
    var longDescription by remember { mutableStateOf(productToEdit?.longDescription ?: "") }
    var specifications by remember { mutableStateOf(productToEdit?.specifications ?: "") }
    var features by remember { mutableStateOf(productToEdit?.features ?: "") }
    var ingredients by remember { mutableStateOf(productToEdit?.ingredients ?: "") }
    var warranty by remember { mutableStateOf(productToEdit?.warranty ?: "") }
    var returnPolicy by remember { mutableStateOf(productToEdit?.returnPolicy ?: "") }
    var shippingInfo by remember { mutableStateOf(productToEdit?.shippingInfo ?: "") }
    var careInstructions by remember { mutableStateOf(productToEdit?.careInstructions ?: "") }
    var countryOfOrigin by remember { mutableStateOf(productToEdit?.countryOfOrigin ?: "") }

    var wholesalePriceStr by remember { mutableStateOf(productToEdit?.wholesalePrice?.toString() ?: "") }
    var dealerPriceStr by remember { mutableStateOf(productToEdit?.dealerPrice?.toString() ?: "") }
    var vipPriceStr by remember { mutableStateOf(productToEdit?.vipPrice?.toString() ?: "") }
    var bulkPriceStr by remember { mutableStateOf(productToEdit?.bulkPrice?.toString() ?: "") }
    var minimumOrderPriceStr by remember { mutableStateOf(productToEdit?.minimumOrderPrice?.toString() ?: "") }
    var salePriceStr by remember { mutableStateOf(productToEdit?.salePrice?.toString() ?: "") }

    var expandedDropdown by remember { mutableStateOf(false) }

    // Automatic SKU Management State & Logic
    var isAutoSku by remember { mutableStateOf(productToEdit == null) }

    fun generateSkuCode(productName: String, categoryName: String, productsList: List<Product>): String {
        val prefix = when {
            productName.contains("Cinnamon", ignoreCase = true) -> "CIN"
            categoryName.equals("Spices", ignoreCase = true) -> "SP"
            categoryName.equals("Tea", ignoreCase = true) -> "TEA"
            categoryName.equals("Gemstones", ignoreCase = true) -> "GEM"
            categoryName.isNotBlank() -> {
                if (categoryName.length >= 3) categoryName.substring(0, 3).uppercase() else categoryName.uppercase()
            }
            else -> "PRD"
        }

        val regex = Regex("^$prefix-(\\d+)$")
        val existingNumbers = productsList.mapNotNull { prod ->
            regex.matchEntire(prod.sku)?.groups?.get(1)?.value?.toIntOrNull()
        }

        val nextNum = if (existingNumbers.isNotEmpty()) existingNumbers.max() + 1 else 1
        val paddingWidth = if (prefix == "SP") 6 else 5
        val paddedNum = nextNum.toString().padStart(paddingWidth, '0')
        return "$prefix-$paddedNum"
    }

    LaunchedEffect(name, selectedCat, isAutoSku) {
        if (isAutoSku && productToEdit == null) {
            sku = generateSkuCode(name, selectedCat, existingProducts)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider()

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("form_product_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { 
                            sku = it 
                            isAutoSku = false
                        },
                        label = { Text("SKU Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("form_product_sku")
                    )

                    // Auto SKU Generator Force Refresh
                    IconButton(
                        onClick = {
                            sku = generateSkuCode(name, selectedCat, existingProducts)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_generate_sku")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Force Generate SKU")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isAutoSku,
                        onCheckedChange = { isChecked ->
                            isAutoSku = isChecked
                            if (isChecked) {
                                sku = generateSkuCode(name, selectedCat, existingProducts)
                            }
                        },
                        modifier = Modifier.testTag("checkbox_auto_sku")
                    )
                    Column {
                        Text(
                            text = "Automatic SKU Management",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Auto-generates e.g. SP-000001, TEA-00125, CIN-00035 based on product & category",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Sell Price") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("form_product_price")
                    )
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Cost Price") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("form_product_cost")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock Level") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("form_product_stock")
                    )
                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it },
                        label = { Text("Low Stock Threshold") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("form_product_threshold")
                    )
                }

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("form_product_brand")
                )

                // Category Selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCat,
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("form_product_category")
                    )
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCat = cat.name
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Description Field (High-end requirement)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Product Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("form_product_description"),
                    maxLines = 4
                )

                var showAdvancedDetails by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showAdvancedDetails = !showAdvancedDetails },
                    modifier = Modifier.fillMaxWidth().testTag("btn_toggle_advanced_fields")
                ) {
                    Icon(
                        imageVector = if (showAdvancedDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showAdvancedDetails) "Hide Advanced Specifications" else "Show Advanced Specifications")
                }

                if (showAdvancedDetails) {
                    OutlinedTextField(
                        value = shortDescription,
                        onValueChange = { shortDescription = it },
                        label = { Text("Short Description") },
                        modifier = Modifier.fillMaxWidth().testTag("form_product_short_desc")
                    )
                    OutlinedTextField(
                        value = longDescription,
                        onValueChange = { longDescription = it },
                        label = { Text("Long Description") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("form_product_long_desc"),
                        maxLines = 4
                    )
                    OutlinedTextField(
                        value = specifications,
                        onValueChange = { specifications = it },
                        label = { Text("Specifications (e.g., Size, Weight, Material)") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("form_product_specs"),
                        maxLines = 4
                    )
                    OutlinedTextField(
                        value = features,
                        onValueChange = { features = it },
                        label = { Text("Features") },
                        modifier = Modifier.fillMaxWidth().testTag("form_product_features")
                    )
                    OutlinedTextField(
                        value = ingredients,
                        onValueChange = { ingredients = it },
                        label = { Text("Ingredients / Components") },
                        modifier = Modifier.fillMaxWidth().testTag("form_product_ingredients")
                    )
                    OutlinedTextField(
                        value = warranty,
                        onValueChange = { warranty = it },
                        label = { Text("Warranty Info") },
                        modifier = Modifier.fillMaxWidth().testTag("form_product_warranty")
                    )
                    OutlinedTextField(
                        value = returnPolicy,
                        onValueChange = { returnPolicy = it },
                        label = { Text("Return Policy") },
                        modifier = Modifier.fillMaxWidth().testTag("form_product_return_policy")
                    )
                    OutlinedTextField(
                        value = shippingInfo,
                        onValueChange = { shippingInfo = it },
                        label = { Text("Shipping & Delivery Info") },
                        modifier = Modifier.fillMaxWidth().testTag("form_product_shipping")
                    )
                    OutlinedTextField(
                        value = careInstructions,
                        onValueChange = { careInstructions = it },
                        label = { Text("Care Instructions") },
                        modifier = Modifier.fillMaxWidth().testTag("form_product_care")
                    )
                    OutlinedTextField(
                        value = countryOfOrigin,
                        onValueChange = { countryOfOrigin = it },
                        label = { Text("Country of Origin") },
                        modifier = Modifier.fillMaxWidth().testTag("form_product_country")
                    )
                }

                var showPricingTiers by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showPricingTiers = !showPricingTiers },
                    modifier = Modifier.fillMaxWidth().testTag("btn_toggle_pricing_tiers")
                ) {
                    Icon(
                        imageVector = if (showPricingTiers) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showPricingTiers) "Hide Pricing Tiers" else "Show Pricing Tiers (Wholesale, VIP, etc.)")
                }

                if (showPricingTiers) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Advanced Pricing Tiers", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = salePriceStr,
                                onValueChange = { salePriceStr = it },
                                label = { Text("Sale Price") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("form_product_sale_price")
                            )
                            
                            OutlinedTextField(
                                value = wholesalePriceStr,
                                onValueChange = { wholesalePriceStr = it },
                                label = { Text("Wholesale Price") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("form_product_wholesale_price")
                            )
                            
                            OutlinedTextField(
                                value = dealerPriceStr,
                                onValueChange = { dealerPriceStr = it },
                                label = { Text("Dealer Price") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("form_product_dealer_price")
                            )
                            
                            OutlinedTextField(
                                value = vipPriceStr,
                                onValueChange = { vipPriceStr = it },
                                label = { Text("VIP Price") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("form_product_vip_price")
                            )
                            
                            OutlinedTextField(
                                value = bulkPriceStr,
                                onValueChange = { bulkPriceStr = it },
                                label = { Text("Bulk Price") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("form_product_bulk_price")
                            )
                            
                            OutlinedTextField(
                                value = minimumOrderPriceStr,
                                onValueChange = { minimumOrderPriceStr = it },
                                label = { Text("Minimum Order Price") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("form_product_min_order_price")
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || sku.isBlank()) return@Button
                            val sellPrice = priceStr.toDoubleOrNull() ?: 0.0
                            val costPrice = costStr.toDoubleOrNull() ?: 0.0
                            val stock = stockStr.toIntOrNull() ?: 0
                            val threshold = thresholdStr.toIntOrNull() ?: 5
                            val finalDesc = if (description.isBlank()) "Premium custom catalog product." else description
                            
                            val wholesalePrice = wholesalePriceStr.toDoubleOrNull() ?: 0.0
                            val dealerPrice = dealerPriceStr.toDoubleOrNull() ?: 0.0
                            val vipPrice = vipPriceStr.toDoubleOrNull() ?: 0.0
                            val bulkPrice = bulkPriceStr.toDoubleOrNull() ?: 0.0
                            val minimumOrderPrice = minimumOrderPriceStr.toDoubleOrNull() ?: 0.0
                            val salePrice = salePriceStr.toDoubleOrNull() ?: 0.0

                            onSave(
                                name, sku, sellPrice, costPrice, stock, threshold, selectedCat, brand, finalDesc, imageUrl,
                                shortDescription, longDescription, specifications, features, ingredients,
                                warranty, returnPolicy, shippingInfo, careInstructions, countryOfOrigin,
                                wholesalePrice, dealerPrice, vipPrice, bulkPrice, minimumOrderPrice, salePrice
                            )
                        },
                        enabled = name.isNotBlank() && sku.isNotBlank(),
                        modifier = Modifier.testTag("btn_save_product")
                    ) {
                        Text("Save Product", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Full product details dialogue
@Composable
fun ProductDetailDialog(
    product: Product,
    selectedCurrency: String,
    usdExchangeRate: Double,
    onDismiss: () -> Unit,
    onAddToCart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Block Gradient with category and close trigger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.sweepGradient(
                                colors = getCategoryGradient(product.category)
                            )
                        )
                ) {
                    // Back/Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    // Huge Icon
                    Icon(
                        imageVector = getCategoryIcon(product.category),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                    )

                    // Stock Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (product.stock > 0) "In Stock: ${product.stock} items" else "Out of Stock",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header textual info
                    Column {
                        Text(
                            text = product.brand.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Detailed specification indicators (SKU, Price, Category)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // SKU Panel
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("SKU", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(product.sku, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Category Panel
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Category", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(product.category, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Price Panel
                        Card(
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Retail Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    CurrencyFormatter.format(product.price, selectedCurrency, usdExchangeRate),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Elegant tab/specification component
                    var activeSubTab by remember { mutableStateOf("About") }
                    val subTabs = listOf("About", "Pricing & Tiers", "Specs & Features", "Support & Info")
                    
                    ScrollableTabRow(
                        selectedTabIndex = subTabs.indexOf(activeSubTab),
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[subTabs.indexOf(activeSubTab)]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        subTabs.forEach { tab ->
                            Tab(
                                selected = activeSubTab == tab,
                                onClick = { activeSubTab = tab },
                                text = {
                                    Text(
                                        text = tab,
                                        fontSize = 12.sp,
                                        fontWeight = if (activeSubTab == tab) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    when (activeSubTab) {
                        "Pricing & Tiers" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Available Pricing Levels", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                
                                val tierPrices = listOf(
                                    Triple("Regular Price", product.price, "Standard listing price"),
                                    Triple("Sale Price", if (product.salePrice > 0) product.salePrice else null, "Discounted retail offer"),
                                    Triple("Wholesale Price", if (product.wholesalePrice > 0) product.wholesalePrice else null, "Bulk merchant rate"),
                                    Triple("Dealer Price", if (product.dealerPrice > 0) product.dealerPrice else null, "Official dealer/distributor tier"),
                                    Triple("VIP Price", if (product.vipPrice > 0) product.vipPrice else null, "Special loyalty client rate"),
                                    Triple("Bulk Price", if (product.bulkPrice > 0) product.bulkPrice else null, "High-volume business rate"),
                                    Triple("Minimum Order Price", if (product.minimumOrderPrice > 0) product.minimumOrderPrice else null, "Starting value required for order")
                                )

                                tierPrices.forEach { (label, value, desc) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (label == "Regular Price" || label == "Sale Price") 
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                else 
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = if (value != null && value > 0) 
                                                CurrencyFormatter.format(value, selectedCurrency, usdExchangeRate)
                                            else 
                                                "Not configured",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = if (value != null && value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                        "About" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (product.shortDescription.isNotBlank()) {
                                    Column {
                                        Text("Short Overview", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(product.shortDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column {
                                    Text("General Description", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        product.description.ifBlank { "No official description available." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )
                                }
                                if (product.longDescription.isNotBlank()) {
                                    Column {
                                        Text("Detailed Description", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(product.longDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                                    }
                                }
                                if (product.countryOfOrigin.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Country of Origin", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(product.countryOfOrigin, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                        "Specs & Features" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column {
                                    Text("Technical Specifications", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    if (product.specifications.isNotBlank()) {
                                        Text(product.specifications, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text("Standard retail SKU catalog specifications apply.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (product.features.isNotBlank()) {
                                    Column {
                                        Text("Key Features", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        product.features.split(",").forEach { feat ->
                                            if (feat.trim().isNotBlank()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(feat.trim(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "Support & Info" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (product.ingredients.isNotBlank()) {
                                    Column {
                                        Text("Ingredients / Composition", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(product.ingredients, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (product.careInstructions.isNotBlank()) {
                                    Column {
                                        Text("Care Instructions", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(product.careInstructions, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (product.shippingInfo.isNotBlank()) {
                                    Column {
                                        Text("Shipping & Delivery Information", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(product.shippingInfo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (product.warranty.isNotBlank()) {
                                    Column {
                                        Text("Warranty Information", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(product.warranty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (product.returnPolicy.isNotBlank()) {
                                    Column {
                                        Text("Return Policy", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(product.returnPolicy, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Divider()

                    // Edit & Delete Catalog Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onEdit,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_detail_edit_product")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit")
                            }

                            if (!showDeleteConfirm) {
                                TextButton(
                                    onClick = { showDeleteConfirm = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("btn_detail_delete_product_confirm_trigger")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            } else {
                                Button(
                                    onClick = onDelete,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_detail_delete_product_confirm")
                                ) {
                                    Text("Confirm Delete", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Main Add to Cart trigger
                        Button(
                            onClick = onAddToCart,
                            enabled = product.stock > 0,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_detail_add_to_cart")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Bag", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Helper gradients based on Category
fun getCategoryGradient(category: String): List<Color> {
    return when (category.lowercase(Locale.ROOT)) {
        "spices" -> listOf(Color(0xFFE64A19), Color(0xFFFFCC80))
        "tea" -> listOf(Color(0xFF2E7D32), Color(0xFFA5D6A7))
        "gemstones" -> listOf(Color(0xFF1565C0), Color(0xFF90CAF9))
        else -> listOf(Color(0xFF673AB7), Color(0xFFD1C4E9))
    }
}

// Helper icons based on Category
fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.ROOT)) {
        "spices" -> Icons.Default.ThumbUp // stand-in for custom spices
        "tea" -> Icons.Default.Favorite // stand-in for tea
        "gemstones" -> Icons.Default.Star // premium shiny gemstones
        else -> Icons.Default.Build // general package
    }
}
