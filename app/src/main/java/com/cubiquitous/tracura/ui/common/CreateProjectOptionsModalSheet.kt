package com.cubiquitous.tracura.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.cubiquitous.tracura.ui.view.businesshead.businessHeadUiPalette

@Composable
fun CreateProjectOptionsModalSheet(
    onDismiss: () -> Unit,
    onCreateNewProject: () -> Unit,
    onSelectTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = businessHeadUiPalette()
    Popup(
        onDismissRequest = onDismiss,
        alignment = Alignment.BottomEnd,
        offset = androidx.compose.ui.unit.IntOffset(x = (-16).dp.value.toInt(), y = (-80).dp.value.toInt()),
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .width(280.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = palette.tier2Surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Option 1: Create New Project
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCreateNewProject()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New Project",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = palette.primaryText
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = palette.primaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Divider(color = palette.outline, thickness = 1.dp)
                
                // Option 2: Select from Template
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectTemplate()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select from Template",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = palette.primaryText
                    )
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = palette.primaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

