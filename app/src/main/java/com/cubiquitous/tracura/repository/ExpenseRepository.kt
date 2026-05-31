package com.cubiquitous.tracura.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.ExpenseSummary
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.*

@Singleton
class ExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val projectRepository: ProjectRepository
) {
    
    // Centralized mapping function to eliminate duplicates
    private fun mapDocumentToExpense(doc: com.google.firebase.firestore.DocumentSnapshot, projectId: String): Expense? {
        return try {
            val expenseData = doc.data ?: return null
            
            // Handle date: can be String (new format) or Timestamp (old format)
            val dateString = when {
                expenseData["date"] is String -> expenseData["date"] as String
                expenseData["date"] is Timestamp -> {
                    val timestamp = expenseData["date"] as Timestamp
                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
                    dateFormat.format(timestamp.toDate())
                }
                else -> ""
            }
            
            // Handle categories: can be List<String> (new format) or String (old format)
            val categoriesList = when {
                expenseData["categories"] is List<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (expenseData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                }
                expenseData["category"] is String -> {
                    val categoryStr = expenseData["category"] as? String ?: ""
                    if (categoryStr.isNotEmpty()) listOf(categoryStr) else emptyList()
                }
                else -> emptyList()
            }
            
            // Handle modeOfPayment: convert String to PaymentMode enum
            val modeOfPaymentStr = expenseData["modeOfPayment"] as? String ?: "cash"
            val modeOfPayment = PaymentMode.fromString(modeOfPaymentStr)
            
            // Handle attachment fields: prefer new format (attachmentURL/attachmentName), fallback to old (attachmentUrl/attachmentFileName)
            val attachmentURL = expenseData["attachmentURL"] as? String 
                ?: expenseData["attachmentUrl"] as? String
            val attachmentName = expenseData["attachmentName"] as? String 
                ?: expenseData["attachmentFileName"] as? String
            
            // Handle payment proof fields: prefer new format, fallback to old
            val paymentProofURL = expenseData["paymentProofURL"] as? String 
                ?: expenseData["paymentProofUrl"] as? String
            val paymentProofName = expenseData["paymentProofName"] as? String 
                ?: expenseData["paymentProofFileName"] as? String
            
            // Handle submittedBy: prefer new format, fallback to userId
            val submittedBy = expenseData["submittedBy"] as? String 
                ?: expenseData["userId"] as? String ?: ""
            
            // Handle status
            val status = when (expenseData["status"] as? String) {
                "APPROVED" -> ExpenseStatus.APPROVED
                "REJECTED" -> ExpenseStatus.REJECTED
                "COMPLETE" -> ExpenseStatus.COMPLETE
                else -> ExpenseStatus.PENDING
            }
            
            // Handle timestamps: prefer createdAt/updatedAt, fallback to submittedAt
            val createdAt = expenseData["createdAt"] as? Timestamp 
                ?: expenseData["submittedAt"] as? Timestamp 
                ?: Timestamp.now()
            val updatedAt = expenseData["updatedAt"] as? Timestamp 
                ?: expenseData["reviewedAt"] as? Timestamp 
                ?: createdAt
            
            Expense(
                id = doc.id,
                expenseCode = expenseData["expenseCode"] as? String ?: "",
                projectId = projectId,
                date = dateString,
                amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                department = expenseData["department"] as? String ?: "",
                phaseId = expenseData["phaseId"] as? String,
                phaseName = expenseData["phaseName"] as? String,
                categories = categoriesList,
                modeOfPayment = modeOfPayment,
                description = expenseData["description"] as? String ?: "",
                attachmentURL = attachmentURL,
                attachmentName = attachmentName,
                paymentProofURL = paymentProofURL,
                paymentProofName = paymentProofName,
                submittedBy = submittedBy,
                status = status,
                remark = expenseData["remark"] as? String,
                isAdmin = expenseData["isAdmin"] as? Boolean ?: false,
                approvedBy = expenseData["approvedBy"] as? String,
                rejectedBy = expenseData["rejectedBy"] as? String ?: "",
                
                // Material Details
                itemType = expenseData["itemType"] as? String,
                item = expenseData["item"] as? String,
                brand = expenseData["brand"] as? String,
                spec = expenseData["spec"] as? String,
                thickness = expenseData["thickness"] as? String,
                quantity = expenseData["quantity"] as? String,
                uom = expenseData["uom"] as? String,
                unitPrice = expenseData["unitPrice"] as? String,
                lineItems = (expenseData["lineItems"] as? List<*>)?.mapNotNull { entry ->
                    (entry as? Map<*, *>)?.let { map ->
                        com.cubiquitous.tracura.model.DepartmentLineItemData(
                            itemType = map["itemType"] as? String ?: "",
                            item = map["item"] as? String ?: "",
                            spec = map["spec"] as? String ?: "",
                            quantity = (map["quantity"] as? Number)?.toDouble() ?: 0.0,
                            uom = map["uom"] as? String ?: "",
                            unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                            remainingQuantity = (map["remainingQuantity"] as? Number)?.toDouble() ?: 0.0
                        )
                    }
                } ?: emptyList(),
                
                // Anonymous Department Tracking
                isAnonymous = expenseData["isAnonymous"] as? Boolean,
                originalDepartment = expenseData["originalDepartment"] as? String,
                departmentDeletedAt = expenseData["departmentDeletedAt"] as? Timestamp,
                
                // Firestore Timestamps
                createdAt = createdAt,
                updatedAt = updatedAt,
                
                // Legacy fields for backward compatibility
                userId = expenseData["userId"] as? String ?: "",
                userName = expenseData["userName"] as? String ?: "",
                // Note: category is now a computed property, not a constructor parameter
                attachmentUrl = attachmentURL ?: "",
                attachmentFileName = attachmentName ?: "",
                paymentProofUrl = paymentProofURL ?: "",
                paymentProofFileName = paymentProofName ?: "",
                submittedAt = createdAt,
                reviewedAt = expenseData["reviewedAt"] as? Timestamp,
                reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                reviewComments = expenseData["reviewComments"] as? String ?: "",
                receiptNumber = expenseData["receiptNumber"] as? String ?: "",
                budgetExceeded = expenseData["budgetExceeded"] as? Boolean ?: false,
                
                // Verification and Payment fields
                isVerified = expenseData["isVerified"] as? Boolean ?: false,
                verifiedBy = expenseData["verifiedBy"] as? String ?: "",
                verifiedAt = expenseData["verifiedAt"] as? Timestamp,
                isPaymentCompleted = expenseData["isPaymentCompleted"] as? Boolean ?: false,
                byCredit = expenseData["By-Credict"] as? Boolean ?: false,
                // Handle vendor: prefer "vendor" (correct spelling), fallback to "vendour" (typo) for backward compatibility
                vendor = (expenseData["vendor"] as? String)?.takeIf { it.isNotEmpty() }
                    ?: (expenseData["vendour"] as? String)?.takeIf { it.isNotEmpty() }
                    ?: "",
                creditPaymentDate = expenseData["CreditPaymentDate"] as? Timestamp,
                remainingBalance = (expenseData["remainingBalance"] as? Number)?.toDouble()
            )
        } catch (e: Exception) {

            null
        }
    }
    
    /**
     * Generate code segment from input string.
     * Per ExpenseCode.md lines 36-44:
     * 1. Uppercase the input
     * 2. Strip non-alphanumeric characters
     * 3. Take first 3 characters
     * 4. Pad with 'X' if shorter than 3
     * 5. Return fallback if empty after cleaning
     */
    private fun codeSegment(value: String, fallback: String): String {
        val clean = value.uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (clean.isEmpty()) return fallback
        val segment = clean.take(3)
        return segment.padEnd(3, 'X')
    }
    
    /**
     * Extract plain department name from composite "{phaseId}_{departmentName}" format.
     * Per ExpenseCode.md lines 152-163:
     * Removes everything before and including the first underscore.
     * Example: "abc123_Civil" → "Civil"
     */
    private fun plainDepartmentName(composite: String): String {
        val idx = composite.indexOf('_')
        return if (idx >= 0) composite.substring(idx + 1) else composite
    }
    
    /**
     * Generate expense code in format: {PROJECT_SEGMENT}_{PHASE_SEGMENT}_{DEPT_SEGMENT}_{SEQUENCE}
     * Per ExpenseCode.md lines 1-80:
     * - Uses codeSegment to create 3-char segments
     * - Fetches all expenses to find max sequence for the prefix
     * - Increments max sequence by 1
     * - NOT transactional (concurrent submissions may produce duplicates, rare in practice)
     * 
     * Example: "TRA_PH1_CIV_3"
     */
    suspend fun generateExpenseCode(
        customerId: String,
        projectId: String,
        projectCode: String?,
        projectName: String,
        phaseName: String,
        departmentName: String
    ): String {
        // Per ExpenseCode.md line 203: prefer projectCode, fallback to projectName
        val projectSegment = codeSegment(projectCode ?: projectName, "PRJ")
        val phaseSegment = codeSegment(phaseName, "PHS")
        val deptSegment = codeSegment(departmentName, "DPT")
        val prefix = "${projectSegment}_${phaseSegment}_${deptSegment}_"
        
        // Per ExpenseCode.md lines 58-71: fetch all expenses and find max sequence
        val snapshot = firestore
            .collection("customers")
            .document(customerId)
            .collection("projects")
            .document(projectId)
            .collection("expenses")
            .get()
            .await()
        
        var maxSequence = 0
        for (document in snapshot.documents) {
            val existingCode = document.getString("expenseCode") ?: continue
            val normalized = existingCode.trim().uppercase()
            if (!normalized.startsWith(prefix.uppercase())) continue
            val suffix = normalized.removePrefix(prefix.uppercase())
            val sequence = suffix.toIntOrNull() ?: continue
            if (sequence > maxSequence) maxSequence = sequence
        }
        
        // Per ExpenseCode.md line 76: return prefix + (maxSequence + 1)
        return "$prefix${maxSequence + 1}"
    }

    fun getProjectExpenses(projectId: String): Flow<List<Expense>> = callbackFlow {
        
        // Use new structure: customers/{customerId}/Projects/{projectId}/expenses
        val customerId = projectRepository.findCustomerIdForProject(projectId)
        if (customerId == null) {

            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val userPhone = projectRepository.getCurrentUserIdentifier()
        if (userPhone == null) {

            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        // According to implementation guide: filter by submittedBy (user phone number) for USER role
        val expensesCollection = firestore.collection("customers")
            .document(customerId)
            .collection("projects")
            .document(projectId)
            .collection("expenses")
        
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        var fallbackListener: com.google.firebase.firestore.ListenerRegistration? = null
        
        try {
            // Optimized: Only fetch pending and rejected expenses - exclude approved for better performance
            listener = expensesCollection
                .whereEqualTo("submittedBy", userPhone)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // If index missing, try without orderBy
                        if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {

                            fallbackListener = expensesCollection
                                .whereEqualTo("submittedBy", userPhone)
                                .addSnapshotListener { noOrderSnapshot, noOrderError ->
                                    if (noOrderError != null) {

                                        trySend(emptyList())
                                        return@addSnapshotListener
                                    }
                                    val noOrderExpenses = noOrderSnapshot?.documents?.mapNotNull { doc ->
                                        mapDocumentToExpense(doc, projectId)
                                    } ?: emptyList()
                                    val sorted = noOrderExpenses.sortedByDescending { it.createdAt.toDate().time }
                                    trySend(sorted)
                                }
                            return@addSnapshotListener
                        }
                        // For other errors, try userId fallback

                        fallbackListener = expensesCollection
                            .whereEqualTo("userId", userPhone)
                            .addSnapshotListener { userIdSnapshot, userIdError ->
                                if (userIdError != null) {

                                    trySend(emptyList())
                                    return@addSnapshotListener
                                }
                                val userIdExpenses = userIdSnapshot?.documents?.mapNotNull { doc ->
                                    mapDocumentToExpense(doc, projectId)
                                } ?: emptyList()
                                val sorted = userIdExpenses.sortedByDescending { it.createdAt.toDate().time }
                                trySend(sorted)
                            }
                        return@addSnapshotListener
                    }
                    
                    
                    val expenses = snapshot?.documents?.mapNotNull { doc ->
                        mapDocumentToExpense(doc, projectId)
                    } ?: emptyList()
                    
                    val sorted = expenses.sortedByDescending { it.createdAt.toDate().time }

                    trySend(sorted)
                }
        } catch (e: Exception) {

            // Fallback to userId query
            listener = expensesCollection
                .whereEqualTo("userId", userPhone)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {

                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val expenses = snapshot?.documents?.mapNotNull { doc ->
                        mapDocumentToExpense(doc, projectId)
                    } ?: emptyList()
                    val sorted = expenses.sortedByDescending { it.createdAt.toDate().time }
                    trySend(sorted)
                }
        }
        
        awaitClose { 

            listener?.remove()
            fallbackListener?.remove()
        }
    }

    fun getExpensesForApprover(projectId: String, allowedDepartments: List<String>): Flow<List<Expense>> = callbackFlow {
        // Use new structure: customers/{customerId}/Projects/{projectId}/expenses
        val customerId = projectRepository.findCustomerIdForProject(projectId)
        if (customerId == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        if (allowedDepartments.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val expensesCollection = firestore.collection("customers")
            .document(customerId)
            .collection("projects")
            .document(projectId)
            .collection("expenses")
        
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        
        try {
            // Firestore 'whereIn' supports up to 10 values
            if (allowedDepartments.size <= 10) {
                listener = expensesCollection
                    // We REMOVED .whereIn because we can't query partial strings
                    // We keep .orderBy if your index allows it, otherwise remove it and sort manually
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ExpenseRepo", "Listen failed.", error)
                            return@addSnapshotListener
                        }

                        val rawExpenses = snapshot?.documents?.mapNotNull { doc ->
                            mapDocumentToExpense(doc, projectId)
                        } ?: emptyList()

                        // --- THIS IS THE MANUAL FILTER LOGIC ---
                        val filteredExpenses = rawExpenses.filter { expense ->
                            // 1. Get the stored string like "12345_Civil"
                            val rawDeptString = expense.department

                            // 2. Extract the name part (after the underscore)
                            // If there is no underscore, assume the whole string is the name
                            val deptName = if (rawDeptString.contains("_")) {
                                rawDeptString.substringAfter("_")
                            } else {
                                rawDeptString
                            }

                            // 3. Check if this extracted name is in your allowed list
                            allowedDepartments.contains(deptName)
                        }
                        // ---------------------------------------

                        trySend(filteredExpenses)
                    }
            } else {
                // If more than 10 departments, fetch all and filter client-side
                // This is less efficient but necessary due to Firestore limits
                listener = expensesCollection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                             expensesCollection.addSnapshotListener { noOrderSnapshot, _ ->
                                 val expenses = noOrderSnapshot?.documents?.mapNotNull { doc ->
                                    mapDocumentToExpense(doc, projectId)
                                }?.filter { it.department in allowedDepartments } ?: emptyList()
                                trySend(expenses.sortedByDescending { it.createdAt.toDate().time })
                             }
                            return@addSnapshotListener
                        }
                        
                        val expenses = snapshot?.documents?.mapNotNull { doc ->
                            mapDocumentToExpense(doc, projectId)
                        }?.filter { it.department in allowedDepartments } ?: emptyList()
                        
                        trySend(expenses)
                    }
            }
        } catch (e: Exception) {
            // Fallback: fetch all and filter
            listener = expensesCollection.addSnapshotListener { snapshot, _ ->
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    mapDocumentToExpense(doc, projectId)
                }?.filter { it.department in allowedDepartments } ?: emptyList()
                trySend(expenses.sortedByDescending { it.createdAt.toDate().time })
            }
        }
        
        awaitClose { 
            listener?.remove()
        }
    }
    
    fun getUserExpenses(userId: String): Flow<List<Expense>> = callbackFlow {

        try {
            // First get all projects
            val projectsSnapshot = firestore.collection("projects").get().await()
            val projectIds = projectsSnapshot.documents.map { it.id }
            
            if (projectIds.isEmpty()) {

                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            
            // Create listeners for each project's user expenses
            val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
            val allUserExpenses = mutableMapOf<String, List<Expense>>()
            
            projectIds.forEach { projectId ->
                val listener = firestore.collection("projects")
                        .document(projectId)
                        .collection("expenses")
                        .whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {

                            return@addSnapshotListener
                        }
                        
                        val projectUserExpenses = snapshot?.documents?.mapNotNull { doc ->
                            mapDocumentToExpense(doc, projectId)
                        } ?: emptyList()
                        
                        // Update the map for this project
                        allUserExpenses[projectId] = projectUserExpenses
                        
                        // Combine all user expenses and emit
                        val combinedExpenses = allUserExpenses.values.flatten()
                            .sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }

                        trySend(combinedExpenses)
                    }
                
                listeners.add(listener)
            }
            
            awaitClose { 

                listeners.forEach { it.remove() }
            }
            
        } catch (e: Exception) {

            trySend(emptyList())
            awaitClose { }
        }
    }
    
    /**
     * Uploads an attachment file to Firebase Storage and returns the download URL
     */
    suspend fun uploadAttachmentToStorage(
        attachmentUri: String,
        fileName: String,
        projectId: String,
        userId: String
    ): Result<Pair<String, String>> { // Returns (downloadUrl, fileName)
        return try {
            val context = FirebaseAuth.getInstance().app?.applicationContext
                ?: return Result.failure(Exception("Application context not available"))
            
            val uri = Uri.parse(attachmentUri)

            // Get actual file name from content resolver if available
            var actualFileName = fileName
            if (uri.scheme == "content") {
                try {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                val displayName = it.getString(nameIndex)
                                if (displayName != null && displayName.isNotEmpty()) {
                                    actualFileName = displayName

                                }
                            }
                        }
                    }
                } catch (e: Exception) {

                    // Use provided fileName as fallback
                }
            }
            
            // Check if URI is accessible
            val inputStream = when (uri.scheme) {
                "content" -> {
                    context.contentResolver.openInputStream(uri)
                }
                "file" -> {
                    java.io.File(uri.path ?: "").inputStream()
                }
                else -> {
                    return Result.failure(Exception("Unsupported URI scheme: ${uri.scheme}"))
                }
            } ?: return Result.failure(Exception("Cannot access attachment file"))
            
            // Determine file extension
            val fileExtension = actualFileName.substringAfterLast(".", "pdf")
            val sanitizedFileName = actualFileName.replace(" ", "_").replace("[^a-zA-Z0-9._-]".toRegex(), "")
            
            // Create storage reference
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val attachmentRef = storageRef.child("expense_attachments/${projectId}/${userId}/${System.currentTimeMillis()}_$sanitizedFileName")

            // Upload file
            val uploadTask = attachmentRef.putStream(inputStream)
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
            }
            
            // Wait for upload to complete
            val uploadResult = uploadTask.await()

            // Get download URL
            val downloadUrl = attachmentRef.downloadUrl.await()

            inputStream.close()
            
            Result.success(Pair(downloadUrl.toString(), sanitizedFileName))
        } catch (e: Exception) {

            Result.failure(e)
        }
    }
    
    suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            // Per ExpenseCode.md lines 169-173: expenseCode is ONLY included on create, NOT on update
            // Generate expenseCode before saving (if not already set)
            val customerId = projectRepository.findCustomerIdForProject(expense.projectId)
                ?: return Result.failure(Exception("Customer not found for project"))
            
            val finalExpenseCode = if (expense.expenseCode.isEmpty()) {
                // Fetch project details for code generation
                val project = projectRepository.getProjectById(expense.projectId)
                val projectCode = project?.projectCode
                val projectName = project?.name ?: "Project"
                
                // Per ExpenseCode.md lines 152-163: strip phaseId prefix from department
                val plainDeptName = plainDepartmentName(expense.department)
                
                // Generate expense code
                generateExpenseCode(
                    customerId = customerId,
                    projectId = expense.projectId,
                    projectCode = projectCode,
                    projectName = projectName,
                    phaseName = expense.phaseName ?: "",
                    departmentName = plainDeptName
                )
            } else {
                expense.expenseCode
            }
            
            // Don't store projectId since it's implicit in the subcollection path
            val expenseData = mutableMapOf<String, Any>(
                "expenseCode" to finalExpenseCode, // Per ExpenseCode.md line 89: include expenseCode in document
                "date" to expense.date,
                "amount" to expense.amount,
                "department" to expense.department,
//                "categories" to expense.categories,
                "description" to expense.description,
//                "modeOfPayment" to PaymentMode.toString(expense.modeOfPayment),
                "status" to expense.status.name,
                "submittedBy" to expense.submittedBy,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "isAdmin" to expense.isAdmin,
                "vendor" to expense.vendor,
                "By-Credict" to expense.byCredit
            )
            
            // Add optional fields
            expense.phaseId?.let { expenseData["phaseId"] = it }
            expense.phaseName?.let { expenseData["phaseName"] = it }
            expense.attachmentURL?.let { expenseData["attachmentURL"] = it }
            expense.attachmentName?.let { expenseData["attachmentName"] = it }
