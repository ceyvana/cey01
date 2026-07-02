package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CoverPageScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // App safety backup: System Back Button immediately bypasses/dismisses splash
    BackHandler(enabled = true) {
        onDismiss()
    }

    // Animation trigger states
    var startAnimations by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    // Start entrance animations
    LaunchedEffect(Unit) {
        startAnimations = true
        // Animate the progress bar from 0% to 100% over 2.4 seconds
        val duration = 2400
        val steps = 100
        val delayPerStep = (duration / steps).toLong()
        for (i in 1..steps) {
            delay(delayPerStep)
            progress = i / 100f
        }
        // Auto transition to the app dashboard when progress completes
        onDismiss()
    }

    // Infinite float animation for the brand logo
    val infiniteTransition = rememberInfiniteTransition(label = "Logo Float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatY"
    )

    // Pulse animation for the glowing rings
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Animated values bound to entrance state
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimations) 1f else 0f,
        animationSpec = tween(1000, easing = EaseOutQuad),
        label = "Alpha"
    )

    val scaleUp by animateFloatAsState(
        targetValue = if (startAnimations) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Scale"
    )

    val brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2027), // Deep space blue/black
            Color(0xFF1B323C), // Slate cyan dark
            Color(0xFF244351)  // Rich marine dark slate
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
            .testTag("cover_page_container"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative pulsing abstract background radar/matrix
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.12f)
        ) {
            val center = Offset(size.width / 2f, size.height * 0.45f)
            
            // Pulsing circles
            drawCircle(
                color = Color(0xFF00D2FF),
                radius = 160.dp.toPx() * pulseScale,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF00D2FF),
                radius = 260.dp.toPx() * (pulseScale * 0.85f),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF00D2FF),
                radius = 380.dp.toPx() * (pulseScale * 0.7f),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Drawing clean circuit background guides
            drawLine(
                color = Color(0xFF00D2FF),
                start = Offset(0f, size.height * 0.45f),
                end = Offset(size.width, size.height * 0.45f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0xFF00D2FF),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Layout responsive structure to handle compact heights and rotations gracefully
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            val screenHeight = maxHeight
            
            // If the screen is very small or in landscape mode, wrap in a scrollable Column with no constraints
            if (screenHeight < 640.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TopIndicator(alpha = contentAlpha)
                    LogoSection(
                        floatOffset = floatOffset,
                        contentAlpha = contentAlpha,
                        scaleUp = scaleUp
                    )
                    BottomSection(
                        progress = progress,
                        contentAlpha = contentAlpha,
                        onDismiss = onDismiss
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // If generous portrait screen size, use a gorgeous, perfectly aligned, non-scrollable layout
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Section
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        TopIndicator(alpha = contentAlpha)
                    }

                    // Middle Section
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                    ) {
                        LogoSection(
                            floatOffset = floatOffset,
                            contentAlpha = contentAlpha,
                            scaleUp = scaleUp
                        )
                    }

                    // Bottom Section
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        BottomSection(
                            progress = progress,
                            contentAlpha = contentAlpha,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopIndicator(alpha: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha * 0.8f)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF00FFCC))
            )
            Text(
                text = "LIVE SYSTEM ONLINE",
                color = Color(0xFF00FFCC),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        Text(
            text = "v2.5.0-SECURE",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun LogoSection(
    floatOffset: Float,
    contentAlpha: Float,
    scaleUp: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = floatOffset }
            .alpha(contentAlpha)
            .scale(scaleUp),
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing brand logo drawn via canvas
        Canvas(
            modifier = Modifier
                .size(130.dp)
                .testTag("cover_brand_logo_canvas")
        ) {
            val w = size.width
            val h = size.height

            // Primary diamond shape
            val p1 = Path().apply {
                moveTo(w / 2f, 8.dp.toPx())
                lineTo(w - 8.dp.toPx(), h / 2f)
                lineTo(w / 2f, h - 8.dp.toPx())
                lineTo(8.dp.toPx(), h / 2f)
                close()
            }

            // Inner shapes
            val p2 = Path().apply {
                moveTo(w / 2f, 32.dp.toPx())
                lineTo(w - 32.dp.toPx(), h / 2f)
                lineTo(w / 2f, h - 32.dp.toPx())
                lineTo(32.dp.toPx(), h / 2f)
                close()
            }

            // Draw glowing shadow background
            drawPath(
                path = p1,
                color = Color(0xFF00D2FF).copy(alpha = 0.2f),
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw main outer neon cyan diamond
            drawPath(
                path = p1,
                color = Color(0xFF00D2FF),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw secondary inner primary blue diamond
            drawPath(
                path = p2,
                color = Color(0xFF0061A4),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw core glowing energy dot in the center
            drawCircle(
                color = Color(0xFF00FFCC),
                radius = 12.dp.toPx(),
                center = Offset(w / 2f, h / 2f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // App Brand Heading
        Text(
            text = "CEYVANA",
            color = Color.White,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp,
                fontFamily = FontFamily.SansSerif
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("cover_brand_title")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // App Brand Subtitle
        Text(
            text = "UNIFIED POS, CARGO & COGNITIVE ADVISOR",
            color = Color(0xFF00D2FF),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "B2B Decanting Terminal • Multi-Currency Ledger • Gemini BI",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun BottomSection(
    progress: Float,
    contentAlpha: Float,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(contentAlpha),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress Bar
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Initializing database kernels...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00FFCC),
                    fontWeight = FontWeight.Bold
                )
            }

            // Custom Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0061A4), Color(0xFF00FFCC))
                            )
                        )
                )
            }
        }

        // Bypassing Button
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.12f),
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(48.dp)
                .testTag("cover_page_skip_btn")
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LAUNCH TERMINAL IMMEDIATELY",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Launch",
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Security Footnote
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.alpha(0.5f)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Shield",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "Encrypted Local AES-256 Room Ledger Active",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}
