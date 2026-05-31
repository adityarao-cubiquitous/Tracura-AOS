package com.cubiquitous.tracura.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.DepartmentCellData
import com.cubiquitous.tracura.model.DepartmentLineItemData
import com.cubiquitous.tracura.utils.FormatUtils
import java.text.NumberFormat
import java.util.Locale

/** Max number of line items to show in the cell before "+ N more". */
private const val MAX_ITEMS_IN_CELL = 4

/** Default width when [cellWidth] is not provided. */
private val DefaultCellWidth = 240.dp

/**
 * Department Cell Component - displays department information in a card format.
 * Shows up to 4 line items; if more, shows "n items + more" with an info icon that opens all line items in a bottom sheet.
 * Material 3 styling.
 *
 * @param cellWidth Width of the cell. When null, uses [DefaultCellWidth]. Pass [LocalConfiguration.current].screenWidthDp.dp for full screen width.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentCell(
    data: DepartmentCellData,
    modifier: Modifier = Modifier,
    cellWidth: Dp? = null,
    onClick: () -> Unit = {}
) {
    var showAllLineItemsSheet by remember { mutableStateOf(false) }
    val width = cellWidth ?: DefaultCellWidth
    val isDark = isSystemInDarkTheme()
    val cardContainerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val innerBadgeColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val hairlineColor = if (isDark) Color(0xFF38383A) else Color(0x1F000000)

    Card(
        modifier = modifier
            .width(width)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, hairlineColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Department Name + Contractor Mode Badge (Material spacing, no clip)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = true)
                )
                if (data.contractorMode.isNotEmpty()) {
                    Text(
                        text = data.contractorMode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(innerBadgeColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Budget Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatUtils.formatCurrency(data.budget),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Line items section: show up to 4 items, then "n items + more" with info icon
            if (data.allLineItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Line items:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                val itemsToShow = data.allLineItems.take(MAX_ITEMS_IN_CELL)
                val moreCount = data.allLineItems.size - MAX_ITEMS_IN_CELL

                itemsToShow.forEach { lineItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lineItemDisplayTitle(lineItem),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = true)
                        )
                        Text(
                            text = "${formatQuantity(lineItem.quantity)} ${lineItem.uom.ifEmpty { "-" }} • ${FormatUtils.formatCurrency(lineItem.total)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(min = 72.dp)
                        )
                    }
                }

                if (moreCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clickable { showAllLineItemsSheet = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "View all line items",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$moreCount items + more",
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Fallback: materials (max 4) + labour when no allLineItems
                if (data.materials.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Materials:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val materialsToShow = data.materials.take(MAX_ITEMS_IN_CELL)
                    val remainingCount = data.materials.size - MAX_ITEMS_IN_CELL
                    materialsToShow.forEach { material ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = material.name,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${formatQuantity(material.quantity)} ${material.uom}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (remainingCount > 0) {
                        Text(
                            text = "+ $remainingCount more",
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                data.labour?.let { labour ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Labour:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = FormatUtils.formatCurrency(labour.totalAmount),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "•",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${formatQuantity(labour.totalQuantity)} ${labour.uom}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAllLineItemsSheet && data.allLineItems.isNotEmpty()) {
        AllLineItemsBottomSheet(
            departmentName = data.name,
            lineItems = data.allLineItems,
            onDismiss = { showAllLineItemsSheet = false }
        )
    }
}

@Composable
private fun lineItemDisplayTitle(lineItem: DepartmentLineItemData): String {
    val itemPart = lineItem.item.ifEmpty { lineItem.itemType.ifEmpty { "Item" } }
    return if (lineItem.spec.isNotBlank()) "$itemPart (${lineItem.spec})" else itemPart
}

/**
 * Material 3 bottom sheet listing all line items in the department.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllLineItemsBottomSheet(
    departmentName: String,
    lineItems: List<DepartmentLineItemData>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            HorizontalDivider(
                modifier = Modifier
                    .width(32.dp)
                    .padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "All line items",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = departmentName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                lineItems.forEachIndexed { index, lineItem ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = lineItemDisplayTitle(lineItem),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${lineItem.itemType.ifEmpty { "-" }} • ${formatQuantity(lineItem.quantity)} ${lineItem.uom.ifEmpty { "-" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(lineItem.unitPrice) + "/" + lineItem.uom.ifEmpty { "unit" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        trailingContent = {
                            Text(
                                text = FormatUtils.formatCurrency(lineItem.total),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = MaterialTheme.colorScheme.onSurface,
                            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    if (index < lineItems.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format quantity - remove decimal if whole number
 */
private fun formatQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        String.format("%.0f", quantity)
    } else {
        String.format("%.1f", quantity)
    }
}
