package com.cubiquitous.tracura.service

import android.graphics.Bitmap
import com.cubiquitous.tracura.model.DepartmentLineItemData
import com.cubiquitous.tracura.model.MatchedLineItemResult
import com.cubiquitous.tracura.model.ReceiptResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptAnalysisService {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun analyze(
        bitmap: Bitmap,
        candidateCategories: List<String> = emptyList(),
        candidateItems: List<String> = emptyList()
    ): ReceiptResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.process(inputImage).await()
        return parseText(visionText, candidateCategories, candidateItems)
    }

    // ---------------------------------------------------------------------------
    // Fuzzy matching — Levenshtein distance
    // ---------------------------------------------------------------------------

    /**
     * Classic dynamic-programming Levenshtein edit distance (case-insensitive).
     */
    internal fun levenshteinDistance(s: String, t: String): Int {
        val a = s.lowercase()
        val b = t.lowercase()
        val m = a.length
        val n = b.length
        if (m == 0) return n
        if (n == 0) return m
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[m][n]
    }

    /**
     * Returns the best-matching candidate string for [query], or null when no
     * candidate reaches [threshold] similarity (0..1, 1 = perfect match).
     *
     * Matching priority:
     *  1. Exact case-insensitive substring (fast path, no edit distance needed)
     *  2. Highest Levenshtein-similarity candidate that is >= [threshold]
     */
    fun fuzzyBestMatch(
        query: String,
        candidates: List<String>,
        threshold: Float = 0.60f
    ): String? {
        if (candidates.isEmpty() || query.isBlank()) return null
        val qLower = query.lowercase().trim()

        // Fast path: exact or substring match
        val substringMatch = candidates.firstOrNull { c ->
            val cLower = c.lowercase()
            cLower.contains(qLower) || qLower.contains(cLower)
        }
        if (substringMatch != null) return substringMatch

        // Levenshtein similarity: 1 - (editDist / maxLen)
        return candidates
            .map { candidate ->
                val cLower = candidate.lowercase()
                val maxLen = maxOf(qLower.length, cLower.length)
                val similarity =
                    if (maxLen == 0) 1f
                    else 1f - levenshteinDistance(qLower, cLower).toFloat() / maxLen
                candidate to similarity
            }
            .filter { (_, sim) -> sim >= threshold }
            .maxByOrNull { (_, sim) -> sim }
            ?.first
    }

    // ---------------------------------------------------------------------------

    private fun parseText(
        visionText: Text,
        candidateCategories: List<String> = emptyList(),
        candidateItems: List<String> = emptyList()
    ): ReceiptResult {
        val rowStrings = buildLogicalRows(visionText)

        var date: String? = null
        var amount: String? = null
        var department: String? = null
        var categories: String? = null
        var modeOfPayment: String? = null
        var description: String? = null
        var itemType: String? = null
        var item: String? = null
        var brand: String? = null
        var spec: String? = null
        var thickness: String? = null
        var quantity: String? = null
        var uom: String? = null
        var unitPrice: String? = null

        // keyword alias → field name
        val keywordAliases: Map<String, String> = mapOf(
            "date" to "date",
            "bill date" to "date",
            "invoice date" to "date",
            "amount" to "amount",
            "total" to "amount",
            "total amount" to "amount",
            "grand total" to "amount",
            "net amount" to "amount",
            "department" to "department",
            "dept" to "department",
            "division" to "department",
            "category" to "categories",
            "categories" to "categories",
            "expense type" to "categories",
            "mode of payment" to "modeOfPayment",
            "payment mode" to "modeOfPayment",
            "paid via" to "modeOfPayment",
            "description" to "description",
            "details" to "description",
            "remarks" to "description",
            "purpose" to "description",
            "narration" to "description",
            "item type" to "itemType",
            "sub category" to "itemType",
            "item" to "item",
            "material" to "item",
            "product" to "item",
            "brand" to "brand",
            "make" to "brand",
            "manufacturer" to "brand",
            "spec" to "spec",
            "grade" to "spec",
            "specification" to "spec",
            "thickness" to "thickness",
            "dia" to "thickness",
            "diameter" to "thickness",
            "size" to "thickness",
            "quantity" to "quantity",
            "qty" to "quantity",
            "uom" to "uom",
            "unit" to "uom",
            "unit of measure" to "uom",
            "unit price" to "unitPrice",
            "rate" to "unitPrice",
            "price per unit" to "unitPrice",
            "mrp" to "unitPrice"
        )

        // Sorted by length descending so longer keywords take precedence (e.g. "grand total" before "total")
        val sortedKeywords = keywordAliases.entries.sortedByDescending { it.key.length }

        for (i in rowStrings.indices) {
            val rowLower = rowStrings[i].lowercase().trim()

            for ((keyword, field) in sortedKeywords) {
                if (!rowLower.startsWith(keyword)) continue

                val afterKeyword = rowLower.substring(keyword.length).trim().trimStart(':', '-', ' ')
                val rawAfter = rowStrings[i].substring(
                    rowStrings[i].lowercase().indexOf(keyword) + keyword.length
                ).trim().trimStart(':', '-', ' ')
                val nextRow = rowStrings.getOrNull(i + 1) ?: ""

                val value = when (field) {
                    "date" -> {
                        extractDateValue(afterKeyword) ?: extractDateValue(nextRow.lowercase())
                    }
                    "amount", "unitPrice", "quantity" -> {
                        extractNumericValue(afterKeyword) ?: extractNumericValue(nextRow)
                    }
                    "modeOfPayment" -> {
                        detectPaymentMode(rawAfter.ifEmpty { nextRow })
                    }
                    else -> {
                        rawAfter.ifEmpty { nextRow.trim() }.takeIf { it.isNotBlank() }
                    }
                } ?: continue

                when (field) {
                    "date" -> if (date == null) date = value
                    "amount" -> if (amount == null) amount = value
                    "department" -> if (department == null) department = value
                    "categories" -> if (categories == null) categories = value
                    "modeOfPayment" -> if (modeOfPayment == null) modeOfPayment = value
                    "description" -> if (description == null) description = value
                    "itemType" -> if (itemType == null) itemType = value
                    "item" -> if (item == null) item = value
                    "brand" -> if (brand == null) brand = value
                    "spec" -> if (spec == null) spec = value
                    "thickness" -> if (thickness == null) thickness = value
                    "quantity" -> if (quantity == null) quantity = value
                    "uom" -> if (uom == null) uom = value
                    "unitPrice" -> if (unitPrice == null) unitPrice = value
                }
                break
            }
        }

        // Thickness fallback: regex scan for "16mm" / "16 mm" / "16.5mm" patterns
        if (thickness == null) {
            val fullText = rowStrings.joinToString(" ")
            val thicknessPattern = Regex("""\b(\d+(?:\.\d+)?)\s*mm\b""", RegexOption.IGNORE_CASE)
            thicknessPattern.find(fullText)?.let { match ->
                thickness = "${match.groupValues[1]} mm"
            }
        }

        // Grade/spec fallback: regex scan for Fe500, M25, 43 Grade, IS2062 patterns
        if (spec == null) {
            val fullText = rowStrings.joinToString(" ")
            val gradePattern = Regex(
                """\b(Fe\s*\d{3}D?|M\s*\d{2,3}|\d{2,3}\s*[Gg]rade|IS\s*\d{4,5}(?:\s*E\s*\d+)?)\b"""
            )
            gradePattern.find(fullText)?.let { spec = it.value.trim() }
        }

        // Amount fallback: largest ₹/$ number in full text
        if (amount == null) {
            val fullText = rowStrings.joinToString(" ")
            val currencyPattern = Regex("[₹\$]\\s*([\\d,]+\\.?\\d*)")
            amount = currencyPattern.findAll(fullText)
                .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
                .maxOrNull()
                ?.let { it.toLong().toString() }
        }

        // Date fallback: scan all text
        if (date == null) {
            val fullText = rowStrings.joinToString(" ")
            date = extractDateValue(fullText)
        }

        // Payment mode fallback: scan full text for keywords
        if (modeOfPayment == null) {
            val fullText = rowStrings.joinToString(" ").lowercase()
            modeOfPayment = detectPaymentMode(fullText)
        }

        // Apply fuzzy matching for category and item fields
        if (categories != null && candidateCategories.isNotEmpty()) {
            categories = fuzzyBestMatch(categories!!, candidateCategories) ?: categories
        }
        if (item != null && candidateItems.isNotEmpty()) {
            item = fuzzyBestMatch(item!!, candidateItems) ?: item
        }

        return ReceiptResult(
            date = date,
            amount = amount,
            department = department,
            categories = categories,
            modeOfPayment = modeOfPayment,
            description = description,
            itemType = itemType,
            item = item,
            brand = brand,
            spec = spec,
            thickness = thickness,
            quantity = quantity,
            uom = uom,
            unitPrice = unitPrice
        )
    }

    private fun buildLogicalRows(visionText: Text): List<String> {
        data class LineEntry(val yMid: Float, val xLeft: Int, val text: String)

        val entries = visionText.textBlocks
            .flatMap { block -> block.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                LineEntry(
                    yMid = (box.top + box.bottom) / 2f,
                    xLeft = box.left,
                    text = line.text
                )
            }
            .sortedBy { it.yMid }

        if (entries.isEmpty()) return emptyList()

        val rows = mutableListOf<MutableList<LineEntry>>()
        val currentRow = mutableListOf<LineEntry>()
        currentRow.add(entries.first())

        for (i in 1 until entries.size) {
            val entry = entries[i]
            val rowRefY = currentRow.first().yMid
            if (Math.abs(entry.yMid - rowRefY) <= 15f) {
                currentRow.add(entry)
            } else {
                rows.add(currentRow.toMutableList())
                currentRow.clear()
                currentRow.add(entry)
            }
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        return rows.map { row ->
            row.sortedBy { it.xLeft }.joinToString(" ") { it.text }
        }
    }

    private fun extractNumericValue(text: String): String? {
        // ₹ or $ prefix
        Regex("[₹\$]\\s*([\\d,]+\\.?\\d*)").find(text)
            ?.groupValues?.get(1)?.replace(",", "")
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        // colon or dash prefix
        Regex("[-:]\\s*([\\d,]+\\.?\\d*)").find(text)
            ?.groupValues?.get(1)?.replace(",", "")
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        // standalone monetary number
        Regex("\\b(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)\\b").find(text)
            ?.groupValues?.get(1)?.replace(",", "")
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        return null
    }

    private fun extractDateValue(text: String): String? {
        // dd/MM/yyyy or dd-MM-yyyy
        Regex("\\b(\\d{2})[/\\-](\\d{2})[/\\-](\\d{4})\\b").find(text)?.let { m ->
            val (d, mo, y) = Triple(m.groupValues[1], m.groupValues[2], m.groupValues[3])
            return "$d/$mo/$y"
        }

        // yyyy-MM-dd
        Regex("\\b(\\d{4})[/\\-](\\d{2})[/\\-](\\d{2})\\b").find(text)?.let { m ->
            val (y, mo, d) = Triple(m.groupValues[1], m.groupValues[2], m.groupValues[3])
            return "$d/$mo/$y"
        }

        // dd MMM yyyy (e.g. "15 Jan 2024" or "15 January 2024")
        Regex("\\b(\\d{1,2})\\s+([A-Za-z]{3,9})\\s+(\\d{4})\\b").find(text)?.let { m ->
            return tryParseMonthName(m.groupValues[1], m.groupValues[2], m.groupValues[3])
        }

        return null
    }

    private fun tryParseMonthName(day: String, month: String, year: String): String? {
        val formats = listOf("dd MMMM yyyy", "dd MMM yyyy", "d MMMM yyyy", "d MMM yyyy")
        for (fmt in formats) {
            return try {
                val parsed = SimpleDateFormat(fmt, Locale.ENGLISH).parse("$day $month $year")!!
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsed)
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    private fun detectPaymentMode(text: String): String? {
        val t = text.lowercase()
        return when {
            t.contains("upi") || t.contains("phonepe") || t.contains("gpay") ||
            t.contains("paytm") || t.contains("bhim") -> "upi"
            t.contains("cheque") || t.contains("check") || t.contains("chq") -> "cheque"
            t.contains("card") || t.contains("credit") || t.contains("debit") ||
            t.contains("swipe") || t.contains("visa") || t.contains("mastercard") -> "card"
            else -> null
        }
    }

    // ---------------------------------------------------------------------------
    // Line-item matching  (mirrors Swift ReceiptAnalysisService.analyzeReceiptForLineItems)
    // ---------------------------------------------------------------------------

    /**
     * Runs ML Kit OCR on [bitmap], then fuzzy-matches each OCR row against
     * [availableLineItems] by item name.  For each match the quantity and
     * unit-price found nearby on the receipt are returned alongside the
     * predefined line-item record.
     *
     * Progress is reported via [onProgress] (0.0 → OCR, 0.8 → done).
     */
    suspend fun analyzeReceiptForLineItems(
        bitmap: Bitmap,
        availableLineItems: List<DepartmentLineItemData>,
        onProgress: (Float) -> Unit = {}
    ): List<MatchedLineItemResult> {
        if (availableLineItems.isEmpty()) return emptyList()

        onProgress(0.1f)
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.process(inputImage).await()
        val rows = buildLogicalRows(visionText)
        onProgress(0.3f)

        if (rows.isEmpty()) return emptyList()

        val results = mutableListOf<MatchedLineItemResult>()
        val usedRowIndices = mutableSetOf<Int>()
        val matchThreshold = 0.55f

        availableLineItems.forEachIndexed { liIdx, lineItem ->
            val searchName = lineItem.item.ifBlank { lineItem.itemType }
            if (searchName.isBlank()) return@forEachIndexed

            var bestRowIndex = -1
            var bestSim = 0f

            rows.forEachIndexed { idx, row ->
                if (idx in usedRowIndices) return@forEachIndexed
                val sim = computeRowSimilarity(searchName, row)
                if (sim > bestSim) {
                    bestSim = sim
                    bestRowIndex = idx
                }
            }

            if (bestRowIndex < 0 || bestSim < matchThreshold) return@forEachIndexed
            usedRowIndices.add(bestRowIndex)

            val qty   = extractQtyFromContext(rows, bestRowIndex)   ?: lineItem.quantity
            val price = extractPriceFromContext(rows, bestRowIndex) ?: lineItem.unitPrice

            results.add(
                MatchedLineItemResult(
                    matchedLineItem    = lineItem,
                    extractedQuantity  = qty.coerceAtLeast(0.0),
                    extractedUnitPrice = price.coerceAtLeast(0.0),
                    confidence         = bestSim
                )
            )

            // Emit progress proportional to items processed
            onProgress(0.3f + 0.5f * (liIdx + 1).toFloat() / availableLineItems.size)
        }

        onProgress(0.8f)
        return results
    }

    /**
     * Returns a similarity score [0..1] for how well [query] appears in [row].
     * Priority: exact substring > word-overlap > Levenshtein on trimmed window.
     */
    private fun computeRowSimilarity(query: String, row: String): Float {
        val qLower = query.lowercase().trim()
        val rLower = row.lowercase()

        if (rLower.contains(qLower)) return 0.90f
        if (qLower.isNotBlank() && rLower.trim() == qLower) return 1.0f

        // Word-overlap: count query words present in the row
        val queryWords = qLower.split(Regex("\\s+")).filter { it.length > 2 }
        if (queryWords.isNotEmpty()) {
            val matched = queryWords.count { rLower.contains(it) }
            val ratio = matched.toFloat() / queryWords.size
            if (ratio >= 0.6f) return 0.70f + ratio * 0.20f
        }

        // Levenshtein on a sliding window the same length as the query
        if (rLower.length >= qLower.length) {
            var bestWindowSim = 0f
            val winLen = minOf(qLower.length + 6, rLower.length)
            for (start in 0..rLower.length - qLower.length) {
                val slice  = rLower.substring(start, minOf(start + winLen, rLower.length))
                val maxLen = maxOf(qLower.length, slice.length)
                val sim    = if (maxLen == 0) 0f
                             else 1f - levenshteinDistance(qLower, slice).toFloat() / maxLen
                if (sim > bestWindowSim) bestWindowSim = sim
            }
            return bestWindowSim
        }

        val maxLen = maxOf(qLower.length, rLower.length)
        return if (maxLen == 0) 0f
               else 1f - levenshteinDistance(qLower, rLower).toFloat() / maxLen
    }

    /** Extract a quantity value from rows surrounding [centerIdx]. */
    private fun extractQtyFromContext(rows: List<String>, centerIdx: Int): Double? {
        val context = buildContext(rows, centerIdx)

        // Explicit keyword: "Qty: 5", "Quantity 10"
        Regex("""(?:qty|quantity|nos?|pcs?)\s*[:.=]?\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .find(context)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }

        // Unit-suffixed: "5 bags", "10 kg", "3 rmt"
        Regex("""(\d+(?:\.\d+)?)\s*(?:nos?|pcs?|bags?|kg|mt|rmt|sqm|sqft|ltr|ton)\b""", RegexOption.IGNORE_CASE)
            .find(context)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }

        // First standalone small integer 1–9999 that is not part of a price
        Regex("""(?<![₹\$\d,])(\b[1-9]\d{0,3}\b)(?!\s*[,.]?\d{3})(?!\.\d{2}\b)""")
            .find(context)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }

        return null
    }

    /** Extract a unit price from rows surrounding [centerIdx]. */
    private fun extractPriceFromContext(rows: List<String>, centerIdx: Int): Double? {
        val context = buildContext(rows, centerIdx)

        // Currency-prefixed: "₹ 1,200", "$ 450.00"
        Regex("""[₹\$]\s*([\d,]+(?:\.\d{1,2})?)""")
            .find(context)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let { return it }

        // Keyword-prefixed: "Rate: 1200", "Unit Price 450"
        Regex("""(?:rate|price|unit\s*price|mrp|cost)\s*[:.=]?\s*[₹\$]?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
            .find(context)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let { return it }

        // Trailing decimal value (e.g. "1200.00" at end of row — looks like a price)
        Regex("""([\d,]+\.\d{2})\s*$""", RegexOption.MULTILINE)
            .findAll(context).mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .maxOrNull()?.let { return it }

        return null
    }

    private fun buildContext(rows: List<String>, centerIdx: Int): String {
        val range = maxOf(0, centerIdx - 1)..minOf(rows.size - 1, centerIdx + 1)
        return range.joinToString(" ") { rows[it] }
    }
}