//            expense.paymentProofURL?.let { expenseData["paymentProofURL"] = it }
//            expense.paymentProofName?.let { expenseData["paymentProofName"] = it }
            expense.remark?.let { expenseData["remark"] = it }
//            expense.approvedBy?.let { expenseData["approvedBy"] = it }
            if (expense.rejectedBy.isNotEmpty()) expenseData["rejectedBy"] = expense.rejectedBy
            
            // Material details
            expense.itemType?.let { expenseData["itemType"] = it }
            expense.item?.let { expenseData["item"] = it }
            expense.brand?.let { expenseData["brand"] = it }
            expense.spec?.let { expenseData["spec"] = it }
            expense.thickness?.let { expenseData["thickness"] = it }
            expense.quantity?.let { expenseData["quantity"] = it }
            expense.uom?.let { expenseData["uom"] = it }
            expense.unitPrice?.let { expenseData["unitPrice"] = it }
            
            // Anonymous department tracking
            expense.isAnonymous?.let { expenseData["isAnonymous"] = it }
            expense.originalDepartment?.let { expenseData["originalDepartment"] = it }
            expense.departmentDeletedAt?.let { expenseData["departmentDeletedAt"] = it }
            
            // Legacy fields for backward compatibility
//            if (expense.userId.isNotEmpty()) expenseData["userId"] = expense.userId
//            if (expense.userName.isNotEmpty()) expenseData["userName"] = expense.userName
//            if (expense.category.isNotEmpty()) expenseData["category"] = expense.category
//            if (expense.paymentProofUrl.isNotEmpty()) expenseData["paymentProofUrl"] = expense.paymentProofUrl
//            if (expense.paymentProofFileName.isNotEmpty()) expenseData["paymentProofFileName"] = expense.paymentProofFileName
//            expense.submittedAt?.let { expenseData["submittedAt"] = it }
            expense.reviewedAt?.let { expenseData["reviewedAt"] = it }
