package com.cubiquitous.tracura.repository

import android.util.Log
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoLibrary
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.Phase
import com.cubiquitous.tracura.model.PhaseWithDepartments
import com.cubiquitous.tracura.model.Department
import com.cubiquitous.tracura.model.DepartmentLineItemData
import com.cubiquitous.tracura.model.LineItem
import com.cubiquitous.tracura.model.ContractorMode
import com.cubiquitous.tracura.model.TemporaryApprover
import com.cubiquitous.tracura.model.DepartmentTemporaryApproverEntry
import com.cubiquitous.tracura.model.isExpired
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val temporaryApproverRepository: TemporaryApproverRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    private data class TimedCacheValue<T>(
        val value: T,
        val savedAtMs: Long,
    )

    private val projectCustomerCache = ConcurrentHashMap<String, TimedCacheValue<String>>()
    private val allowedPhaseIdsCache = ConcurrentHashMap<String, TimedCacheValue<Set<String>>>()
    private val projectCustomerCacheTtlMs = 5 * 60 * 1000L
    private val allowedPhaseIdsCacheTtlMs = 60 * 1000L

    private fun <T> getCachedValue(
        cache: ConcurrentHashMap<String, TimedCacheValue<T>>,
        key: String,
        ttlMs: Long,
    ): T? {
        val cached = cache[key] ?: return null
        return if (System.currentTimeMillis() - cached.savedAtMs <= ttlMs) {
            cached.value
        } else {
            cache.remove(key)
            null
        }
    }

    private fun <T> putCachedValue(
        cache: ConcurrentHashMap<String, TimedCacheValue<T>>,
        key: String,
        value: T,
    ) {
        cache[key] = TimedCacheValue(value = value, savedAtMs = System.currentTimeMillis())
    }
    
    // Helper function to parse project from Firestore document
    private fun parseProjectFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): Project? {
        return try {
            val projectData = doc.data ?: return null
            val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            
            // Convert Timestamp to String for dates
            // Check both startDate and plannedDate (for backward compatibility)
            val startDateString = when {
                projectData.containsKey("startDate") -> {
                    when (val startDate = projectData["startDate"]) {
                is com.google.firebase.Timestamp -> dateFormatter.format(startDate.toDate())
                        is String -> if (startDate.isNotBlank()) startDate else null
                        else -> null
                    }
                }
                projectData.containsKey("plannedDate") -> {
                    when (val plannedDate = projectData["plannedDate"]) {
                        is com.google.firebase.Timestamp -> dateFormatter.format(plannedDate.toDate())
                        is String -> if (plannedDate.isNotBlank()) plannedDate else null
                        else -> null
                    }
                }
                else -> null
            }
            
            if (startDateString != null) {
            }
            
            val endDateString = when (val endDate = projectData["endDate"]) {
                is com.google.firebase.Timestamp -> dateFormatter.format(endDate.toDate())
                is String -> endDate
                else -> null
            }
            
            val handoverDateString = when (val handoverDate = projectData["handoverDate"]) {
                is com.google.firebase.Timestamp -> dateFormatter.format(handoverDate.toDate())
                is String -> if (handoverDate.isNotBlank()) handoverDate else null
                else -> null
            }
            
            if (handoverDateString != null) {
            } else {
            }
            
            val maintenanceDateString = when (val maintenanceDate = projectData["maintenanceDate"]) {
                is com.google.firebase.Timestamp -> dateFormatter.format(maintenanceDate.toDate())
                is String -> if (maintenanceDate.isNotBlank()) maintenanceDate else null
                else -> null
            }
            
            // Log maintenance date for debugging
            if (maintenanceDateString != null) {
            } else {
            }
            
            // Handle createdAt and updatedAt - ensure they're non-null Timestamps
            val createdAt = when (val created = projectData["createdAt"]) {
                is com.google.firebase.Timestamp -> created
                else -> com.google.firebase.Timestamp.now()
            }
            
            val updatedAt = when (val updated = projectData["updatedAt"]) {
                is com.google.firebase.Timestamp -> updated
                else -> com.google.firebase.Timestamp.now()
            }
            
            val normalizePhone: (Any?) -> String? = { raw ->
                when (raw) {
                    null -> null
                    is String -> raw.trim().ifBlank { null }
                    is Number -> raw.toLong().toString()
                    else -> raw.toString().trim().ifBlank { null }
                }
            }

            Project(
                id = doc.id,
                name = projectData["name"] as? String ?: "",
                description = projectData["description"] as? String ?: "",
                client = projectData["client"] as? String ?: "",
                clientPrimaryNumber = normalizePhone(projectData["clientPrimaryNumber"]),
                clientSecondaryNumber = normalizePhone(projectData["clientSecondaryNumber"]),
                location = projectData["location"] as? String ?: "",
                currency = projectData["currency"] as? String ?: "",
                budget = (projectData["budget"] as? Number ?: projectData["totalBudget"] as? Number)?.toDouble() ?: 0.0,
                status = projectData["status"] as? String ?: "INACTIVE",
                plannedDate = startDateString, // Map Firebase's "plannedDate" to plannedDate property
                startDate = startDateString, // Keep for backward compatibility
                endDate = endDateString,
                handoverDate = handoverDateString,
                maintenanceDate = maintenanceDateString,
                teamMembers = (projectData["teamMembers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                departmentUserAssignments = (projectData["departmentUserAssignments"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap(),
                departmentApproverAssignments = (projectData["departmentApproverAssignments"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap(),
                tempApproverID = projectData["tempApproverID"] as? String,
                Allow_Template_Overrides = projectData["Allow_Template_Overrides"] as? Boolean,
                projectCode = projectData["projectCode"] as? String ?: "",
                createdAt = createdAt,
                updatedAt = updatedAt,
                // Legacy fields for backward compatibility
                spent = (projectData["spent"] as? Number)?.toDouble(),
                managerId = projectData["managerId"] as? String,
                approverIds = (projectData["approverIds"] as? List<*>)?.mapNotNull { it as? String },
                BusinessHeadIds = (projectData["BusinessHeadIds"] as? List<*>)?.mapNotNull { it as? String },
                code = projectData["code"] as? String,
                departmentBudgets = (projectData["departmentBudgets"] as? Map<*, *>)?.entries?.associate {
                    it.key.toString() to ((it.value as? Number)?.toDouble() ?: 0.0)
                },
                categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String },
                temporaryApproverPhone = projectData["temporaryApproverPhone"] as? String,
                rejectionReason = projectData["rejectionReason"] as? String,
                rejectedBy = projectData["rejectedBy"] as? String,
                // Suspension fields
                isSuspended = projectData["isSuspended"] as? Boolean,
                suspendedDate = (projectData["suspendedDate"] as? String) ?: (projectData["suspensionDate"] as? String), // Support both old and new field names during migration
                suspensionReason = projectData["suspensionReason"] as? String,
                remainingBalance = (projectData["remainingBalance"] as? Number)?.toDouble(),
                departmentTemporaryApprover = (projectData["DepartmentTemporaryApprover"] as? List<*>)?.mapNotNull { item ->
                    val m = item as? Map<*, *> ?: return@mapNotNull null
                    DepartmentTemporaryApproverEntry(
                        phone = m["phone"] as? String ?: "",
                        startDate = (m["startDate"] as? com.google.firebase.Timestamp)?.toDate()
                            ?: m["startDate"] as? java.util.Date,
                        endDate = (m["endDate"] as? com.google.firebase.Timestamp)?.toDate()
                            ?: m["endDate"] as? java.util.Date,
                        isAccepted = m["isAccepted"] as? Boolean ?: true,
                        isActive = m["isActive"] as? Boolean ?: true,
                        departmentName = m["departmentName"] as? String ?: ""
                    )
                } ?: emptyList()
            ).also {
                // Log suspension fields for debugging
                if (it.isSuspended == true) {
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getUserProjects(userId: String): Flow<List<Project>> = callbackFlow {
        
        // Get customer ID first with retry mechanism
        var customerId: String? = null
        var retryCount = 0
        val maxRetries = 3
        
        while (customerId == null && retryCount < maxRetries) {
            try {
                customerId = getCurrentUserCustomerId()
                if (customerId == null) {
                    retryCount++
                    if (retryCount < maxRetries) {
                        delay(1000L * retryCount) // Exponential backoff
                    }
                }
            } catch (e: Exception) {
                retryCount++
                if (retryCount < maxRetries) {
                    delay(1000L * retryCount)
                }
            }
        }
        
        if (customerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        
        // Normalize phone number (remove +91 prefix if present)
        val normalizedUserId = userId.replace("+91", "").trim()
        
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        
        try {
            // Try querying with normalized phone number first
            listener = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .whereArrayContains("teamMembers", normalizedUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    var projectCount = snapshot?.documents?.size ?: 0
                    
                    // Log first few projects' teamMembers for debugging
                    snapshot?.documents?.take(3)?.forEach { doc ->
                        val teamMembers = doc.data?.get("teamMembers") as? List<*>
                        val contains = teamMembers?.any { it.toString().replace("+91", "").trim() == normalizedUserId } == true
                    }
                    
                    // If no projects found, log a warning
                    if (projectCount == 0) {
                    }
                    
                    val projects = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            parseProjectFromDocument(doc)
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()
                    
                    // Skip expensive status checks during initial load for better performance
                    // Status will be checked on-demand when project is opened
                    
                    trySend(projects)
                }
        } catch (e: Exception) {
            trySend(emptyList())
            close(e)
            return@callbackFlow
        }
        
        awaitClose { 
            listener?.remove()
        }
    }

    /**
     * Get projects where the user is a temporary approver
     */
    fun getTemporaryApproverProjects(userId: String): Flow<List<Project>> = callbackFlow {

        // Get customer ID first
        val customerId = kotlinx.coroutines.runBlocking { getCurrentUserCustomerId() }
        if (customerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        

        val listener = firestore.collection("customers")
            .document(customerId)
            .collection("projects")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }


                val allProjects = snapshot?.documents?.mapNotNull { doc ->
                    parseProjectFromDocument(doc)?.also { project ->
                    }
                } ?: emptyList()

                // Filter projects where user is a temporary approver (only accepted assignments)
                val temporaryProjects = allProjects.filter { project ->
                    // Normalize phone numbers for comparison (remove +91, spaces, etc.)
                    val projectPhone = project.temporaryApproverPhone ?: ""
                    val normalizedProjectPhone = projectPhone.replace("+91", "").replace(" ", "").trim()
                    val normalizedUserId = userId.replace("+91", "").replace(" ", "").trim()
                    
                    val isMatch = normalizedProjectPhone == normalizedUserId
                    isMatch
                }

                // Filter out rejected assignments - rejected assignments have empty temporaryApproverPhone
                // because the reject logic removes this field from the project document
                val acceptedTemporaryProjects = temporaryProjects.filter { project ->
                    val hasTemporaryApproverPhone = !project.temporaryApproverPhone.isNullOrEmpty()
                    hasTemporaryApproverPhone
                }
                
                // Filter out projects where delegation has expired
                // Note: We'll do this filtering in a different way since we can't call suspend functions in filter
                val activeTemporaryProjects = acceptedTemporaryProjects

                // Skip expensive status checks during initial load for better performance
                
                trySend(activeTemporaryProjects)
            }
        
        awaitClose { 
            listener.remove() 
        }
    }

    fun getApproverProjects(userId: String): Flow<List<Project>> = callbackFlow {

        // Get customer ID first
        val customerId = kotlinx.coroutines.runBlocking { getCurrentUserCustomerId() }
        if (customerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        

        val listener = firestore.collection("customers")
            .document(customerId)
            .collection("projects")
            .whereEqualTo("managerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }


                val regularProjects = snapshot?.documents?.mapNotNull { doc ->
                    parseProjectFromDocument(doc)
                } ?: emptyList()

                // For now, we'll only show regular projects
                // Temporary approver projects will be handled by a separate query
                val temporaryProjects = emptyList<Project>()

                // Combine regular and temporary projects
                val allProjects = (regularProjects + temporaryProjects).distinctBy { it.id }

                // Skip expensive status checks during initial load for better performance
                
                trySend(allProjects)
            }
        
        awaitClose { 
            listener.remove() 
        }
    }
    
    // Get a single project by ID - ONLY from Project1 collection
    suspend fun getProjectById(projectId: String): Project? {
        return try {
            
            // First, try to get from current user's customer (most common case)
            val customerId = getCurrentUserCustomerId()
            if (customerId != null) {
                val document = firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .get()
                    .await()
                
                if (document.exists()) {
                    val project = parseProjectFromDocument(document)
                    if (project != null) {
                            return checkAndSuspendProjectIfNeeded(project)
                    }
                }
            }
            
            // If not found in current customer, search across all customers
            val customersSnapshot = firestore.collection("customers").get().await()
            
            for (customerDoc in customersSnapshot.documents) {
                val customerId = customerDoc.id
                try {
                    val document = firestore.collection("customers")
                        .document(customerId)
                        .collection("projects")
                        .document(projectId)
                        .get()
                        .await()
                    
                    if (document.exists()) {
                        val project = parseProjectFromDocument(document)
                        if (project != null) {
                            return checkAndSuspendProjectIfNeeded(project)
                        }
                    }
                } catch (e: Exception) {
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get customer ID by email. If not found, returns null.
     */
    suspend fun getCustomerIdByEmail(email: String): String? {
        return try {
            val querySnapshot = firestore.collection("customers")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                querySnapshot.documents.first().id
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get or create customer for the given email
     */
    suspend fun getOrCreateCustomer(email: String, name: String, phoneNumber: String? = null): Result<String> {
        return try {
            // First try to get existing customer
            val existingCustomerId = getCustomerIdByEmail(email)
            if (existingCustomerId != null) {
                return Result.success(existingCustomerId)
            }
            
            // Create new customer if not found
            val customer = com.cubiquitous.tracura.model.Customer(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                businessName = "",
                location = null,
                createdAt = com.google.firebase.Timestamp.now(),
                updatedAt = com.google.firebase.Timestamp.now()
            )
            
            val docRef = firestore.collection("customers").add(customer).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get or create customer document with specific UID as document ID
     */
    suspend fun getOrCreateCustomerWithUid(uid: String, email: String, name: String, phoneNumber: String? = null): Result<String> {
        return try {
            // Check if customer document with this UID exists
            val customerDoc = firestore.collection("customers").document(uid).get().await()
            if (customerDoc.exists()) {
                // Update customer info if needed (don't overwrite existing data)
                val existingData = customerDoc.data
                if (existingData != null) {
                    val updates = mutableMapOf<String, Any>()
                    if (existingData["email"] != email && email.isNotBlank()) updates["email"] = email
                    if (existingData["name"] != name && name.isNotBlank()) updates["name"] = name
                    if (existingData["phoneNumber"] != phoneNumber && phoneNumber != null && phoneNumber.isNotBlank()) {
                        updates["phoneNumber"] = phoneNumber
                    }
                    if (updates.isNotEmpty()) {
                        updates["updatedAt"] = com.google.firebase.Timestamp.now()
                        firestore.collection("customers").document(uid).update(updates).await()
                    }
                }
                return Result.success(uid)
            }
            
            // Create customer document with UID as document ID (only if it doesn't exist)
            val customer = com.cubiquitous.tracura.model.Customer(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                businessName = "",
                location = null,
                createdAt = com.google.firebase.Timestamp.now(),
                updatedAt = com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("customers").document(uid).set(customer).await()
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get customer by customer ID
     */
    suspend fun getCustomerById(customerId: String): com.cubiquitous.tracura.model.Customer? {
        return try {
            val document = firestore.collection("customers")
                .document(customerId)
                .get()
                .await()
            
            if (document.exists()) {
                val data = document.data
                if (data != null) {
                    // Prefer departments from departmentUserCounts map (keys), fall back to legacy departments list
                    val departmentsFromMap = (data["departmentUserCounts"] as? Map<*, *>)?.keys
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    val departmentsFromList = (data["departments"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val departments = if (departmentsFromMap.isNotEmpty()) departmentsFromMap else departmentsFromList

                    com.cubiquitous.tracura.model.Customer(
                        id = document.id,
                        name = data["name"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        phoneNumber = data["phoneNumber"] as? String,
                        businessName = data["businessName"] as? String ?: "",
                        businessType = data["businessType"] as? String,
                        location = data["location"] as? String,
                        departments = departments,
                        createdAt = data["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                        updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Update customer departments:
     * - Store department → userCount mapping in a dictionary field: "departmentUserCounts"
     *   (keys are department names, values are user counts)
     * - The list of departments can be derived from the keys of this map, so we no longer
     *   persist a separate "departments" list field in Firestore.
     */
    suspend fun updateCustomerDepartments(customerId: String, departments: List<String>): Result<Unit> {
        return try {
            val docRef = firestore.collection("customers").document(customerId)
            val snapshot = docRef.get().await()

            // Read existing user count map (if any) so we don't lose counts
            val existingCountsAny = snapshot.get("departmentUserCounts")
            val existingCounts: Map<String, Long> = when (existingCountsAny) {
                is Map<*, *> -> {
                    existingCountsAny.mapNotNull { (k, v) ->
                        val key = k as? String
                        val value = when (v) {
                            is Long -> v
                            is Int -> v.toLong()
                            is Double -> v.toLong()
                            else -> null
                        }
                        if (key != null && value != null) key to value else null
                    }.toMap()
                }
                else -> emptyMap()
            }

            // Build new counts map, preserving existing counts where possible,
            // and initializing new departments with count 0
            val newCounts = mutableMapOf<String, Long>()
            departments.forEach { dept ->
                if (dept.isNotBlank()) {
                    val count = existingCounts[dept] ?: 0L
                    newCounts[dept] = count
                }
            }

            val updates = mapOf(
                // Dictionary: key = department name, value = number of users in that department
                "departmentUserCounts" to newCounts,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get department user/approver counts for a customer.
     * Returns Pair<departmentUserCounts, departmentApproverCounts>.
     */
    suspend fun getCustomerDepartmentCounts(
        customerId: String
    ): Pair<Map<String, Long>, Map<String, Long>> {
        return try {
            val snapshot = firestore.collection("customers")
                .document(customerId)
                .get()
                .await()

            if (!snapshot.exists()) {
                emptyMap<String, Long>() to emptyMap()
            } else {
                val userCountsAny = snapshot.get("departmentUserCounts")
                val approverCountsAny = snapshot.get("departmentApproverCounts")

                fun parseCounts(any: Any?): Map<String, Long> {
                    return when (any) {
                        is Map<*, *> -> any.mapNotNull { (k, v) ->
                            val key = k as? String
                            val value = when (v) {
                                is Long -> v
                                is Int -> v.toLong()
                                is Double -> v.toLong()
                                else -> null
                            }
                            if (key != null && value != null) key to value else null
                        }.toMap()
                        else -> emptyMap()
                    }
                }

                parseCounts(userCountsAny) to parseCounts(approverCountsAny)
            }
        } catch (e: Exception) {
            emptyMap<String, Long>() to emptyMap()
        }
    }
    
    /**
     * Get vendors dictionary from customer document.
     * Returns Map<String, String> where key = vendor name, value = department name.
     */
    suspend fun getCustomerVendors(customerId: String): Map<String, String> {
        return try {
            val snapshot = firestore.collection("customers")
                .document(customerId)
                .get()
                .await()

            if (!snapshot.exists()) {
                emptyMap()
            } else {
                val vendorsAny = snapshot.get("vendors")
                when (vendorsAny) {
                    is Map<*, *> -> vendorsAny.mapNotNull { (k, v) ->
                        val key = k as? String
                        val value = v as? String
                        if (key != null && value != null) key to value else null
                    }.toMap()
                    else -> emptyMap()
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Add or update a vendor in the customers collection.
     * vendors dictionary: key = vendor name, value = department name
     */
    suspend fun addOrUpdateVendor(customerId: String, vendorName: String, departmentName: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("customers").document(customerId)
            val snapshot = docRef.get().await()

            // Read existing vendors map
            val existingVendorsAny = snapshot.get("vendors")
            val existingVendors: Map<String, String> = when (existingVendorsAny) {
                is Map<*, *> -> {
                    existingVendorsAny.mapNotNull { (k, v) ->
                        val key = k as? String
                        val value = v as? String
                        if (key != null && value != null) key to value else null
                    }.toMap()
                }
                else -> emptyMap()
            }

            // Add or update vendor
            val updatedVendors = existingVendors.toMutableMap()
            updatedVendors[vendorName] = departmentName

            val updates = mapOf(
                "vendors" to updatedVendors,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a vendor from the customers collection.
     */
    suspend fun deleteVendor(customerId: String, vendorName: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("customers").document(customerId)
            val snapshot = docRef.get().await()

            // Read existing vendors map
            val existingVendorsAny = snapshot.get("vendors")
            val existingVendors: Map<String, String> = when (existingVendorsAny) {
                is Map<*, *> -> {
                    existingVendorsAny.mapNotNull { (k, v) ->
                        val key = k as? String
                        val value = v as? String
                        if (key != null && value != null) key to value else null
                    }.toMap()
                }
                else -> emptyMap()
            }

            // Remove vendor
            val updatedVendors = existingVendors.toMutableMap()
            updatedVendors.remove(vendorName)

            val updates = mapOf(
                "vendors" to updatedVendors,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add a new department to customer with 0 counts in both departmentUserCounts and departmentApproverCounts.
     */
    suspend fun addNewDepartmentToCustomer(customerId: String, departmentName: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("customers").document(customerId)
            val snapshot = docRef.get().await()

            // Read existing counts
            val (existingUserCounts, existingApproverCounts) = getCustomerDepartmentCounts(customerId)

            // Add new department with 0 counts if it doesn't exist
            val updatedUserCounts = existingUserCounts.toMutableMap()
            val updatedApproverCounts = existingApproverCounts.toMutableMap()
            
            if (!updatedUserCounts.containsKey(departmentName)) {
                updatedUserCounts[departmentName] = 0L
            }
            if (!updatedApproverCounts.containsKey(departmentName)) {
                updatedApproverCounts[departmentName] = 0L
            }

            val updates = mapOf(
                "departmentUserCounts" to updatedUserCounts,
                "departmentApproverCounts" to updatedApproverCounts,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current user's customer ID
     * - For Production Head: Use their UID as customerId
     * - For Users/Approvers: Use their customerId field from the user document (don't create customer documents)
     */
    suspend fun getCurrentUserCustomerId(): String? {
        return try {
            val selectedCustomerId = context
                .getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .getString("SelectedCustomerID", null)
                ?.takeIf { it.isNotBlank() }

            if (!selectedCustomerId.isNullOrBlank()) {
                return selectedCustomerId
            }

            // Get Firebase Auth current user UID
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (firebaseUser == null || firebaseUser.uid.isBlank()) {
                return null
            }
            
            // Get current user from Firestore to check their role and customerId
            val currentUser = authRepository.getCurrentUserFromFirebase()
            if (currentUser == null) {
                return null
            }
            
            // Check if user is Production Head
            if (currentUser.role == com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD) {
                // For Production Head: Use their UID as customerId and ensure customer document exists
                val customerId = firebaseUser.uid
                
                val userEmail = if (currentUser.email.isNotBlank()) {
                    currentUser.email
                } else if (currentUser.phone.isNotBlank()) {
                    "${currentUser.phone}@avr.local"
                } else {
                    "user@avr.local"
                }
                
                // Ensure customer document exists with this UID (only for Production Head)
                getOrCreateCustomerWithUid(
                    uid = customerId,
                    email = userEmail,
                    name = currentUser.name.ifBlank { "User ${currentUser.phone}" },
                    phoneNumber = currentUser.phone
                )
                
                return customerId
            } else {
                // For Users/Approvers: ownerID is the Production Head's UID who created them
                // This ownerID should be used as customerId to query projects
                // Projects are stored under customers/{BusinessHeadUID}/projects
                var customerId = currentUser.customerId
                
                // Always try to get ownerID directly from user document (most reliable)
                // ownerID = Production Head's UID = customer ID for projects
                try {
                    val phoneNumber = currentUser.phone
                    if (phoneNumber.isNotBlank()) {
                        // Query user document by phone number to get ownerID
                        val userQuery = firestore.collection("users")
                            .whereEqualTo("phoneNumber", phoneNumber)
                            .limit(1)
                            .get()
                            .await()
                        
                        if (!userQuery.isEmpty) {
                            val userDoc = userQuery.documents.first()
                            val ownerID = userDoc.data?.get("ownerID") as? String
                            if (ownerID != null && ownerID.isNotBlank()) {
                                customerId = ownerID
                            } else {
                            }
                        } else {
                        }
                    }
                } catch (e: Exception) {
                }
                
                if (customerId != null && customerId.isNotBlank()) {
                    return customerId
                } else {
                    return null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
   /**
     * Get current user's identifier
     * - Fetches current user from Firebase Authentication
     * - IF phone number exists: returns it starting from index 3 (removes first 3 chars like "+91")
     * - ELSE returns the User UID
     */
    fun getCurrentUserIdentifier(): String? {
        return try {
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            
            if (firebaseUser == null) {
                return null
            }

            val phoneNumber = firebaseUser.phoneNumber

            if (!phoneNumber.isNullOrBlank()) {
                // Safety check: Ensure string is long enough before slicing
                if (phoneNumber.length > 3) {
                    val slicedNumber = phoneNumber.substring(3)
                    return slicedNumber
                } else {
                    // Fallback if number is unusually short (e.g. just "+91")
                    return phoneNumber
                }
            } else {
                // Return UID if phone is null
                val uid = firebaseUser.uid
                return uid
            }

        } catch (e: Exception) {
            null
        }
    }
    /**
     * Add a phase override request to Firebase
     * Path: customers/{customerId}/Projects/{projectId}/phases/{phaseId}/requests/{requestId}
     */
    suspend fun addPhaseOverrideRequest(
        projectId: String,
        phaseId: String,
        reason: String,
        extendedDate: String,
        userPhoneNumber: String,
        phase: Phase
    ): Result<String> {
        return try {
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Customer not found for project"))
            }
            
            
            // Create request document
            val requestData = hashMapOf<String, Any>(
                "createdAt" to com.google.firebase.Timestamp.now(),
                "extendedDate" to extendedDate,
                "reason" to reason,
                "reasonToReact" to "",
                "status" to "PENDING",
                "updatedAt" to com.google.firebase.Timestamp.now(),
                "userID" to userPhoneNumber,
                "categories" to hashMapOf<String, Any>(
                    "createdAt" to com.google.firebase.Timestamp.now()
                ),
                "departments" to hashMapOf<String, Any>().apply {
                    // Add all department budgets
                    phase.departments.forEach { (deptName, budget) ->
                        put(deptName, budget)
                    }
                    // Add phase details
                    put("endDate", phase.endDate ?: "")
                    put("isEnabled", phase.isEnabledValue)
                    put("phaseName", phase.phaseName)
                    put("phaseNumber", phase.phaseNumber)
                    put("startDate", phase.startDate ?: "")
                    put("updatedAt", phase.updatedAt)
                }
            )
            
            val requestRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
                .collection("requests")
                .document()
            
            val requestId = requestRef.id
            
            // Add the id field to the request data
            requestData["id"] = requestId
            
            requestRef.set(requestData).await()
            
            
            Result.success(requestId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get phase override requests for a specific phase
     * Path: customers/{customerId}/Projects/{projectId}/phases/{phaseId}/requests
     */
    suspend fun getPhaseOverrideRequests(
        projectId: String,
        phaseId: String,
        userPhoneNumber: String
    ): List<Map<String, Any>> {
        return try {
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return emptyList()
            }
            
            
            val requestsSnapshot = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
                .collection("requests")
                .whereEqualTo("userID", userPhoneNumber)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            val requests = requestsSnapshot.documents.map { doc ->
                val data = doc.data ?: emptyMap()
                data.toMutableMap().apply {
                    put("id", doc.id)
                }
            }
            
            requests
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Check if a phase has an approved override request
     * Returns true if the phase has at least one approved request
     */
    suspend fun hasApprovedPhaseRequest(projectId: String, phaseId: String): Boolean {
        return try {
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return false
            }
            
            val requestsSnapshot = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
                .collection("requests")
                .whereEqualTo("status", "APPROVED")
                .limit(1)
                .get()
                .await()
            
            val hasApproved = !requestsSnapshot.isEmpty
            hasApproved
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get all pending phase override requests for a project (across all phases)
     * Path: customers/{customerId}/Projects/{projectId}/phases/{phaseId}/requests
     * Optimized with parallel queries for faster loading
     */
    suspend fun getAllPendingPhaseRequests(projectId: String): List<Map<String, Any>> {
        return try {
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return emptyList()
            }
            
            
            // Get all phases for the project
            val phases = getPhasesForProject(projectId)
            
            // Fetch requests from all phases in parallel for faster loading
            val allRequests = coroutineScope {
                phases.map { phase ->
                    async {
                        try {
                            val requestsSnapshot = firestore.collection("customers")
                                .document(customerId)
                                .collection("projects")
                                .document(projectId)
                                .collection("phases")
                                .document(phase.id)
                                .collection("requests")
                                .whereEqualTo("status", "PENDING")
                                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                .get()
                                .await()
                            
                            requestsSnapshot.documents.map { doc ->
                                val data = doc.data ?: emptyMap()
                                data.toMutableMap().apply {
                                    put("id", doc.id)
                                    put("phaseId", phase.id)
                                    put("phaseName", phase.phaseName)
                                }
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }.awaitAll().flatten()
            }
            
            allRequests
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get all pending phase requests across all projects for production head
     */
    suspend fun getAllPendingPhaseRequestsAcrossAllProjects(): List<Map<String, Any>> {
        return try {
            val customerId = getCurrentUserCustomerId()
            if (customerId == null) {
                return emptyList()
            }
            
            
            // Get all projects for this customer
            val allProjects = getAllProjects()
            
            // Fetch requests from all projects in parallel
            val allRequests = coroutineScope {
                allProjects.map { project ->
                    async {
                        try {
                            project.id?.let { projectId ->
                                getAllPendingPhaseRequests(projectId).map { request ->
                                    request.toMutableMap().apply {
                                        put("projectId", projectId)
                                        put("projectName", project.name)
                                    }
                                }
                            } ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }.awaitAll().flatten()
            }
            
            // Sort by createdAt descending (most recent first)
            val sortedRequests = allRequests.sortedByDescending { 
                (it["createdAt"] as? com.google.firebase.Timestamp)?.seconds ?: 0L
            }
            
            sortedRequests
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Approve a phase request
     */
    suspend fun approvePhaseRequest(
        projectId: String,
        phaseId: String,
        requestId: String,
        extendedDate: String,
        reason: String
    ): Result<Unit> {
        return try {
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Could not find customer for project"))
            }
            
            val requestRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
                .collection("requests")
                .document(requestId)
            
            // Get request data to get userID
            val requestDoc = requestRef.get().await()
            val userID = requestDoc.getString("userID") ?: ""
            
            // Update request status to APPROVED with both reasonToReact and reason fields
            // reasonToReact is used internally, reason is used for user display
            requestRef.update(
                mapOf(
                    "status" to "APPROVED",
                    "reasonToReact" to reason,
                    "reason" to reason, // Also update reason field for user display
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            
            
            // Update phase end date to the extended date
            val phaseRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
            
            // Parse extended date (dd/MM/yyyy) and update phase endDate
            val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val date = dateFormatter.parse(extendedDate)
            val formattedDate = dateFormatter.format(date ?: java.util.Date())
            
            phaseRef.update("endDate", formattedDate).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reject a phase request
     */
    suspend fun rejectPhaseRequest(
        projectId: String,
        phaseId: String,
        requestId: String,
        reason: String
    ): Result<Unit> {
        return try {
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Could not find customer for project"))
            }
            
            val requestRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
                .collection("requests")
                .document(requestId)
            
            // Get request data to get userID
            val requestDoc = requestRef.get().await()
            val userID = requestDoc.getString("userID") ?: ""
            
            // Update request status to REJECTED with both reasonToReact and reason fields
            // reasonToReact is used internally, reason is used for user display
            requestRef.update(
                mapOf(
                    "status" to "REJECTED",
                    "reasonToReact" to reason,
                    "reason" to reason, // Also update reason field for user display
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Find customer ID for a given project ID
     */
    suspend fun findCustomerIdForProject(projectId: String): String? {
        return try {
            getCachedValue(projectCustomerCache, projectId, projectCustomerCacheTtlMs)?.let { return it }

            // First try current user's customer
            val customerId = getCurrentUserCustomerId()
            if (customerId != null) {
                val document = firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .get()
                    .await()
                
                if (document.exists()) {
                    putCachedValue(projectCustomerCache, projectId, customerId)
                    return customerId
                }
            }
            
            // Search across all customers
            val customersSnapshot = firestore.collection("customers").get().await()
            for (customerDoc in customersSnapshot.documents) {
                val customerId = customerDoc.id
                val document = firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .get()
                    .await()
                
                if (document.exists()) {
                    putCachedValue(projectCustomerCache, projectId, customerId)
                    return customerId
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get project document from the correct path: customers/{customerId}/Projects/{projectId}
     * This replaces the old top-level projects collection access
     */
    suspend fun getProjectDocument(projectId: String): com.google.firebase.firestore.DocumentSnapshot? {
        return try {
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return null
            }
            
            val projectDoc = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .get()
                .await()
            
            if (!projectDoc.exists()) {
                return null
            }
            
            projectDoc
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate project code prefix from project name
     * Rules:
     * - 3+ words: First letter of first 3 words (e.g., "Royal Green Villas" -> "RGV")
     * - 2 words: First letter of first word + first 2 letters of second word (e.g., "Sky Tower" -> "STO")
     * - 1 word: First 3 letters (e.g., "Prime" -> "PRI")
     * - Pad with 'X' if less than 3 characters
     * - Default to "PRJ" if no valid words
     */
    private fun generateProjectCodePrefix(projectName: String): String {
        // Convert to uppercase and split by non-alphanumeric characters
        val words = projectName.uppercase()
            .split(Regex("[^A-Z0-9]+"))
            .filter { it.isNotEmpty() }
        
        return when {
            words.isEmpty() -> "PRJ"
            words.size >= 3 -> {
                // First letter of first 3 words
                "${words[0].first()}${words[1].first()}${words[2].first()}"
            }
            words.size == 2 -> {
                // First letter of first word + first 2 letters of second word
                val firstLetter = words[0].first().toString()
                val secondPart = words[1].take(2)
                val result = firstLetter + secondPart
                // Pad with X if less than 3 characters
                result.padEnd(3, 'X')
            }
            else -> {
                // Single word - take first 3 letters
                val result = words[0].take(3)
                // Pad with X if less than 3 characters
                result.padEnd(3, 'X')
            }
        }
    }
    
    /**
     * Generate unique project code for a new project
     * Format: PREFIX + SEQUENCE_NUMBER
     * Example: RGV1, RGV2, STO1, etc.
     */
    private suspend fun generateProjectCode(projectName: String, customerId: String): String {
        return try {
            // Generate prefix from project name
            val prefix = generateProjectCodePrefix(projectName)
            
            // Fetch all existing projects for this customer
            val existingProjects = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .get()
                .await()
            
            // Find all project codes with the same prefix
            val existingCodes = existingProjects.documents.mapNotNull { doc ->
                doc.getString("projectCode")
            }.filter { code ->
                code.startsWith(prefix)
            }
            
            // Extract sequence numbers and find the highest
            val maxSequence = existingCodes.mapNotNull { code ->
                val numberPart = code.removePrefix(prefix)
                numberPart.toIntOrNull()
            }.maxOrNull() ?: 0
            
            // Generate new code with next sequence number
            val newCode = "$prefix${maxSequence + 1}"
            
            Log.d("ProjectRepository", "Generated project code: $newCode (prefix: $prefix, max sequence: $maxSequence)")
            newCode
        } catch (e: Exception) {
            Log.e("ProjectRepository", "Error generating project code: ${e.message}", e)
            // Fallback to a default code
            "PRJ1"
        }
    }
    
    suspend fun createProject(project: Project, customerId: String): Result<String> {
        return try {
            // Generate project code for new projects
            val projectCode = if (project.projectCode.isEmpty()) {
                generateProjectCode(project.name, customerId)
            } else {
                // If projectCode already exists (e.g., from edit/resubmit), keep it
                project.projectCode
            }
            
            Log.d("ProjectRepository", "Creating project with code: $projectCode")
            
            // Create project under customers/{customerId}/Projects subcollection
            val projectData = mutableMapOf<String, Any>(
                "name" to project.name,
                "description" to project.description,
                "client" to project.client,
                "location" to project.location,
                "currency" to project.currency,
                "budget" to project.budget,
                "totalBudget" to project.budget,
                "remainingBalance" to (project.remainingBalance ?: project.budget),
                "status" to project.status,
                "projectCode" to projectCode,
                "startDate" to (project.startDate ?: ""),
                "endDate" to (project.endDate ?: ""),
                "teamMembers" to project.teamMembers,
                "departmentUserAssignments" to project.departmentUserAssignments,
                "departmentApproverAssignments" to project.departmentApproverAssignments,
                "createdAt" to project.createdAt,
                "updatedAt" to project.updatedAt
            )

            // Add client phone numbers if provided
            if (!project.clientPrimaryNumber.isNullOrBlank()) {
                projectData["clientPrimaryNumber"] = project.clientPrimaryNumber
            }
            if (!project.clientSecondaryNumber.isNullOrBlank()) {
                projectData["clientSecondaryNumber"] = project.clientSecondaryNumber
            }
            
            // Add handoverDate if set
            if (!project.handoverDate.isNullOrBlank()) {
                projectData["handoverDate"] = project.handoverDate
            }
            
            // Add maintenanceDate if set
            if (!project.maintenanceDate.isNullOrBlank()) {
                projectData["maintenanceDate"] = project.maintenanceDate
            }
            
            // Add optional fields
            project.tempApproverID?.let { projectData["tempApproverID"] = it }
            project.Allow_Template_Overrides?.let { projectData["Allow_Template_Overrides"] = it }

            // Save managerId and managerIds (legacy array) when a manager is assigned
            if (!project.managerId.isNullOrBlank()) {
                projectData["managerId"] = project.managerId
                projectData["managerIds"] = listOf(project.managerId)
            }

            // Also save as plannedDate for backward compatibility (if startDate is set)
            if (!project.startDate.isNullOrBlank()) {
                projectData["plannedDate"] = project.startDate
            }


            val docRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .add(projectData)
                .await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isProjectNameAlreadyUsed(
        customerId: String,
        projectName: String,
        excludeProjectId: String? = null
    ): Boolean {
        return try {
            val normalizedName = projectName.trim()
            if (normalizedName.isEmpty()) return false

            val snapshot = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .get()
                .await()

            snapshot.documents.any { doc ->
                if (excludeProjectId != null && doc.id == excludeProjectId) {
                    false
                } else {
                    val existingName = doc.getString("name")?.trim().orEmpty()
                    existingName.equals(normalizedName, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    

    suspend fun addPhasesToProject(customerId: String, projectId: String, phases: List<Phase>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            // Phases under customers/{customerId}/Projects/{projectId}/phases
            val phasesCol = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
            
            // Track phase document IDs and their corresponding phases
            val phaseIdToPhaseMap = mutableMapOf<String, Pair<Phase, Map<String, Double>>>()
            
            phases.forEach { phase ->
                // Generate document ID first
                val doc = phasesCol.document()
                val phaseDocumentId = doc.id
                
                // Store original departments map before transformation
                val originalDepartments = phase.departments
                
                // Transform departments map to use format: phaseDocumentId_enteredDepartmentName
                val transformedDepartments = phase.departments.mapKeys { (enteredDepartmentName, _) ->
                    "${phaseDocumentId}_$enteredDepartmentName"
                }
                
                phase.departments.forEach { (originalName, budget) ->
                    val transformedName = "${phaseDocumentId}_$originalName"
                }
                
                // Create new Phase object with transformed departments
                val phaseWithTransformedDepartments = phase.copy(
                    departments = transformedDepartments
                )
                
                batch.set(doc, phaseWithTransformedDepartments)
                
                // Store mapping for later use when saving departments
                phaseIdToPhaseMap[phaseDocumentId] = Pair(phase, originalDepartments)
            }
            batch.commit().await()
            
            // Save each department as separate document in phases/{phaseId}/departments collection
            phaseIdToPhaseMap.forEach { (phaseDocumentId, phaseData) ->
                val (phase, originalDepartments) = phaseData
                
                // Save each department as separate document
                originalDepartments.forEach { (departmentName, budget) ->
                    val department = Department.fromNameAndBudget(departmentName, budget, phaseDocumentId, projectId)
                    val deptResult = saveDepartmentToPhase(customerId, projectId, phaseDocumentId, department)
                    if (deptResult.isSuccess) {
                    } else {
                    }
                }
            }
            
            // After phases are added, calculate and update handover and maintenance dates
            calculateAndUpdateProjectDates(customerId, projectId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add phases with departments as separate documents
     * Each department is saved as a separate document in phases/{phaseId}/departments
     */
    suspend fun addPhasesWithDepartmentsToProject(
        customerId: String,
        projectId: String,
        phaseDrafts: List<com.cubiquitous.tracura.model.PhaseDraft>
    ): Result<Unit> {
        return try {
            val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val phasesCol = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
            
            var phaseNumber = 1
            phaseDrafts.forEach { phaseDraft ->
                // Create phase document
                val phaseDoc = phasesCol.document()
                val phaseId = phaseDoc.id
                
                // Create Phase object WITHOUT departments dictionary
                // Departments are now stored only in the departments subcollection
                val phase = Phase(
                    id = phaseId,
                    phaseName = phaseDraft.phaseName,
                    phaseNumber = phaseNumber++,
                    startDate = phaseDraft.start?.let { dateFormatter.format(it) },
                    endDate = phaseDraft.end?.let { dateFormatter.format(it) },
                    departments = emptyMap(), // No longer storing departments in phase document
                    categories = phaseDraft.categories,
                    isEnabled = true,
                    isanonymous = emptyMap(),
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                
                // Save phase
                phaseDoc.set(phase).await()
                
                // Save each department as separate document
                phaseDraft.departments.forEach { deptDraft ->
                    deptDraft.lineItems.forEachIndexed { idx, item ->
                    }
                    
                    val department = Department.fromDepartmentDraft(deptDraft, phaseId, projectId)
                    
                    val deptResult = saveDepartmentToPhase(customerId, projectId, phaseId, department)
                    if (deptResult.isSuccess) {
                    } else {
                    }
                }
            }
            
            // After phases are added, calculate and update handover and maintenance dates
            calculateAndUpdateProjectDates(customerId, projectId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate handover date (last phase's end date) and maintenance date (handover + 1 month)
     * and update the project document
     */
    private suspend fun calculateAndUpdateProjectDates(customerId: String, projectId: String) {
        try {
            // Get all phases for this project
            val phases = getPhasesForProject(projectId)
            
            if (phases.isEmpty()) {
                return
            }
            
            // Find the last phase's end date (the phase with the latest end date)
            val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            var latestEndDate: java.util.Date? = null
            var latestEndDateString: String? = null
            
            phases.forEach { phase ->
                if (!phase.endDate.isNullOrBlank()) {
                    try {
                        val endDate = dateFormatter.parse(phase.endDate)
                        if (endDate != null) {
                            if (latestEndDate == null || endDate.after(latestEndDate)) {
                                latestEndDate = endDate
                                latestEndDateString = phase.endDate
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }
            
            if (latestEndDateString == null) {
                return
            }
            
            // Handover date = last phase's end date
            val handoverDate = latestEndDateString
            
            // Maintenance date = handover date + 1 month
            val maintenanceDate = if (latestEndDate != null) {
                val calendar = java.util.Calendar.getInstance()
                calendar.time = latestEndDate
                calendar.add(java.util.Calendar.MONTH, 1)
                dateFormatter.format(calendar.time)
            } else {
                null
            }
            
            // Update project document with calculated dates
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
            
            val updateData = mutableMapOf<String, Any>(
                "handoverDate" to handoverDate,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            if (maintenanceDate != null) {
                updateData["maintenanceDate"] = maintenanceDate
            }
            
            projectRef.update(updateData).await()
            
        } catch (e: Exception) {
            // Don't fail the entire operation if date calculation fails
        }
    }
    
    // Add a single phase to a project
    suspend fun addPhaseToProject(
        customerId: String,
        projectId: String,
        phase: Phase,
        departmentDrafts: List<com.cubiquitous.tracura.model.DepartmentDraft> = emptyList()
    ): Result<String> {
        return try {
            val phasesCol = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
            
            // Generate document ID first
            val doc = phasesCol.document()
            val phaseDocumentId = doc.id
            
            // Store original departments map before transformation
            val originalDepartments = phase.departments
            
            // Transform departments map to use format: phaseDocumentId_enteredDepartmentName
            val transformedDepartments = phase.departments.mapKeys { (enteredDepartmentName, _) ->
                "${phaseDocumentId}_$enteredDepartmentName"
            }
            
            phase.departments.forEach { (originalName, budget) ->
                val transformedName = "${phaseDocumentId}_$originalName"
            }
            
            // Create new Phase object with transformed departments
            val phaseWithTransformedDepartments = phase.copy(
                departments = transformedDepartments
            )
            
            doc.set(phaseWithTransformedDepartments).await()
            
            // Save each department as separate document in phases/{phaseId}/departments collection
            if (departmentDrafts.isNotEmpty()) {
                departmentDrafts.forEach { deptDraft ->
                    val department = Department.fromDepartmentDraft(deptDraft, phaseDocumentId, projectId)
                    val deptResult = saveDepartmentToPhase(customerId, projectId, phaseDocumentId, department)
                    if (deptResult.isSuccess) {
                    } else {
                    }
                }
            } else {
                // Backward-compatible path
                originalDepartments.forEach { (departmentName, budget) ->
                    val department = Department.fromNameAndBudget(departmentName, budget, phaseDocumentId, projectId)
                    val deptResult = saveDepartmentToPhase(customerId, projectId, phaseDocumentId, department)
                    if (deptResult.isSuccess) {
                    } else {
                    }
                }
            }
            
            // Update project total budget after all departments are saved
            // This ensures the budget is correctly calculated even if saveDepartmentToPhase had issues
            val allPhasesWithDepts = getPhasesWithDepartmentsForProject(projectId)
            val newTotalBudget = allPhasesWithDepts
                .filter { it.phase.isEnabledValue }
                .sumOf { phaseWithDepts ->
                    phaseWithDepts.departments.sumOf { it.totalBudget }
                }
            
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
            
            // Get current project data to calculate budget increase
            val projectSnapshot = projectRef.get().await()
            val currentTotalBudget = projectSnapshot.getDouble("totalBudget")
                ?: projectSnapshot.getDouble("budget")
                ?: 0.0
            val currentRemainingBalance = projectSnapshot.getDouble("remainingBalance")
                ?: currentTotalBudget
            
            // Calculate the budget increase
            val budgetIncrease = newTotalBudget - currentTotalBudget
            
            // Add the increase to remaining balance
            val newRemainingBalance = currentRemainingBalance + budgetIncrease
            
            projectRef.update(
                mapOf(
                    "totalBudget" to newTotalBudget,
                    "budget" to newTotalBudget,
                    "remainingBalance" to newRemainingBalance,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            
            // After phase is added, recalculate and update handover and maintenance dates
            calculateAndUpdateProjectDates(customerId, projectId)
            
            Result.success(phaseDocumentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    

    private fun parsePhaseFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): Phase? {
        return try {
            val isEnabled = when {
                doc.contains("isEnabled") -> doc.getBoolean("isEnabled") ?: true
                doc.contains("enabled") -> doc.getBoolean("enabled") ?: true
                doc.contains("active") -> doc.getBoolean("active") ?: true
                doc.contains("isActive") -> doc.getBoolean("isActive") ?: true
                else -> true
            }

            val phaseDocumentId = doc.id
            val departmentsWithPrefix = (doc.get("departments") as? Map<*, *>)
                ?.entries?.associate { it.key.toString() to ((it.value as? Number)?.toDouble() ?: 0.0) }
                ?: emptyMap()

            val departments = departmentsWithPrefix.mapKeys { (key, _) ->
                if (key.startsWith("${phaseDocumentId}_")) {
                    key.substring(phaseDocumentId.length + 1)
                } else {
                    key
                }
            }

            val totalBudgetValue = (doc.get("totalBudget") as? Number)?.toDouble()
                ?: doc.getString("totalBudget")?.toDoubleOrNull()
            val remainingBudgetValue = (doc.get("remainingBudget") as? Number)?.toDouble()
                ?: doc.getString("remainingBudget")?.toDoubleOrNull()

            Phase(
                id = doc.id,
                phaseName = doc.getString("phaseName") ?: "",
                phaseNumber = (doc.getLong("phaseNumber") ?: 0L).toInt(),
                startDate = doc.getString("startDate"),
                endDate = doc.getString("endDate"),
                departments = departments,
                categories = (doc.get("categories") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                isEnabled = isEnabled,
                isanonymous = (doc.get("isanonymous") as? Map<*, *>)?.entries?.associate { it.key.toString() to ((it.value as? Boolean) ?: false) } ?: emptyMap(),
                totalBudgetValue = totalBudgetValue,
                remainingBudget = remainingBudgetValue,
                createdAt = doc.getTimestamp("createdAt") ?: com.google.firebase.Timestamp.now(),
                updatedAt = doc.getTimestamp("updatedAt") ?: com.google.firebase.Timestamp.now(),
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getPhasesForProjectInternal(
        customerId: String,
        projectId: String,
        phaseIds: Set<String>? = null,
    ): List<Phase> {
        val phasesQuery = firestore.collection("customers")
            .document(customerId)
            .collection("projects")
            .document(projectId)
            .collection("phases")

        val documents = if (phaseIds == null) {
            phasesQuery.get().await().documents
        } else if (phaseIds.isEmpty()) {
            emptyList()
        } else {
            phaseIds.toList().chunked(30).flatMap { chunk ->
                phasesQuery
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                    .documents
            }
        }

        return documents
            .mapNotNull { parsePhaseFromDocument(it) }
            .sortedBy { it.phaseNumber }
    }

    // Fetch phases for a project - ONLY from Project1 collection
    suspend fun getPhasesForProject(projectId: String): List<Phase> {
        return try {
            // Find which customer this project belongs to
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return emptyList()
            }

            getPhasesForProjectInternal(customerId, projectId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPhasesForProjectByIds(
        projectId: String,
        phaseIds: Set<String>,
        customerId: String? = null,
    ): List<Phase> {
        return try {
            val resolvedCustomerId = customerId ?: findCustomerIdForProject(projectId) ?: return emptyList()
            getPhasesForProjectInternal(resolvedCustomerId, projectId, phaseIds)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fetch phases for a project with their departments from subcollection.
     * @param source Use [Source.SERVER] after a write to ensure fresh data for project budget recalculation.
     */
    suspend fun getPhasesWithDepartmentsForProject(projectId: String, source: Source = Source.DEFAULT): List<PhaseWithDepartments> {
        return try {
            val customerId = findCustomerIdForProject(projectId)
                ?: return emptyList()
            val phases = getPhasesForProject(projectId)
            coroutineScope {
                phases.map { phase ->
                    async {
                        val departments = getDepartmentsForPhase(customerId, projectId, phase.id, source)
                        PhaseWithDepartments(phase = phase, departments = departments)
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPhasesWithDepartmentsForProjectByIds(
        projectId: String,
        phaseIds: Set<String>,
        source: Source = Source.DEFAULT,
        customerId: String? = null,
    ): List<PhaseWithDepartments> {
        return try {
            val resolvedCustomerId = customerId ?: findCustomerIdForProject(projectId) ?: return emptyList()
            val phases = getPhasesForProjectInternal(resolvedCustomerId, projectId, phaseIds)
            coroutineScope {
                phases.map { phase ->
                    async {
                        val departments = getDepartmentsForPhase(resolvedCustomerId, projectId, phase.id, source)
                        PhaseWithDepartments(phase = phase, departments = departments)
                    }
                }.awaitAll()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun updatePhaseEnabled(
        projectId: String,
        phaseId: String,
        isEnabled: Boolean
    ): Result<Unit> {
        return try {
            // Find which customer this project belongs to
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Could not find customer for project"))
            }
            
            firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
                .update(mapOf(
                    "isEnabled" to isEnabled,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                ))
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Complete a phase by setting end date to today and disabling it in a single operation
     */
    suspend fun completePhase(
        projectId: String,
        phaseId: String,
        endDate: String
    ): Result<Unit> {
        return try {
            
            // Find which customer this project belongs to
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Could not find customer for project"))
            }
            
            
            val phaseRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
            
            // Verify document exists before updating
            val docSnapshot = phaseRef.get().await()
            if (!docSnapshot.exists()) {
                return Result.failure(Exception("Phase document not found"))
            }
            
            
            // Prepare update data
            val updateData = mapOf(
                "endDate" to endDate,
                "isEnabled" to false,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            
            // Update both endDate and isEnabled in a single operation
            phaseRef.update(updateData).await()
            
            // Verify the update was successful
            val updatedSnapshot = phaseRef.get().await()
            val updatedEndDate = updatedSnapshot.getString("endDate")
            val updatedIsEnabled = updatedSnapshot.getBoolean("isEnabled")
            
            
            if (updatedEndDate != endDate) {
                return Result.failure(Exception("endDate was not updated correctly"))
            }
            
            // Recalculate project handover and maintenance dates after phase completion
            calculateAndUpdateProjectDates(customerId, projectId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    
    // Update phase name, start date, and end date
    suspend fun updatePhaseDetails(
        projectId: String,
        phaseId: String,
        phaseName: String,
        startDate: String?,
        endDate: String?
    ): Result<Unit> {
        return try {
            // Find which customer this project belongs to
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Could not find customer for project"))
            }
            
            val updateData = mutableMapOf<String, Any>(
                "phaseName" to phaseName,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            if (startDate != null) {
                updateData["startDate"] = startDate
            }
            
            if (endDate != null) {
                updateData["endDate"] = endDate
                
                // Check if the new end date is in the future
                // If so, set isEnabled to true to make the phase active again
                try {
                    val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val endDateObj = dateFormatter.parse(endDate)
                    val today = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.time
                    
                    // If new end date is in the future (greater than today), enable the phase
                    if (endDateObj != null && endDateObj.time > today.time) {
                        updateData["isEnabled"] = true
                    }
                } catch (e: Exception) {
                    // Continue with update even if date parsing fails
                }
            }
            
            // Use the correct path: customers/{customerId}/Projects/{projectId}/phases/{phaseId}
            firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
                .update(updateData)
                .await()
            
            // If end date was updated, recalculate project handover and maintenance dates
            if (endDate != null) {
                calculateAndUpdateProjectDates(customerId, projectId)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateProject(projectId: String, project: Project): Result<Unit> {
        return try {
            // Find which customer this project belongs to
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Project not found"))
            }
            
            // Update project data
            val projectData = mutableMapOf<String, Any>(
                "name" to project.name,
                "description" to project.description,
                "client" to project.client,
                "location" to project.location,
                "currency" to project.currency,
                "budget" to project.budget,
                "status" to project.status,
                "startDate" to (project.startDate ?: ""),
                "endDate" to (project.endDate ?: ""),
                "teamMembers" to project.teamMembers,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            // Add client phone numbers (write empty string to clear, or value to set)
            projectData["clientPrimaryNumber"] = project.clientPrimaryNumber ?: ""
            projectData["clientSecondaryNumber"] = project.clientSecondaryNumber ?: ""
            
            // Preserve existing projectCode (DO NOT regenerate on update)
            if (project.projectCode.isNotEmpty()) {
                projectData["projectCode"] = project.projectCode
            }
            
            // Add handoverDate if set
            if (!project.handoverDate.isNullOrBlank()) {
                projectData["handoverDate"] = project.handoverDate
            } else {
                // Remove handoverDate if it's empty (use FieldValue.delete())
                projectData["handoverDate"] = com.google.firebase.firestore.FieldValue.delete()
            }
            
            // Add maintenanceDate if set
            if (!project.maintenanceDate.isNullOrBlank()) {
                projectData["maintenanceDate"] = project.maintenanceDate
            } else {
                // Remove maintenanceDate if it's empty (use FieldValue.delete())
                projectData["maintenanceDate"] = com.google.firebase.firestore.FieldValue.delete()
            }
            
            // Also save as plannedDate for backward compatibility (if startDate is set)
            if (!project.startDate.isNullOrBlank()) {
                projectData["plannedDate"] = project.startDate
            }
            
            // Add optional fields only if they are not null
            project.tempApproverID?.let { projectData["tempApproverID"] = it }
            project.Allow_Template_Overrides?.let { projectData["Allow_Template_Overrides"] = it }

            // Keep managerId and managerIds in sync
            if (!project.managerId.isNullOrBlank()) {
                projectData["managerId"] = project.managerId
                projectData["managerIds"] = listOf(project.managerId)
            }
            
            
            // Add rejectionReason if present
            if (project.rejectionReason != null && project.rejectionReason.isNotBlank()) {
                projectData["rejectionReason"] = project.rejectionReason
            }
            
            // Add suspension fields if present
            if (project.isSuspended == true) {
                projectData["isSuspended"] = true
                if (!project.suspendedDate.isNullOrBlank()) {
                    projectData["suspendedDate"] = project.suspendedDate
                }
                if (!project.suspensionReason.isNullOrBlank()) {
                    projectData["suspensionReason"] = project.suspensionReason
                }
            }
            
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
            
            // Build delete map for fields that should be removed
            val deleteData = mutableMapOf<String, Any>()
            if (project.rejectionReason == null || project.rejectionReason.isBlank()) {
                deleteData["rejectionReason"] = com.google.firebase.firestore.FieldValue.delete()
            }
            if (project.handoverDate.isNullOrBlank()) {
                deleteData["handoverDate"] = com.google.firebase.firestore.FieldValue.delete()
            }
            if (project.maintenanceDate.isNullOrBlank()) {
                deleteData["maintenanceDate"] = com.google.firebase.firestore.FieldValue.delete()
            }
            if (project.managerId.isNullOrBlank()) {
                deleteData["managerId"] = com.google.firebase.firestore.FieldValue.delete()
            }
            // Remove suspension fields if suspension is disabled
            if (project.isSuspended != true) {
                deleteData["isSuspended"] = com.google.firebase.firestore.FieldValue.delete()
                deleteData["suspendedDate"] = com.google.firebase.firestore.FieldValue.delete()
                deleteData["suspensionDate"] = com.google.firebase.firestore.FieldValue.delete() // Also delete old field name
                deleteData["suspensionReason"] = com.google.firebase.firestore.FieldValue.delete()
            }
            
            // Update project data
            if (deleteData.isNotEmpty()) {
                projectRef.update(deleteData as Map<String, Any>).await()
            }
            
            // Update the main project data
            projectRef.update(projectData).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Replace all phases and their department subcollections for an existing project.
     *
     * Used during DECLINED project resubmission to atomically replace the old phase/department
     * structure with the user's updated data while preserving the project document itself.
     *
     * Steps:
     *  1. Fetch all existing phase docs for the project
     *  2. For each phase, delete its departments subcollection then delete the phase doc
     *  3. Write the new phases and their departments from [phaseDrafts]
     *  4. Recalculate project-level budget and update the project document
     */
    suspend fun replacePhasesWithDepartmentsForProject(
        customerId: String,
        projectId: String,
        phaseDrafts: List<com.cubiquitous.tracura.model.PhaseDraft>
    ): Result<Unit> {
        return try {
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)

            val phasesCol = projectRef.collection("phases")

            // --- Step 1 & 2: delete existing phases and their department/request subcollections ---
            val existingPhases = phasesCol.get().await()
            for (phaseDoc in existingPhases.documents) {
                // Delete department documents inside this phase
                val deptDocs = phaseDoc.reference.collection("departments").get().await()
                for (deptDoc in deptDocs.documents) {
                    deptDoc.reference.delete().await()
                }
                // Delete request documents inside this phase (if any)
                val reqDocs = phaseDoc.reference.collection("requests").get().await()
                for (reqDoc in reqDocs.documents) {
                    reqDoc.reference.delete().await()
                }
                // Delete the phase document itself
                phaseDoc.reference.delete().await()
            }
            Log.d("ProjectRepository", "🗑️ Deleted ${existingPhases.size()} existing phases for project: $projectId")

            // --- Step 3: write new phases and departments from phaseDrafts ---
            val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            var phaseNumber = 1
            var newTotalBudget = 0.0

            for (phaseDraft in phaseDrafts) {
                val phaseDoc = phasesCol.document()
                val phaseId = phaseDoc.id

                val phase = Phase(
                    id = phaseId,
                    phaseName = phaseDraft.phaseName,
                    phaseNumber = phaseNumber++,
                    startDate = phaseDraft.start?.let { dateFormatter.format(it) },
                    endDate = phaseDraft.end?.let { dateFormatter.format(it) },
                    departments = emptyMap(), // departments stored in subcollection
                    categories = phaseDraft.categories,
                    isEnabled = true,
                    isanonymous = emptyMap(),
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                phaseDoc.set(phase).await()

                // Write each department as its own subcollection document
                for (deptDraft in phaseDraft.departments) {
                    val department = Department.fromDepartmentDraft(deptDraft, phaseId, projectId)
                    saveDepartmentToPhase(customerId, projectId, phaseId, department)
                    newTotalBudget += department.totalBudget
                }
            }
            Log.d("ProjectRepository", "✅ Wrote ${phaseDrafts.size} new phases for project: $projectId, total budget: $newTotalBudget")

            // --- Step 4: update project-level budget ---
            projectRef.update(
                mapOf(
                    "totalBudget" to newTotalBudget,
                    "budget" to newTotalBudget,
                    "remainingBalance" to newTotalBudget,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()

            // Recalculate handover/maintenance dates from new phases
            calculateAndUpdateProjectDates(customerId, projectId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProjectRepository", "❌ replacePhasesWithDepartmentsForProject failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a project can be deleted (must be in DRAFT status with no expenses)
     */
    suspend fun canDeleteProject(projectId: String): Boolean {
        return try {
            // Find customerId for this project
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return false
            }
            
            // Use the new path: customers/{customerId}/Projects/{projectId}
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
            
            val projectDoc = projectRef.get().await()
            
            if (!projectDoc.exists()) {
                return false
            }
            
            val status = projectDoc.getString("status") ?: "ACTIVE"
            
//            if (status != "DRAFT") {
//                return false
//            }
            
            // Check if there are any expenses
            val expensesSnapshot = projectRef.collection("expenses").limit(1).get().await()
            val hasExpenses = expensesSnapshot.documents.isNotEmpty()
            
            
            // Can delete only if DRAFT and no expenses
            val canDelete = !hasExpenses
            canDelete
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete a project and all its subcollections (phases, expenses, etc.)
     */
    suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            // Find customerId for this project
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Project not found"))
            }

            // Use the new path: customers/{customerId}/projects/{projectId}
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)

            // First verify the project can be deleted
            if (!canDeleteProject(projectId)) {
                return Result.failure(Exception("Project cannot be deleted. It must be in DRAFT status with no expenses."))
            }

            // Delete all phases and their nested subcollections (requests, departments)
            val phasesSnapshot = projectRef.collection("phases").get().await()
            phasesSnapshot.documents.forEach { phaseDoc ->
                // Delete requests subcollection within each phase
                val requestsSnapshot = phaseDoc.reference.collection("requests").get().await()
                requestsSnapshot.documents.forEach { it.reference.delete().await() }
                // Delete departments subcollection within each phase
                val departmentsSnapshot = phaseDoc.reference.collection("departments").get().await()
                departmentsSnapshot.documents.forEach { it.reference.delete().await() }
                // Delete the phase document itself
                phaseDoc.reference.delete().await()
            }

            // Delete all expenses and their chats/messages subcollections
            val expensesSnapshot = projectRef.collection("expenses").get().await()
            expensesSnapshot.documents.forEach { expenseDoc ->
                val chatsSnapshot = expenseDoc.reference.collection("chats").get().await()
                chatsSnapshot.documents.forEach { chatDoc ->
                    val messagesSnapshot = chatDoc.reference.collection("messages").get().await()
                    messagesSnapshot.documents.forEach { it.reference.delete().await() }
                    chatDoc.reference.delete().await()
                }
                expenseDoc.reference.delete().await()
            }

            // Delete the project document itself
            projectRef.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add or update a single department allocation under a specific phase document.
     * Phase documents are stored under customers/{customerId}/Projects/{projectId}/phases/{phaseId}.
     */
    suspend fun addDepartmentToPhase(
        projectId: String,
        phaseId: String,
        departmentName: String,
        budget: Double
    ): Result<Unit> {
        return try {
            
            // Find customerId for this project
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Could not find customer for project"))
            }
            
            
            // Use the new path: customers/{customerId}/Projects/{projectId}/phases/{phaseId}
            val phaseRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)

            // Verify phase exists
            val snapshot = phaseRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("Phase not found: $phaseId"))
            }
            
            // Note: We no longer read or update the phase's departments map dictionary
            // Departments are stored only in the departments subcollection
            // Create a Department object and save it to the subcollection
            val department = Department.fromNameAndBudget(departmentName, budget, phaseId, projectId)
            val saveResult = saveDepartmentToPhase(customerId, projectId, phaseId, department)
            
            if (saveResult.isFailure) {
                return Result.failure(saveResult.exceptionOrNull() ?: Exception("Failed to save department"))
            }


            // Update project total budget from departments subcollection
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                
            val allPhasesWithDepts = getPhasesWithDepartmentsForProject(projectId)
            val newTotalBudget = allPhasesWithDepts
                .filter { it.phase.isEnabledValue }
                .sumOf { phaseWithDepts ->
                    phaseWithDepts.departments.sumOf { it.totalBudget }
                }
            
            
            // Get current project data to calculate budget increase
            val projectSnapshot = projectRef.get().await()
            val currentTotalBudget = projectSnapshot.getDouble("totalBudget")
                ?: projectSnapshot.getDouble("budget")
                ?: 0.0
            val currentRemainingBalance = projectSnapshot.getDouble("remainingBalance")
                ?: currentTotalBudget
            
            // Calculate the budget increase
            val budgetIncrease = newTotalBudget - currentTotalBudget
            
            // Add the increase to remaining balance
            val newRemainingBalance = currentRemainingBalance + budgetIncrease
            
            projectRef.update(
                mapOf(
                    "budget" to newTotalBudget,
                    "totalBudget" to newTotalBudget,
                    "remainingBalance" to newRemainingBalance,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update department budget in a specific phase
     * Now updates the department in the subcollection instead of the phase dictionary
     */
    suspend fun updateDepartmentBudgetInPhase(
        projectId: String,
        phaseId: String,
        departmentName: String,
        newBudget: Double
    ): Result<Unit> {
        return try {
            
            // Find customerId for this project
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Could not find customer for project"))
            }
            
            // Find the department in the subcollection
            val departmentsCol = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
                .collection("departments")
            
            // Find department by name (case-insensitive)
            val existingDept = departmentsCol
                .whereEqualTo("name", departmentName)
                .get()
                .await()
            
            if (existingDept.documents.isEmpty()) {
                return Result.failure(Exception("Department not found in this phase"))
            }
            
            // Update the department's totalBudget by updating lineItems or directly
            // For now, we'll update the totalBudget field directly
            val deptDoc = existingDept.documents.first()
            val currentDept = deptDoc.toObject(Department::class.java)
            
            if (currentDept != null) {
                // Update the department document
                deptDoc.reference.update(
                    mapOf(
                        "totalBudget" to newBudget,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                ).await()
            } else {
                return Result.failure(Exception("Could not parse department"))
            }

            // Update project total budget from departments subcollection
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                
            val allPhasesWithDepts = getPhasesWithDepartmentsForProject(projectId)
            val newTotalBudget = allPhasesWithDepts
                .filter { it.phase.isEnabledValue }
                .sumOf { phaseWithDepts ->
                    phaseWithDepts.departments.sumOf { it.totalBudget }
                }
            
            
            projectRef.update(
                mapOf(
                    "totalBudget" to newTotalBudget,
                    "budget" to newTotalBudget,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Save a department as a separate document in phases/{phaseId}/departments collection
     */
    suspend fun saveDepartmentToPhase(
        customerId: String,
        projectId: String,
        phaseId: String,
        department: Department
    ): Result<String> {
        return try {
            department.lineItems.forEachIndexed { index, item ->
            }
            
            val departmentsCol = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
                .collection("departments")
            
            // Check if department with same name already exists
            val existingDept = departmentsCol
                .whereEqualTo("name", department.name)
                .get()
                .await()
            
            
            // Ensure phaseId and projectId are set
            val departmentToSave = department.copy(
                phaseId = phaseId,
                projectId = projectId
            )
            
            // Convert lineItems to maps for proper Firestore serialization
            val lineItemsAsMaps = departmentToSave.lineItems.map { lineItem ->
                mapOf(
                    "itemType" to lineItem.itemType,
                    "item" to lineItem.item,
                    "spec" to lineItem.spec,
                    "quantity" to lineItem.quantity,
                    "unitPrice" to lineItem.unitPrice,
                    "uom" to lineItem.uom,
                    "remainingQuantity" to lineItem.remainingQuantity
                )
            }
            
            lineItemsAsMaps.forEachIndexed { index, map ->
            }
            
            // Create a map with only the fields we want to save (excluding id, totalBudget, contractorModeEnum)
            val now = com.google.firebase.Timestamp.now()
            val dataToSave = mutableMapOf<String, Any>(
                "name" to departmentToSave.name,
                "contractorMode" to departmentToSave.contractorMode,
                "lineItems" to lineItemsAsMaps,
                "phaseId" to departmentToSave.phaseId,
                "projectId" to departmentToSave.projectId,
                "remainingAmount" to departmentToSave.remainingAmount,
                "totalBudget" to departmentToSave.totalBudget,
                "contractorAmount" to departmentToSave.contractorAmount,
                "contractorRemainingAmount" to departmentToSave.contractorRemainingAmount,
                "isFinished" to departmentToSave.isFinished,
                "updatedAt" to now
            )
            
            
            val departmentId = if (existingDept.documents.isNotEmpty()) {
                // Update existing department - preserve createdAt
                val existingId = existingDept.documents.first().id
                val existingDoc = existingDept.documents.first()
                val existingContractorMode = existingDoc.getString("contractorMode")
                
                val existingCreatedAt = existingDoc.getTimestamp("createdAt")
                if (existingCreatedAt != null) {
                    dataToSave["createdAt"] = existingCreatedAt
                }
                departmentsCol.document(existingId).set(dataToSave, com.google.firebase.firestore.SetOptions.merge()).await()
                
                // Verify what was actually saved
                val savedDoc = departmentsCol.document(existingId).get().await()
                val savedContractorMode = savedDoc.getString("contractorMode")
                val savedLineItems = savedDoc.get("lineItems")
                if (savedLineItems is List<*>) {
                }
                
                existingId
            } else {
                // Create new department - set createdAt
                val newDoc = departmentsCol.document()
                dataToSave["createdAt"] = departmentToSave.createdAt
                departmentsCol.document(newDoc.id).set(dataToSave, com.google.firebase.firestore.SetOptions.merge()).await()
                
                // Verify what was actually saved
                val savedDoc = departmentsCol.document(newDoc.id).get().await()
                val savedContractorMode = savedDoc.getString("contractorMode")
                val savedLineItems = savedDoc.get("lineItems")
                if (savedLineItems is List<*>) {
                }
                
                newDoc.id
            }
            
            // Recalculate phase total budget and update the phase document
            val allPhasesWithDepts = getPhasesWithDepartmentsForProject(projectId, Source.SERVER)
            val currentPhaseWithDepts = allPhasesWithDepts.find { it.phase.id == phaseId }
            
            if (currentPhaseWithDepts != null) {
                val newPhaseTotalBudget = currentPhaseWithDepts.departments.sumOf { it.totalBudget }
                val newPhaseRemainingBudget = currentPhaseWithDepts.departments.sumOf { it.remainingAmount }
                
                // Also update the departments dictionary on the phase document.
                // Key format: "{phaseId}_{departmentName}" → department total budget
                // This keeps the legacy departments map in sync so consumers that read it remain correct.
                val deptKey = "${phaseId}_${department.name}"
                val deptBudget = department.totalBudget
                
                firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .collection("phases")
                    .document(phaseId)
                    .update(mapOf(
                        "remainingBudget" to newPhaseRemainingBudget,
                        "totalBudget" to newPhaseTotalBudget,
                        "departments.$deptKey" to deptBudget, // e.g. "phaseId_Civil": 50000.0
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )).await()
            }
            
            // Recalculate project total budget using SERVER source so we read the department we just updated
            val newTotalBudget = allPhasesWithDepts
                .filter { it.phase.isEnabledValue }
                .sumOf { phaseWithDepts ->
                    phaseWithDepts.departments.sumOf { it.totalBudget }
                }

            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)

            // Calculate the budget delta to update remainingBalance correctly
            val projectSnapshot = projectRef.get().await()
            val currentTotalBudget = projectSnapshot.getDouble("totalBudget")
                ?: projectSnapshot.getDouble("budget")
                ?: 0.0
            val currentRemainingBalance = projectSnapshot.getDouble("remainingBalance")
                ?: currentTotalBudget
            
            val budgetDelta = newTotalBudget - currentTotalBudget
            val newRemainingBalance = currentRemainingBalance + budgetDelta

            projectRef.update(
                mapOf(
                    "totalBudget" to newTotalBudget,
                    "budget" to newTotalBudget,
                    "remainingBalance" to newRemainingBalance,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()

            Result.success(departmentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all departments for a phase from the departments subcollection.
     * @param source Use [Source.SERVER] to force a fresh read after a write.
     */
    suspend fun getDepartmentsForPhase(
        customerId: String,
        projectId: String,
        phaseId: String,
        source: Source = Source.DEFAULT
    ): List<Department> {
        return try {
            val departmentsCol = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)
                .collection("departments")

            val snapshot = departmentsCol.get(source).await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    // Always use manual parsing for lineItems since they're stored as maps
                    // toObject doesn't properly deserialize List<Map> to List<DepartmentLineItemData>
                    var department: Department? = null
                    
                    // Log what's actually in the document
                    val lineItemsRaw = doc.get("lineItems")
                    
                    // Manual parsing to handle lineItems as maps
                    val name = doc.getString("name") ?: ""
                    // Handle both old format enums and display strings
                    val contractorModeRaw = doc.getString("contractorMode") ?: "Labour-Only"
                    val contractorMode = when (contractorModeRaw) {
                        "LABOUR_ONLY" -> "Labour-Only"
                        "SELF_EXECUTION" -> "Self Execution"
                        "TURNKEY" -> "Turnkey" // legacy self execution
                        "TURNKEY_CONTRACTOR_ONLY" -> "Turnkey"
                        else -> contractorModeRaw // Already in correct format
                    }
                    
                    // Parse lineItems - handle both List<Map> and List<DocumentSnapshot>
                    val lineItemsList = doc.get("lineItems")
                    
                    val lineItems = when {
                        lineItemsList is List<*> -> {
                            lineItemsList.mapIndexedNotNull { index, item ->
                                when (item) {
                                    is Map<*, *> -> {
                                        try {
                                            val itemMap = item
                                            val lineItem = DepartmentLineItemData(
                                                itemType = itemMap["itemType"] as? String ?: "",
                                                item = itemMap["item"] as? String ?: "",
                                                spec = itemMap["spec"] as? String ?: "",
                                                quantity = (itemMap["quantity"] as? Number)?.toDouble() ?: 0.0,
                                                unitPrice = (itemMap["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                                                uom = itemMap["uom"] as? String ?: "",
                                                remainingQuantity = (itemMap["remainingQuantity"] as? Number)?.toDouble() ?: (itemMap["quantity"] as? Number)?.toDouble() ?: 0.0
                                            )
                                            lineItem
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    else -> {
                                        try {
                                            val lineItem = (item as? com.google.firebase.firestore.DocumentSnapshot)?.toObject(DepartmentLineItemData::class.java)
                                            if (lineItem != null) {
                                            }
                                            lineItem
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            emptyList()
                        }
                    }
                    
                    
                    val phaseIdFromDoc = doc.getString("phaseId") ?: phaseId // Use provided phaseId as fallback
                    val projectIdFromDoc = doc.getString("projectId") ?: projectId // Use provided projectId as fallback
                    
                    val contractorAmount = (doc.get("contractorAmount") as? Number)?.toDouble() ?: 0.0
                    val contractorRemainingAmount = (doc.get("contractorRemainingAmount") as? Number)?.toDouble()
                        ?: contractorAmount

                    // Calculate total budget (fallback)
                    val calculatedTotalBudget = when (contractorMode) {
                        "Labour-Only" -> lineItems.sumOf { it.total } + contractorAmount
                        "Turnkey" -> contractorAmount
                        else -> lineItems.sumOf { it.total }
                    }
                    
                    // Get remainingAmount from Firestore, default to calculatedTotalBudget for backward compatibility
                    val remainingAmount = (doc.get("remainingAmount") as? Number)?.toDouble() ?: calculatedTotalBudget
                    
                    // Get totalBudget from Firestore, default to calculatedTotalBudget for backward compatibility
                    val totalBudget = (doc.get("totalBudget") as? Number)?.toDouble() ?: calculatedTotalBudget

                    // Get finish state from Firestore
                    val isFinished = doc.getBoolean("isFinished") ?: false
                    
                    department = Department(
                        id = doc.id,
                        name = name,
                        contractorMode = contractorMode,
                        lineItems = lineItems,
                        phaseId = phaseIdFromDoc,
                        projectId = projectIdFromDoc,
                        remainingAmount = remainingAmount,
                        totalBudget = totalBudget,
                        contractorAmount = contractorAmount,
                        contractorRemainingAmount = contractorRemainingAmount,
                        isFinished = isFinished,
                        createdAt = doc.getTimestamp("createdAt") ?: com.google.firebase.Timestamp.now(),
                        updatedAt = doc.getTimestamp("updatedAt") ?: com.google.firebase.Timestamp.now()
                    )
                    
                    // Log detailed lineItems information
                    department.lineItems.forEachIndexed { index, lineItem ->
                    }
                    
                    // Log itemTypes found
                    val itemTypes = department.lineItems.mapNotNull { it.itemType }.distinct().filter { it.isNotEmpty() }
                    
                    department
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Delete department from a specific phase
     */
    suspend fun deleteDepartmentFromPhase(
        projectId: String,
        phaseId: String,
        departmentName: String
    ): Result<Unit> {
        return try {
            // Find customerId for this project
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Could not find customer for project"))
            }
            
            // Use the new path: customers/{customerId}/Projects/{projectId}/phases/{phaseId}
            val phaseRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .document(phaseId)

            val snapshot = phaseRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("Phase not found"))
            }
            
            val currentDepartments = (snapshot.get("departments") as? Map<*, *>)
                ?.entries?.associate { it.key.toString() to ((it.value as? Number)?.toDouble() ?: 0.0) }
                ?: emptyMap()
            
            // Check both old format (just department name) and new format (phaseDocumentId_departmentName)
            val departmentKeyWithPrefix = "${phaseId}_$departmentName"
            val matchingDeptKey = currentDepartments.keys.firstOrNull { key ->
                // Check if it matches the new format
                key.equals(departmentKeyWithPrefix, ignoreCase = true) ||
                // Or if it matches the old format (backward compatibility)
                (key.trim().equals(departmentName.trim(), ignoreCase = true) && !key.contains("_"))
            }
            
            if (matchingDeptKey == null) {
                return Result.failure(Exception("Department not found in this phase"))
            }

            val updated = currentDepartments.toMutableMap()
            updated.remove(matchingDeptKey)

            phaseRef.update(
                mapOf(
                    "departments" to updated,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()

            // Update project total budget - sum of all department budgets across all active phases
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
            val allPhases = getPhasesForProject(projectId)
            val newTotalBudget = allPhases
                .filter { it.isEnabledValue }
                .sumOf { phase ->
                    phase.departments.values.sum()
                }
            
            projectRef.update(
                mapOf(
                    "totalBudget" to newTotalBudget,
                    "budget" to newTotalBudget
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete department from all phases in a project
     * Instead of removing the department, sets isanonymous=true to mark it as deleted
     * This allows existing expenses to be shown under "Others" category
     */
    suspend fun deleteDepartmentFromAllPhases(
        projectId: String,
        departmentName: String
    ): Result<Unit> {
        return try {
            
            // Find customerId for this project
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Could not find customer for project"))
            }
            
            
            val phases = getPhasesForProject(projectId)
            val batch = firestore.batch()
            
            phases.forEach { phase ->
                // Use the new path: customers/{customerId}/Projects/{projectId}/phases/{phaseId}
                val phaseRef = firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .collection("phases")
                    .document(phase.id)
                
                // Check if department exists in this phase
                val matchingDeptName = phase.departments.keys.firstOrNull { 
                    it.trim().equals(departmentName.trim(), ignoreCase = true) 
                }
                
                if (matchingDeptName != null) {
                    // Get current isanonymous map
                    val currentIsAnonymous = (phase.isanonymous ?: emptyMap()).toMutableMap()
                    
                    // Mark this department as anonymous (deleted)
                    currentIsAnonymous[matchingDeptName] = true
                    
                    // Update phase with isanonymous field
                    // Keep the department in the map but mark it as anonymous
                    batch.update(
                        phaseRef,
                        mapOf(
                            "isanonymous" to currentIsAnonymous,
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        )
                    )
                    
                }
            }
            
            batch.commit().await()
            
            // Update project total budget - sum of all non-anonymous department budgets across all active phases
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                
            val allPhases = getPhasesForProject(projectId)
            val newTotalBudget = allPhases
                .filter { it.isEnabledValue }
                .sumOf { phase ->
                    // Only count non-anonymous departments
                    phase.departments.filter { (deptName, _) ->
                        phase.isanonymous[deptName] != true
                    }.values.sum()
                }
            
            
            projectRef.update(
                mapOf(
                    "totalBudget" to newTotalBudget,
                    "budget" to newTotalBudget,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if at least one department exists in each phase
     * Returns false if any phase would have zero departments after deletion
     * A phase must have at least one department at all times
     */
    /**
     * Get department name by ID from departments collection
     */
    suspend fun getDepartmentNameById(departmentId: String): String? {
        return try {
            val doc = firestore.collection("departments")
                .document(departmentId)
                .get()
                .await()
            
            if (doc.exists()) {
                val data = doc.data
                val name = data?.get("name") as? String 
                    ?: data?.get("departmentName") as? String
                    ?: data?.get("title") as? String
                name
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get multiple department names by IDs (batch fetch)
     */
    suspend fun getDepartmentNamesByIds(departmentIds: List<String>): Map<String, String> {
        return try {
            val results = mutableMapOf<String, String>()
            departmentIds.forEach { id ->
                val name = getDepartmentNameById(id)
                if (name != null) {
                    results[id] = name
                }
            }
            
            results
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    suspend fun canDeleteDepartment(
        projectId: String,
        departmentName: String
    ): Boolean {
        return try {
            val phases = getPhasesForProject(projectId)
            if (phases.isEmpty()) return true // No phases, can delete
            
            // Check if every phase has at least one other department besides the one being deleted
            // A phase must always have at least one department
            phases.all { phase ->
                // If phase contains this department, check if there are other departments
                if (phase.departments.containsKey(departmentName)) {
                    val otherDepartments = phase.departments.keys.filter { it != departmentName }
                    // Must have at least one other department to allow deletion
                    otherDepartments.isNotEmpty()
                } else {
                    // Department not in this phase, so deletion is fine
                    true
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    // Update project approvers and production heads
    suspend fun updateProjectAssignments(
        projectId: String,
        approverIds: List<String>,
        BusinessHeadIds: List<String>
    ): Result<Unit> {
        return try {
            
            val projectRef = firestore.collection("Project1").document(projectId)
            projectRef.update(
                mapOf(
                    "approverIds" to approverIds,
                    "BusinessHeadIds" to BusinessHeadIds,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update department assignments in project document
     * Merges new assignments with existing ones (doesn't overwrite)
     */
    suspend fun updateProjectDepartmentAssignments(
        projectId: String,
        departmentName: String,
        approverPhone: String?,
        userPhone: String?
    ): Result<Unit> {
        return try {
            android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
            android.util.Log.d("DEPT_ADD_FLOW", "📋 Step 4A: Updating Project Department Assignments (projects collection)")
            android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
            
            val customerId = getCurrentUserCustomerId()
            if (customerId == null) {
                android.util.Log.e("DEPT_ADD_FLOW", "❌ ERROR: User not authenticated - cannot get customerId")
                return Result.failure(Exception("User not authenticated"))
            }
            
            android.util.Log.d("DEPT_ADD_FLOW", "   - Customer ID: $customerId")
            android.util.Log.d("DEPT_ADD_FLOW", "   - Project ID: $projectId")
            android.util.Log.d("DEPT_ADD_FLOW", "   - Department name: '$departmentName'")
            android.util.Log.d("DEPT_ADD_FLOW", "   - Approver phone: $approverPhone")
            android.util.Log.d("DEPT_ADD_FLOW", "   - User phone: $userPhone")
            
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
            
            android.util.Log.d("DEPT_ADD_FLOW", "   📍 Firestore path: customers/$customerId/projects/$projectId")
            
            // Get existing assignments
            android.util.Log.d("DEPT_ADD_FLOW", "   🔍 Fetching existing assignments from project document...")
            val snapshot = projectRef.get().await()
            val existingUserAssignments = (snapshot.get("departmentUserAssignments") as? Map<*, *>)?.mapValues { it.value.toString() } ?: emptyMap<String, String>()
            val existingApproverAssignments = (snapshot.get("departmentApproverAssignments") as? Map<*, *>)?.mapValues { it.value.toString() } ?: emptyMap<String, String>()
            
            android.util.Log.d("DEPT_ADD_FLOW", "   📋 Existing user assignments (${existingUserAssignments.size}): ${existingUserAssignments.keys.joinToString()}")
            android.util.Log.d("DEPT_ADD_FLOW", "   📋 Existing approver assignments (${existingApproverAssignments.size}): ${existingApproverAssignments.keys.joinToString()}")
            
            // Merge with new assignments
            val updatedUserAssignments = existingUserAssignments.toMutableMap()
            val updatedApproverAssignments = existingApproverAssignments.toMutableMap()
            
            if (userPhone != null && userPhone.isNotBlank()) {
                updatedUserAssignments[departmentName] = userPhone
                android.util.Log.d("DEPT_ADD_FLOW", "   ➕ Adding user assignment: '$departmentName' → $userPhone")
            } else {
                android.util.Log.d("DEPT_ADD_FLOW", "   ⚠️ Skipping user assignment (null or blank)")
            }
            
            if (approverPhone != null && approverPhone.isNotBlank()) {
                updatedApproverAssignments[departmentName] = approverPhone
                android.util.Log.d("DEPT_ADD_FLOW", "   ➕ Adding approver assignment: '$departmentName' → $approverPhone")
            } else {
                android.util.Log.d("DEPT_ADD_FLOW", "   ⚠️ Skipping approver assignment (null or blank)")
            }
            
            android.util.Log.d("DEPT_ADD_FLOW", "   📋 Updated user assignments (${updatedUserAssignments.size}): ${updatedUserAssignments.keys.joinToString()}")
            android.util.Log.d("DEPT_ADD_FLOW", "   📋 Updated approver assignments (${updatedApproverAssignments.size}): ${updatedApproverAssignments.keys.joinToString()}")
            
            // Update project document
            android.util.Log.d("DEPT_ADD_FLOW", "   💾 Writing to Firestore (projects collection)...")
            projectRef.update(
                mapOf(
                    "departmentUserAssignments" to updatedUserAssignments,
                    "departmentApproverAssignments" to updatedApproverAssignments,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            
            android.util.Log.d("DEPT_ADD_FLOW", "   ✅ SUCCESS: Project department assignments updated")
            android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("DEPT_ADD_FLOW", "❌ ERROR: Failed to update project assignments: ${e.message}", e)
            android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
            Result.failure(e)
        }
    }
    
    // Direct method to get all projects - optimized
    suspend fun getAllProjects(): List<Project> {
        return try {
            // Get current user's customer ID
            val customerId = getCurrentUserCustomerId()
            if (customerId == null) {
                return emptyList()
            }
            
            val snapshot = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .get()
                .await()
            
            val projects = snapshot.documents.mapNotNull { doc ->
                parseProjectFromDocument(doc)
            }
            
            projects
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Get all projects as a Flow for production heads - optimized to only fetch current customer's projects
    fun getAllProjectsFlow(): Flow<List<Project>> = callbackFlow {

        // Get current user's customer ID using coroutine
        var customerId: String? = null
        try {
            customerId = getCurrentUserCustomerId()
        } catch (e: Exception) {
        }
        
        if (customerId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        
        // Listen to only the current customer's projects for better performance
        val listener = firestore.collection("customers")
            .document(customerId)
            .collection("projects")
            .addSnapshotListener { projectsSnapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                val projects = projectsSnapshot?.documents?.mapNotNull { doc ->
                    parseProjectFromDocument(doc)
                } ?: emptyList()
                
                trySend(projects)
            }

        awaitClose {
            listener.remove()
        }
    }
    
    /**
     * Get projects based on user role following the guide's role-based fetching pattern
     * - BUSINESSHEAD: All projects (no filter)
     * - APPROVER: Projects where user is manager OR temp approver
     * - USER: Projects where user is a team member
     */
    fun getProjectsByRole(userId: String, role: com.cubiquitous.tracura.model.UserRole, customerId: String?): Flow<List<Project>> = callbackFlow {
        val scope = CoroutineScope(Dispatchers.IO)
        val worker = scope.launch {
            try {
                val finalCustomerId = customerId ?: getCurrentUserCustomerId()
                if (finalCustomerId.isNullOrBlank()) {
                    trySend(emptyList())
                    close()
                    return@launch
                }

                val cleanPhone = normalizePhoneNumber(userId)
                val isBusinessHead = userId == "admin@avr.com" ||
                    role == com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD ||
                    role == com.cubiquitous.tracura.model.UserRole.ADMIN
                val isManager = role == com.cubiquitous.tracura.model.UserRole.MANAGER

                val projects = when {
                    isBusinessHead -> fetchAllProjectsForCustomer(finalCustomerId)
                    isManager -> fetchManagerProjects(cleanPhone, finalCustomerId)
                    else -> {
                        val relationProjectIds = fetchAccessibleProjectIdsFromRelation(cleanPhone, finalCustomerId)
                        val tempProjectIds = if (role == com.cubiquitous.tracura.model.UserRole.APPROVER) {
                            fetchTemporaryApproverProjectIds(cleanPhone, finalCustomerId)
                        } else {
                            emptySet()
                        }
                        val mergedIds = (relationProjectIds + tempProjectIds).toSet()
                        if (mergedIds.isEmpty()) emptyList() else fetchProjectsByIds(finalCustomerId, mergedIds)
                    }
                }

                val sortedProjects = projects.sortedWith(
                    compareBy<Project> { getProjectStatusPriority(it) }
                        .thenByDescending { it.createdAt }
                )

                trySend(sortedProjects)
            } catch (_: Exception) {
                trySend(emptyList())
            } finally {
                close()
            }
        }

        awaitClose {
            worker.cancel()
        }
    }

    /**
     * Real-time version of getProjectsByRole using addSnapshotListener.
     * The returned Flow stays open and emits a new list whenever any matched
     * project document changes in Firestore — no polling required.
     */
    fun getProjectsByRoleRealtime(
        userId: String,
        role: com.cubiquitous.tracura.model.UserRole,
        customerId: String?
    ): Flow<List<Project>> = callbackFlow {
        val scope = CoroutineScope(Dispatchers.IO)
        var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null

        val job = scope.launch {
            try {
                val finalCustomerId = customerId ?: getCurrentUserCustomerId()
                if (finalCustomerId.isNullOrBlank()) {
                    trySend(emptyList())
                    close()
                    return@launch
                }

                val cleanPhone = normalizePhoneNumber(userId)
                val projectsRef = firestore
                    .collection("customers")
                    .document(finalCustomerId)
                    .collection("projects")

                firestoreListener = when (role) {
                    com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD,
                    com.cubiquitous.tracura.model.UserRole.ADMIN -> {
                        projectsRef.addSnapshotListener { snapshot, error ->
                            if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                            val projects = snapshot?.documents?.mapNotNull { parseProjectFromDocument(it) } ?: emptyList()
                            trySend(projects.sortedWith(compareBy { getProjectStatusPriority(it) }))
                        }
                    }
                    com.cubiquitous.tracura.model.UserRole.MANAGER -> {
                        projectsRef.whereEqualTo("managerId", cleanPhone)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                                val projects = snapshot?.documents?.mapNotNull { parseProjectFromDocument(it) } ?: emptyList()
                                trySend(projects.sortedWith(compareBy { getProjectStatusPriority(it) }))
                            }
                    }
                    else -> {
                        val relationIds = fetchAccessibleProjectIdsFromRelation(cleanPhone, finalCustomerId)
                        val tempIds = if (role == com.cubiquitous.tracura.model.UserRole.APPROVER) {
                            fetchTemporaryApproverProjectIds(cleanPhone, finalCustomerId)
                        } else emptySet()
                        val mergedIds = (relationIds + tempIds).toList()

                        if (mergedIds.isEmpty()) {
                            trySend(emptyList())
                            close()
                            return@launch
                        }

                        // Firestore whereIn supports up to 30 IDs per query
                        val chunk = mergedIds.take(30)
                        projectsRef.whereIn(FieldPath.documentId(), chunk)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                                val projects = snapshot?.documents?.mapNotNull { parseProjectFromDocument(it) } ?: emptyList()
                                trySend(projects.sortedWith(compareBy { getProjectStatusPriority(it) }))
                            }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProjectRepository", "❌ Realtime listener setup failed: ${e.message}", e)
                trySend(emptyList())
            }
        }

        awaitClose {
            firestoreListener?.remove()
            job.cancel()
        }
    }

    private fun normalizePhoneNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits.trim()
    }

    private suspend fun fetchManagerProjects(cleanPhone: String, customerId: String): List<Project> {
        val snapshot = firestore.collection("customers")
            .document(customerId)
            .collection("projects")
            .whereEqualTo("managerId", cleanPhone)
            .get()
            .await()
        return snapshot.documents.mapNotNull { parseProjectFromDocument(it) }
    }

    private suspend fun fetchAllProjectsForCustomer(customerId: String): List<Project> {
        val snapshot = firestore.collection("customers")
            .document(customerId)
            .collection("projects")
            .get()
            .await()

        return snapshot.documents.mapNotNull { parseProjectFromDocument(it) }
    }

    private suspend fun fetchAccessibleProjectIdsFromRelation(cleanPhone: String, customerId: String): Set<String> {
        val relationDoc = firestore.collection("User_Customer_Project_Phase_Relation")
            .document(cleanPhone)
            .get()
            .await()

        val relationData = relationDoc.data ?: return emptySet()
        val customerMapAny = relationData[customerId] as? Map<*, *> ?: return emptySet()

        return customerMapAny.keys
            .mapNotNull { it as? String }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private suspend fun fetchTemporaryApproverProjectIds(cleanPhone: String, customerId: String): Set<String> {
        val snapshot = firestore.collection("CustomerTemporaryApproverRelation")
            .whereEqualTo("customerId", customerId)
            .whereEqualTo("userId", cleanPhone)
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { doc ->
                val projectId = (doc.getString("projectid") ?: doc.getString("projectId")).orEmpty().trim()
                projectId.ifBlank { null }
            }
            .toSet()
    }

    private suspend fun fetchProjectsByIds(customerId: String, projectIds: Set<String>): List<Project> {
        if (projectIds.isEmpty()) return emptyList()

        val ids = projectIds.toList()
        val chunks = ids.chunked(30)
        val projectsRef = firestore.collection("customers")
            .document(customerId)
            .collection("projects")

        return chunks.flatMap { chunk ->
            val snapshot = projectsRef
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            snapshot.documents.mapNotNull { parseProjectFromDocument(it) }
        }
    }

    suspend fun getAllowedPhaseIdsForCurrentUserProject(
        projectId: String,
        customerId: String? = null,
    ): Set<String> {
        val currentUserIdentifier = getCurrentUserIdentifier() ?: return emptySet()
        val cleanPhone = normalizePhoneNumber(currentUserIdentifier)
        if (cleanPhone.isBlank()) return emptySet()

        val resolvedCustomerId = customerId ?: getCurrentUserCustomerId() ?: return emptySet()
        val cacheKey = "$cleanPhone|$resolvedCustomerId|$projectId"
        getCachedValue(allowedPhaseIdsCache, cacheKey, allowedPhaseIdsCacheTtlMs)?.let { return it }

        val relationDoc = firestore.collection("User_Customer_Project_Phase_Relation")
            .document(cleanPhone)
            .get()
            .await()

        val customerMap = relationDoc.get(resolvedCustomerId) as? Map<*, *> ?: return emptySet()
        val phasesAny = customerMap[projectId] as? List<*> ?: return emptySet()

        val resolved = phasesAny
            .mapNotNull { it as? String }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        putCachedValue(allowedPhaseIdsCache, cacheKey, resolved)
        return resolved
    }
    
    /**
     * Get status priority for sorting projects
     * Order: active, maintenance, standby, suspended, completed, locked, review rejected, declined, archive
     * Returns lower number for higher priority
     */
    private fun getProjectStatusPriority(project: Project): Int {
        // Normalize status to uppercase for comparison
        val normalizedStatus = project.status.uppercase()
        
        // Check actual status from project (handles computed statuses like MAINTENANCE, SUSPENDED, etc.)
        val actualStatus = when {
            // Check suspension status first
            project.isSuspended == true && !project.suspendedDate.isNullOrBlank() -> {
                try {
                    val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    dateFormatter.isLenient = false
                    val suspendedDate = dateFormatter.parse(project.suspendedDate)
                    if (suspendedDate != null) {
                        val today = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        val suspendedDateAtMidnight = java.util.Calendar.getInstance().apply {
                            time = suspendedDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        if (today.time <= suspendedDateAtMidnight.time) "SUSPENDED" else null
                    } else null
                } catch (e: Exception) { null }
            }
            // Check standby (suspended but no date/reason)
            project.isSuspended == true && project.suspendedDate.isNullOrBlank() && project.suspensionReason.isNullOrBlank() -> "STANDBY"
            // Check maintenance status
            !project.maintenanceDate.isNullOrBlank() && !project.handoverDate.isNullOrBlank() -> {
                try {
                    val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    dateFormatter.isLenient = false
                    val handoverDate = dateFormatter.parse(project.handoverDate)
                    val maintenanceDate = dateFormatter.parse(project.maintenanceDate)
                    if (handoverDate != null && maintenanceDate != null) {
                        val today = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        val handoverDateAtMidnight = java.util.Calendar.getInstance().apply {
                            time = handoverDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        val maintenanceDateAtMidnight = java.util.Calendar.getInstance().apply {
                            time = maintenanceDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.time
                        val isHandoverDatePassed = today.time >= handoverDateAtMidnight.time
                        val isMaintenanceDatePassed = today.time > maintenanceDateAtMidnight.time
                        val daysSinceMaintenance = if (isMaintenanceDatePassed) {
                            val diffInMillis = today.time - maintenanceDateAtMidnight.time
                            diffInMillis / (1000 * 60 * 60 * 24)
                        } else 0L
                        
                        when {
                            isHandoverDatePassed && !isMaintenanceDatePassed -> "MAINTENANCE"
                            isMaintenanceDatePassed && daysSinceMaintenance >= 30 -> "ARCHIVE"
                            isMaintenanceDatePassed -> "COMPLETED"
                            else -> null
                        }
                    } else null
                } catch (e: Exception) { null }
            }
            else -> null
        }
        
        // Use actual status if computed, otherwise use project status (normalized to uppercase)
        val statusToCheck = (actualStatus ?: normalizedStatus).uppercase()
        
        // Handle status variations (case-insensitive matching)
        val normalizedStatusToCheck = when {
            statusToCheck.contains("REVIEW REJECTED", ignoreCase = true) || 
            statusToCheck.contains("REVIEW_REJECTED", ignoreCase = true) -> "REVIEW REJECTED"
            statusToCheck.contains("IN REVIEW", ignoreCase = true) || 
            statusToCheck.contains("IN_REVIEW", ignoreCase = true) -> "IN REVIEW"
            statusToCheck.contains("MAINTENANCE", ignoreCase = true) || 
            statusToCheck.contains("MAINTENENCE", ignoreCase = true) -> "MAINTENANCE"
            statusToCheck.contains("STANDBY", ignoreCase = true) || 
            statusToCheck.contains("STANBY", ignoreCase = true) -> "STANDBY"
            statusToCheck.contains("SUSPENDED", ignoreCase = true) -> "SUSPENDED"
            statusToCheck.contains("COMPLETED", ignoreCase = true) -> "COMPLETED"
            statusToCheck.contains("LOCKED", ignoreCase = true) -> "LOCKED"
            statusToCheck.contains("DECLINED", ignoreCase = true) -> "DECLINED"
            statusToCheck.contains("ARCHIVE", ignoreCase = true) -> "ARCHIVE"
            statusToCheck.contains("ACTIVE", ignoreCase = true) -> "ACTIVE"
            else -> statusToCheck
        }
        
        // Priority order: active (0), maintenance (1), standby (2), suspended (3), completed (4), locked (5), review rejected (6), declined (7), archive (8)
        return when (normalizedStatusToCheck) {
            "ACTIVE" -> 0
            "MAINTENANCE" -> 1
            "STANDBY" -> 2
            "SUSPENDED" -> 3
            "COMPLETED" -> 4
            "LOCKED" -> 5
            "REVIEW REJECTED", "IN REVIEW" -> 6
            "DECLINED" -> 7
            "ARCHIVE" -> 8
            else -> 99 // Unknown status goes to end
        }
    }
    
    /**
     * Check if a phase is currently active based on:
     * 1. Phase is enabled
     * 2. Phase start date <= current date (or no start date)
     * 3. Phase end date >= current date (or no end date)
     */
    private fun isPhaseCurrentlyActive(phase: com.cubiquitous.tracura.model.Phase, currentDate: java.util.Date): Boolean {
        // Phase must be enabled
        if (!phase.isEnabledValue) {
            return false
        }
        
        val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        
        // Check start date - phase must have started (or no start date means always active)
        if (!phase.startDate.isNullOrBlank()) {
            try {
                val phaseStartDate = dateFormatter.parse(phase.startDate)
                phaseStartDate?.let {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = it
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val phaseStart = cal.time
                    if (currentDate.before(phaseStart)) {
                        return false // Phase hasn't started yet
                    }
                }
            } catch (e: Exception) {
                // Invalid date format, skip date check
            }
        }
        
        // Check end date - phase must not have ended (or no end date means always active)
        if (!phase.endDate.isNullOrBlank()) {
            try {
                val phaseEndDate = dateFormatter.parse(phase.endDate)
                phaseEndDate?.let {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = it
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val phaseEnd = cal.time
                    if (currentDate.after(phaseEnd)) {
                        return false // Phase has ended
                    }
                }
            } catch (e: Exception) {
                // Invalid date format, skip date check
            }
        }
        
        return true
    }
    
    /**
     * Check if a project should be suspended based on:
     * 1. Planned start date has passed or is today (current date >= planned start date)
     * 2. No active phases exist (enabled and within their date range)
     */
    private suspend fun shouldSuspendProject(project: Project): Boolean {
        return try {
            // Check if planned start date exists and has passed or is today
            val startDate = project.startDate
            if (startDate.isNullOrBlank()) {
                return false // No start date, can't determine if it should be suspended
            }
            
            val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val plannedStartDate = dateFormatter.parse(startDate)
            
            if (plannedStartDate == null) {
                return false
            }
            
            // Get current date (without time component for comparison)
            val currentDate = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            
            val plannedDate = java.util.Calendar.getInstance().apply {
                time = plannedStartDate
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            
            // Check if planned start date has passed or is today (current date >= planned start date)
            val hasPlannedStartDatePassed = currentDate >= plannedDate
            
            if (!hasPlannedStartDatePassed) {
                return false // Planned start date hasn't arrived yet
            }
            
            // Check if there are any currently active phases (enabled and within date range)
            val projectId = project.id
            if (projectId.isNullOrBlank()) {
                return false
            }
            
            val phases = getPhasesForProject(projectId)
            val hasActivePhases = phases.any { phase -> isPhaseCurrentlyActive(phase, currentDate) }
            
            // Should suspend if planned start date has passed/today AND there are no active phases
            val shouldSuspend = !hasActivePhases
            
            if (shouldSuspend) {
                val activePhaseCount = phases.count { phase -> isPhaseCurrentlyActive(phase, currentDate) }
            }
            
            shouldSuspend
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if a project should be locked based on planned start date
     * Project should be locked if planned start date > current date
     */
    private fun shouldLockProject(project: Project): Boolean {
        return try {
            val startDate = project.startDate
            if (startDate.isNullOrBlank()) {
                return false // No start date, can't determine if it should be locked
            }
            
            val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val plannedStartDate = dateFormatter.parse(startDate)
            
            if (plannedStartDate == null) {
                return false
            }
            
            // Get current date (without time component for comparison)
            val currentDate = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            
            val plannedDate = java.util.Calendar.getInstance().apply {
                time = plannedStartDate
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            
            // Project should be locked if planned start date > current date
            val shouldLock = plannedDate > currentDate
            
            if (shouldLock) {
            }
            
            shouldLock
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check and update project status to LOCKED or SUSPENDED if conditions are met
     * LOCKED takes priority over SUSPENDED
     */
    private suspend fun checkAndSuspendProjectIfNeeded(project: Project): Project {
        return try {
            // Only check projects that are not already COMPLETED, HANDOVER, IN REVIEW, or REVIEW REJECTED
            // IN REVIEW and REVIEW REJECTED should only be changed by approver actions
            // LOCKED and SUSPENDED can be changed dynamically
            if (project.status == "COMPLETED" ||
                project.status == "HANDOVER" ||
                project.status == "IN REVIEW" ||
                project.status == "IN_REVIEW" ||
                project.status == "REVIEW REJECTED") {
                return project
            }
            
            // First check if project should be locked (planned date > current date)
            if (shouldLockProject(project)) {
                val projectId = project.id
                if (!projectId.isNullOrBlank()) {
                    // Only update if not already locked
                    if (project.status != "LOCKED") {
                        val updateResult = updateProjectStatus(projectId, "LOCKED")
                        if (updateResult.isSuccess) {
                            return project.copy(status = "LOCKED")
                        } else {
                        }
                    } else {
                        // Already locked, return as is
                        return project
                    }
                }
            } else {
                // Validation removed: do not auto-change LOCKED -> ACTIVE.
            }
            
            // If not locked, check if it should be suspended
            // This handles cases where project was ACTIVE but phases ended or were disabled
            if (shouldSuspendProject(project)) {
                val projectId = project.id
                if (!projectId.isNullOrBlank()) {
                    // Only update if not already suspended
                    if (project.status != "SUSPENDED") {
                        val updateResult = updateProjectStatus(projectId, "SUSPENDED")
                        if (updateResult.isSuccess) {
                            return project.copy(status = "SUSPENDED")
                        } else {
                        }
                    }
                }
            } else {
                // Validation removed: do not auto-change SUSPENDED -> ACTIVE.
            }
            
            project
        } catch (e: Exception) {
            project
        }
    }
    
    suspend fun updateProjectStatus(projectId: String, newStatus: String, rejectionReason: String? = null, rejectedBy: String? = null): Result<Unit> {
        return try {
            // Find which customer this project belongs to
            val customerId = findCustomerIdForProject(projectId)
            if (customerId == null) {
                return Result.failure(Exception("Project not found"))
            }
            
            // Get current project data to check dates
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
            
            val projectDoc = projectRef.get().await()
            if (!projectDoc.exists()) {
                return Result.failure(Exception("Project not found"))
            }
            
            val updateData = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            // Date formatter for parsing and formatting dates
            val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            val todayString = dateFormatter.format(today)
            
            // Handle date updates based on status change
            when (newStatus.uppercase()) {
                "ACTIVE" -> {
                    // If plannedDate (startDate) > current date, set plannedDate = current date
                    val startDate = projectDoc.getString("startDate") ?: projectDoc.getString("plannedDate")
                    if (!startDate.isNullOrBlank()) {
                        try {
                            val startDateParsed = dateFormatter.parse(startDate)
                            if (startDateParsed != null && startDateParsed.after(today)) {
                                updateData["startDate"] = todayString
                                updateData["plannedDate"] = todayString // Also update plannedDate for backward compatibility
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                "MAINTENANCE" -> {
                    // If handoverDate > current date, set handoverDate = current date
                    val handoverDate = projectDoc.getString("handoverDate")
                    if (!handoverDate.isNullOrBlank()) {
                        try {
                            val handoverDateParsed = dateFormatter.parse(handoverDate)
                            if (handoverDateParsed != null && handoverDateParsed.after(today)) {
                                updateData["handoverDate"] = todayString
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                "COMPLETED" -> {
                    // If handoverDate > current date, set handoverDate = current date
                    val handoverDate = projectDoc.getString("handoverDate")
                    if (!handoverDate.isNullOrBlank()) {
                        try {
                            val handoverDateParsed = dateFormatter.parse(handoverDate)
                            if (handoverDateParsed != null && handoverDateParsed.after(today)) {
                                updateData["handoverDate"] = todayString
                            }
                        } catch (e: Exception) {
                        }
                    }
                    
                    // If maintenanceDate > current date, set maintenanceDate = current date
                    val maintenanceDate = projectDoc.getString("maintenanceDate")
                    if (!maintenanceDate.isNullOrBlank()) {
                        try {
                            val maintenanceDateParsed = dateFormatter.parse(maintenanceDate)
                            if (maintenanceDateParsed != null && maintenanceDateParsed.after(today)) {
                                updateData["maintenanceDate"] = todayString
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
            
            // Add rejection reason if provided
            if (rejectionReason != null && rejectionReason.isNotBlank()) {
                updateData["rejectionReason"] = rejectionReason
            }
            
            // Add rejectedBy if provided (when status is REVIEW REJECTED)
            if (newStatus == "REVIEW REJECTED" && rejectedBy != null && rejectedBy.isNotBlank()) {
                updateData["rejectedBy"] = rejectedBy
            }
            
            projectRef.update(updateData).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove a user from all projects where they are team members
     * This is called when a user is disabled
     */
    suspend fun removeUserFromAllProjects(userId: String): Result<Int> {
        return try {
            
            // Get all projects where this user is a team member - ONLY from Project1 collection
            val snapshot = firestore.collection("Project1")
                .whereArrayContains("teamMembers", userId)
                .get()
                .await()
            
            var removedCount = 0
            
            // Remove user from each project
            snapshot.documents.forEach { doc ->
                val projectData = doc.data ?: emptyMap()
                val currentTeamMembers = (projectData["teamMembers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                
                // Remove the user from the list
                val updatedTeamMembers = currentTeamMembers.filter { it != userId }
                
                if (updatedTeamMembers.size < currentTeamMembers.size) {
                    // Update the project with the new team members list
                    firestore.collection("Project1")
                        .document(doc.id)
                        .update(
                            mapOf(
                                "teamMembers" to updatedTeamMembers,
                                "updatedAt" to com.google.firebase.Timestamp.now()
                            )
                        )
                        .await()
                    
                    removedCount++
                }
            }
            
            Result.success(removedCount)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if delegation is still active for a user in a project
     * This method can be called from UI layer to filter expired delegations
     */
    suspend fun isDelegationActiveForUser(projectId: String, userId: String): Boolean {
        return try {
            
            // Get temporary approvers for this project
            val tempApprovers = temporaryApproverRepository.getTemporaryApproversForProject(projectId)
            
            // Find active delegation for this user
            val userDelegation = tempApprovers.find { approver ->
                approver.approverId == userId && approver.isActive && approver.status == "ACCEPTED"
            }
            
            if (userDelegation == null) {
                return false
            }
            
            // Check if delegation has expired
            val isExpired = userDelegation.isExpired()
            val isActive = !isExpired
            
            
            isActive
            
        } catch (e: Exception) {
            false // Return false on error to be safe
        }
    }
    
    /**
     * Get customer's businessType from Firebase
     */
    suspend fun getCustomerBusinessType(): String? {
        return try {
            val customerId = getCurrentUserCustomerId()
            if (customerId == null) {
                return null
            }
            
            val customer = getCustomerById(customerId)
            customer?.businessType
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Fetch templates from Firebase for a specific business type
     * Templates are stored in firestore collection "templates" with businessType field
     */
    fun getTemplatesFromFirebase(businessType: String): Flow<List<com.cubiquitous.tracura.model.ProjectTemplate>> = callbackFlow {
        
        try {
            val listener = firestore.collection("templates")
                .whereEqualTo("businessType", businessType)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val templates = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            
                            // Parse template from Firebase document
                            val id = doc.id
                            val name = data["name"] as? String ?: ""
                            val description = data["description"] as? String ?: ""
                            val templateBusinessType = data["businessType"] as? String
                            
                            // Parse phases (simplified - you'll need to implement full parsing)
                            val phasesData = (data["phases"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                            val phases = phasesData.mapNotNull { phaseData ->
                                try {
                                    val phaseName = phaseData["phaseName"] as? String ?: ""
                                    val departmentsData = (phaseData["departments"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                                    val departments = departmentsData.mapNotNull { deptData ->
                                        try {
                                            com.cubiquitous.tracura.model.DepartmentDraft(
                                                departmentName = deptData["departmentName"] as? String ?: "",
                                                contractorMode = when (deptData["contractorMode"] as? String) {
                                                    "LABOUR_ONLY" -> com.cubiquitous.tracura.model.ContractorMode.LABOUR_ONLY
                                                    "SELF_EXECUTION" -> com.cubiquitous.tracura.model.ContractorMode.SELF_EXECUTION
                                                    "TURNKEY", "TURNKEY_CONTRACTOR_ONLY" -> com.cubiquitous.tracura.model.ContractorMode.TURNKEY_AMOUNT_ONLY
                                                    else -> com.cubiquitous.tracura.model.ContractorMode.SELF_EXECUTION
                                                },
                                                lineItems = mutableListOf()
                                            )
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }.toMutableList()
                                    
                                    com.cubiquitous.tracura.model.PhaseDraft(
                                        phaseName = phaseName,
                                        departments = departments
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            // Get icon based on template name or use default
                            val icon = when {
                                name.contains("Video", ignoreCase = true) -> androidx.compose.material.icons.Icons.Default.VideoLibrary
                                name.contains("Content", ignoreCase = true) -> androidx.compose.material.icons.Icons.Default.Create
                                name.contains("Residential", ignoreCase = true) -> androidx.compose.material.icons.Icons.Default.Home
                                name.contains("Commercial", ignoreCase = true) -> androidx.compose.material.icons.Icons.Default.Apartment
                                else -> androidx.compose.material.icons.Icons.Default.Build
                            }
                            
                            com.cubiquitous.tracura.model.ProjectTemplate(
                                id = id,
                                name = name,
                                description = description,
                                icon = icon,
                                phases = phases,
                                businessType = templateBusinessType
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(templates)
                }
            
            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            trySend(emptyList())
            awaitClose { }
        }
    }
    
    /**
     * Migration function to rename suspensionDate to suspendedDate in all project documents
     * This should be called once to migrate existing Firestore data
     */
    suspend fun migrateSuspensionDateField(): Result<Int> {
        return try {
            val customerId = getCurrentUserCustomerId()
            if (customerId == null) {
                return Result.failure(Exception("No customer ID found"))
            }
            
            
            val projectsRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
            
            val snapshot = projectsRef.get().await()
            
            var migratedCount = 0
            var batch = firestore.batch()
            var batchCount = 0
            val batchLimit = 500 // Firestore batch limit
            
            snapshot.documents.forEach { doc ->
                val data = doc.data
                val oldFieldValue = data?.get("suspensionDate")
                
                if (oldFieldValue != null && data["suspendedDate"] == null) {
                    // Only migrate if suspendedDate doesn't already exist
                    val projectRef = projectsRef.document(doc.id)
                    batch.update(projectRef, "suspendedDate", oldFieldValue)
                    batch.update(projectRef, "suspensionDate", com.google.firebase.firestore.FieldValue.delete())
                    batchCount++
                    migratedCount++
                    
                    
                    // Commit batch if we reach the limit
                    if (batchCount >= batchLimit) {
                        batch.commit().await()
                        batch = firestore.batch() // Create new batch for next set
                        batchCount = 0
                    }
                }
            }
            
            // Commit remaining updates
            if (batchCount > 0) {
                batch.commit().await()
            }
            
            Result.success(migratedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Migration function to rename enabled to isEnabled in all phase documents
     * This should be called once to migrate existing Firestore data
     */
    suspend fun migratePhaseEnabledField(): Result<Int> {
        return try {
            val customerId = getCurrentUserCustomerId()
            if (customerId == null) {
                return Result.failure(Exception("No customer ID found"))
            }
            
            
            val projectsRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
            
            val projectsSnapshot = projectsRef.get().await()
            
            var migratedCount = 0
            var batch = firestore.batch()
            var batchCount = 0
            val batchLimit = 500 // Firestore batch limit
            
            projectsSnapshot.documents.forEach { projectDoc ->
                val phasesRef = projectDoc.reference.collection("phases")
                val phasesSnapshot = phasesRef.get().await()
                
                phasesSnapshot.documents.forEach { phaseDoc ->
                    val data = phaseDoc.data
                    val oldFieldValue = data?.get("enabled")
                    
                    if (oldFieldValue != null && data["isEnabled"] == null) {
                        // Only migrate if isEnabled doesn't already exist
                        val phaseRef = phasesRef.document(phaseDoc.id)
                        batch.update(phaseRef, "isEnabled", oldFieldValue)
                        batch.update(phaseRef, "enabled", com.google.firebase.firestore.FieldValue.delete())
                        batchCount++
                        migratedCount++
                        
                        
                        // Commit batch if we reach the limit
                        if (batchCount >= batchLimit) {
                            batch.commit().await()
                            batch = firestore.batch() // Create new batch for next set
                            batchCount = 0
                        }
                    }
                }
            }
            
            // Commit remaining updates
            if (batchCount > 0) {
                batch.commit().await()
            }
            
            Result.success(migratedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    /**
     * Get all departments for a project across all phases using Collection Group Query.
     * This is useful for filtering phases based on department assignments even if phase.departments map is empty.
     */
    fun getAllDepartmentsForProject(projectId: String): Flow<List<Department>> = callbackFlow {
        try {
            android.util.Log.d("ProjectRepository", "🔄 Fetching all departments for project: $projectId using CollectionGroup")
            
            // Query the "departments" collection group where projectId matches
            // Requires a composite index on projectId ASC/DESC
            val listener = firestore.collectionGroup("departments")
                .whereEqualTo("projectId", projectId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("ProjectRepository", "❌ Error fetching project departments: ${error.message}")
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    val departments = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Department::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("ProjectRepository", "❌ Error parsing department ${doc.id}: ${e.message}")
                            null
                        }
                    } ?: emptyList()
                    
                    android.util.Log.d("ProjectRepository", "✅ Loaded ${departments.size} departments for project $projectId via CollectionGroup")
                    trySend(departments)
                }
                
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "❌ Error setting up department listener: ${e.message}")
            trySend(emptyList())
            close(e)
        }
    }

    /**
     * Deduct budget from a department's remaining amount.
     * Uses a transaction to ensure atomic updates.
     */
    suspend fun deductDepartmentBudget(
        projectId: String,
        phaseId: String?,
        departmentName: String,
        amount: Double
    ): Result<Unit> {
        return try {
            val customerId = getCurrentUserCustomerId()
                ?: return Result.failure(Exception("No customer ID found"))

            // Find the department document reference
            val departmentRef = if (phaseId != null) {
                // Direct access if phaseId is known
                firestore.collection("customers")
                    .document(customerId)
                    .collection("projects")
                    .document(projectId)
                    .collection("phases")
                    .document(phaseId)
                    .collection("departments")
                    .whereEqualTo("name", departmentName)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.reference
            } else {
                // Collection group query if phaseId is unknown (less efficient but necessary fallback)
                firestore.collectionGroup("departments")
                    .whereEqualTo("projectId", projectId)
                    .whereEqualTo("name", departmentName)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.reference
            }

            if (departmentRef == null) {
                return Result.failure(Exception("Department not found: $departmentName"))
            }

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(departmentRef)
                
                // Get current remaining amount, defaulting to totalBudget if not set
                val totalBudget = snapshot.getDouble("totalBudget") ?: 0.0
                val currentRemaining = snapshot.getDouble("remainingAmount") ?: totalBudget
                
                // Calculate new remaining amount
                val newRemaining = currentRemaining - amount
                
                transaction.update(departmentRef, "remainingAmount", newRemaining)
                transaction.update(departmentRef, "updatedAt", com.google.firebase.Timestamp.now())
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "❌ Error deducting budget: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Replace a department's user or approver assignment across ALL projects for the current customer.
     *
     * Updates three fields per project:
     * 1. departmentUserAssignments   — Map<departmentName, phone>  (if !isApprover)
     * 2. departmentApproverAssignments — Map<departmentName, phone> (if isApprover)
     * 3. DepartmentTemporaryApprover — List<Map> with keys: departmentName, phone, startDate, endDate, isAccepted, isActive
     *
     * For maps (1 & 2): if map[departmentName] == oldPhone → set to newPhone
     * For list (3):      if entry.departmentName matches AND entry.phone == oldPhone → update phone to newPhone
     *
     * @return number of projects updated
     */
    suspend fun replaceDepartmentAssignmentInAllProjects(
        departmentName: String,
        oldPhone: String,
        newPhone: String,
        isApprover: Boolean
    ): Result<Int> {
        return try {
            val customerId = getCurrentUserCustomerId()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val fieldName = if (isApprover) "departmentApproverAssignments" else "departmentUserAssignments"
            val cleanOldPhone = oldPhone.replace("+91", "").trim()
            val cleanNewPhone = newPhone.replace("+91", "").trim()
            
            android.util.Log.d("ProjectRepository", "🔄 Replacing $fieldName.$departmentName: $cleanOldPhone → $cleanNewPhone (including DepartmentTemporaryApprover)")
            
            // Fetch all projects for this customer
            val snapshot = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .get()
                .await()
            
            val batch = firestore.batch()
            var updateCount = 0
            
            for (doc in snapshot.documents) {
                val updates = mutableMapOf<String, Any>()
                var needsUpdate = false
                
                // ── 1. Update departmentUserAssignments / departmentApproverAssignments ──
                val assignments = (doc.get(fieldName) as? Map<*, *>)
                    ?.mapValues { it.value.toString() }
                
                if (assignments != null) {
                    val currentAssigned = assignments[departmentName]?.replace("+91", "")?.trim()
                    if (currentAssigned == cleanOldPhone) {
                        val updatedAssignments = assignments.toMutableMap()
                        updatedAssignments[departmentName] = cleanNewPhone
                        updates[fieldName] = updatedAssignments
                        needsUpdate = true
                    }
                }
                
                // ── 2. Update DepartmentTemporaryApprover list ──
                @Suppress("UNCHECKED_CAST")
                val tempApproverList = doc.get("DepartmentTemporaryApprover") as? List<Map<String, Any>>
                if (tempApproverList != null) {
                    var listChanged = false
                    val updatedList = tempApproverList.map { entry ->
                        val entryDept = entry["departmentName"]?.toString() ?: ""
                        val entryPhone = entry["phone"]?.toString()?.replace("+91", "")?.trim() ?: ""
                        
                        if (entryDept == departmentName && entryPhone == cleanOldPhone) {
                            listChanged = true
                            // Create a new map with the phone replaced
                            val updated = entry.toMutableMap()
                            updated["phone"] = cleanNewPhone
                            updated
                        } else {
                            entry
                        }
                    }
                    
                    if (listChanged) {
                        updates["DepartmentTemporaryApprover"] = updatedList
                        needsUpdate = true
                    }
                }
                
                // ── 3. Commit if anything changed ──
                if (needsUpdate) {
                    updates["updatedAt"] = com.google.firebase.Timestamp.now()
                    
                    val projectRef = firestore.collection("customers")
                        .document(customerId)
                        .collection("projects")
                        .document(doc.id)
                    
                    batch.update(projectRef, updates)
                    updateCount++
                }
            }
            
            if (updateCount > 0) {
                batch.commit().await()
                android.util.Log.d("ProjectRepository", "✅ Updated $updateCount project(s) for $fieldName.$departmentName (including DepartmentTemporaryApprover)")
            } else {
                android.util.Log.d("ProjectRepository", "ℹ️ No projects needed updating for $fieldName.$departmentName")
            }
            
            Result.success(updateCount)
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "❌ Error replacing department assignment in projects: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Updates User_Customer_Project_Phase_Relation collection for approvers and users
     * based on department assignments in the newly created project.
     * 
     * Structure: Document ID = phone number
     *   └─ {customerId: {projectId: [phaseId1, phaseId2, ...]}}
     * 
     * @param customerId The customer ID who owns this project
     * @param projectId The newly created project ID
     * @param phaseDrafts List of phases with their departments
     * @param departmentUserAssignments Map of department name to assigned user phone
     * @param departmentApproverAssignments Map of department name to assigned approver phone
     */
    suspend fun updateUserCustomerProjectPhaseRelation(
        customerId: String,
        projectId: String,
        phaseDrafts: List<com.cubiquitous.tracura.model.PhaseDraft>,
        departmentUserAssignments: Map<String, String>,
        departmentApproverAssignments: Map<String, String>
    ): Result<Unit> {
        return try {
            android.util.Log.d("ProjectRepository", "🔄 Updating User_Customer_Project_Phase_Relation for project: $projectId")
            
            // First, fetch the created phases to get their IDs
            val phasesSnapshot = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
                .collection("phases")
                .get()
                .await()
            
            // Create a map of phase names to phase IDs
            val phaseNameToIdMap = phasesSnapshot.documents.associate { phaseDoc ->
                val phaseName = phaseDoc.getString("phaseName") ?: ""
                phaseName to phaseDoc.id
            }
            
            android.util.Log.d("ProjectRepository", "📋 Found ${phaseNameToIdMap.size} phases in project")
            
            // Build a map of phone numbers to list of phase IDs they should have access to
            val phoneToPhaseIds = mutableMapOf<String, MutableSet<String>>()
            
            // Iterate through each phase draft
            phaseDrafts.forEach { phaseDraft ->
                val phaseId = phaseNameToIdMap[phaseDraft.phaseName]
                if (phaseId != null) {
                    // For each department in this phase, add the phase ID to relevant users/approvers
                    phaseDraft.departments.forEach { dept ->
                        val deptName = dept.departmentName
                        
                        // Add phase ID for assigned user
                        departmentUserAssignments[deptName]?.let { userPhone ->
                            val normalizedPhone = normalizePhoneNumber(userPhone)
                            if (normalizedPhone.isNotBlank()) {
                                phoneToPhaseIds.getOrPut(normalizedPhone) { mutableSetOf() }.add(phaseId)
                                android.util.Log.d("ProjectRepository", "  User $normalizedPhone -> Phase: ${phaseDraft.phaseName} (Dept: $deptName)")
                            }
                        }
                        
                        // Add phase ID for assigned approver
                        departmentApproverAssignments[deptName]?.let { approverPhone ->
                            val normalizedPhone = normalizePhoneNumber(approverPhone)
                            if (normalizedPhone.isNotBlank()) {
                                phoneToPhaseIds.getOrPut(normalizedPhone) { mutableSetOf() }.add(phaseId)
                                android.util.Log.d("ProjectRepository", "  Approver $normalizedPhone -> Phase: ${phaseDraft.phaseName} (Dept: $deptName)")
                            }
                        }
                    }
                }
            }
            
            android.util.Log.d("ProjectRepository", "📱 Processing ${phoneToPhaseIds.size} phone numbers")
            
            // Update each user/approver document in User_Customer_Project_Phase_Relation
            phoneToPhaseIds.forEach { (phone, phaseIds) ->
                try {
                    val docRef = firestore.collection("User_Customer_Project_Phase_Relation")
                        .document(phone)
                    
                    // Get existing document
                    val docSnapshot = docRef.get().await()
                    
                    // Prepare the updated structure
                    val existingData = docSnapshot.data?.toMutableMap() ?: mutableMapOf()
                    
                    // Get or create customer map
                    @Suppress("UNCHECKED_CAST")
                    val customerMap = (existingData[customerId] as? MutableMap<String, Any>)?.toMutableMap()
                        ?: mutableMapOf()
                    
                    // Add or update project with its phase IDs
                    customerMap[projectId] = phaseIds.toList()
                    
                    // Update customer map in the document
                    existingData[customerId] = customerMap
                    
                    // Write back to Firestore
                    docRef.set(existingData).await()
                    
                    android.util.Log.d("ProjectRepository", "✅ Updated relation for phone: $phone (${phaseIds.size} phases)")
                } catch (e: Exception) {
                    android.util.Log.e("ProjectRepository", "❌ Error updating relation for phone $phone: ${e.message}", e)
                    // Continue with other phones even if one fails
                }
            }
            
            android.util.Log.d("ProjectRepository", "✅ Successfully updated User_Customer_Project_Phase_Relation for ${phoneToPhaseIds.size} users/approvers")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "❌ Error updating User_Customer_Project_Phase_Relation: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Updates User_Customer_Project_Phase_Relation collection for a newly added phase.
     * This matches the exact iOS logic from AddPhaseSheet.swift.
     * 
     * For each department in the phase, collects:
     * - selectedUserPhone (from new assignments)
     * - selectedApproverPhone (from new assignments)
     * - existing project-level user phone for that department
     * - existing project-level approver phone for that department
     * 
     * Then updates each unique phone number's document with the new phaseId.
     * 
     * @param customerId The customer ID
     * @param projectId The project ID
     * @param phaseId The newly created phase ID
     * @param departments Map of department names to budgets
     * @param departmentAssignments Map of department name to (approverPhone, userPhone) - NEW assignments
     */
    suspend fun updateUserCustomerProjectPhaseRelationForNewPhase(
        customerId: String,
        projectId: String,
        phaseId: String,
        departments: Map<String, Double>,
        departmentAssignments: Map<String, Pair<String?, String?>>
    ): Result<Unit> {
        return try {
            android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
            android.util.Log.d("DEPT_ADD_FLOW", "📋 Step 4B: Updating User_Customer_Project_Phase_Relation Collection")
            android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
            android.util.Log.d("DEPT_ADD_FLOW", "   - Customer ID: $customerId")
            android.util.Log.d("DEPT_ADD_FLOW", "   - Project ID: $projectId")
            android.util.Log.d("DEPT_ADD_FLOW", "   - Phase ID: $phaseId")
            android.util.Log.d("DEPT_ADD_FLOW", "   - Departments (${departments.size}): ${departments.keys.joinToString()}")
            android.util.Log.d("DEPT_ADD_FLOW", "   - Department assignments (${departmentAssignments.size}):")
            departmentAssignments.forEach { (dept, phones) ->
                android.util.Log.d("DEPT_ADD_FLOW", "      • $dept → approver: ${phones.first}, user: ${phones.second}")
            }
            
            // Get existing project-level assignments
            val projectRef = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .document(projectId)
            
            android.util.Log.d("DEPT_ADD_FLOW", "   🔍 Fetching existing project-level assignments...")
            val projectSnapshot = projectRef.get().await()
            val existingUserAssignments = (projectSnapshot.get("departmentUserAssignments") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { it.value.toString() }
                ?: emptyMap()
            val existingApproverAssignments = (projectSnapshot.get("departmentApproverAssignments") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { it.value.toString() }
                ?: emptyMap()
            
            android.util.Log.d("DEPT_ADD_FLOW", "   📋 Found ${existingUserAssignments.size} existing user assignments:")
            existingUserAssignments.forEach { (dept, phone) ->
                android.util.Log.d("DEPT_ADD_FLOW", "      • $dept → $phone")
            }
            android.util.Log.d("DEPT_ADD_FLOW", "   📋 Found ${existingApproverAssignments.size} existing approver assignments:")
            existingApproverAssignments.forEach { (dept, phone) ->
                android.util.Log.d("DEPT_ADD_FLOW", "      • $dept → $phone")
            }
            
            // Build a set of unique phone numbers to update (matching iOS logic)
            val phonesToUpdate = mutableSetOf<String>()
            
            android.util.Log.d("DEPT_ADD_FLOW", "   🔍 Collecting phone numbers to update...")
            // For each department, collect all relevant phone numbers
            departments.keys.forEach { deptName ->
                android.util.Log.d("DEPT_ADD_FLOW", "      Processing department: '$deptName'")
                // Per AddDepartment.md: project-level assignments stored WITHOUT phase prefix
                // e.g., departmentUserAssignments["Plumbing"] = "phone", NOT "phaseId_Plumbing"
                
                // 1. Add selectedUserPhone (from new assignments)
                departmentAssignments[deptName]?.second?.let { userPhone ->
                    val normalized = normalizePhoneNumber(userPhone)
                    if (normalized.isNotBlank()) {
                        phonesToUpdate.add(normalized)
                        android.util.Log.d("DEPT_ADD_FLOW", "         ➕ NEW user phone: $normalized")
                    }
                }
                
                // 2. Add selectedApproverPhone (from new assignments)
                departmentAssignments[deptName]?.first?.let { approverPhone ->
                    val normalized = normalizePhoneNumber(approverPhone)
                    if (normalized.isNotBlank()) {
                        phonesToUpdate.add(normalized)
                        android.util.Log.d("DEPT_ADD_FLOW", "         ➕ NEW approver phone: $normalized")
                    }
                }
                
                // 3. Add existing project-level user phone (unprefixed key)
                existingUserAssignments[deptName]?.let { userPhone ->
                    val normalized = normalizePhoneNumber(userPhone)
                    if (normalized.isNotBlank()) {
                        phonesToUpdate.add(normalized)
                        android.util.Log.d("DEPT_ADD_FLOW", "         ➕ EXISTING user phone: $normalized")
                    }
                }
                
                // 4. Add existing project-level approver phone (unprefixed key)
                existingApproverAssignments[deptName]?.let { approverPhone ->
                    val normalized = normalizePhoneNumber(approverPhone)
                    if (normalized.isNotBlank()) {
                        phonesToUpdate.add(normalized)
                        android.util.Log.d("DEPT_ADD_FLOW", "         ➕ EXISTING approver phone: $normalized")
                    }
                }
            }
            
            android.util.Log.d("DEPT_ADD_FLOW", "   📱 Total unique phones to update: ${phonesToUpdate.size}")
            phonesToUpdate.forEach { phone ->
                android.util.Log.d("DEPT_ADD_FLOW", "      • $phone")
            }
            
            if (phonesToUpdate.isEmpty()) {
                android.util.Log.d("DEPT_ADD_FLOW", "   ⚠️ No phone numbers to update - skipping relation update")
                android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
                return Result.success(Unit)
            }
            
            // Update each phone number's document (matching iOS logic exactly)
            android.util.Log.d("DEPT_ADD_FLOW", "   💾 Writing to Firestore (User_Customer_Project_Phase_Relation)...")
            phonesToUpdate.forEachIndexed { index, phone ->
                try {
                    android.util.Log.d("DEPT_ADD_FLOW", "      [${index + 1}/${phonesToUpdate.size}] Processing phone: $phone")
                    val docRef = firestore.collection("User_Customer_Project_Phase_Relation")
                        .document(phone)
                    
                    android.util.Log.d("DEPT_ADD_FLOW", "         📍 Path: User_Customer_Project_Phase_Relation/$phone")
                    
                    // Read existing document
                    val docSnapshot = docRef.get().await()
                    
                    // Start with a set containing the new phaseId
                    val phaseIds = mutableSetOf(phaseId)
                    
                    // If document exists, merge existing phase IDs
                    if (docSnapshot.exists()) {
                        android.util.Log.d("DEPT_ADD_FLOW", "         📄 Document exists - merging with existing phases")
                        val data = docSnapshot.data
                        @Suppress("UNCHECKED_CAST")
                        val customerMap = data?.get(customerId) as? Map<String, Any>
                        @Suppress("UNCHECKED_CAST")
                        val existingPhaseList = customerMap?.get(projectId) as? List<String>
                        
                        if (existingPhaseList != null) {
                            phaseIds.addAll(existingPhaseList)
                            android.util.Log.d("DEPT_ADD_FLOW", "         ➕ Merged ${existingPhaseList.size} existing phases: ${existingPhaseList.joinToString()}")
                        } else {
                            android.util.Log.d("DEPT_ADD_FLOW", "         ℹ️ No existing phases for this project")
                        }
                    } else {
                        android.util.Log.d("DEPT_ADD_FLOW", "         📄 Document doesn't exist - creating new")
                    }
                    
                    // Create payload with sorted phase IDs (matching iOS behavior)
                    val sortedPhaseIds = phaseIds.toList().sorted()
                    val payload = mapOf(
                        customerId to mapOf(
                            projectId to sortedPhaseIds
                        )
                    )
                    
                    android.util.Log.d("DEPT_ADD_FLOW", "         📦 Payload: {$customerId: {$projectId: ${sortedPhaseIds.joinToString()}}}")
                    
                    // Write with merge (matching iOS setData(..., merge: true))
                    docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
                    
                    android.util.Log.d("DEPT_ADD_FLOW", "         ✅ Successfully updated (${phaseIds.size} total phases)")
                } catch (e: Exception) {
                    android.util.Log.e("DEPT_ADD_FLOW", "         ❌ Error updating relation for phone $phone: ${e.message}", e)
                    // Continue with other phones even if one fails
                }
            }
            
            android.util.Log.d("DEPT_ADD_FLOW", "   ✅ SUCCESS: Updated User_Customer_Project_Phase_Relation for ${phonesToUpdate.size} unique users/approvers")
            android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("DEPT_ADD_FLOW", "❌ ERROR: Failed to update User_Customer_Project_Phase_Relation: ${e.message}", e)
            android.util.Log.d("DEPT_ADD_FLOW", "════════════════════════════════════════════════════════════")
            Result.failure(e)
        }
    }
    
    /**
     * Updates User_Customer_Project_Phase_Relation collection when a department assignment is made.
     * Adds the phaseId to the relation for the assigned user and approver.
     * 
     * @param projectId The project containing the department
     * @param phaseId The phase ID where the department is being added
     * @param departmentName The name of the department (plain name, NOT prefixed with phaseId)
     * @param userPhone The phone number of the assigned user (can be null)
     * @param approverPhone The phone number of the assigned approver (can be null)
     */
    suspend fun updateUserCustomerProjectPhaseRelationForDepartment(
        projectId: String,
        phaseId: String,
        departmentName: String,
        userPhone: String?,
        approverPhone: String?
    ): Result<Unit> {
        return try {
            android.util.Log.d("DEPT_ADD_FLOW", "      ════════════════════════════════════════════════════════════")
            android.util.Log.d("DEPT_ADD_FLOW", "      📋 Step 4B Details: updateUserCustomerProjectPhaseRelationForDepartment")
            android.util.Log.d("DEPT_ADD_FLOW", "      ════════════════════════════════════════════════════════════")
            
            val customerId = getCurrentUserCustomerId()
            if (customerId == null) {
                android.util.Log.e("DEPT_ADD_FLOW", "      ❌ ERROR: User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }
            
            android.util.Log.d("DEPT_ADD_FLOW", "         - Customer ID: $customerId")
            android.util.Log.d("DEPT_ADD_FLOW", "         - Project ID: $projectId")
            android.util.Log.d("DEPT_ADD_FLOW", "         - Phase ID: $phaseId")
            android.util.Log.d("DEPT_ADD_FLOW", "         - Department name: '$departmentName' (plain name, no prefix)")
            android.util.Log.d("DEPT_ADD_FLOW", "         - User phone: $userPhone")
            android.util.Log.d("DEPT_ADD_FLOW", "         - Approver phone: $approverPhone")
            
            // Collect phone numbers to update
            val phonesToUpdate = mutableListOf<String>()
            
            android.util.Log.d("DEPT_ADD_FLOW", "         🔍 Collecting and normalizing phone numbers...")
            
            userPhone?.let { phone ->
                val normalized = normalizePhoneNumber(phone)
                if (normalized.isNotBlank()) {
                    phonesToUpdate.add(normalized)
                    android.util.Log.d("DEPT_ADD_FLOW", "            ➕ User phone: $phone → normalized: $normalized")
                } else {
                    android.util.Log.d("DEPT_ADD_FLOW", "            ⚠️ User phone is blank after normalization")
                }
            }
            
            approverPhone?.let { phone ->
                val normalized = normalizePhoneNumber(phone)
                if (normalized.isNotBlank()) {
                    phonesToUpdate.add(normalized)
                    android.util.Log.d("DEPT_ADD_FLOW", "            ➕ Approver phone: $phone → normalized: $normalized")
                } else {
                    android.util.Log.d("DEPT_ADD_FLOW", "            ⚠️ Approver phone is blank after normalization")
                }
            }
            
            if (phonesToUpdate.isEmpty()) {
                android.util.Log.d("DEPT_ADD_FLOW", "         ⚠️ No valid phone numbers to update - skipping")
                android.util.Log.d("DEPT_ADD_FLOW", "      ════════════════════════════════════════════════════════════")
                return Result.success(Unit)
            }
            
            android.util.Log.d("DEPT_ADD_FLOW", "         📱 Total phones to update: ${phonesToUpdate.size}")
            
            // Update each phone number's document (matching iOS logic)
            android.util.Log.d("DEPT_ADD_FLOW", "         💾 Writing to User_Customer_Project_Phase_Relation collection...")
            phonesToUpdate.forEachIndexed { index, phone ->
                try {
                    android.util.Log.d("DEPT_ADD_FLOW", "         [${index + 1}/${phonesToUpdate.size}] Processing phone: $phone")
                    val docRef = firestore.collection("User_Customer_Project_Phase_Relation")
                        .document(phone)
                    
                    android.util.Log.d("DEPT_ADD_FLOW", "            📍 Path: User_Customer_Project_Phase_Relation/$phone")
                    
                    // Read existing document
                    val docSnapshot = docRef.get().await()
                    
                    // Start with a set containing the new phaseId
                    val phaseIds = mutableSetOf(phaseId)
                    
                    // If document exists, merge existing phase IDs
                    if (docSnapshot.exists()) {
                        android.util.Log.d("DEPT_ADD_FLOW", "            📄 Document exists - merging phases")
                        val data = docSnapshot.data
                        @Suppress("UNCHECKED_CAST")
                        val customerMap = data?.get(customerId) as? Map<String, Any>
                        @Suppress("UNCHECKED_CAST")
                        val existingPhaseList = customerMap?.get(projectId) as? List<String>
                        
                        if (existingPhaseList != null) {
                            phaseIds.addAll(existingPhaseList)
                            android.util.Log.d("DEPT_ADD_FLOW", "            ➕ Merged ${existingPhaseList.size} existing phases: ${existingPhaseList.joinToString()}")
                        } else {
                            android.util.Log.d("DEPT_ADD_FLOW", "            ℹ️ No existing phases for this project")
                        }
                    } else {
                        android.util.Log.d("DEPT_ADD_FLOW", "            📄 Document doesn't exist - creating new")
                    }
                    
                    // Create payload with sorted phase IDs (matching iOS behavior)
                    val sortedPhaseIds = phaseIds.toList().sorted()
                    val payload = mapOf(
                        customerId to mapOf(
                            projectId to sortedPhaseIds
                        )
                    )
                    
                    android.util.Log.d("DEPT_ADD_FLOW", "            📦 Payload: {$customerId: {$projectId: ${sortedPhaseIds.joinToString()}}}")
                    
                    // Write with merge (matching iOS setData(..., merge: true))
                    docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
                    
                    android.util.Log.d("DEPT_ADD_FLOW", "            ✅ Updated (phase: $phaseId, ${phaseIds.size} total phases)")
                } catch (e: Exception) {
                    android.util.Log.e("DEPT_ADD_FLOW", "            ❌ Error for phone $phone: ${e.message}", e)
                    // Continue with other phones even if one fails
                }
            }
            
            android.util.Log.d("DEPT_ADD_FLOW", "         ✅ SUCCESS: Updated ${phonesToUpdate.size} phone documents")
            android.util.Log.d("DEPT_ADD_FLOW", "      ════════════════════════════════════════════════════════════")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("DEPT_ADD_FLOW", "      ❌ ERROR: Failed to update relation: ${e.message}", e)
            android.util.Log.d("DEPT_ADD_FLOW", "      ════════════════════════════════════════════════════════════")
            Result.failure(e)
        }
    }

    /**
     * Syncs User_Customer_Project_Phase_Relation for one user/approver by scanning
     * current project-level department assignments for a customer.
     *
     * Stored format:
     * User_Customer_Project_Phase_Relation/{phoneWithoutCountryCode} = {
     *   {customerId}: {
     *     {projectId}: [phaseId1, phaseId2, ...]
     *   }
     * }
     */
    suspend fun syncUserCustomerProjectPhaseRelationForAssignment(
        customerId: String,
        phoneNumber: String,
        isApprover: Boolean
    ): Result<Int> {
        return try {
            val cleanPhone = normalizePhoneNumber(phoneNumber)
            if (cleanPhone.isBlank()) {
                return Result.failure(Exception("Invalid phone number"))
            }

            val assignmentField = if (isApprover) {
                "departmentApproverAssignments"
            } else {
                "departmentUserAssignments"
            }

            android.util.Log.d(
                "ProjectRepository",
                "🔄 Syncing User_Customer_Project_Phase_Relation for phone=$cleanPhone, customer=$customerId, field=$assignmentField"
            )

            val projectsSnapshot = firestore.collection("customers")
                .document(customerId)
                .collection("projects")
                .get()
                .await()

            val customerProjectPhaseMap = mutableMapOf<String, List<String>>()

            for (projectDoc in projectsSnapshot.documents) {
                val projectId = projectDoc.id

                val assignments = (projectDoc.get(assignmentField) as? Map<*, *>)
                    ?.mapKeys { it.key.toString() }
                    ?.mapValues { it.value.toString() }
                    ?: emptyMap()

                if (assignments.isEmpty()) continue

                val matchingDepartments = assignments
                    .filterValues { normalizePhoneNumber(it) == cleanPhone }
                    .keys

                if (matchingDepartments.isEmpty()) continue

                val phasesSnapshot = projectDoc.reference
                    .collection("phases")
                    .get()
                    .await()

                val phaseIds = mutableSetOf<String>()

                for (phaseDoc in phasesSnapshot.documents) {
                    val phaseId = phaseDoc.id

                    val departmentsSnapshot = phaseDoc.reference
                        .collection("departments")
                        .get()
                        .await()

                    val matchesSubcollection = departmentsSnapshot.documents.any { deptDoc ->
                        val deptName = (deptDoc.getString("name") ?: deptDoc.getString("departmentName") ?: "").trim()
                        matchingDepartments.any { it.trim().equals(deptName, ignoreCase = true) }
                    }

                    if (matchesSubcollection) {
                        phaseIds.add(phaseId)
                        continue
                    }

                    val legacyDepartments = phaseDoc.get("departments") as? Map<*, *>
                    val matchesLegacy = legacyDepartments?.keys?.map { key ->
                        val rawKey = key.toString().trim()
                        if (rawKey.startsWith("${phaseId}_")) rawKey.removePrefix("${phaseId}_") else rawKey
                    }?.any { normalizedDeptName ->
                        matchingDepartments.any { it.trim().equals(normalizedDeptName, ignoreCase = true) }
                    } ?: false

                    if (matchesLegacy) {
                        phaseIds.add(phaseId)
                    }
                }

                if (phaseIds.isNotEmpty()) {
                    customerProjectPhaseMap[projectId] = phaseIds.toList().sorted()
                }
            }

            val relationDoc = firestore.collection("User_Customer_Project_Phase_Relation")
                .document(cleanPhone)

            if (customerProjectPhaseMap.isEmpty()) {
                runCatching {
                    relationDoc.update(customerId, com.google.firebase.firestore.FieldValue.delete()).await()
                }
                android.util.Log.d(
                    "ProjectRepository",
                    "ℹ️ No matching assignment phases found. Cleared key '$customerId' for phone=$cleanPhone"
                )
                return Result.success(0)
            }

            val payload = mapOf(customerId to customerProjectPhaseMap)
            relationDoc.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()

            android.util.Log.d(
                "ProjectRepository",
                "✅ Synced relation for phone=$cleanPhone with ${customerProjectPhaseMap.size} project(s)"
            )
            Result.success(customerProjectPhaseMap.size)
        } catch (e: Exception) {
            android.util.Log.e(
                "ProjectRepository",
                "❌ Error syncing User_Customer_Project_Phase_Relation for assignment: ${e.message}",
                e
            )
            Result.failure(e)
        }
    }

    /**
     * Removes the customer-level map entry from User_Customer_Project_Phase_Relation for a phone.
     * Document id is normalized phone (without +91). Deleted key: {customerId}
     */
    suspend fun removeCustomerFromUserCustomerProjectPhaseRelation(phoneNumber: String, customerId: String): Result<Unit> {
        return try {
            val cleanPhone = normalizePhoneNumber(phoneNumber)
            if (cleanPhone.isBlank()) {
                return Result.failure(Exception("Invalid phone number"))
            }

            android.util.Log.d(
                "ProjectRepository",
                "🔄 Removing customer key from User_Customer_Project_Phase_Relation/$cleanPhone: key=$customerId"
            )

            firestore.collection("User_Customer_Project_Phase_Relation")
                .document(cleanPhone)
                .update(customerId, com.google.firebase.firestore.FieldValue.delete())
                .await()

            android.util.Log.d(
                "ProjectRepository",
                "✅ Removed customer key '$customerId' from User_Customer_Project_Phase_Relation/$cleanPhone"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(
                "ProjectRepository",
                "❌ Error removing customer key from User_Customer_Project_Phase_Relation: ${e.message}",
                e
            )
            Result.failure(e)
        }
    }
}
