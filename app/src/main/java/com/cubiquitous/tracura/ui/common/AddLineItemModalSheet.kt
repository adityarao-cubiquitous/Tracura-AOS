package com.cubiquitous.tracura.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.cubiquitous.tracura.model.ContractorMode
import com.cubiquitous.tracura.model.LineItem
import com.cubiquitous.tracura.utils.FormatUtils

private data class AddLineItemDraftCache(
    val itemType: String,
    val item: String,
    val spec: String,
    val quantity: String,
    val unitPrice: String,
    val uom: String
)

private object AddLineItemDraftCacheStore {
    private val cache = mutableMapOf<String, AddLineItemDraftCache>()

    fun get(key: String): AddLineItemDraftCache? = cache[key]

    fun set(key: String, value: AddLineItemDraftCache) {
        cache[key] = value
    }

    fun clear(key: String) {
        cache.remove(key)
    }

    fun clearAll() {
        cache.clear()
    }
}

private fun addLineItemCacheKey(
    contractorMode: ContractorMode,
    isEditMode: Boolean
): String = listOf(contractorMode.name, isEditMode.toString()).joinToString("|")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLineItemModalSheet(
    lineItem: LineItem,
    contractorMode: ContractorMode,
    onDismiss: () -> Unit,
    onAdd: (LineItem) -> Unit,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val cacheKey = remember(contractorMode, isEditMode) {
        addLineItemCacheKey(contractorMode, isEditMode)
    }
    val cachedDraft = remember(cacheKey) {
        AddLineItemDraftCacheStore.get(cacheKey)
    }
    var hasRestoredDraft by remember(cacheKey) { mutableStateOf(false) }
    var isClosing by remember(cacheKey) { mutableStateOf(false) }

    val initialItemType = if (!isEditMode) cachedDraft?.itemType ?: lineItem.itemType else lineItem.itemType
    val initialItem = if (!isEditMode) cachedDraft?.item ?: lineItem.item else lineItem.item
    val initialSpec = if (!isEditMode) cachedDraft?.spec ?: lineItem.spec else lineItem.spec
    val initialQuantity = if (!isEditMode) {
        cachedDraft?.quantity ?: if (lineItem.quantity > 0) lineItem.quantity.toString() else ""
    } else {
        if (lineItem.quantity > 0) lineItem.quantity.toString() else ""
    }
    val initialUom = if (!isEditMode) cachedDraft?.uom ?: lineItem.uom else lineItem.uom
    val initialUnitPrice = if (!isEditMode) {
        cachedDraft?.unitPrice ?: if (lineItem.unitPrice > 0) lineItem.unitPrice.toString() else ""
    } else {
        if (lineItem.unitPrice > 0) lineItem.unitPrice.toString() else ""
    }

    var itemType by remember { mutableStateOf(initialItemType) }
    var item by remember { mutableStateOf(initialItem) }
    var spec by remember { mutableStateOf(initialSpec) }
    // Initialize with empty string instead of "0" so placeholder shows
    var quantity by remember { mutableStateOf(initialQuantity) }
    var uom by remember { mutableStateOf(initialUom) }
    // Initialize with empty string instead of "0" so placeholder shows
    var unitPrice by remember { mutableStateOf(initialUnitPrice) }

    var expandedItemType by remember { mutableStateOf(false) }
    var expandedItem by remember { mutableStateOf(false) }
    var expandedSpec by remember { mutableStateOf(false) }
    var expandedUom by remember { mutableStateOf(false) }

    // Focus states for quantity and unit price
    var quantityFocused by remember { mutableStateOf(false) }
    var unitPriceFocused by remember { mutableStateOf(false) }

    // Calculate total
    val total = remember(quantity, unitPrice) {
        val qty = quantity.toDoubleOrNull() ?: 0.0
        val price = unitPrice.toDoubleOrNull() ?: 0.0
        qty * price
    }

    // Check if form is valid
    val isAddEnabled = itemType.isNotBlank() &&
            item.isNotBlank() &&
            (itemType == "Labour" || spec.isNotBlank()) &&
            quantity.toDoubleOrNull() ?: 0.0 > 0 &&
            unitPrice.toDoubleOrNull() ?: 0.0 > 0 &&
            uom.isNotBlank()

    // Use ModalBottomSheet; constrain to ~95% height to keep a small peek above
    // Set skipPartiallyExpanded = true so it opens to its max (our 95% constraint)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Expand immediately on launch
    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    LaunchedEffect(cachedDraft) {
        if (!isEditMode && cachedDraft != null && !hasRestoredDraft) {
            itemType = cachedDraft.itemType
            item = cachedDraft.item
            spec = cachedDraft.spec
            quantity = cachedDraft.quantity
            unitPrice = cachedDraft.unitPrice
            uom = cachedDraft.uom
            hasRestoredDraft = true
        }
    }

    LaunchedEffect(itemType, item, spec, quantity, unitPrice, uom) {
        if (isClosing || isEditMode) {
            return@LaunchedEffect
        }
        AddLineItemDraftCacheStore.set(
            cacheKey,
            AddLineItemDraftCache(
                itemType = itemType,
                item = item,
                spec = spec,
                quantity = quantity,
                unitPrice = unitPrice,
                uom = uom
            )
        )
    }

    val resetDraft = {
        AddLineItemDraftCacheStore.clearAll()
        hasRestoredDraft = true
        if (isEditMode) {
            itemType = lineItem.itemType
            item = lineItem.item
            spec = lineItem.spec
            quantity = if (lineItem.quantity > 0) lineItem.quantity.toString() else ""
            unitPrice = if (lineItem.unitPrice > 0) lineItem.unitPrice.toString() else ""
            uom = lineItem.uom
        } else {
            itemType = ""
            item = ""
            spec = ""
            quantity = ""
            unitPrice = ""
            uom = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        //dragHandle = null // Remove drag handle for full screen
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.97f)
        ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.offset(y = (-30).dp),
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (isEditMode) "Edit Line Item" else "Add Line Item",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text(
                                "Close",
                                color = colorScheme.primary,
                                fontSize = 22.sp,
                                fontWeight= FontWeight.Medium
                            )
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = resetDraft,
                                enabled = !isClosing
                            ) {
                                Text(
                                    "Reset",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    val newLineItem = LineItem(
                                        itemType = itemType,
                                        item = item,
                                        spec = spec,
                                        quantity = quantity.toDoubleOrNull() ?: 0.0,
                                        unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                                        uom = uom
                                    )
                                    isClosing = true
                                    AddLineItemDraftCacheStore.clear(cacheKey)
                                    onAdd(newLineItem)
                                },
                                enabled = isAddEnabled
                            ) {
                                Text(
                                    text = if (isEditMode) "Save" else "Add",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    style = TextStyle(
                                        brush = Brush.horizontalGradient(
                                            colors = if (isAddEnabled) {
                                                listOf(
                                                    colorScheme.primary,
                                                    colorScheme.tertiary
                                                )
                                            } else {
                                                listOf(
                                                    colorScheme.outline,
                                                    colorScheme.outline
                                                )
                                            }
                                        )
                                    )
                                )

                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.surface
                    )
                )
            },
            containerColor = colorScheme.surface
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // Item Type
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Item Type",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedItemType,
                        onExpandedChange = { expandedItemType = !expandedItemType },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = itemType.ifEmpty { "" },
                            onValueChange = { },
                            readOnly = true,
                            textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp),
                            placeholder = { Text("Select Item Type") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colorScheme.surface,
                                unfocusedContainerColor = colorScheme.surface,
                                focusedBorderColor = colorScheme.primary,
                                unfocusedBorderColor = colorScheme.outline
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedItemType,
                            onDismissRequest = { expandedItemType = false },
                            modifier = Modifier.background(colorScheme.surface)
                        ) {
                            // Filter item types based on contractor mode
                            val availableItemTypes = when (contractorMode) {
                                 ContractorMode.LABOUR_ONLY -> ItemData.itemTypes.filter { it != "Labour" }
                                 ContractorMode.SELF_EXECUTION -> ItemData.itemTypes
                                 ContractorMode.TURNKEY_AMOUNT_ONLY -> emptyList()
                             }

                            availableItemTypes.forEachIndexed { index, type ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = type,
                                                color = if (itemType == type) colorScheme.primary else colorScheme.onSurface,
                                                fontSize = 17.sp
                                            )
                                            if (itemType == type) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        itemType = type
                                        item = ""
                                        spec = if (type == "Labour") "" else spec
                                        expandedItemType = false
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    ),
                                    colors = MenuDefaults.itemColors(
                                        textColor = if (itemType == type) colorScheme.primary else colorScheme.onSurface
                                    )
                                )
                                if (index < availableItemTypes.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 0.dp),
                                        color = colorScheme.outlineVariant,
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                }

                // Item + Spec stacked to match reference
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item Dropdown
                    val availableItems = if (itemType.isNotEmpty()) {
                        ItemData.getItemsForType(itemType)
                    } else {
                        emptyList()
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Item",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedItem,
                            onExpandedChange = { expandedItem = !expandedItem },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = item.ifEmpty { "" },
                                onValueChange = { },
                                readOnly = true,
                                textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp),
                                placeholder = { Text("Select Item") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                enabled = itemType.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colorScheme.surface,
                                    unfocusedContainerColor = colorScheme.surface,
                                    focusedBorderColor = colorScheme.primary,
                                    unfocusedBorderColor = colorScheme.outline
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expandedItem,
                                onDismissRequest = { expandedItem = false },
                                modifier = Modifier.background(colorScheme.surface)
                            ) {
                                availableItems.forEachIndexed { index, itemValue ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = itemValue,
                                                    color = if (item == itemValue) colorScheme.primary else colorScheme.onSurface,
                                                    fontSize = 17.sp
                                                )
                                                if (item == itemValue) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            item = itemValue
                                            spec = ""
                                            expandedItem = false
                                        },
                                        contentPadding = PaddingValues(
                                            horizontal = 16.dp,
                                            vertical = 12.dp
                                        ),
                                        colors = MenuDefaults.itemColors(
                                            textColor = if (item == itemValue) colorScheme.primary else colorScheme.onSurface
                                        )
                                    )
                                    if (index < availableItems.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 0.dp),
                                            color = colorScheme.outlineVariant,
                                            thickness = 1.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Spec Dropdown - Only show if NOT Labour
                    if (itemType != "Labour") {
                        val availableSpecs = if (itemType.isNotEmpty() && item.isNotEmpty()) {
                            ItemData.getSpecsForItem(itemType, item)
                        } else {
                            emptyList()
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Spec",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ExposedDropdownMenuBox(
                                expanded = expandedSpec,
                                onExpandedChange = { expandedSpec = !expandedSpec },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = spec.ifEmpty { "" },
                                    onValueChange = { },
                                    readOnly = true,
                                    textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp),
                                    placeholder = { Text("Select Spec") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    enabled = item.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colorScheme.surface,
                                        unfocusedContainerColor = colorScheme.surface,
                                        focusedBorderColor = colorScheme.primary,
                                        unfocusedBorderColor = colorScheme.outline
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedSpec,
                                    onDismissRequest = { expandedSpec = false },
                                    modifier = Modifier.background(colorScheme.surface)
                                ) {
                                    availableSpecs.forEachIndexed { index, specValue ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = specValue,
                                                        color = if (spec == specValue) colorScheme.primary else colorScheme.onSurface,
                                                        fontSize = 17.sp
                                                    )
                                                    if (spec == specValue) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                spec = specValue
                                                expandedSpec = false
                                            },
                                            contentPadding = PaddingValues(
                                                horizontal = 16.dp,
                                                vertical = 12.dp
                                            ),
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (spec == specValue) colorScheme.primary else colorScheme.onSurface
                                            )
                                        )
                                        if (index < availableSpecs.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 0.dp),
                                                color = colorScheme.outlineVariant,
                                                thickness = 1.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quantity + UOM (Two fields side by side)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quantity
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Quantity",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { newValue ->
                                    quantity = newValue
                                },
                                placeholder = { Text("0") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .onFocusChanged { focusState ->
                                        quantityFocused = focusState.isFocused
                                        // Clear "0" when field is focused so user can type immediately
                                        if (focusState.isFocused && quantity == "0") {
                                            quantity = ""
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.merge(
                                    TextStyle(fontSize = 15.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
                                ),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colorScheme.surface,
                                    unfocusedContainerColor = colorScheme.surface,
                                    focusedBorderColor = colorScheme.primary,
                                    unfocusedBorderColor = colorScheme.outline
                                )
                            )
                        }

                        // UOM (Unit of Measure)
                        val availableUomOptions = if (itemType.isNotEmpty()) {
                            ItemData.uomOptions(itemType)
                        } else {
                            ItemData.uomOptions("")
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "UOM",
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ExposedDropdownMenuBox(
                                expanded = expandedUom,
                                onExpandedChange = { expandedUom = !expandedUom },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = uom.ifEmpty { "" },
                                    onValueChange = { },
                                    readOnly = true,
                                    placeholder = { Text("Select UOM") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                    .height(52.dp)
                                        .menuAnchor(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.merge(
                                    TextStyle(fontSize = 15.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
                                ),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colorScheme.surface,
                                        unfocusedContainerColor = colorScheme.surface,
                                        focusedBorderColor = colorScheme.primary,
                                        unfocusedBorderColor = colorScheme.outline
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedUom,
                                    onDismissRequest = { expandedUom = false },
                                    modifier = Modifier.background(colorScheme.surface)
                                ) {
                                    availableUomOptions.forEachIndexed { index, uomValue ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = uomValue,
                                                        color = if (uom == uomValue) colorScheme.primary else colorScheme.onSurface,
                                                        fontSize = 17.sp
                                                    )
                                                    if (uom == uomValue) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                uom = uomValue
                                                expandedUom = false
                                            },
                                            contentPadding = PaddingValues(
                                                horizontal = 16.dp,
                                                vertical = 12.dp
                                            ),
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (uom == uomValue) colorScheme.primary else colorScheme.onSurface
                                            )
                                        )
                                        if (index < availableUomOptions.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 0.dp),
                                                color = colorScheme.outlineVariant,
                                                thickness = 1.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Unit Price
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Unit Price",
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = unitPrice,
                            onValueChange = { newValue ->
                                unitPrice = newValue
                            },
                            placeholder = { Text("0") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .onFocusChanged { focusState ->
                                    unitPriceFocused = focusState.isFocused
                                    // Clear "0" when field is focused so user can type immediately
                                    if (focusState.isFocused && unitPrice == "0") {
                                        unitPrice = ""
                                    }
                                },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                            textStyle = LocalTextStyle.current.merge(
                                TextStyle(fontSize = 15.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colorScheme.surface,
                                unfocusedContainerColor = colorScheme.surface,
                                focusedBorderColor = colorScheme.primary,
                                unfocusedBorderColor = colorScheme.outline
                            )
                        )
                    }

                    // Total Section at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colorScheme.primary.copy(alpha = 0.12f),
                                    colorScheme.tertiary.copy(alpha = 0.12f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Yellow sigma symbol (Σ)
                                Text(
                                    text = "Σ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.tertiary
                                )
                                Text(
                                    text = "Total",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                            }
                            Text(
                                text = FormatUtils.formatCurrencyWithoutDecimals(total),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(
                                    //Total color
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            colorScheme.primary,
                                            colorScheme.tertiary
                                        )
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

