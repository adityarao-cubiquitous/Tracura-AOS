package com.cubiquitous.tracura.ui.view.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessRestrictedScreen(
    phoneNumber: String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = authGradientBrush)
    ) {
        // Decorative blob
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ─── Top App Bar ─────────────────────────────────────────────────
            TopAppBar(
                title = {
                    Text(
                        text = "Access Restricted",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ─── Icon + Title ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Access Restricted",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFF59E0B)   // Amber — less alarming than red on gradient
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Access Restricted",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your phone number is not authorized\nto access this application.",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = phoneNumber,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ─── White card ───────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 36.dp)
                ) {
                    Text(
                        text = "What to do next?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B5BDB)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val steps = listOf(
                        "Contact your system administrator",
                        "Request access to Tracura",
                        "Provide your phone: $phoneNumber",
                        "Wait for admin approval & role assignment"
                    )
                    steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Numbered chip
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF3B5BDB), Color(0xFF7950F2))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = step,
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280),
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Contact Admin — gradient button
                    GradientButton(
                        text = "Contact Administrator",
                        onClick = { /* open contact in production */ },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Try Different Number — outlined
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Color(0xFF3B5BDB)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF3B5BDB)
                        )
                    ) {
                        Text(
                            text = "Try Different Number",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Tracura · Authorized Access Only",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0B0),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}