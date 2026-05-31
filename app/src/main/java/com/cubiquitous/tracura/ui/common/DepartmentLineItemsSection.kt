package com.cubiquitous.tracura.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.ContractorMode
import com.cubiquitous.tracura.model.LineItem
import com.cubiquitous.tracura.utils.FormatUtils

private data class LineItemsPalette(
    val tier2Surface: Color,
    val tier3Field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val hairline: Color,
    val accent: Color,
    val success: Color,
    val danger: Color,
)

@Composable
private fun rememberLineItemsPalette(): LineItemsPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark) {
        LineItemsPalette(
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            hairline = if (isDark) Color(0xFF38383A) else Color(0x1F000000),
            accent = Color(0xFF0A84FF),
            success = Color(0xFF30D158),
            danger = Color(0xFFD32F2F),
        )
    }
}

@Composable
private fun lineItemTextFieldColors(palette: LineItemsPalette): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = palette.accent,
        unfocusedBorderColor = palette.hairline,
        focusedTextColor = palette.textPrimary,
        unfocusedTextColor = palette.textPrimary,
        disabledTextColor = palette.textSecondary,
        focusedLabelColor = palette.accent,
        unfocusedLabelColor = palette.textSecondary,
        disabledLabelColor = palette.textSecondary,
        focusedContainerColor = palette.tier3Field,
        unfocusedContainerColor = palette.tier3Field,
        disabledContainerColor = palette.tier3Field,
        cursorColor = palette.accent,
    )
}

// Data structure for items based on JSON
object ItemData {
    // MARK: - Raw Materials
    val rawMaterialItems = mapOf(
        "Steel" to listOf(
            "Fe500 • 6 mm",
            "Fe500 • 8 mm",
            "Fe500 • 10 mm",
            "Fe500 • 12 mm",
            "Fe500 • 16 mm",
            "Fe500 • 20 mm"
        ),
        "Cement" to listOf(
            "OPC 43",
            "OPC 53",
            "PPC"
        ),
        "Sand" to listOf(
            "M-Sand • Zone I",
            "M-Sand • Zone II",
            "River Sand (Coarse)",
            "River Sand (Fine)"
        ),
        "Aggregates" to listOf(
            "6mm Jelly",
            "12mm Jelly",
            "20mm Jelly"
        ),
        "Bricks" to listOf(
            "Clay Brick",
            "Solid Block",
            "Hollow Block",
            "Fly Ash Brick"
        ),
        "Water" to listOf(
            "Drinking Water",
            "Construction Water"
        )
    )
    
    // MARK: - Labour
    val labourItems = mapOf(
        "Men" to emptyList(),
        "Women" to emptyList(),
        "Skilled" to listOf(
            "Mason",
            "Carpenter",
            "Bar Bender",
            "Electrician",
            "Plumber",
            "Painter"
        ),
        "Unskilled" to listOf(
            "Helper"
        )
    )
    
    // MARK: - Machines & Equipment
    val machinesItems = mapOf(
        "JCB" to listOf(
            "Per-day hire",
            "Per-hour hire"
        ),
        "Tractor / Trolley" to listOf(
            "Per-trip",
            "Per-day"
        ),
        "Concrete Mixer" to listOf(
            "Per-day hire"
        ),
        "Vibrator" to listOf(
            "Per-day hire"
        ),
        "Lift" to listOf(
            "Material Lift",
            "Passenger Lift"
        ),
        "Scaffolding" to listOf(
            "Steel Scaffolding",
            "Aluminium Scaffolding"
        ),
        "Cutter" to listOf(
            "Marble Cutter",
            "Wood Cutter"
        )
    )
    
