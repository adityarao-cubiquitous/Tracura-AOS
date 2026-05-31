package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.runtime.Composable
import com.cubiquitous.tracura.ui.view.approver.CategoryDetailScreen

@Composable
fun BusinessHeadCategoryDetail(
    projectId: String,
    categoryName: String,
    onNavigateBack: () -> Unit
) {
    // Reuse the existing CategoryDetailScreen from approver flow
    // The functionality is identical for both roles
    CategoryDetailScreen(
        projectId = projectId,
        categoryName = categoryName,
        onNavigateBack = onNavigateBack
    )
} 