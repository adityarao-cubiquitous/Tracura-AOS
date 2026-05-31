package com.cubiquitous.tracura.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.utils.FormatUtils

@Composable
fun StatusSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double? = null,
    count: Int,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .let { if (isSelected) it.padding(2.dp) else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.9f) else 
                            if (amount != null) Color.White else color
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else if (amount != null) 2.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(if (amount != null) 32.dp else 24.dp),
                tint = if (amount != null) color else Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (amount != null) {
                // For expense summary cards with amounts
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = FormatUtils.formatCurrency(amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "$count item${if (count != 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            } else {
                // For status filter cards
                Text(
                    text = count.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
                tint = Color(0xFF4285F4)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onProjectClick: () -> Unit,
    showBudget: Boolean = true,
    showEndDate: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project Code Circle
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4285F4).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = FormatUtils.getProjectInitials(project.name),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Project Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                if (showBudget) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Budget: ${FormatUtils.formatCurrency(project.budget)}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                // Show end date if available and requested
                if (showEndDate) {
                    project.endDate?.let { endDate ->
                        val daysLeft = FormatUtils.calculateDaysLeft(endDate)
                        if (daysLeft > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📅 Ends: ${FormatUtils.formatDate(endDate)} ($daysLeft days left)",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryAmountCard(
    categoryName: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Text(
                text = FormatUtils.formatCurrency(amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4)
            )
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector? = null,
    backgroundColor: Color = Color.White,
    titleColor: Color = Color.Black,
    valueColor: Color = Color.Black
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = if (icon != null) Alignment.CenterHorizontally else Alignment.Start
        ) {
            icon?.let {
                Icon(
                    it,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = valueColor
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
} 