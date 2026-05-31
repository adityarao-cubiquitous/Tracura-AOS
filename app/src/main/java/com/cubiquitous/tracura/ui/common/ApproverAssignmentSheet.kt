package com.cubiquitous.tracura.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.User

private data class ApproverSheetPalette(
    val background: Color,
    val surface: Color,
    val fieldColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val hairline: Color,
    val successColor: Color,
    val successBg: Color,
    val unselectedIconBg: Color,
    val selectedBg: Color,
    val selectedText: Color
)

@Composable
private fun rememberApproverSheetPalette(): ApproverSheetPalette {
    val isDark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary
    return remember(isDark, accent) {
        ApproverSheetPalette(
            background    = if (isDark) Color(0xFF000000) else Color(0xFFF8F9FA),
            surface       = if (isDark) Color(0xFF1C1C1E) else Color.White,
            fieldColor    = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF0F4F8),
            textPrimary   = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1A1C1E),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0xFF757575),
            accent        = accent,
            onAccent      = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF),
            hairline      = if (isDark) Color(0xFF38383A) else Color(0xFFE0E0E0),
            successColor  = Color(0xFF34C759),
            successBg     = if (isDark) Color(0xFF1A3A1A) else Color(0xFFE8F5E9),
            unselectedIconBg = if (isDark) Color(0xFF3A3A3C) else Color(0xFFECEFF1),
            selectedBg    = if (isDark) Color(0xFF1A2A3A) else Color(0xFFE3F2FD),
            selectedText  = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)
        )
    }
}

@Composable
fun ApproverAssignmentSheet(
    assignmentCountString: MutableState<String>,
    departments: List<String>,
    approvers: List<User>,
    initialAssignments: Map<String, String> = emptyMap(),
    onAssignmentComplete: (Map<String, String>) -> Unit
) {
    val p = rememberApproverSheetPalette()
    val assignments = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(initialAssignments) {
        assignments.clear()
        assignments.putAll(initialAssignments)
    }

    var expandedDepartment by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(assignments.size) {
        assignmentCountString.value = "${assignments.size}/${departments.size}"
    }

    val progress by animateFloatAsState(
        targetValue = if (departments.isNotEmpty()) assignments.size.toFloat() / departments.size else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "ProgressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .background(p.background)
            .padding(top = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(p.hairline)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assign Approvers",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary
                )

                Button(
                    onClick = { onAssignmentComplete(assignments.toMap()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = p.accent,
                        contentColor = p.onAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% Completed",
                    fontSize = 13.sp,
                    color = p.textSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(100.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(p.hairline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF34C759), Color(0xFF30D158))
                                )
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(departments) { department ->
                val approversForDepartment = approvers.filter { it.department == department }
                val selectedApproverPhone = assignments[department]
                val selectedApprover = approversForDepartment.find { it.phone == selectedApproverPhone }
                val isExpanded = expandedDepartment == department

                DepartmentCard(
                    departmentName = department,
                    selectedApprover = selectedApprover,
                    isExpanded = isExpanded,
                    approvers = approversForDepartment,
                    palette = p,
                    onExpandToggle = {
                        expandedDepartment = if (isExpanded) null else department
                    },
                    onSelectApprover = { approver ->
                        assignments[department] = approver.phone
                        expandedDepartment = null
                    }
                )
            }
        }
    }
}

@Composable
private fun DepartmentCard(
    departmentName: String,
    selectedApprover: User?,
    isExpanded: Boolean,
    approvers: List<User>,
    palette: ApproverSheetPalette,
    onExpandToggle: () -> Unit,
    onSelectApprover: (User) -> Unit
) {
    val p = palette
    val cardColor by animateColorAsState(
        targetValue = if (selectedApprover != null) p.surface else p.fieldColor,
        label = "CardColor"
    )
    val borderColor = if (isExpanded) p.accent else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onExpandToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 8.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedApprover != null) p.successBg else p.unselectedIconBg
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedApprover != null) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = p.successColor,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = departmentName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = p.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = departmentName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = p.textPrimary
                        )
                        AnimatedVisibility(visible = !isExpanded) {
                            Text(
                                text = selectedApprover?.name ?: "Tap to assign",
                                fontSize = 13.sp,
                                color = if (selectedApprover != null) p.successColor else p.textSecondary
                            )
                        }
                    }
                }

                val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "Arrow")
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                    tint = p.textSecondary
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = p.hairline)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "SELECT APPROVER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    approvers.forEach { approver ->
                        ApproverSelectionRow(
                            name = approver.name,
                            isSelected = selectedApprover?.phone == approver.phone,
                            palette = p,
                            onClick = { onSelectApprover(approver) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApproverSelectionRow(
    name: String,
    isSelected: Boolean,
    palette: ApproverSheetPalette,
    onClick: () -> Unit
) {
    val p = palette
    val backgroundColor = if (isSelected) p.selectedBg else Color.Transparent
    val textColor = if (isSelected) p.selectedText else p.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = if (isSelected) p.selectedText else p.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 15.sp,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = p.selectedText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
