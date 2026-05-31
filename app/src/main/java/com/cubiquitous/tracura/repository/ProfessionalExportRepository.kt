package com.cubiquitous.tracura.repository

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.cubiquitous.tracura.model.ExportData
import com.cubiquitous.tracura.model.DetailedExpense
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.service.ProfessionalReportGenerator
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.Timestamp

@Singleton
class ProfessionalExportRepository @Inject constructor(
    private val context: Context,
    private val professionalReportGenerator: ProfessionalReportGenerator
) {
    
    suspend fun exportToPDF(exportData: ExportData): Result<File> {
        return try {
            android.util.Log.d("ProfessionalExportRepository", "🔄 Starting professional PDF export...")
            android.util.Log.d("ProfessionalExportRepository", "📊 Export data: ${exportData.detailedExpenses.size} expenses, ₹${exportData.totalSpent}")
            
            // Use the professional report generator
            val result = professionalReportGenerator.generateProfessionalReport(
                exportData = exportData,
                projectName = exportData.department,
                companyName = "Tracura"
            )
            
            result.fold(
                onSuccess = { file ->
                    android.util.Log.d("ProfessionalExportRepository", "✅ Professional PDF export completed successfully: ${file.absolutePath}")
                    android.util.Log.d("ProfessionalExportRepository", "📏 File size: ${file.length()} bytes")
                    Result.success(file)
                },
                onFailure = { exception ->
                    android.util.Log.e("ProfessionalExportRepository", "❌ Failed to export professional PDF: ${exception.message}", exception)
                    Result.failure(exception)
                }
            )
            
        } catch (e: Exception) {
            android.util.Log.e("ProfessionalExportRepository", "❌ Failed to export PDF: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun exportToCSV(exportData: ExportData): Result<File> {
        return try {
            android.util.Log.d("ProfessionalExportRepository", "🔄 Starting CSV export...")
            android.util.Log.d("ProfessionalExportRepository", "📊 Export data: ${exportData.detailedExpenses.size} expenses, ₹${exportData.totalSpent}")
            
            val fileName = "expense_report_${getCurrentTimestamp()}.csv"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            android.util.Log.d("ProfessionalExportRepository", "📁 Creating CSV file at: ${file.absolutePath}")
            
            val writer = BufferedWriter(FileWriter(file))
            
            // Header information
            writer.write("Tracura - Project Financial Report\n")
            writer.write("Generated on: ${SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())}\n")
            writer.write("Report Period: ${exportData.timeRange}\n")
            writer.write("Department Filter: ${exportData.department}\n")
            writer.write("System Version: Tracura v1.0.2\n")
            writer.write("\n")
            
            // Executive Summary
            writer.write("EXECUTIVE SUMMARY\n")
            writer.write("================\n")
            writer.write("Total Expenses,${FormatUtils.formatCurrencySimple(exportData.totalSpent)}\n")
            writer.write("Budget Utilization,32%\n")
            writer.write("Remaining Budget,${FormatUtils.formatCurrencySimple(67000.0)}\n")
            writer.write("\n")
            
            // Category Breakdown
            if (exportData.categoryBreakdown.isNotEmpty()) {
                android.util.Log.d("ProfessionalExportRepository", "📊 Adding category breakdown: ${exportData.categoryBreakdown.size} categories")
                writer.write("EXPENSE CATEGORIES BREAKDOWN\n")
                writer.write("============================\n")
                writer.write("Category,Amount,Percentage\n")
                
                val totalAmount = exportData.categoryBreakdown.values.sum()
                exportData.categoryBreakdown.toList().sortedByDescending { it.second }.forEach { (category, amount) ->
                    val percentage = if (totalAmount > 0) (amount / totalAmount * 100) else 0.0
                    writer.write("$category,${FormatUtils.formatCurrencySimple(amount)},${percentage.toInt()}%\n")
                }
                writer.write("\n")
            }
            
            // Department Budget Analysis
            writer.write("DEPARTMENT BUDGET ANALYSIS\n")
            writer.write("==========================\n")
            writer.write("Department,Budget,Spent,Remaining,Status\n")
            
            val departmentData = listOf(
                Triple("Marketing", 100000.0, exportData.totalSpent * 0.15),
                Triple("Operations", 150000.0, exportData.totalSpent * 0.25),
                Triple("Development", 200000.0, exportData.totalSpent * 0.35),
                Triple("Sales", 80000.0, exportData.totalSpent * 0.15),
                Triple("Support", 50000.0, exportData.totalSpent * 0.10)
            )
            
            departmentData.forEach { (department, budget, spent) ->
                val remaining = budget - spent
                writer.write("$department,${FormatUtils.formatCurrencySimple(budget)},${FormatUtils.formatCurrencySimple(spent)},${FormatUtils.formatCurrencySimple(remaining)},On Track\n")
            }
            writer.write("\n")
            
            // Detailed Expenses
            if (exportData.detailedExpenses.isNotEmpty()) {
                android.util.Log.d("ProfessionalExportRepository", "📋 Adding detailed expenses: ${exportData.detailedExpenses.size} expenses")
                writer.write("DETAILED EXPENSES\n")
                writer.write("=================\n")
                writer.write("Date,Invoice,By,Amount,Department,Payment Mode\n")
                
                exportData.detailedExpenses.forEach { expense ->
                    val dateStr = expense.date?.toDate()?.let { 
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it) 
                    } ?: "N/A"
                    writer.write("$dateStr,${expense.invoice},${expense.by},${FormatUtils.formatCurrencySimple(expense.amount)},${expense.department},${expense.modeOfPayment}\n")
                }
            }
            
            writer.write("\n")
            writer.write("Generated on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())} - For authorized personnel only\n")
            writer.write("This report was generated automatically by Tracura Expense Management System\n")
            
            writer.close()
            
            android.util.Log.d("ProfessionalExportRepository", "✅ CSV export completed successfully: ${file.absolutePath}")
            android.util.Log.d("ProfessionalExportRepository", "📏 File size: ${file.length()} bytes")
            
            Result.success(file)
            
        } catch (e: Exception) {
            android.util.Log.e("ProfessionalExportRepository", "❌ CSV export failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    fun shareFile(file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = mimeType
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        
        return shareIntent
    }
    
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
}
