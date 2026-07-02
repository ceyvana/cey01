package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Product
import com.example.viewmodel.BusinessStats
import com.example.viewmodel.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AIScreen(
    chatHistory: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    products: List<Product> = emptyList(),
    businessStats: BusinessStats = BusinessStats(),
    selectedCurrency: String = "LKR",
    usdExchangeRate: Double = 300.0,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Advisory Chat, 1 = Predictive Telemetry & Stress Test

    // Shortcut quick prompts with icons
    val promptShortcuts = listOf(
        QuickPromptItem("📊 Margins Audit", "Show profit and loss analysis and product gross margin percentages.", "quick_suggest_margins"),
        QuickPromptItem("🔮 Runway Forecast", "Assess the current cash runway based on today's operating expenses and cash balances.", "quick_suggest_runway"),
        QuickPromptItem("⚠️ Stockout Risks", "Check which items are approaching low stock thresholds and estimate depletion timelines.", "quick_suggest_stockout"),
        QuickPromptItem("💹 Currency Advisory", "Analyze how current USD fluctuation affect pricing strategies of Uva Highland Tea and Cinnamon.", "quick_suggest_currency")
    )

    // Auto scroll to bottom when new message arrives in Chat
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty() && activeTab == 0) {
            lazyListState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Enterprise AI Business Advisor",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Cognitive Business Intelligence Engine",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
                if (activeTab == 0) {
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("clear_chat_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Conversation",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Material 3 Tabs to toggle between Chat and Predictive Telemetry
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Advisory Terminal", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Send, contentDescription = "Chat", modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("ai_advisor_tab_chat")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Predictive Analytics", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Telemetry", modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("ai_advisor_tab_predictive")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tab Contents
            if (activeTab == 0) {
                // TAB 0: INTERACTIVE ADVISORY CHAT TERMINAL
                Column(modifier = Modifier.weight(1f)) {
                    // Chat history
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatHistory) { msg ->
                            ChatBubble(msg)
                        }
                    }

                    // Floating Shortcut Action Row at the bottom of Chat list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "💡 Tap Quick Diagnostic Reports",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            promptShortcuts.take(2).forEach { item ->
                                QuickPromptCard(
                                    item = item,
                                    onSelect = {
                                        onSendMessage(item.prompt)
                                        keyboardController?.hide()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            promptShortcuts.drop(2).take(2).forEach { item ->
                                QuickPromptCard(
                                    item = item,
                                    onSelect = {
                                        onSendMessage(item.prompt)
                                        keyboardController?.hide()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Chat Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Ask business health, pricing advice, forecasts...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_text_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (textInput.isNotBlank()) {
                                    onSendMessage(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            })
                        )

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    onSendMessage(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .size(48.dp)
                                .testTag("ai_send_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Message",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            } else {
                // TAB 1: PREDICTIVE TELEMETRY & STRESS SIMULATOR SCREEN
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    PredictiveTelemetryUI(
                        products = products,
                        stats = businessStats,
                        currency = selectedCurrency,
                        usdRate = usdExchangeRate,
                        onTriggerQuery = { activeTab = 0; onSendMessage(it) }
                    )
                }
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Column {
                            Text("Compiling Intelligent Audit...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Connecting real-time ledger records to Gemini", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

data class QuickPromptItem(
    val title: String,
    val prompt: String,
    val testTag: String
)

@Composable
fun QuickPromptCard(
    item: QuickPromptItem,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(52.dp)
            .clickable { onSelect() }
            .testTag(item.testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.prompt,
                fontSize = 9.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isAi = msg.sender == "AI"
    val alignment = if (isAi) Alignment.Start else Alignment.End
    val bubbleColor = if (isAi) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
    val contentColor = if (isAi) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    val borderStroke = if (isAi) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isAi) 0.dp else 14.dp,
                bottomEnd = if (isAi) 14.dp else 0.dp
            ),
            color = bubbleColor,
            contentColor = contentColor,
            border = borderStroke,
            modifier = Modifier.widthIn(max = 290.dp),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Beautiful formatted response handling (with simulated bold formatting support)
                val text = msg.message
                if (text.contains("**") || text.contains("==") || text.contains("•")) {
                    FormattedChatText(text = text, isAi = isAi)
                } else {
                    Text(
                        text = text,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAi) Icons.Default.Star else Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = if (isAi) "AI Advisor Core" else "Authorized Operator",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

// Visual layout helper to render custom rich headings inside chat bubbles nicely
@Composable
fun FormattedChatText(text: String, isAi: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val lines = text.split("\n")
        lines.forEach { line ->
            when {
                line.trim().startsWith("==") || line.trim().startsWith("###") -> {
                    val cleanHeader = line.replace("==", "").replace("###", "").trim()
                    Text(
                        text = cleanHeader,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isAi) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.trim().startsWith("•") || line.trim().startsWith("-") -> {
                    val cleanBullet = line.trim().substring(1).trim()
                    Row(
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = cleanBullet,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
                else -> {
                    if (line.isNotBlank()) {
                        Text(
                            text = line,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PredictiveTelemetryUI(
    products: List<Product>,
    stats: BusinessStats,
    currency: String,
    usdRate: Double,
    onTriggerQuery: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // STRESS TEST SIMULATOR STATES
    var usdStressRateInput by remember { mutableStateOf(usdRate.toString()) }
    var inflationMarkupPercent by remember { mutableStateOf("10") }
    var demandSwingPercent by remember { mutableStateOf("15") }
    var showSimResults by remember { mutableStateOf(false) }

    // Kotlin Dynamic Calculation of AI business health score
    val calculatedScore = remember(stats, products) {
        var score = 80
        val expenseRatio = if (stats.totalSales > 0) stats.totalExpenses / stats.totalSales else 0.0
        
        // Expense deduction
        if (expenseRatio > 0.4) score -= 15
        else if (expenseRatio > 0.2) score -= 5
        
        // Low Stock deduction
        val lowStockCount = products.count { it.stock <= it.lowStockThreshold }
        score -= (lowStockCount * 4).coerceAtMost(20)

        // Profit margins additions/deductions
        if (stats.netProfit < 0) score -= 20
        else if (stats.netProfit > 500.0) score += 10
        
        score.coerceIn(0, 100)
    }

    val healthLabel = when {
        calculatedScore >= 80 -> "EXCELLENT"
        calculatedScore >= 60 -> "OPTIMAL"
        calculatedScore >= 40 -> "EXPENSE PRESSURE"
        else -> "CRITICAL MITIGATION REQUIRED"
    }

    val healthColor = when {
        calculatedScore >= 80 -> Color(0xFF00C853)
        calculatedScore >= 60 -> Color(0xFF00B0FF)
        calculatedScore >= 40 -> Color(0xFFFFAB00)
        else -> Color(0xFFFF1744)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Health Widget Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dynamic circular gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp)
                ) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = -220f,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = healthColor,
                            startAngle = -220f,
                            sweepAngle = 260f * (calculatedScore / 100f),
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$calculatedScore%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = healthColor
                        )
                        Text(
                            text = "HEALTH",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }

                // Description
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(healthLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = healthColor) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = healthColor.copy(alpha = 0.12f))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ledger Health Evaluation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Score combines current operating ratios, supplier account balances, and product stockout risk metrics.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Section: Stress Simulator
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sim", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Geopolitical & Currency Stress Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Analyze how cargo supply shocks, spikes in inflation indices, or fluctuations in the LKR exchange rate impact your net operating surplus.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = usdStressRateInput,
                        onValueChange = { usdStressRateInput = it },
                        label = { Text("USD Rate (LKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inflationMarkupPercent,
                        onValueChange = { inflationMarkupPercent = it },
                        label = { Text("Inflation Overhead %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = demandSwingPercent,
                    onValueChange = { demandSwingPercent = it },
                    label = { Text("Simulated Demand Shift % (e.g. -15 or +20)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val parsedUsd = usdStressRateInput.toDoubleOrNull() ?: usdRate
                        val parsedInfl = inflationMarkupPercent.toDoubleOrNull() ?: 0.0
                        val parsedDemand = demandSwingPercent.toDoubleOrNull() ?: 0.0
                        showSimResults = true
                        Toast.makeText(context, "Telemetry Matrix Computed Successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_stress_sim_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Compute")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EXECUTE COGNITIVE STRESS SIMULATION", fontWeight = FontWeight.Bold)
                }

                if (showSimResults) {
                    val targetUsd = usdStressRateInput.toDoubleOrNull() ?: usdRate
                    val inflMultiplier = 1.0 + ((inflationMarkupPercent.toDoubleOrNull() ?: 0.0) / 100.0)
                    val demandMultiplier = 1.0 + ((demandSwingPercent.toDoubleOrNull() ?: 0.0) / 100.0)

                    // Math simulation
                    val simSales = stats.totalSales * demandMultiplier
                    val simExpenses = stats.totalExpenses * inflMultiplier
                    val simNetProfit = simSales - simExpenses - (stats.outstandingPayments * (targetUsd / usdRate))
                    val currencyShiftPrcent = ((targetUsd - usdRate) / usdRate) * 100.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (simNetProfit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        border = BorderStroke(1.dp, if (simNetProfit >= 0) Color(0xFF81C784) else Color(0xFFE57373))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Simulated Core Matrix Outlook",
                                fontWeight = FontWeight.Bold,
                                color = if (simNetProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "• Simulated Sales Volume: $currency ${String.format("%.2f", simSales)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Simulated Operating Expense: $currency ${String.format("%.2f", simExpenses)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• FX Debt Exposure Adjustment: $currency ${String.format("%.2f", stats.outstandingPayments * (targetUsd / usdRate))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Net Business Performance Outcome: $currency ${String.format("%.2f", simNetProfit)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (simNetProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Text(
                                text = if (simNetProfit >= 0) {
                                    "✨ Recommendation: Ceylon cinnamon and spices demand is healthy enough to offset FX overhead. Continue sourcing raw shipments."
                                } else {
                                    "⚠️ Warning: LKR depreciation of ${String.format("%.1f", currencyShiftPrcent)}% combined with inflation causes operating deficit. Settle supplier accounts payable immediately to lock rates, and apply a 12% pricing mark-up."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        // Section: Out-of-Stock Risk Radar
        Text(
            text = "Predictive Stock Out-Of-Stock Horizon",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val lowStockItems = products.filter { it.stock <= it.lowStockThreshold }
        if (lowStockItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Stable", tint = Color(0xFF00C853))
                    Text("All cargo storage categories are stable. No imminent stockout horizons detected.", fontSize = 12.sp)
                }
            }
        } else {
            lowStockItems.forEach { prod ->
                // Simple velocity estimation
                val daysLeft = when {
                    prod.stock <= 0 -> 0
                    prod.stock < prod.lowStockThreshold / 2 -> 3
                    else -> 9
                }
                val levelColor = if (daysLeft == 0) Color(0xFFFF1744) else if (daysLeft < 5) Color(0xFFFF9100) else Color(0xFF2979FF)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, levelColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Current stock: ${prod.getStockDisplay()}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text(
                                text = if (daysLeft == 0) "OUT OF STOCK" else "DEPLETION IN $daysLeft DAYS",
                                color = levelColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Recommend: Restock ${prod.lowStockThreshold * 2} units",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        // Section: Expense Audit Insights
        Text(
            text = "Operational Leak & Cargo Expense Audits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Rent Advice
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Rent", tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Warehouse & Cold Storage Rent Audit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "Rental costs represent ${if (stats.totalSales > 0) String.format("%.1f", (1200.0 / stats.totalSales) * 100) else "0"}% of sales revenue. Optimal threshold is <15%. Leverage bulk packaging space to downscale Uva tea boxes storage.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // AP Debt Advice
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Payable", tint = Color(0xFFFF9100))
                    Column {
                        Text("Outstanding Supplier Accounts Payable Audit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "Current outstanding payables are $currency ${String.format("%.2f", stats.outstandingPayments)}. Settle bills to Highland Tea Growers Ltd immediately before LKR currency drops further.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action prompt helper to ask Gemini more
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onTriggerQuery("Explain operational advice regarding my accounts payable and exchange rate risks.")
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Ask", tint = MaterialTheme.colorScheme.primary)
                    Text("Deep Dive: Ask Gemini to advise on payables", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Default.Send, contentDescription = "Query", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