    // MARK: - Electrical
    val electricalItems = mapOf(
        "Wires & Cables" to listOf(
            "1.5 sq mm",
            "2.5 sq mm",
            "4 sq mm",
            "6 sq mm",
            "10 sq mm",
            "16 sq mm"
        ),
        "MCB & DB" to listOf(
            "6A MCB",
            "10A MCB",
            "16A MCB",
            "20A MCB",
            "32A MCB",
            "Distribution Board"
        ),
        "Lighting" to listOf(
            "LED Bulb",
            "LED Tube",
            "LED Panel",
            "LED Strip",
            "Halogen"
        ),
        "Switches & Sockets" to listOf(
            "Single pole",
            "Double pole",
            "5A socket",
            "15A socket",
            "Modular switches"
        ),
        "Conduits & Accessories" to listOf(
            "20mm PVC",
            "25mm PVC",
            "32mm PVC",
            "Elbow",
            "Coupler",
            "Bend"
        )
    )
    
    // MARK: - Plumbing
    val plumbingItems = mapOf(
        "Pipes" to listOf(
            "PVC • 1 inch",
            "PVC • 2 inch",
            "CPVC • 1 inch",
            "UPVC • 1 inch"
        ),
        "Fittings" to listOf(
            "Elbow",
            "Tee",
            "Reducer",
            "Coupler",
            "GI Clamp"
        ),
        "Valves" to listOf(
            "Ball Valve",
            "Gate Valve",
            "Check Valve"
        ),
        "Water Tanks" to listOf(
            "500 Litre",
            "1000 Litre",
            "1500 Litre"
        )
    )
    
    // MARK: - Flooring
    val flooringItems = mapOf(
        "Tiles" to listOf(
            "Ceramic Tile",
            "Vitrified Tile",
            "Porcelain Tile"
        ),
        "Marble" to listOf(
            "Makrana White",
            "Indian Marble"
        ),
        "Granite" to listOf(
            "Black Granite",
            "Red Granite",
            "Grey Granite"
        ),
        "Wooden Flooring" to listOf(
            "Laminate",
            "Engineered Wood"
        )
    )
    
    // MARK: - Tiles Work
    val tilesGraniteItems = mapOf(
        "Wall Tiles" to listOf(
            "Bathroom Tiles",
            "Kitchen Tiles"
        ),
        "Floor Tiles" to listOf(
            "600x600 mm",
            "800x800 mm",
            "1200x600 mm"
        ),
        "Granite Slabs" to listOf(
            "20mm",
            "25mm"
        ),
        "Marble Slabs" to listOf(
            "Premium",
            "Standard"
        )
    )
    
    // MARK: - Sanitary
    val sanitaryItems = mapOf(
        "WC" to listOf(
            "Floor Mount",
            "Wall Hung"
        ),
        "Wash Basins" to listOf(
            "Pedestal Basin",
            "Table Top Basin"
        ),
        "Showers" to listOf(
            "Overhead Shower",
            "Hand Shower"
        ),
        "Taps" to listOf(
            "Wall Tap",
            "Pillar Tap",
            "Mixer Tap"
        )
    )
    
    // MARK: - Paint & Finishing
    val paintingItems = mapOf(
        "Interior" to listOf(
            "Primer",
            "Putty",
            "Distemper",
            "Emulsion Paint"
        ),
        "Exterior" to listOf(
            "Primer",
            "Texture Coat",
            "Weather Shield Paint"
        ),
        "Wood Polish" to listOf(
            "Melamine",
            "PU Polish"
        )
    )
    
    // MARK: - Carpentry & Woodwork
    val carpentryItems = mapOf(
        "Plywood" to listOf(
            "12mm",
            "18mm"
        ),
        "Laminates" to listOf(
            "0.8mm",
            "1mm"
        ),
        "Veneer" to listOf(
            "Natural",
            "Reconstituted"
        ),
        "Doors" to listOf(
            "Flush Door",
            "Panel Door"
        )
    )
    
    // MARK: - Glass & Aluminium
    val glassAluminiumItems = mapOf(
        "Glass Types" to listOf(
            "Clear Glass",
            "Frosted Glass",
            "Toughened Glass"
        ),
        "Aluminium Sections" to listOf(
            "2 Track",
            "3 Track",
            "Sliding Sections"
        ),
        "Partitions" to listOf(
            "Office Partition",
            "Bathroom Partition"
        )
    )
    
