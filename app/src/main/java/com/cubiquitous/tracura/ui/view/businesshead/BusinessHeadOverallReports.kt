package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.ui.view.businesshead.reports.TracuraDashboardReports
import com.cubiquitous.tracura.viewmodel.OverallReportsViewModel
import com.cubiquitous.tracura.viewmodel.ProjectViewModel

@Composable
fun BusinessHeadOverallReports(
    onNavigateBack: () -> Unit,
    onNavigateToProject: ((String) -> Unit)? = null,
    overallReportsViewModel: OverallReportsViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel()
) {
    TracuraDashboardReports(
        onNavigateBack = onNavigateBack,
        onNavigateToProject = onNavigateToProject,
        overallReportsViewModel = overallReportsViewModel,
        projectViewModel = projectViewModel
    )
} 