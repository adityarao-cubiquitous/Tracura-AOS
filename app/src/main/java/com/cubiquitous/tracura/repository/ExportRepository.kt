package com.cubiquitous.tracura.repository

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.cubiquitous.tracura.model.ExportData
import com.cubiquitous.tracura.model.DetailedExpense
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.service.ProfessionalReportGenerator
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.Timestamp

@Singleton
class ExportRepository @Inject constructor(
    private val context: Context,
    private val professionalReportGenerator: ProfessionalReportGenerator
) {
    
    suspend fun exportToPDF(exportData: ExportData): Result<File> {
        return try {
            android.util.Log.d("ExportRepository", "🔄 Starting PDF export...")
            android.util.Log.d("ExportRepository", "📊 Export data: ${exportData.detailedExpenses.size} expenses, ₹${exportData.totalSpent}")
            
            val fileName = "expense_report_${getCurrentTimestamp()}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            android.util.Log.d("ExportRepository", "📁 Creating PDF file at: ${file.absolutePath}")
            
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Title
            document.add(
                Paragraph("Expense Report")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20f)
                    .setBold()
            )
            
            // Report Info
            document.add(
                Paragraph("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12f)
            )
            
            document.add(
                Paragraph("Time Range: ${exportData.timeRange}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12f)
            )
            
            document.add(
                Paragraph("Department: ${exportData.department}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12f)
            )
            
            // Total Amount
            document.add(
                Paragraph("Total Amount: ${FormatUtils.formatCurrencySimple(exportData.totalSpent)}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16f)
                    .setBold()
            )
            
            // Category Breakdown
            if (exportData.categoryBreakdown.isNotEmpty()) {
                android.util.Log.d("ExportRepository", "📊 Adding category breakdown: ${exportData.categoryBreakdown.size} categories")
                document.add(Paragraph("Category Breakdown").setBold().setFontSize(14f))
                val categoryTable = Table(2)
                categoryTable.addCell("Category")
                categoryTable.addCell("Amount")
                
                exportData.categoryBreakdown.forEach { (category, amount) ->
                    categoryTable.addCell(category)
                    categoryTable.addCell(FormatUtils.formatCurrencySimple(amount))
                }
                
                document.add(categoryTable)
            }
            
            // Detailed Expenses
            if (exportData.detailedExpenses.isNotEmpty()) {
                android.util.Log.d("ExportRepository", "📋 Adding detailed expenses: ${exportData.detailedExpenses.size} expenses")
                document.add(Paragraph("Detailed Expenses").setBold().setFontSize(14f))
                val expenseTable = Table(5)
                expenseTable.addCell("Date")
                expenseTable.addCell("Invoice")
                expenseTable.addCell("By")
                expenseTable.addCell("Amount")
                expenseTable.addCell("Department")
                
                exportData.detailedExpenses.forEach { expense ->
                    expenseTable.addCell(expense.date?.toDate()?.let { 
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it) 
                    } ?: "N/A")
                    expenseTable.addCell(expense.invoice)
                    expenseTable.addCell(expense.by)
                    expenseTable.addCell(FormatUtils.formatCurrencySimple(expense.amount))
                    expenseTable.addCell(expense.department)
                }
                
                document.add(expenseTable)
            }
            
            document.close()
            
            android.util.Log.d("ExportRepository", "✅ PDF export completed successfully: ${file.absolutePath}")
            android.util.Log.d("ExportRepository", "📏 File size: ${file.length()} bytes")
            
            Result.success(file)
        } catch (e: Exception) {
            android.util.Log.e("ExportRepository", "❌ PDF export failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun exportToCSV(exportData: ExportData): Result<File> {
        return try {
            android.util.Log.d("ExportRepository", "🔄 Starting CSV export...")
            android.util.Log.d("ExportRepository", "📊 Export data: ${exportData.detailedExpenses.size} expenses, ₹${exportData.totalSpent}")
            
            val fileName = "expense_report_${getCurrentTimestamp()}.csv"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            android.util.Log.d("ExportRepository", "📁 Creating CSV file at: ${file.absolutePath}")
            
            val writer = BufferedWriter(FileWriter(file))
            
            // Header information
            writer.write("Expense Report\n")
            writer.write("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
            writer.write("Time Range: ${exportData.timeRange}\n")
            writer.write("Department: ${exportData.department}\n")
            writer.write("Total Amount: ${FormatUtils.formatCurrencySimple(exportData.totalSpent)}\n")
            writer.write("\n")
            
            // Category Breakdown
            if (exportData.categoryBreakdown.isNotEmpty()) {
                android.util.Log.d("ExportRepository", "📊 Adding category breakdown: ${exportData.categoryBreakdown.size} categories")
                writer.write("Category Breakdown\n")
                writer.write("Category,Amount\n")
                
                exportData.categoryBreakdown.forEach { (category, amount) ->
                    writer.write("$category,${FormatUtils.formatCurrencySimple(amount)}\n")
                }
                writer.write("\n")
            }
            
            // Detailed Expenses
            if (exportData.detailedExpenses.isNotEmpty()) {
                android.util.Log.d("ExportRepository", "📋 Adding detailed expenses: ${exportData.detailedExpenses.size} expenses")
                writer.write("Detailed Expenses\n")
                writer.write("Date,Invoice,By,Amount,Department,Mode of Payment\n")
                
                exportData.detailedExpenses.forEach { expense ->
                    val dateStr = expense.date?.toDate()?.let { 
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it) 
                    } ?: "N/A"
                    writer.write("$dateStr,${expense.invoice},${expense.by},${FormatUtils.formatCurrencySimple(expense.amount)},${expense.department},${expense.modeOfPayment}\n")
                }
            }
            
            writer.close()
            
            android.util.Log.d("ExportRepository", "✅ CSV export completed successfully: ${file.absolutePath}")
            android.util.Log.d("ExportRepository", "📏 File size: ${file.length()} bytes")
            
            Result.success(file)
        } catch (e: Exception) {
            android.util.Log.e("ExportRepository", "❌ CSV export failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    
    fun shareFile(file: File, mimeType: String): Intent? {
        return try {
            android.util.Log.d("ExportRepository", "📤 Creating share intent for file: ${file.absolutePath}")
            android.util.Log.d("ExportRepository", "📁 File exists: ${file.exists()}")
            android.util.Log.d("ExportRepository", "📏 File size: ${file.length()} bytes")
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            android.util.Log.d("ExportRepository", "🔗 File URI: $uri")
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Expense Report")
                putExtra(Intent.EXTRA_TEXT, "Please find the attached expense report.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Share Report")
            android.util.Log.d("ExportRepository", "✅ Share intent created successfully")
            
            chooserIntent
        } catch (e: Exception) {
            android.util.Log.e("ExportRepository", "❌ Failed to create share intent: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    // Test function to verify export functionality
    suspend fun testExport(): Result<File> {
        return try {
            android.util.Log.d("ExportRepository", "🧪 Testing export functionality...")
            
            val testData = ExportData(
                totalSpent = 50000.0,
                timeRange = "This Month",
                department = "Test Department",
                categoryBreakdown = mapOf(
                    "Travel" to 20000.0,
                    "Equipment" to 15000.0,
                    "Supplies" to 10000.0,
                    "Other" to 5000.0
                ),
                detailedExpenses = listOf(
                    DetailedExpense(
                        id = "test1",
                        date = Timestamp.now(),
                        invoice = "INV001",
                        by = "John Doe",
                        amount = 20000.0,
                        department = "Test Department",
                        category = "Test Category",
                        description = "Test Description",
                        modeOfPayment = "UPI",
                        attachmentUrl = "",
                        attachmentFileName = "",
                        paymentProofUrl = "",
                        paymentProofFileName = ""
                    ),
                    DetailedExpense(
                        id = "test2",
                        date = Timestamp.now(),
                        invoice = "INV002",
                        by = "Jane Smith",
                        amount = 15000.0,
                        department = "Test Department",
                        category = "Test Category",
                        description = "Test Description",
                        modeOfPayment = "Cash",
                        attachmentUrl = "",
                        attachmentFileName = "",
                        paymentProofUrl = "",
                        paymentProofFileName = ""
                    )
                ),
                generatedAt = Timestamp.now()
            )
            
            val result = exportToPDF(testData)
            android.util.Log.d("ExportRepository", "🧪 Test export result: ${result.isSuccess}")
            result
        } catch (e: Exception) {
            android.util.Log.e("ExportRepository", "❌ Test export failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
} 