    // MARK: - False Ceiling
    val falseCeilingItems = mapOf(
        "Gypsum" to listOf(
            "12mm Board",
            "Moisture Resistant Board"
        ),
        "POP Work" to listOf(
            "Design Work",
            "Straight Ceiling"
        ),
        "Grid Ceiling" to listOf(
            "2x2 Panel",
            "Acoustic Tiles"
        )
    )
    
    // MARK: - Hardware
    val hardwareItems = mapOf(
        "Screws" to listOf(
            "Wood Screw",
            "Machine Screw"
        ),
        "Hinges" to listOf(
            "3 inch",
            "4 inch"
        ),
        "Locks" to listOf(
            "Door Lock",
            "Pad Lock"
        ),
        "Handles" to listOf(
            "Door Handle",
            "Cabinet Handle"
        )
    )
    
    // MARK: - Waterproofing
    val waterproofingItems = mapOf(
        "Chemicals" to listOf(
            "Liquid Membrane",
            "Cementitious Coating"
        ),
        "Sheets" to listOf(
            "Bitumen Sheet",
            "APP Membrane"
        ),
        "Additives" to listOf(
            "Waterproofing Compound"
        )
    )
    
    val itemTypes = listOf(
        "Raw material",
        "Labour",
        "Machines & eq",
        "Electrical",
        "Plumbing",
        "Flooring",
        "Tiles & Granite",
        "Sanitary",
        "Painting",
        "Carpentry",
        "Glass & Aluminium",
        "False Ceiling",
        "Hardware",
        "Waterproofing"
    )
    
    // Common construction Unit of Measure (UOM) options
    val uomOptions = listOf(
        "KG",
        "Ton",
        "Litre",
        "m³",
        "m²",
        "m",
        "Day",
        "Hour",
        "Piece",
        "Bag",
        "ft³",
        "ft²",
        "ft",
        "Trip",
        "Per-day hire",
        "Per-hour hire"
    )
    
    /**
     * Get UOM options based on item type
     * Returns filtered UOM options specific to the item type
     */
    fun uomOptions(itemType: String): List<String> {
        return when (itemType) {
            // RAW MATERIAL
            "Raw material" -> {
                listOf("KG", "Tonne", "m³", "Cft", "Bag", "Litre")
            }
            // LABOUR
            "Labour" -> {
                listOf("Per Day", "Per Hour", "Nos", "Unit")
            }
            // MACHINES
            "Machines & eq" -> {
                listOf("Per Day", "Per Hour", "Per Trip")
            }
            // ELECTRICAL
            "Electrical" -> {
                listOf("m", "Rft", "Piece", "Nos", "Unit", "Roll", "Set", "Box", "Bundle", "m²", "Sqft", "Sqmt")
            }
            // PLUMBING
            "Plumbing" -> {
                listOf("m", "Rft", "Piece", "Nos", "Unit", "Set", "Kg", "Litre")
            }
            // FLOORING
            "Flooring" -> {
                listOf("Sqft", "Sqmt", "m²", "Piece", "Nos", "Box")
            }
            // TILES & GRANITE
            "Tiles & Granite" -> {
                listOf("Sqft", "Sqmt", "m²", "Piece", "Box", "Slab")
            }
            // SANITARY
            "Sanitary" -> {
                listOf("Nos", "Set", "Unit", "Piece")
            }
            // PAINTING
            "Painting" -> {
                listOf("Litre", "Kg", "Sqft", "Sqmt", "m²", "Bucket")
            }
            // CARPENTRY (Woodwork)
            "Carpentry" -> {
                listOf("Sheet", "Nos", "Piece", "Sqft", "Rft", "Unit")
            }
            // GLASS & ALUMINIUM
            "Glass & Aluminium" -> {
                listOf("Sqft", "Sqmt", "m²", "Rft", "Piece", "Nos", "Set")
            }
            // FALSE CEILING
            "False Ceiling" -> {
                listOf("Sqft", "Sqmt", "m²", "Piece", "Box")
            }
            // HARDWARE
            "Hardware" -> {
                listOf("Piece", "Nos", "Set", "Box", "Packet")
            }
            // WATERPROOFING
            "Waterproofing" -> {
                listOf("Litre", "Kg", "Sqft", "Sqmt", "m²", "Unit", "Set")
            }
            // DEFAULT → return ALL UOMs
            else -> {
                val allUOMs = setOf(
                    // Raw material
                    "KG", "Tonne", "m³", "Cft", "Bag", "Litre",
                    // Labour
                    "Per Day", "Per Hour", "Nos", "Unit",
                    // Machines
                    "Per Trip",
                    // Electrical
                    "m", "Rft", "Piece", "Roll", "Set", "Box", "Bundle", "m²", "Sqft", "Sqmt",
                    // Plumbing
                    "Kg",
                    // Flooring
                    "Sheet", "Slab", "Bucket", "Packet"
                )
                allUOMs.sorted()
            }
        }
    }
    