//            if (expense.reviewedBy.isNotEmpty()) expenseData["reviewedBy"] = expense.reviewedBy
//            if (expense.reviewComments.isNotEmpty()) expenseData["reviewComments"] = expense.reviewComments
//            if (expense.receiptNumber.isNotEmpty()) expenseData["receiptNumber"] = expense.receiptNumber
            if (expense.phaseId != null && expense.phaseId.isNotEmpty()) expenseData["phaseId"] = expense.phaseId
            if (expense.budgetExceeded) expenseData["budgetExceeded"] = expense.budgetExceeded
            
            // customerId already fetched at the beginning for expenseCode generation
            // Store expense in new structure: customers/{customerId}/Projects/{projectId}/expenses

            val docRef = firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(expense.projectId)
                    .collection("expenses")
                    .add(expenseData)
                    .await()

                // Check if this is the first expense and update project status from DRAFT to ACTIVE
                try {
                    val projectDoc = projectRepository.getProjectDocument(expense.projectId)
                    if (projectDoc != null && projectDoc.exists()) {
                        val currentStatus = projectDoc.get("status") as? String ?: "DRAFT"
                        
                        // Count expenses for this project
                        val expensesSnapshot = firestore.collection("customers")
                            .document(customerId)
                            .collection("projects")
                            .document(expense.projectId)
                            .collection("expenses")
                            .get()
                            .await()
                        
                        // If this is the first expense, update project status
                        if (expensesSnapshot.documents.size == 1 && currentStatus == "DRAFT") {
                            firestore.collection("customers")
                                .document(customerId)
                                .collection("projects")
                                .document(expense.projectId)
                                .update("status", "ACTIVE", "updatedAt", Timestamp.now())
                                .await()

                        }
                }
            } catch (e: Exception) {

            }
            
            Result.success(docRef.id)
        } catch (e: Exception)  {

            Result.failure(e)
        }
    }
    suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            // Per ExpenseCode.md lines 169-173: expenseCode is NEVER updated/regenerated on edit
            // Don't store projectId since it's implicit in the subcollection path
            val expenseData = mutableMapOf<String, Any>(
                // NOTE: expenseCode is intentionally NOT included - it's frozen at creation time
                "date" to expense.date,
                "amount" to expense.amount,
                "department" to expense.department,
//                "categories" to expense.categories,
                "description" to expense.description,
//                "modeOfPayment" to PaymentMode.toString(expense.modeOfPayment),
                "status" to expense.status.name,
                "submittedBy" to expense.submittedBy,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isAdmin" to expense.isAdmin,
                "vendor" to expense.vendor,
                "By-Credict" to expense.byCredit
            )
            
            // Add optional fields
            expense.phaseId?.let { expenseData["phaseId"] = it }
            expense.phaseName?.let { expenseData["phaseName"] = it }
            expense.attachmentURL?.let { expenseData["attachmentURL"] = it }
            expense.attachmentName?.let { expenseData["attachmentName"] = it }
//            expense.paymentProofURL?.let { expenseData["paymentProofURL"] = it }
//            expense.paymentProofName?.let { expenseData["paymentProofName"] = it }
            expense.remark?.let { expenseData["remark"] = it }
//            expense.approvedBy?.let { expenseData["approvedBy"] = it }
            if (expense.rejectedBy.isNotEmpty()) expenseData["rejectedBy"] = expense.rejectedBy
            
            // Material details
            expense.itemType?.let { expenseData["itemType"] = it }
            expense.item?.let { expenseData["item"] = it }
            expense.brand?.let { expenseData["brand"] = it }
            expense.spec?.let { expenseData["spec"] = it }
            expense.thickness?.let { expenseData["thickness"] = it }
            expense.quantity?.let { expenseData["quantity"] = it }
            expense.uom?.let { expenseData["uom"] = it }
            expense.unitPrice?.let { expenseData["unitPrice"] = it }
            
            // Anonymous department tracking
            expense.isAnonymous?.let { expenseData["isAnonymous"] = it }
            expense.originalDepartment?.let { expenseData["originalDepartment"] = it }
            expense.departmentDeletedAt?.let { expenseData["departmentDeletedAt"] = it }
            
            // Legacy fields for backward compatibility
