package com.cubiquitous.tracura.service

import android.content.Context
import com.cubiquitous.tracura.model.ExportData
import com.cubiquitous.tracura.model.DetailedExpense
import com.cubiquitous.tracura.utils.FormatUtils
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfessionalReportGenerator @Inject constructor(
    private val context: Context
) {
    
    suspend fun generateProfessionalReport(
        exportData: ExportData,
        projectName: String = "Tracura",
        companyName: String = "Tracura"
    ): Result<File> {
        return try {
            val fileName = "${projectName.replace(" ", "_")}_Report_${getCurrentTimestamp()}.pdf"
            val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Set margins
            document.setMargins(40f, 40f, 40f, 40f)
            
            // Generate the report
            addHeader(document, projectName, companyName, exportData)
            addReportMetadata(document, exportData)
            addExecutiveSummary(document, exportData)
            addExpenseCategoriesBreakdown(document, exportData)
            addDepartmentBudgetAnalysis(document, exportData)
            addFooter(document)
            
            document.close()
            Result.success(file)
            
        } catch (e: Exception) {
            android.util.Log.e("ProfessionalReportGenerator", "Error generating PDF: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun addHeader(document: Document, projectName: String, companyName: String, exportData: ExportData) {
        // Company logo placeholder (blue square with text)
        val logoTable = Table(1)
        logoTable.setWidth(UnitValue.createPercentValue(100f))
        
        val logoCell = Cell()
        logoCell.setBackgroundColor(ColorConstants.BLUE)
        logoCell.setPadding(15f)
        logoCell.setBorder(Border.NO_BORDER)
        
        val logoText = Paragraph("Tracura")
            .setFontSize(16f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.WHITE)
        
        logoCell.add(logoText)
        logoTable.addCell(logoCell)
        
        document.add(logoTable)
        document.add(Paragraph().setHeight(10f))
        
        // Company name and report title
        val titleTable = Table(1)
        titleTable.setWidth(UnitValue.createPercentValue(100f))
        
        val companyNameCell = Cell()
        companyNameCell.setBorder(Border.NO_BORDER)
        companyNameCell.setPadding(0f)
        
        val companyNameText = Paragraph(companyName)
            .setFontSize(24f)
            .setBold()
            .setTextAlignment(TextAlignment.LEFT)
            .setMargin(0f)
        
        val reportTitleText = Paragraph("Project Financial Report")
            .setFontSize(14f)
            .setTextAlignment(TextAlignment.LEFT)
            .setFontColor(ColorConstants.BLUE)
            .setMargin(5f)
        
        companyNameCell.add(companyNameText)
        companyNameCell.add(reportTitleText)
        titleTable.addCell(companyNameCell)
        
        document.add(titleTable)
        document.add(Paragraph().setHeight(20f))
    }
    
    private fun addReportMetadata(document: Document, exportData: ExportData) {
        val metadataTable = Table(2)
        metadataTable.setWidth(UnitValue.createPercentValue(100f))
        metadataTable.setBackgroundColor(ColorConstants.DARK_GRAY)
        
        // Left column
        val leftCell = Cell()
        leftCell.setBorder(Border.NO_BORDER)
        leftCell.setPadding(15f)
        leftCell.setVerticalAlignment(VerticalAlignment.TOP)
        
        val reportGenerated = Paragraph("Report Generated:")
            .setFontSize(10f)
            .setFontColor(ColorConstants.WHITE)
            .setMargin(0f)
        
        val generatedDate = Paragraph(SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault()).format(exportData.generatedAt.toDate()))
            .setFontSize(10f)
            .setFontColor(ColorConstants.WHITE)
            .setMargin(2f)
        
        val systemVersion = Paragraph("System Version:")
            .setFontSize(10f)
            .setFontColor(ColorConstants.WHITE)
            .setMargin(5f)
        
        val version = Paragraph("Tracura v1.0.2")
            .setFontSize(10f)
            .setFontColor(ColorConstants.WHITE)
            .setMargin(2f)
        
        leftCell.add(reportGenerated)
        leftCell.add(generatedDate)
        leftCell.add(systemVersion)
        leftCell.add(version)
        
        // Right column
        val rightCell = Cell()
        rightCell.setBorder(Border.NO_BORDER)
        rightCell.setPadding(15f)
        rightCell.setVerticalAlignment(VerticalAlignment.TOP)
        
        val reportPeriod = Paragraph("Report Period:")
            .setFontSize(10f)
            .setFontColor(ColorConstants.WHITE)
            .setMargin(0f)
        
        val period = Paragraph(exportData.timeRange)
            .setFontSize(10f)
            .setFontColor(ColorConstants.WHITE)
            .setMargin(2f)
        
        val departmentFilter = Paragraph("Department Filter:")
            .setFontSize(10f)
            .setFontColor(ColorConstants.WHITE)
            .setMargin(5f)
        
        val department = Paragraph(exportData.department)
            .setFontSize(10f)
            .setFontColor(ColorConstants.WHITE)
            .setMargin(2f)
        
        rightCell.add(reportPeriod)
        rightCell.add(period)
        rightCell.add(departmentFilter)
        rightCell.add(department)
        
        metadataTable.addCell(leftCell)
        metadataTable.addCell(rightCell)
        
        document.add(metadataTable)
        document.add(Paragraph().setHeight(20f))
    }
    
    private fun addExecutiveSummary(document: Document, exportData: ExportData) {
        // Section title
        val sectionTitle = Paragraph("Executive Summary")
            .setFontSize(16f)
            .setBold()
            .setMargin(0f)
            .setMarginBottom(15f)
        
        document.add(sectionTitle)
        
        // Summary cards
        val summaryTable = Table(3)
        summaryTable.setWidth(UnitValue.createPercentValue(100f))
        
        // Total Expenses card
        val totalExpensesCard = createSummaryCard(
            "Total Expenses",
            FormatUtils.formatCurrencySimple(exportData.totalSpent),
            ColorConstants.BLUE,
            "Total amount spent across all categories"
        )
        summaryTable.addCell(totalExpensesCard)
        
        // Budget Utilization card
        val budgetUtilization = calculateBudgetUtilization(exportData)
        val budgetUtilizationCard = createSummaryCard(
            "Budget Utilization",
            "${budgetUtilization.toInt()}%",
            ColorConstants.GREEN,
            "Percentage of total budget utilized"
        )
        summaryTable.addCell(budgetUtilizationCard)
        
        // Remaining Budget card
        val remainingBudget = calculateRemainingBudget(exportData)
        val remainingBudgetCard = createSummaryCard(
            "Remaining Budget",
            FormatUtils.formatCurrencySimple(remainingBudget),
            ColorConstants.GREEN,
            "Amount remaining in budget"
        )
        summaryTable.addCell(remainingBudgetCard)
        
        document.add(summaryTable)
        document.add(Paragraph().setHeight(20f))
    }
    
    private fun createSummaryCard(title: String, value: String, color: com.itextpdf.kernel.colors.Color, description: String): Cell {
        val card = Cell()
        card.setBorder(SolidBorder(color, 1f))
        card.setPadding(15f)
        card.setBackgroundColor(ColorConstants.LIGHT_GRAY)
        
        val titleText = Paragraph(title)
            .setFontSize(12f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMargin(0f)
            .setMarginBottom(5f)
        
        val valueText = Paragraph(value)
            .setFontSize(18f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(color)
            .setMargin(0f)
        
        card.add(titleText)
        card.add(valueText)
        
        return card
    }
    
    private fun addExpenseCategoriesBreakdown(document: Document, exportData: ExportData) {
        // Section title
        val sectionTitle = Paragraph("Expense Categories Breakdown")
            .setFontSize(16f)
            .setBold()
            .setMargin(0f)
            .setMarginBottom(15f)
        
        document.add(sectionTitle)
        
        if (exportData.categoryBreakdown.isNotEmpty()) {
            // Create detailed table
            addCategoryTable(document, exportData.categoryBreakdown)
        } else {
            val noDataText = Paragraph("No expense categories found for the selected period.")
                .setFontSize(12f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
            
            document.add(noDataText)
        }
        
        document.add(Paragraph().setHeight(20f))
    }
    
    private fun addCategoryTable(document: Document, categoryBreakdown: Map<String, Double>) {
        val totalAmount = categoryBreakdown.values.sum()
        
        val table = Table(4)
        table.setWidth(UnitValue.createPercentValue(100f))
        
        // Header
        val headerStyle = Cell()
        headerStyle.setBackgroundColor(ColorConstants.DARK_GRAY)
        headerStyle.setPadding(10f)
        headerStyle.setBorder(Border.NO_BORDER)
        
        val categoryHeader = Paragraph("Category")
            .setFontSize(12f)
            .setBold()
            .setFontColor(ColorConstants.WHITE)
            .setMargin(0f)
        
        val amountHeader = Paragraph("Amount")
            .setFontSize(12f)
            .setBold()
            .setFontColor(ColorConstants.WHITE)
            .setMargin(0f)
            .setTextAlignment(TextAlignment.CENTER)
        
        val percentageHeader = Paragraph("Percentage")
            .setFontSize(12f)
            .setBold()
            .setFontColor(ColorConstants.WHITE)
            .setMargin(0f)
            .setTextAlignment(TextAlignment.CENTER)
        
        val visualHeader = Paragraph("Visual")
            .setFontSize(12f)
            .setBold()
            .setFontColor(ColorConstants.WHITE)
            .setMargin(0f)
            .setTextAlignment(TextAlignment.CENTER)
        
        headerStyle.add(categoryHeader)
        table.addCell(headerStyle)
        
        val amountHeaderCell = Cell()
        amountHeaderCell.setBackgroundColor(ColorConstants.DARK_GRAY)
        amountHeaderCell.setPadding(10f)
        amountHeaderCell.setBorder(Border.NO_BORDER)
        amountHeaderCell.add(amountHeader)
        table.addCell(amountHeaderCell)
        
        val percentageHeaderCell = Cell()
        percentageHeaderCell.setBackgroundColor(ColorConstants.DARK_GRAY)
        percentageHeaderCell.setPadding(10f)
        percentageHeaderCell.setBorder(Border.NO_BORDER)
        percentageHeaderCell.add(percentageHeader)
        table.addCell(percentageHeaderCell)
        
        val visualHeaderCell = Cell()
        visualHeaderCell.setBackgroundColor(ColorConstants.DARK_GRAY)
        visualHeaderCell.setPadding(10f)
        visualHeaderCell.setBorder(Border.NO_BORDER)
        visualHeaderCell.add(visualHeader)
        table.addCell(visualHeaderCell)
        
        // Data rows
        categoryBreakdown.toList().sortedByDescending { it.second }.forEach { (category, amount) ->
            val percentage = if (totalAmount > 0) (amount / totalAmount * 100) else 0.0
            
            // Category cell
            val categoryCell = Cell()
            categoryCell.setPadding(10f)
            categoryCell.setBorder(Border.NO_BORDER)
            categoryCell.setBackgroundColor(ColorConstants.WHITE)
            
            val categoryText = Paragraph(category)
                .setFontSize(11f)
                .setMargin(0f)
            
            categoryCell.add(categoryText)
            table.addCell(categoryCell)
            
            // Amount cell
            val amountCell = Cell()
            amountCell.setPadding(10f)
            amountCell.setBorder(Border.NO_BORDER)
            amountCell.setBackgroundColor(ColorConstants.WHITE)
            amountCell.setTextAlignment(TextAlignment.CENTER)
            
            val amountText = Paragraph(FormatUtils.formatCurrencySimple(amount))
                .setFontSize(11f)
                .setMargin(0f)
            
            amountCell.add(amountText)
            table.addCell(amountCell)
            
            // Percentage cell
            val percentageCell = Cell()
            percentageCell.setPadding(10f)
            percentageCell.setBorder(Border.NO_BORDER)
            percentageCell.setBackgroundColor(ColorConstants.WHITE)
            percentageCell.setTextAlignment(TextAlignment.CENTER)
            
            val percentageText = Paragraph("${percentage.toInt()}%")
                .setFontSize(11f)
                .setMargin(0f)
            
            percentageCell.add(percentageText)
            table.addCell(percentageCell)
            
            // Visual cell (progress bar)
            val visualCell = Cell()
            visualCell.setPadding(10f)
            visualCell.setBorder(Border.NO_BORDER)
            visualCell.setBackgroundColor(ColorConstants.WHITE)
            
            val progressBar = createProgressBar(percentage.toFloat(), 100f)
            visualCell.add(progressBar)
            table.addCell(visualCell)
        }
        
        document.add(table)
    }
    
    private fun createProgressBar(percentage: Float, maxWidth: Float): Table {
        val progressTable = Table(1)
        progressTable.setWidth(UnitValue.createPercentValue(100f))
        progressTable.setBorder(Border.NO_BORDER)
        
        val progressCell = Cell()
        progressCell.setHeight(8f)
        progressCell.setBackgroundColor(ColorConstants.BLUE)
        progressCell.setBorder(Border.NO_BORDER)
        progressCell.setPadding(0f)
        
        val progressWidth = (percentage / 100f * maxWidth).coerceAtMost(maxWidth)
        progressCell.setWidth(UnitValue.createPointValue(progressWidth))
        
        progressTable.addCell(progressCell)
        
        return progressTable
    }
    
    private fun addDepartmentBudgetAnalysis(document: Document, exportData: ExportData) {
        // Section title
        val sectionTitle = Paragraph("Department Budget Analysis")
            .setFontSize(16f)
            .setBold()
            .setMargin(0f)
            .setMarginBottom(15f)
        
        document.add(sectionTitle)
        
        // Create department analysis table
        val table = Table(5)
        table.setWidth(UnitValue.createPercentValue(100f))
        
        // Header
        val headerStyle = Cell()
        headerStyle.setBackgroundColor(ColorConstants.DARK_GRAY)
        headerStyle.setPadding(10f)
        headerStyle.setBorder(Border.NO_BORDER)
        
        val headers = listOf("Department", "Budget", "Spent", "Remaining", "Status")
        headers.forEach { headerText ->
            val headerCell = Cell()
            headerCell.setBackgroundColor(ColorConstants.DARK_GRAY)
            headerCell.setPadding(10f)
            headerCell.setBorder(Border.NO_BORDER)
            
            val header = Paragraph(headerText)
                .setFontSize(12f)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setMargin(0f)
                .setTextAlignment(TextAlignment.CENTER)
            
            headerCell.add(header)
            table.addCell(headerCell)
        }
        
        // Sample data row (you can replace this with actual department data)
        val departmentData = listOf(
            Triple("Marketing", 100000.0, exportData.totalSpent * 0.15),
            Triple("Operations", 150000.0, exportData.totalSpent * 0.25),
            Triple("Development", 200000.0, exportData.totalSpent * 0.35),
            Triple("Sales", 80000.0, exportData.totalSpent * 0.15),
            Triple("Support", 50000.0, exportData.totalSpent * 0.10)
        )
        
        departmentData.forEach { (department, budget, spent) ->
            val remaining = budget - spent
            
            // Department
            val deptCell = Cell()
            deptCell.setPadding(10f)
            deptCell.setBorder(Border.NO_BORDER)
            deptCell.setBackgroundColor(ColorConstants.WHITE)
            
            val deptText = Paragraph(department)
                .setFontSize(11f)
                .setMargin(0f)
            
            deptCell.add(deptText)
            table.addCell(deptCell)
            
            // Budget
            val budgetCell = Cell()
            budgetCell.setPadding(10f)
            budgetCell.setBorder(Border.NO_BORDER)
            budgetCell.setBackgroundColor(ColorConstants.WHITE)
            budgetCell.setTextAlignment(TextAlignment.CENTER)
            
            val budgetText = Paragraph(FormatUtils.formatCurrencySimple(budget))
                .setFontSize(11f)
                .setMargin(0f)
            
            budgetCell.add(budgetText)
            table.addCell(budgetCell)
            
            // Spent
            val spentCell = Cell()
            spentCell.setPadding(10f)
            spentCell.setBorder(Border.NO_BORDER)
            spentCell.setBackgroundColor(ColorConstants.WHITE)
            spentCell.setTextAlignment(TextAlignment.CENTER)
            
            val spentText = Paragraph(FormatUtils.formatCurrencySimple(spent))
                .setFontSize(11f)
                .setMargin(0f)
            
            spentCell.add(spentText)
            table.addCell(spentCell)
            
            // Remaining
            val remainingCell = Cell()
            remainingCell.setPadding(10f)
            remainingCell.setBorder(Border.NO_BORDER)
            remainingCell.setBackgroundColor(ColorConstants.WHITE)
            remainingCell.setTextAlignment(TextAlignment.CENTER)
            
            val remainingText = Paragraph(FormatUtils.formatCurrencySimple(remaining))
                .setFontSize(11f)
                .setMargin(0f)
            
            remainingCell.add(remainingText)
            table.addCell(remainingCell)
            
            // Status
            val statusCell = Cell()
            statusCell.setPadding(10f)
            statusCell.setBorder(Border.NO_BORDER)
            statusCell.setBackgroundColor(ColorConstants.WHITE)
            statusCell.setTextAlignment(TextAlignment.CENTER)
            
            val statusText = Paragraph("✔ On Track")
                .setFontSize(11f)
                .setMargin(0f)
                .setFontColor(ColorConstants.GREEN)
            
            statusCell.add(statusText)
            table.addCell(statusCell)
        }
        
        document.add(table)
        
        // Footer text
        val footerText = Paragraph("Generated on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())} - For authorized personnel only")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.GRAY)
            .setMargin(10f)
        
        document.add(footerText)
    }
    
    private fun addFooter(document: Document) {
        // Add some space
        document.add(Paragraph().setHeight(30f))
        
        // Footer text
        val footerText = Paragraph("This report was generated automatically by Tracura Expense Management System")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.GRAY)
            .setMargin(10f)
        
        document.add(footerText)
    }
    
    private fun calculateBudgetUtilization(exportData: ExportData): Double {
        // This is a sample calculation - you should replace with actual budget data
        val totalBudget = 100000.0 // Sample total budget
        return if (totalBudget > 0) (exportData.totalSpent / totalBudget * 100) else 0.0
    }
    
    private fun calculateRemainingBudget(exportData: ExportData): Double {
        // This is a sample calculation - you should replace with actual budget data
        val totalBudget = 100000.0 // Sample total budget
        return (totalBudget - exportData.totalSpent).coerceAtLeast(0.0)
    }
    
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
}