    fun getItemsForType(itemType: String): List<String> {
        return when (itemType) {
            "Raw material" -> rawMaterialItems.keys.toList()
            "Labour" -> labourItems.keys.toList()
            "Machines & eq" -> machinesItems.keys.toList()
            "Electrical" -> electricalItems.keys.toList()
            "Plumbing" -> plumbingItems.keys.toList()
            "Flooring" -> flooringItems.keys.toList()
            "Tiles & Granite" -> tilesGraniteItems.keys.toList()
            "Sanitary" -> sanitaryItems.keys.toList()
            "Painting" -> paintingItems.keys.toList()
            "Carpentry" -> carpentryItems.keys.toList()
            "Glass & Aluminium" -> glassAluminiumItems.keys.toList()
            "False Ceiling" -> falseCeilingItems.keys.toList()
            "Hardware" -> hardwareItems.keys.toList()
            "Waterproofing" -> waterproofingItems.keys.toList()
            else -> emptyList()
        }
    }
    
    fun getSpecsForItem(itemType: String, item: String): List<String> {
        return when (itemType) {
            "Raw material" -> rawMaterialItems[item] ?: emptyList()
            "Labour" -> labourItems[item] ?: emptyList()
            "Machines & eq" -> machinesItems[item] ?: emptyList()
            "Electrical" -> electricalItems[item] ?: emptyList()
            "Plumbing" -> plumbingItems[item] ?: emptyList()
            "Flooring" -> flooringItems[item] ?: emptyList()
            "Tiles & Granite" -> tilesGraniteItems[item] ?: emptyList()
            "Sanitary" -> sanitaryItems[item] ?: emptyList()
            "Painting" -> paintingItems[item] ?: emptyList()
            "Carpentry" -> carpentryItems[item] ?: emptyList()
            "Glass & Aluminium" -> glassAluminiumItems[item] ?: emptyList()
            "False Ceiling" -> falseCeilingItems[item] ?: emptyList()
            "Hardware" -> hardwareItems[item] ?: emptyList()
            "Waterproofing" -> waterproofingItems[item] ?: emptyList()
            else -> emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentLineItemsSection(
    contractorMode: ContractorMode,
    onContractorModeChange: (ContractorMode) -> Unit,
    lineItems: List<LineItem>,
    onLineItemsChange: (List<LineItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLineItemsPalette()
    var cachedLineItems by remember { mutableStateOf(lineItems) }

    LaunchedEffect(lineItems, contractorMode) {
        if (contractorMode != ContractorMode.TURNKEY_AMOUNT_ONLY && lineItems != cachedLineItems) {
            cachedLineItems = lineItems
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Contractor Mode Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = palette.tier3Field),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Contractor Mode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Labour-Only Option
                    ContractorModeCard(
                        title = "Labour-Only",
                        isSelected = contractorMode == ContractorMode.LABOUR_ONLY,
                        onClick = {
                            // When switching to LABOUR_ONLY, filter out Labour items
                            if (contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) {
                                val restoredItems = cachedLineItems.filter { it.itemType != "Labour" }
                                onLineItemsChange(restoredItems)
                            } else if (contractorMode != ContractorMode.LABOUR_ONLY) {
                                val filteredItems = lineItems.filter { it.itemType != "Labour" }
                                onLineItemsChange(filteredItems)
                            }
                            onContractorModeChange(ContractorMode.LABOUR_ONLY)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Self Execution Option
                    ContractorModeCard(
                        title = "Self Execution",
                        isSelected = contractorMode == ContractorMode.SELF_EXECUTION,
                        onClick = {
                            if (contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) {
                                onLineItemsChange(cachedLineItems)
                            }
                            onContractorModeChange(ContractorMode.SELF_EXECUTION)
                        },
                        modifier = Modifier.weight(1f)
                    )

                     // Turnkey (contractor-only) Option
                     ContractorModeCard(
                          title = "Turnkey",
                          isSelected = contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY,
                          onClick = {
                              if (contractorMode != ContractorMode.TURNKEY_AMOUNT_ONLY) {
                                  cachedLineItems = lineItems
                                  onLineItemsChange(emptyList())
                              }
                              onContractorModeChange(ContractorMode.TURNKEY_AMOUNT_ONLY)
                          },
                          modifier = Modifier.weight(1f)
                       )
                  }
              }
          }
          
          // Line Items Section (not applicable for Turnkey)
          if (contractorMode != ContractorMode.TURNKEY_AMOUNT_ONLY) Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Line Items (sum must equal Department Budget)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = palette.textPrimary
            )
            
            // Line Items List
            lineItems.forEachIndexed { index, lineItem ->
                LineItemRow(
                    lineItem = lineItem,
                    contractorMode = contractorMode,
                    palette = palette,
                    onLineItemChange = { updatedItem ->
                        val updatedList = lineItems.toMutableList()
                        updatedList[index] = updatedItem
                        onLineItemsChange(updatedList)
                    },
                    onDelete = {
                        val updatedList = lineItems.toMutableList()
                        updatedList.removeAt(index)
                        onLineItemsChange(updatedList)
                    }
                )
            }
            
                // Add Row Button
                Button(
                onClick = {
                    val newItem = LineItem()
                    onLineItemsChange(lineItems + newItem)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.textPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add row")
            }
            
            // Total Budget Display
            if (lineItems.isNotEmpty()) {
                val totalBudget = lineItems.sumOf { it.total }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = FormatUtils.formatCurrencyWithoutDecimals(totalBudget),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.success
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractorModeCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLineItemsPalette()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) palette.tier3Field else palette.tier2Surface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) palette.accent else palette.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineItemRow(
    lineItem: LineItem,
    contractorMode: ContractorMode,
    palette: LineItemsPalette,
    onLineItemChange: (LineItem) -> Unit,
    onDelete: () -> Unit
) {
    var expandedItemType by remember { mutableStateOf(false) }
    var expandedItem by remember { mutableStateOf(false) }
    var expandedSpec by remember { mutableStateOf(false) }
    var expandedUom by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.tier2Surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Item Type and Item + Spec Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Item Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedItemType,
                    onExpandedChange = { expandedItemType = !expandedItemType },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = lineItem.itemType.ifEmpty { "Item Type" },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Item Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItemType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        colors = lineItemTextFieldColors(palette)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedItemType,
                        onDismissRequest = { expandedItemType = false },
                        containerColor = palette.tier2Surface
                    ) {
                        // Filter item types based on contractor mode
                                 val availableItemTypes = when (contractorMode) {
                                     ContractorMode.LABOUR_ONLY -> ItemData.itemTypes.filter { it != "Labour" }
                                     ContractorMode.SELF_EXECUTION -> ItemData.itemTypes
                                     ContractorMode.TURNKEY_AMOUNT_ONLY -> emptyList()
                                 }
                        
                        availableItemTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = palette.textPrimary) },
                                onClick = {
                                    // Clear spec when switching item types, especially when selecting Labour
                                    onLineItemChange(
                                        lineItem.copy(
                                            itemType = type,
                                            item = "",
                                            spec = if (type == "Labour") "" else lineItem.spec
                                        )
                                    )
                                    expandedItemType = false
                                }
                            )
                        }
                    }
                }
                
                // Item Dropdown
                val availableItems = if (lineItem.itemType.isNotEmpty()) {
                    ItemData.getItemsForType(lineItem.itemType)
                } else {
                    emptyList()
                }
                
                ExposedDropdownMenuBox(
                    expanded = expandedItem,
                    onExpandedChange = { expandedItem = !expandedItem },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = lineItem.item.ifEmpty { "Item" },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Item") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItem) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = lineItem.itemType.isNotEmpty(),
                        colors = lineItemTextFieldColors(palette)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedItem,
                        onDismissRequest = { expandedItem = false },
                        containerColor = palette.tier2Surface
                    ) {
                        availableItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, color = palette.textPrimary) },
                                onClick = {
                                    onLineItemChange(
                                        lineItem.copy(
                                            item = item,
                                            spec = ""
                                        )
                                    )
                                    expandedItem = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Spec Dropdown - Only show if NOT Labour
            if (lineItem.itemType != "Labour") {
                val availableSpecs = if (lineItem.itemType.isNotEmpty() && lineItem.item.isNotEmpty()) {
                    ItemData.getSpecsForItem(lineItem.itemType, lineItem.item)
                } else {
                    emptyList()
                }
                
                ExposedDropdownMenuBox(
                    expanded = expandedSpec,
                    onExpandedChange = { expandedSpec = !expandedSpec },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = lineItem.spec.ifEmpty { "Spec" },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Spec") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpec) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = lineItem.item.isNotEmpty(),
                        colors = lineItemTextFieldColors(palette)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSpec,
                        onDismissRequest = { expandedSpec = false },
                        containerColor = palette.tier2Surface
                    ) {
                        availableSpecs.forEach { spec ->
                            DropdownMenuItem(
                                text = { Text(spec, color = palette.textPrimary) },
                                onClick = {
                                    onLineItemChange(lineItem.copy(spec = spec))
                                    expandedSpec = false
                                }
                            )
                        }
                    }
                }
            }
            
            // UOM Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedUom,
                onExpandedChange = { expandedUom = !expandedUom },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = lineItem.uom.ifEmpty { "UOM" },
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Unit of Measure") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUom) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = lineItemTextFieldColors(palette)
                )
                ExposedDropdownMenu(
                    expanded = expandedUom,
                    onDismissRequest = { expandedUom = false },
                    containerColor = palette.tier2Surface
                ) {
                    // Get UOM options based on item type
                    val availableUomOptions = if (lineItem.itemType.isNotEmpty()) {
                        ItemData.uomOptions(lineItem.itemType)
                    } else {
                        ItemData.uomOptions("")
                    }
                    
                    availableUomOptions.forEach { uom ->
                        DropdownMenuItem(
                            text = { Text(uom, color = palette.textPrimary) },
                            onClick = {
                                onLineItemChange(lineItem.copy(uom = uom))
                                expandedUom = false
                            }
                        )
                    }
                }
            }
            
            // Quantity, Unit Price, Total, and Delete Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity (or Members for Labour)
                OutlinedTextField(
                    value = if (lineItem.quantity > 0) lineItem.quantity.toString() else "",
                    onValueChange = { value ->
                        val qty = value.toDoubleOrNull() ?: 0.0
                        onLineItemChange(lineItem.copy(quantity = qty))
                    },
                    label = { Text(if (lineItem.itemType == "Labour") "Members" else "Quantity") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = lineItemTextFieldColors(palette)
                )
                
                // Unit Price (or UOM + Price for Labour)
                OutlinedTextField(
                    value = if (lineItem.unitPrice > 0) lineItem.unitPrice.toString() else "",
                    onValueChange = { value ->
                        val price = value.toDoubleOrNull() ?: 0.0
                        onLineItemChange(lineItem.copy(unitPrice = price))
                    },
                    label = { 
                        Text(
                            if (lineItem.itemType == "Labour") {
                                if (lineItem.uom.isNotEmpty()) {
                                    "${lineItem.uom} Price"
                                } else {
                                    "UOM Price"
                                }
                            } else {
                                "Unit Price"
                            }
                        )
                    },
                    prefix = { Text("₹") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = lineItemTextFieldColors(palette)
                )
                
                // Total
                Text(
                    text = FormatUtils.formatCurrencyWithoutDecimals(lineItem.total),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.success,
                    modifier = Modifier.weight(1f)
                )
                
                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = palette.danger,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