//            if (expense.userId.isNotEmpty()) expenseData["userId"] = expense.userId
//            if (expense.userName.isNotEmpty()) expenseData["userName"] = expense.userName
//            if (expense.category.isNotEmpty()) expenseData["category"] = expense.category
//            if (expense.paymentProofUrl.isNotEmpty()) expenseData["paymentProofUrl"] = expense.paymentProofUrl
//            if (expense.paymentProofFileName.isNotEmpty()) expenseData["paymentProofFileName"] = expense.paymentProofFileName
//            expense.submittedAt?.let { expenseData["submittedAt"] = it }
            expense.reviewedAt?.let { expenseData["reviewedAt"] = it }
//            if (expense.reviewedBy.isNotEmpty()) expenseData["reviewedBy"] = expense.reviewedBy
//            if (expense.reviewComments.isNotEmpty()) expenseData["reviewComments"] = expense.reviewComments
//            if (expense.receiptNumber.isNotEmpty()) expenseData["receiptNumber"] = expense.receiptNumber
            if (expense.phaseId != null && expense.phaseId.isNotEmpty()) expenseData["phaseId"] = expense.phaseId
            if (expense.budgetExceeded) expenseData["budgetExceeded"] = expense.budgetExceeded
            
            // Preserve createdAt if it exists
            expense.createdAt.let { expenseData["createdAt"] = it }
            
            // Find customer ID for this project
            val customerId = projectRepository.findCustomerIdForProject(expense.projectId)
            
            if (customerId != null) {
                // Store expense in new structure: customers/{customerId}/Projects/{projectId}/expenses
                firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(expense.projectId)
                    .collection("expenses")
                    .document(expense.id)
                    .set(expenseData).await()
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Customer not found for project"))
            }
        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    suspend fun updateExpenseStatus(
        projectId: String,
        expenseId: String,
        reviewedBy: String = "",
        status: ExpenseStatus,
        reviewComments: String,
        isVerified: Boolean,
        verifiedBy : String,
        byCredit : Boolean,
        verifiedAt: com.google.firebase.Timestamp
    ) {
        try {

            val updates = mapOf(
                "status" to status.name,
                "reviewComments" to reviewComments,
                "verifiedAt" to verifiedAt,
                "isVerified" to isVerified,
                "verifiedBy" to verifiedBy,
                "By-Credict" to byCredit,
            )

            // Use new structure: customers/{customerId}/Projects/{projectId}/expenses/{expenseId}
            val customerId = projectRepository.findCustomerIdForProject(projectId)
            if (customerId == null) {

                throw Exception("Customer not found for project")
            }

            val docRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)

            val docSnapshot = docRef.get().await()
            
            if (!docSnapshot.exists()) {

                throw Exception("Expense document not found")
            }

            docRef.update(updates).await()

            // Verify the update was applied
            val updatedDoc = docRef.get().await()
            if (updatedDoc.exists()) {
                val updatedData = updatedDoc.data

            } else {

            }
                
        } catch (e: Exception) {

            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Update payment mode and payment proof for an expense
     * Also updates status to COMPLETE when payment is submitted
     */
    suspend fun updateExpensePayment(
        expense: Expense,
        paymentMode: PaymentMode,
        paymentProofURL: String? = null,
        paymentProofName: String? = null,
        paidBy: String = ""
    ) {
        // Wrap entire operation in NonCancellable to ensure completion even if user navigates back
        withContext(NonCancellable) {
            try {
                Log.d("UpdatingPaymentModel", "preparing variable of updates for upating the expense collection ")

                val updates = mutableMapOf<String, Any>(
                    "modeOfPayment" to PaymentMode.toString(paymentMode),
                    "status" to ExpenseStatus.COMPLETE.name,
                    "remainingBalance" to 0.0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                Log.d("UpdatingPaymentModel", "Variable updated for upating the expense collection ")

                // Add payment proof if provided
                if (paymentProofURL != null) {
                    Log.d("UpdatingPaymentModel", "Payment proof URL is updated ")
                    updates["paymentProofURL"] = paymentProofURL
                }
                if (paymentProofName != null) {
                    Log.d("UpdatingPaymentModel", "payment proof name is updated ")
                    updates["paymentProofName"] = paymentProofName
                }


                // Use new structure: customers/{customerId}/Projects/{projectId}/expenses/{expenseId}
                val customerId = projectRepository.findCustomerIdForProject(expense.projectId)
                if (customerId == null) {
                    throw Exception("Customer not found for project")
                }
                Log.d("UpdatingPaymentModel", "Fetched customerid which is $customerId ")

                val docRef = firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(expense.projectId)
                    .collection("expenses")
                    .document(expense.id)

                // Re-read the live Firestore document so the deduction amount is always
                // authoritative, even if the in-memory Expense object is stale.
                Log.d("UpdatingPaymentModel", "Getting values like currentAmount and remaining amount and submitted by")
                val freshSnapshot = withContext(Dispatchers.IO) { docRef.get().await() }
                val currentAmount = freshSnapshot.getDouble("amount") ?: expense.amount
                val contractorPaymentAmount = freshSnapshot.getDouble("contractorPaymentAmount") ?: 0.0
                val freshRemainingBalance = freshSnapshot.getDouble("remainingBalance")
                val currentRemainingBalance = if (freshRemainingBalance != null && freshRemainingBalance >= 1.0) {
                    freshRemainingBalance
                } else {
                    currentAmount
                }
                val submittedBy = expense.submittedBy

                // --- BATCH START ---
                // Run network operations on IO thread with timeout
                withContext(Dispatchers.IO) {
                    withTimeout(15000L) { // 15 seconds timeout
                        val batch = firestore.batch()

                        // 1. Update expense document
                        batch.set(docRef, updates, com.google.firebase.firestore.SetOptions.merge())
                        Log.d("UpdatingPaymentModel", "Added expense update to batch")

                        // Add full payment to paymentHistory subcollection
                        val paymentHistoryData = mutableMapOf<String, Any>(
                            "amount" to currentRemainingBalance,
                            "paymentMode" to PaymentMode.toString(paymentMode),
                            "paidAt" to FieldValue.serverTimestamp(),
                            "paidBy" to paidBy.ifEmpty { submittedBy }
                        )

                        // Add payment proof if provided
                        if (paymentProofURL != null) {
                            paymentHistoryData["paymentProofURL"] = paymentProofURL
                        }
                        if (paymentProofName != null) {
                            paymentHistoryData["paymentProofName"] = paymentProofName
                        }
                        
                        // 2. Add to paymentHistory subcollection
                        val historyRef = docRef.collection("paymentHistory").document()
                        batch.set(historyRef, paymentHistoryData)
                        Log.d("UpdatingPaymentModel", "Added payment history to batch")

                        // 3. Commit batch
                        Log.d("UpdatingPaymentModel", "Committing batch...")
                        batch.commit().await()
                        Log.d("UpdatingPaymentModel", "Batch committed successfully ✅")
                    }
                }
                // --- BATCH END ---

                Log.d("UpdatingPaymentModel", "Started the budget deduction from the department logic")

                // --- Budget Deduction Logic (Keep separate as it touches different collections potentially safer to keep outside batch for now if cross-collection logic is complex, though transactions are better. Keeping as is to minimize regression risk in logic flow) ---
                try {
                    Log.d("UpdatingPaymentModel", "Going into the budget deduction logic")
                    // Parse department string to extract phaseId and departmentName
                    // Format: phaseId_departmentName (e.g., eFsTmlTURiME9QnUvpE8_civil)
                    val departmentString = expense.department
                    val expensePhaseId = expense.phaseId
                    
                    var targetPhaseId: String? = expensePhaseId
                    var targetDeptName = departmentString
                    Log.d("UpdatingPaymentModel", "budget deduction logic varibales prepared")
                    if (departmentString.contains("_")) {
                        val parts = departmentString.split("_", limit = 2)
                        if (parts.size == 2) {
                            targetPhaseId = parts[0]
                            targetDeptName = parts[1]
                        }
                    }
                    Log.d("UpdatingPaymentModel", "budget deduction logic variables set")
                    if (!targetPhaseId.isNullOrEmpty() && targetDeptName.isNotEmpty()) {
                        val departmentsQuery = firestore.collection("customers")
                            .document(customerId)
                            .collection("projects")
                            .document(expense.projectId)
                            .collection("phases")
                            .document(targetPhaseId)
                            .collection("departments")
                            .whereEqualTo("name", targetDeptName)
                            .limit(1)
                            .get()
                            .await()

                        val departmentDoc = departmentsQuery.documents.firstOrNull()
                        Log.d("UpdatingPaymentModel", "Going into the department query")
                        if (departmentDoc != null) {
                            firestore.runTransaction { transaction ->
                                val snapshot = transaction.get(departmentDoc.reference)
                                // Default to totalBudget if remainingAmount is null
                                val totalBudget = snapshot.getDouble("totalBudget") ?: 0.0
                                val currentRemaining = snapshot.getDouble("remainingAmount") ?: totalBudget

                                // Deduct only the remaining unpaid balance (partial payments already deducted their share)
                                val newRemaining = currentRemaining - currentRemainingBalance

                                transaction.update(departmentDoc.reference, "remainingAmount", newRemaining)
                                transaction.update(departmentDoc.reference, "updatedAt", FieldValue.serverTimestamp())

                                // Deduct contractorPaymentAmount from contractorRemainingAmount if present
                                if (contractorPaymentAmount > 0) {
                                    val currentContractorRemaining = snapshot.getDouble("contractorRemainingAmount") ?: 0.0
                                    transaction.update(departmentDoc.reference, "contractorRemainingAmount", currentContractorRemaining - contractorPaymentAmount)
                                    Log.d("UpdatingPaymentModel", "✅ Deducted contractor payment $contractorPaymentAmount from contractorRemainingAmount")
                                }
                                
                                // --- Deduct remainingQuantity from matching line item ---
                                val expenseQuantity = expense.quantity?.toDoubleOrNull() ?: 0.0
                                val expenseItemType = expense.itemType ?: ""
                                val expenseItem = expense.item ?: ""
                                val expenseSpec = expense.spec ?: ""
                                
                                if (expenseQuantity > 0 && expenseItemType.isNotEmpty()) {
                                    @Suppress("UNCHECKED_CAST")
                                    val lineItemsList = snapshot.get("lineItems") as? List<Map<String, Any>>
                                    if (lineItemsList != null) {
                                        val updatedLineItems = lineItemsList.map { lineItemMap ->
                                            val liItemType = lineItemMap["itemType"] as? String ?: ""
                                            val liItem = lineItemMap["item"] as? String ?: ""
                                            val liSpec = lineItemMap["spec"] as? String ?: ""
                                            
                                            // Match line item by itemType, item, and spec
                                            if (liItemType == expenseItemType &&
                                                (expenseItem.isEmpty() || liItem == expenseItem) &&
                                                (expenseSpec.isEmpty() || liSpec == expenseSpec)) {
                                                val currentRemainingQty = (lineItemMap["remainingQuantity"] as? Number)?.toDouble()
                                                    ?: (lineItemMap["quantity"] as? Number)?.toDouble()
                                                    ?: 0.0
                                                val newRemainingQty = currentRemainingQty - expenseQuantity
                                                // Create updated map with new remainingQuantity
                                                lineItemMap.toMutableMap().apply {
                                                    put("remainingQuantity", newRemainingQty)
                                                }
                                            } else {
                                                lineItemMap
                                            }
                                        }
                                        transaction.update(departmentDoc.reference, "lineItems", updatedLineItems)
                                        Log.d("UpdatingPaymentModel", "✅ Deducted $expenseQuantity from remainingQuantity for $expenseItemType/$expenseItem/$expenseSpec")
                                    }
                                }
                            }.await()
                            Log.d("UpdatingPaymentModel", "✅ Budget deducted for $targetDeptName in phase $targetPhaseId")
                        } else {
                            Log.w("UpdatingPaymentModel", "⚠️ Department document not found for deduction: $targetDeptName")
                        }
                    } else {
                            Log.w("UpdatingPaymentModel", "⚠️ Could not parse phase/department for deduction: $departmentString")
                    }
                } catch (deductionError: Exception) {
                    Log.e("UpdatingPaymentModel", "❌ Error deducting budget: ${deductionError.message}", deductionError)
                    // We do not rethrow here to avoid failing the payment update if deduction fails
                }

                // --- Phase Remaining Budget Update Logic ---
                try {
                    var phaseIdForDeduction = expense.phaseId
                    if (phaseIdForDeduction.isNullOrEmpty() && expense.department.contains("_")) {
                        phaseIdForDeduction = expense.department.split("_", limit = 2)[0]
                    }
                    if (!phaseIdForDeduction.isNullOrEmpty()) {
                        val phaseDocRef = firestore.collection("customers")
                            .document(customerId)
                            .collection("projects")
                            .document(expense.projectId)
                            .collection("phases")
                            .document(phaseIdForDeduction)
                        firestore.runTransaction { transaction ->
                            val phaseSnapshot = transaction.get(phaseDocRef)
                            val phaseTotalBudget = phaseSnapshot.getDouble("totalBudget") ?: 0.0
                            val currentRemaining = phaseSnapshot.getDouble("remainingBudget") ?: phaseTotalBudget
                            transaction.update(phaseDocRef, "remainingBudget", currentRemaining - currentRemainingBalance)
                            transaction.update(phaseDocRef, "updatedAt", FieldValue.serverTimestamp())
                        }.await()
                        Log.d("UpdatingPaymentModel", "✅ Phase remainingBudget updated for $phaseIdForDeduction")
                    }
                } catch (phaseUpdateError: Exception) {
                    Log.e("UpdatingPaymentModel", "❌ Error updating phase budget: ${phaseUpdateError.message}", phaseUpdateError)
                }

                // --- Project Remaining Balance Update Logic ---
                try {
                    Log.d("UpdatingPaymentModel", "Starting project remaining balance update")
                    val projectDocRef = firestore.collection("customers")
                        .document(customerId)
                        .collection("projects")
                        .document(expense.projectId)

                    firestore.runTransaction { transaction ->
                        val projectSnapshot = transaction.get(projectDocRef)
                        val projectBudget = projectSnapshot.getDouble("budget") ?: 0.0
                        // If remainingBalance is null, assume it's the first deduction from total budget
                        val currentProjectBalance = projectSnapshot.getDouble("remainingBalance") ?: projectBudget
                        
                        val newProjectBalance = currentProjectBalance - currentRemainingBalance

                        transaction.update(projectDocRef, "remainingBalance", newProjectBalance)
                        transaction.update(projectDocRef, "updatedAt", FieldValue.serverTimestamp())
                    }.await()
                    Log.d("UpdatingPaymentModel", "✅ Project remaining balance updated")
                } catch (projectUpdateError: Exception) {
                    Log.e("UpdatingPaymentModel", "❌ Error updating project balance: ${projectUpdateError.message}", projectUpdateError)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }
    
    /**
     * Update expense with partial payment
     * Calculates remaining balance and adds payment to history
     */
    suspend fun updateExpensePartialPayment(
        projectId: String,
        expenseId: String,
        paymentAmount: Double,
        paymentMode: PaymentMode,
        paymentProofURL: String? = null,
        paymentProofName: String? = null,
        paidBy: String
    ) {
        try {
            // Use new structure: customers/{customerId}/Projects/{projectId}/expenses/{expenseId}
            val customerId = projectRepository.findCustomerIdForProject(projectId)
            if (customerId == null) {
                throw Exception("Customer not found for project")
            }
            val docRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)
            val docSnapshot = docRef.get().await()
            if (!docSnapshot.exists()) {
                throw Exception("Expense document not found")
            }
            // Get current expense data
            val currentAmount = docSnapshot.getDouble("amount") ?: 0.0
            val currentRemainingBalance = docSnapshot.getDouble("remainingBalance") ?: currentAmount
            val contractorPaymentAmount = docSnapshot.getDouble("contractorPaymentAmount") ?: 0.0
            // Calculate new remaining balance
            val newRemainingBalance = currentRemainingBalance - paymentAmount


            // Prepare expense updates
            val updates = mutableMapOf<String, Any>(
                "remainingBalance" to newRemainingBalance,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            // If fully paid, mark as COMPLETE
            if (newRemainingBalance <= 0) {
                updates["status"] = ExpenseStatus.COMPLETE.name
                updates["remainingBalance"] = 0.0
            }


            // Update expense document using set with merge to handle missing fields
            try {
                docRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (updateError: Exception) {
                throw updateError
            }

            // Add payment to paymentHistory subcollection
            val paymentHistoryData = mutableMapOf<String, Any>(
                "amount" to paymentAmount,
                "paymentMode" to PaymentMode.toString(paymentMode),
                "paidAt" to FieldValue.serverTimestamp(),
                "paidBy" to paidBy
            )


            // Add payment proof if provided
            if (paymentProofURL != null) {
                paymentHistoryData["paymentProofURL"] = paymentProofURL
            }
            if (paymentProofName != null) {
                paymentHistoryData["paymentProofName"] = paymentProofName
            }


            // Add to paymentHistory subcollection
            try {
                docRef.collection("paymentHistory").add(paymentHistoryData).await()
            } catch (historyError: Exception) {
                throw historyError
            }

            // ---- Budget Deduction Logic (partial payment) ----
            try {
                val departmentString = docSnapshot.getString("department") ?: ""
                val expensePhaseId = docSnapshot.getString("phaseId")

                var targetPhaseId: String? = expensePhaseId
                var targetDeptName = departmentString

                if (departmentString.contains("_")) {
                    val parts = departmentString.split("_", limit = 2)
                    if (parts.size == 2) {
                        targetPhaseId = parts[0]
                        targetDeptName = parts[1]
                    }
                }

                if (!targetPhaseId.isNullOrEmpty() && targetDeptName.isNotEmpty()) {
                    // 1. Deduct from department.remainingAmount
                    try {
                        val deptQuery = firestore.collection("customers")
                            .document(customerId)
                            .collection("projects")
                            .document(projectId)
                            .collection("phases")
                            .document(targetPhaseId)
                            .collection("departments")
                            .whereEqualTo("name", targetDeptName)
                            .limit(1)
                            .get().await()
                        val deptDoc = deptQuery.documents.firstOrNull()
                        if (deptDoc != null) {
                            firestore.runTransaction { tx ->
                                val snap = tx.get(deptDoc.reference)
                                val totalBudget = snap.getDouble("totalBudget") ?: 0.0
                                val currentRemaining = snap.getDouble("remainingAmount") ?: totalBudget
                                tx.update(deptDoc.reference, "remainingAmount", currentRemaining - paymentAmount)
                                tx.update(deptDoc.reference, "updatedAt", FieldValue.serverTimestamp())

                                // Deduct proportional contractor amount from contractorRemainingAmount if present
                                if (contractorPaymentAmount > 0 && currentAmount > 0) {
                                    val contractorDeduction = (paymentAmount / currentAmount) * contractorPaymentAmount
                                    val currentContractorRemaining = snap.getDouble("contractorRemainingAmount") ?: 0.0
                                    tx.update(deptDoc.reference, "contractorRemainingAmount", currentContractorRemaining - contractorDeduction)
                                    Log.d("PartialPayment", "Deducted contractor $contractorDeduction from contractorRemainingAmount")
                                }
                            }.await()
                            Log.d("PartialPayment", "Deducted from dept $targetDeptName remainingAmount")
                        }
                    } catch (deptError: Exception) {
                        Log.e("PartialPayment", "Dept deduction failed: ${deptError.message}")
                    }

                    // 2. Deduct from phase.remainingBudget
                    try {
                        val phaseRef = firestore.collection("customers")
                            .document(customerId)
                            .collection("projects")
                            .document(projectId)
                            .collection("phases")
                            .document(targetPhaseId)
                        firestore.runTransaction { tx ->
                            val snap = tx.get(phaseRef)
                            val phaseTotalBudget = snap.getDouble("totalBudget") ?: 0.0
                            val currentRemaining = snap.getDouble("remainingBudget") ?: phaseTotalBudget
                            tx.update(phaseRef, "remainingBudget", currentRemaining - paymentAmount)
                            tx.update(phaseRef, "updatedAt", FieldValue.serverTimestamp())
                        }.await()
                        Log.d("PartialPayment", "Deducted from phase $targetPhaseId remainingBudget")
                    } catch (phaseError: Exception) {
                        Log.e("PartialPayment", "Phase deduction failed: ${phaseError.message}")
                    }
                }

                // 3. Deduct from project.remainingBalance
                try {
                    val projectRef = firestore.collection("customers")
                        .document(customerId)
                        .collection("projects")
                        .document(projectId)
                    firestore.runTransaction { tx ->
                        val snap = tx.get(projectRef)
                        val budget = snap.getDouble("budget") ?: 0.0
                        val currentBalance = snap.getDouble("remainingBalance") ?: budget
                        tx.update(projectRef, "remainingBalance", currentBalance - paymentAmount)
                        tx.update(projectRef, "updatedAt", FieldValue.serverTimestamp())
                    }.await()
                    Log.d("PartialPayment", "Deducted from project $projectId remainingBalance")
                } catch (projectError: Exception) {
                    Log.e("PartialPayment", "Project deduction failed: ${projectError.message}")
                }
            } catch (deductionError: Exception) {
                Log.e("PartialPayment", "Budget deduction block error: ${deductionError.message}", deductionError)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }


    /**
     * Bulk update multiple expenses payment
     */
    suspend fun bulkUpdateExpensesPayment(
        expenses: List<Expense>,
        paymentMode: PaymentMode,
        paymentProofURL: String? = null,
        paymentProofName: String? = null
    ) {
        try {
            val updates = mutableMapOf<String, Any>(
                "modeOfPayment" to PaymentMode.toString(paymentMode),
                "status" to ExpenseStatus.COMPLETE.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            // Add payment proof if provided
            if (paymentProofURL != null) {
                updates["paymentProofURL"] = paymentProofURL
            }
            if (paymentProofName != null) {
                updates["paymentProofName"] = paymentProofName
            }

            // Group expenses by projectId and customerId for efficient batch updates
            val expensesByProject = expenses.groupBy { it.projectId }
            
            expensesByProject.forEach { (projectId, projectExpenses) ->
                val customerId = projectRepository.findCustomerIdForProject(projectId)
                if (customerId == null) {
                    return@forEach
                }

                val batch = firestore.batch()
                projectExpenses.forEach { expense ->
                    val docRef = firestore.collection("customers")
                        .document(customerId)
                        .collection("projects")
                        .document(projectId)
                        .collection("expenses")
                        .document(expense.id)
                    batch.update(docRef, updates)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Update credit payment date for an expense
     */
    suspend fun updateCreditPaymentDate(
        projectId: String,
        expenseId: String,
        creditPaymentDate: com.google.firebase.Timestamp
    ) {
        try {
            val updates = mapOf(
                "CreditPaymentDate" to creditPaymentDate,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Use new structure: customers/{customerId}/Projects/{projectId}/expenses/{expenseId}
            val customerId = projectRepository.findCustomerIdForProject(projectId)
            if (customerId == null) {
                throw Exception("Customer not found for project")
            }

            val docRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)

            val docSnapshot = docRef.get().await()
            
            if (!docSnapshot.exists()) {
                throw Exception("Expense document not found")
            }

            docRef.update(updates).await()
                
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun updateIsToManager(
        projectId: String,
        expenseId: String,
        isToManager: Boolean
    ) {
        try {
            val updates = mapOf(
                "isToManager" to isToManager,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val customerId = projectRepository.findCustomerIdForProject(projectId)
                ?: throw Exception("Customer not found for project")

            val docRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)

            val docSnapshot = docRef.get().await()
            if (!docSnapshot.exists()) {
                throw Exception("Expense document not found")
            }

            docRef.update(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun updateExpenseAmountAndStatus(
        projectId: String,
        expenseId: String,
        newAmount: Double,
        status: ExpenseStatus,
        reviewedBy: String,
        reviewComments: String,
        reviewedAt: com.google.firebase.Timestamp
    ) {
        try {

            val updates = mapOf(
                "amount" to newAmount,
                "netAmount" to newAmount, // Update net amount as well
                "status" to status.name,
                "reviewedBy" to reviewedBy,
                "reviewComments" to reviewComments,
                "reviewedAt" to reviewedAt
            )

            // Verify the document exists first
            val docRef = firestore.collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)

            val docSnapshot = docRef.get().await()
            
            if (!docSnapshot.exists()) {

                throw Exception("Expense document not found")
            }

            docRef.update(updates).await()

            // Verify the update was applied

            val updatedDoc = docRef.get().await()
            if (updatedDoc.exists()) {
                val updatedData = updatedDoc.data

            } else {

            }
                
        } catch (e: Exception) {

            e.printStackTrace()
            throw e
        }
    }
    
    // Method to update user names for existing expenses
    suspend fun updateExpenseUserNames(projectId: String, userId: String, userName: String): Result<Int> {
        return try {

            // Get all expenses for this user in this project
            val snapshot = firestore.collection("projects")
                .document(projectId)
                .collection("expenses")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            var updatedCount = 0
            for (doc in snapshot.documents) {
                val expenseData = doc.data
                val currentUserName = expenseData?.get("userName") as? String ?: ""
                
                // Only update if userName is empty or different
                if (currentUserName.isEmpty() || currentUserName != userName) {
                    firestore.collection("projects")
                        .document(projectId)
                        .collection("expenses")
                        .document(doc.id)
                        .update("userName", userName)
                        .await()
                    
                    updatedCount++

                }
            }

            Result.success(updatedCount)
        } catch (e: Exception) {

            Result.failure(e)
        }
    }
    
    suspend fun getApprovedExpensesForProject(projectId: String): List<Expense> {
        return try {

            // Use new structure: customers/{customerId}/Projects/{projectId}/expenses
            val customerId = projectRepository.findCustomerIdForProject(projectId)
            val snapshot = if (customerId == null) {

                // Fallback to old structure for backward compatibility
                val projectDoc = firestore.collection("projects")
                    .document(projectId)
                    .get()
                    .await()
                
                if (!projectDoc.exists()) {

                    return emptyList()
                }
                
                val oldSnapshot = firestore.collection("projects")
                    .document(projectId)
                    .collection("expenses")
                    .whereEqualTo("status", "APPROVED")
                    .orderBy("submittedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                oldSnapshot
            } else {

                // Query approved expenses from new structure
                val newSnapshot = firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .collection("expenses")
                    .whereEqualTo("status", "APPROVED")
                    .orderBy("submittedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                newSnapshot
            }
            
            val expenses = snapshot.documents.mapNotNull { doc ->
                try {
                    val expenseData = doc.data ?: run {

                        return@mapNotNull null
                    }
                    
                    // Log raw data for debugging

                    mapDocumentToExpense(doc, projectId)?.also {

                    }
                } catch (e: Exception) {

                    e.printStackTrace()
                    null
                }
            }

            expenses
        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }
    
    // Method to get ALL expenses for a project (for approvers/reports - no user filtering)
    suspend fun getAllExpensesForProject(projectId: String): List<Expense> {
        return try {

            // Use new structure: customers/{customerId}/Projects/{projectId}/expenses
            val customerId = projectRepository.findCustomerIdForProject(projectId)
            if (customerId == null) {

                return emptyList()
            }

            val expensesCollection = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
            
            // Try ordering by createdAt first (new format), fallback to submittedAt, then no ordering
            val snapshot = try {
                val result = expensesCollection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                result
            } catch (e: Exception) {

                try {
                    val result = expensesCollection
                        .orderBy("submittedAt", Query.Direction.DESCENDING)
                        .get()
                        .await()

                    result
                } catch (e2: Exception) {

                    // Last resort: fetch without ordering (will be sorted client-side)

                    val result = expensesCollection
                        .get()
                        .await()

                    result
                }
            }

            if (snapshot.documents.isEmpty()) {

                return emptyList()
            }
            
            // Log raw document IDs for debugging
            
            val expenses = snapshot.documents.mapNotNull { doc ->
                try {
                    val expenseData = doc.data
                    
                    mapDocumentToExpense(doc, projectId)?.also {

                    }
                } catch (e: Exception) {

                    e.printStackTrace()
                    null
                }
            }

            // Sort by createdAt (client-side) if we couldn't order in query
            val sortedExpenses = expenses.sortedByDescending { 
                it.createdAt.toDate().time
            }
            
            // Log status breakdown
            val statusCounts = sortedExpenses.groupBy { it.status }

            statusCounts.forEach { (status, expenseList) ->

            }
            
            // Log department breakdown
            val deptCounts = sortedExpenses.groupBy { it.department }
            deptCounts.toList().sortedByDescending { it.second.size }.take(5).forEach { (dept, expenseList) ->

            }
            
            // Log sample expenses for debugging
            if (sortedExpenses.isNotEmpty()) {
                sortedExpenses.take(5).forEachIndexed { index, expense ->

                }
            } else {

            }

            sortedExpenses
        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }
    

    /**
     * OPTIMIZED: Get expense by ID with optional projectId for direct lookup
     * If projectId is provided, uses direct lookup (fastest - O(1))
     * Otherwise, searches efficiently using parallel queries
     */
    suspend fun getExpenseById(expenseId: String, projectId: String? = null): Expense? {
        return try {
            
            // OPTIMIZATION 1: If projectId is provided, use direct lookup (fastest path)
            if (projectId != null) {
                val customerId = projectRepository.findCustomerIdForProject(projectId)
                if (customerId != null) {
                    try {
                        val expenseSnapshot = firestore.collection("customers")
                            .document(customerId)
                            .collection("projects")
                            .document(projectId)
                            .collection("expenses")
                            .document(expenseId)
                            .get()
                            .await()
                        
                        if (expenseSnapshot.exists()) {
                            val expense = mapDocumentToExpense(expenseSnapshot, projectId)
                            if (expense != null) {
                                return expense
                            }
                        }
                    } catch (e: Exception) {

                    }
                }
            }
            
            // OPTIMIZATION 2: Check current user's customer first (most common case - O(1) instead of O(n))
            val currentCustomerId = projectRepository.getCurrentUserCustomerId()
            if (currentCustomerId != null) {
                try {
                    val projectsSnapshot = firestore.collection("customers")
                        .document(currentCustomerId)
                        .collection("projects")
                        .get()
                        .await()
                    
                    // Use parallel queries for projects in current customer
                    val expense = coroutineScope {
                        projectsSnapshot.documents.map { projectDoc ->
                            async {
                                val pid = projectDoc.id
                                try {
                                    val expenseSnapshot = firestore.collection("customers")
                                        .document(currentCustomerId)
                                        .collection("projects")
                                        .document(pid)
                                        .collection("expenses")
                                        .document(expenseId)
                                        .get()
                                        .await()
                                    
                                    if (expenseSnapshot.exists()) {
                                        mapDocumentToExpense(expenseSnapshot, pid)
                                    } else null
                                } catch (e: Exception) {

                                    null
                                }
                            }
                        }.awaitAll().firstOrNull { it != null }
                    }
                    
                    if (expense != null) {

                        return expense
                    }
                } catch (e: Exception) {

                }
            }
            
            // OPTIMIZATION 3: Search remaining customers in parallel (instead of sequential)
            val customersSnapshot = firestore.collection("customers").get().await()
            
            // Filter out current customer if already checked
            val customersToSearch = if (currentCustomerId != null) {
                customersSnapshot.documents.filter { it.id != currentCustomerId }
            } else {
                customersSnapshot.documents
            }
            
            // Use parallel queries for all remaining customers
            val expense = coroutineScope {
                customersToSearch.map { customerDoc ->
                    async {
                        val customerId = customerDoc.id
                        try {
                            val projectsSnapshot = firestore.collection("customers")
                                .document(customerId)
                                .collection("projects")
                                .get()
                                .await()
                            
                            // Parallel search within this customer's projects
                            projectsSnapshot.documents.map { projectDoc ->
                                async {
                                    val pid = projectDoc.id
                                    try {
                                        val expenseSnapshot = firestore.collection("customers")
                                            .document(customerId)
                                            .collection("projects")
                                            .document(pid)
                                            .collection("expenses")
                                            .document(expenseId)
                                            .get()
                                            .await()
                                        
                                        if (expenseSnapshot.exists()) {
                                            mapDocumentToExpense(expenseSnapshot, pid)
                                        } else null
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }.awaitAll().firstOrNull { it != null }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll().firstOrNull { it != null }
            }
            
            if (expense != null) {

                return expense
            }

            null
        } catch (e: Exception) {

            null
        }
    }
    
    fun getUserExpensesForProject(projectId: String, userId: String): Flow<List<Expense>> = callbackFlow {

        // Use new structure: customers/{customerId}/Projects/{projectId}/expenses
        val customerId = projectRepository.findCustomerIdForProject(projectId)
        if (customerId == null) {

            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        // According to implementation guide: filter by submittedBy (user phone number) for USER role
        val expensesCollection = firestore.collection("customers")
            .document(customerId)
            .collection("projects")
            .document(projectId)
            .collection("expenses")
        
        // Try submittedBy query first (primary field per guide)
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        var fallbackListener: com.google.firebase.firestore.ListenerRegistration? = null
        
        try {
            // Optimized: Only fetch pending and rejected expenses - exclude approved for better performance
            listener = expensesCollection
                .whereEqualTo("submittedBy", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {

                        // If index missing, try without orderBy
                        if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {

                            fallbackListener = expensesCollection
                                .whereEqualTo("submittedBy", userId)
                                .addSnapshotListener { fallbackSnapshot, fallbackError ->
                                    if (fallbackError != null) {

                                        trySend(emptyList())
                                        return@addSnapshotListener
                                    }
                                    val fallbackExpenses = fallbackSnapshot?.documents?.mapNotNull { doc ->
                                        mapDocumentToExpense(doc, projectId)
                                    } ?: emptyList()
                                    val sortedFallback = fallbackExpenses.sortedByDescending { it.createdAt.toDate().time }
                                    trySend(sortedFallback)
                                }
                            return@addSnapshotListener
                        }
                        // For other errors, try userId fallback

                        fallbackListener = expensesCollection
                            .whereEqualTo("userId", userId)
                            .addSnapshotListener { userIdSnapshot, userIdError ->
                                if (userIdError != null) {

                                    trySend(emptyList())
                                    return@addSnapshotListener
                                }
                                val userIdExpenses = userIdSnapshot?.documents?.mapNotNull { doc ->
                                    mapDocumentToExpense(doc, projectId)
                                } ?: emptyList()
                                val sortedUserId = userIdExpenses.sortedByDescending { it.createdAt.toDate().time }
                                trySend(sortedUserId)
                            }
                        return@addSnapshotListener
                    }
                    
                    // Process snapshot
                    if (snapshot?.isEmpty == true) {

                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val expenses = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val expenseData = doc.data ?: return@mapNotNull null
                            val expenseSubmittedBy = expenseData["submittedBy"] as? String ?: ""
                            val expenseUserId = expenseData["userId"] as? String ?: ""
                            if (expenseSubmittedBy != userId && expenseUserId != userId) {
                                return@mapNotNull null
                            }
                            mapDocumentToExpense(doc, projectId)
                        } catch (e: Exception) {

                            null
                        }
                    } ?: emptyList()
                    
                    val sortedExpenses = expenses.sortedByDescending { it.createdAt.toDate().time }

                    trySend(sortedExpenses)
                }
        } catch (e: Exception) {
            // Fallback to userId query
            listener = expensesCollection
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {

                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val fallbackExpenses = snapshot?.documents?.mapNotNull { doc ->
                        mapDocumentToExpense(doc, projectId)
                    } ?: emptyList()
                    val sortedFallback = fallbackExpenses.sortedByDescending { it.createdAt.toDate().time }
                    trySend(sortedFallback)
                }
        }
        
        awaitClose { 

            listener?.remove()
            fallbackListener?.remove()
        }
    }
    
    
    // Direct query method for debugging/fallback (NEW STRUCTURE ONLY)
    suspend fun getUserExpensesForProjectDirect(projectId: String, userId: String): List<Expense> {
        return try {
            
            // Use new structure: customers/{customerId}/Projects/{projectId}/expenses
            val customerId = projectRepository.findCustomerIdForProject(projectId)
            if (customerId == null) {

                return emptyList()
            }
            
            // According to implementation guide: filter by submittedBy (user phone number) for USER role
            val expensesCollection = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
            
            // Try submittedBy first (primary field per guide)
            // Optimized: Only fetch pending and rejected expenses - exclude approved for better performance
            val snapshot = try {
                expensesCollection
                    .whereEqualTo("submittedBy", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {

                // Fallback to userId for backward compatibility
                try {
                    expensesCollection
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                } catch (e2: Exception) {

                    return emptyList()
                }
            }

            val expenses = snapshot.documents.mapNotNull { doc ->
                try {
                    val expenseData = doc.data ?: return@mapNotNull null

                    mapDocumentToExpense(doc, projectId)
                } catch (e: Exception) {

                    null
                }
            }
            
            val sortedExpenses = expenses.sortedByDescending { 
                it.createdAt.toDate().time
            }

            sortedExpenses
        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch all expenses for a member in a project by trying every known identifier
     * (phone with/without +91, uid, email) against both `submittedBy` and `userId` fields.
     *
     * Firestore `whereIn` supports at most 10 values, so we chunk the candidates.
     * Results are deduplicated by expense id.
     */
    suspend fun getMemberExpensesForProject(
        projectId: String,
        candidates: List<String>
    ): List<Expense> {
        return try {
            val customerId = projectRepository.findCustomerIdForProject(projectId)
                ?: return emptyList()

            val expensesCollection = firestore
                .collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")

            val seenIds = mutableSetOf<String>()
            val allExpenses = mutableListOf<Expense>()

            // Query both `submittedBy` and `userId` fields in chunks of 10
            val fields = listOf("submittedBy", "userId")
            val chunks = candidates.chunked(10)

            for (field in fields) {
                for (chunk in chunks) {
                    try {
                        val snapshot = expensesCollection
                            .whereIn(field, chunk)
                            .get()
                            .await()

                        snapshot.documents.forEach { doc ->
                            if (seenIds.add(doc.id)) {
                                mapDocumentToExpense(doc, projectId)?.let { allExpenses.add(it) }
                            }
                        }
                    } catch (e: Exception) {
                        // Index may be missing — try without ordering; already no orderBy here
                        e.printStackTrace()
                    }
                }
            }

            allExpenses.sortedByDescending { it.createdAt.toDate().time }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Fallback method to get pending expenses without real-time listeners
    suspend fun getPendingExpensesDirectly(userRole: UserRole): List<Expense> {
        return try {
            // Get current user's customer ID - only query their data for performance
            val customerId = projectRepository.getCurrentUserCustomerId()
            if (customerId == null) {

                return emptyList()
            }
            
            val allExpenses = mutableListOf<Expense>()
            
            // Query only current user's customer projects
            val projectsSnapshot = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .get()
                .await()
            
            // For APPROVER role: Extract assigned departments from all projects
            val assignedDepartments = if (userRole == UserRole.APPROVER) {
                val currentUserPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "")?.trim()
                val departments = mutableSetOf<String>()
                
                if (currentUserPhone != null) {
                    projectsSnapshot.documents.forEach { projectDoc ->
                        val departmentAssignments = projectDoc.get("departmentApproverAssignments") as? Map<String, String>
                        departmentAssignments?.forEach { (deptName, phone) ->
                            if (phone.replace("+91", "").trim() == currentUserPhone) {
                                departments.add(deptName)
                            }
                        }
                    }
                }
                
                Log.d("ExpenseFilter", "APPROVER assigned departments: $departments")
                departments
            } else {
                null // BUSINESS_HEAD sees all
            }
            
            // Query expenses from all projects in parallel for better performance
            val expenseSnapshots = coroutineScope {
                Log.d("CurentUserRole", "current user role is $userRole")
                if (userRole == UserRole.APPROVER){
                    projectsSnapshot.documents.map { projectDoc ->
                        async {
                            val projectId = projectDoc.id
                            val snapshot = firestore.collection("customers")
                                .document(customerId)
                                .collection("projects")
                                .document(projectId)
                                .collection("expenses")
                                .whereEqualTo("status", ExpenseStatus.PENDING.name)
                                .get()
                                .await()
                            Pair(projectId, snapshot)
                        }
                    }.awaitAll()
                } else {
                    // For BUSINESS_HEAD: Get PENDING and APPROVED expenses (exclude COMPLETE)
                    projectsSnapshot.documents.map { projectDoc ->
                        async {
                            val projectId = projectDoc.id
                            val snapshot = firestore.collection("customers")
                                .document(customerId)
                                .collection("projects")
                                .document(projectId)
                                .collection("expenses")
                                .whereIn("status", listOf(ExpenseStatus.PENDING.name, ExpenseStatus.APPROVED.name))
                                .get()
                                .await()
                            Pair(projectId, snapshot)
                        }
                    }.awaitAll()
                }
            }
            
            // Parse all expenses using the centralized mapping function
            expenseSnapshots.forEach { (projectId, expensesSnapshot) ->
                val projectExpenses = expensesSnapshot.documents.mapNotNull { doc ->
                    mapDocumentToExpense(doc, projectId)
                }
                allExpenses.addAll(projectExpenses)
            }
            
            // Filter out COMPLETE expenses (shouldn't be in pending approvals)
            var filteredExpenses = allExpenses.filter { it.status != ExpenseStatus.COMPLETE }
            
            // For APPROVER role: Filter by assigned departments
            if (userRole == UserRole.APPROVER && assignedDepartments != null && assignedDepartments.isNotEmpty()) {
                filteredExpenses = filteredExpenses.filter { expense ->
                    // Extract department name from "phaseId_departmentName" format
                    val deptName = if (expense.department.contains("_")) {
                        expense.department.substringAfter("_")
                    } else {
                        expense.department
                    }
                    
                    val isAssigned = assignedDepartments.contains(deptName)
                    Log.d("ExpenseFilter", "Expense dept: ${expense.department} -> extracted: $deptName, isAssigned: $isAssigned")
                    isAssigned
                }
                
                Log.d("ExpenseFilter", "Filtered ${filteredExpenses.size} expenses from ${allExpenses.size} total")
            }
            
            // Sort by submission date (most recent first)
            return filteredExpenses.sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
        } catch (e: Exception) {

            return emptyList()
        }
    }

    // Method to get expenses by project (used by BudgetValidationService)
    fun getExpensesByProject(projectId: String): Flow<List<Expense>> = callbackFlow {

        val listener = firestore.collection("projects")
            .document(projectId)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {

                    close(error)
                    return@addSnapshotListener
                }
                
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    mapDocumentToExpense(doc, projectId)
                } ?: emptyList()
                
                trySend(expenses)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Fetch all payment history records for a given expense.
     */
    suspend fun getPaymentHistory(projectId: String, expenseId: String): List<com.cubiquitous.tracura.model.PaymentHistory> {
        return try {
            val customerId = projectRepository.findCustomerIdForProject(projectId) ?: return emptyList()
            val snapshot = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)
                .collection("paymentHistory")
                .orderBy("paidAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    com.cubiquitous.tracura.model.PaymentHistory(
                        id = doc.id,
                        amount = doc.getDouble("amount") ?: 0.0,
                        paymentMode = com.cubiquitous.tracura.model.PaymentMode.fromString(doc.getString("paymentMode")),
                        paymentProofURL = doc.getString("paymentProofURL"),
                        paymentProofName = doc.getString("paymentProofName"),
                        paidAt = doc.getTimestamp("paidAt") ?: com.google.firebase.Timestamp.now(),
                        paidBy = doc.getString("paidBy") ?: ""
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepo", "getPaymentHistory failed: ${e.message}")
            emptyList()
        }
    }
}