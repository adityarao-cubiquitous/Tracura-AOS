package com.cubiquitous.tracura.ui.view.approver

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.ui.common.OverallReportsContent
import com.cubiquitous.tracura.viewmodel.OverallReportsViewModel
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel

@Composable
fun OverallReportsScreen(
    onNavigateBack: () -> Unit,
    overallReportsViewModel: OverallReportsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
    
    // Set user context and load data when screen opens
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            android.util.Log.d("OverallReportsScreen", "🔄 Setting user context: ${currentUser.phone} (${currentUser.role.name})")
            overallReportsViewModel.setUserContextAndLoadData(
                userId = currentUser.phone,
                userRole = currentUser.role.name
            )
            android.util.Log.d("OverallReportsScreen", "✅ User context set successfully")
        } else {
            android.util.Log.w("OverallReportsScreen", "⚠️ Current user is null, cannot set context")
        }
    }
    
    OverallReportsContent(
        title = "Overall Reports",
        primaryColor = Color(0xFF8B5FBF),
        onNavigateBack = onNavigateBack,
        overallReportsViewModel = overallReportsViewModel,
        onExportPDF = {
            overallReportsViewModel.exportToPDF(
                onSuccess = { intent ->
                    if (intent != null) {
                        try {
                            context.startActivity(intent)
                            Toast.makeText(context, "PDF exported successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to open PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to create PDF export", Toast.LENGTH_LONG).show()
                    }
                },
                onError = { errorMessage ->
                    Toast.makeText(context, "Export failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        },
        onExportCSV = {
            overallReportsViewModel.exportToCSV(
                onSuccess = { intent ->
                    if (intent != null) {
                        try {
                            context.startActivity(intent)
                            Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to open CSV: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to create CSV export", Toast.LENGTH_LONG).show()
                    }
                },
                onError = { errorMessage ->
                    Toast.makeText(context, "Export failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        }
    )
} 