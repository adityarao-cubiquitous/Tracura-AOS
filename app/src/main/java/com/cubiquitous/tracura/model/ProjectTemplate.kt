package com.cubiquitous.tracura.model

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

/**
 * Project template with predefined phases and departments
 */
data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val phases: List<PhaseDraft>,
    val phaseCount: Int = phases.size,
    val departmentCount: Int = phases.sumOf { it.departments.size },
    val businessType: String? = null // "Construction", "Interior Design", or "Media"
)

/**
 * Predefined project templates
 */
object ProjectTemplates {
    val templates = listOf(
        ProjectTemplate(
            id = "residential_building",
            name = "Residential Building",
            description = "Standard template for residential building construction with common phases and departments",
            icon = Icons.Default.Home,
            businessType = "Construction",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Foundation & Structure",
                    start = run {
                        val cal = Calendar.getInstance()
                        cal.set(2025, Calendar.DECEMBER, 5)
                        cal.time
                    },
                    end = run {
                        val cal = Calendar.getInstance()
                        cal.set(2026, Calendar.MARCH, 5)
                        cal.time
                    },
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Civil",
                            contractorMode = ContractorMode.LABOUR_ONLY,
                            lineItems = mutableListOf(
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Cement OPC 53",
                                    spec = "",
                                    quantity = 500.0,
                                    unitPrice = 380.0,
                                    uom = "Bag"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Cement OPC 43",
                                    spec = "",
                                    quantity = 200.0,
                                    unitPrice = 360.0,
                                    uom = "Bag"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Cement PPC",
                                    spec = "",
                                    quantity = 100.0,
                                    unitPrice = 350.0,
                                    uom = "Bag"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Steel Fe500",
                                    spec = "6 mm",
                                    quantity = 10.0,
                                    unitPrice = 58000.0,
                                    uom = "MT"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Steel Fe500",
                                    spec = "8 mm",
                                    quantity = 15.0,
                                    unitPrice = 59000.0,
                                    uom = "MT"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Steel Fe500",
                                    spec = "10 mm",
                                    quantity = 20.0,
                                    unitPrice = 60000.0,
                                    uom = "MT"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Steel Fe500",
                                    spec = "12 mm",
                                    quantity = 25.0,
                                    unitPrice = 62000.0,
                                    uom = "MT"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Steel Fe500",
                                    spec = "16 mm",
                                    quantity = 30.0,
                                    unitPrice = 64000.0,
                                    uom = "MT"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Steel Fe500",
                                    spec = "20 mm",
                                    quantity = 35.0,
                                    unitPrice = 66000.0,
                                    uom = "MT"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Steel Fe500",
                                    spec = "25 mm",
                                    quantity = 40.0,
                                    unitPrice = 68000.0,
                                    uom = "MT"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Coarse Aggregate",
                                    spec = "20 mm",
                                    quantity = 500.0,
                                    unitPrice = 1200.0,
                                    uom = "MT"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Fine Aggregate",
                                    spec = "River Sand",
                                    quantity = 400.0,
                                    unitPrice = 1500.0,
                                    uom = "MT"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Bricks",
                                    spec = "Red Clay",
                                    quantity = 50000.0,
                                    unitPrice = 8.0,
                                    uom = "Nos"
                                )
                            )
                        ),
                        DepartmentDraft(
                            departmentName = "Electrical",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf(
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Electrical Wire",
                                    spec = "2.5 sqmm",
                                    quantity = 2000.0,
                                    unitPrice = 120.0,
                                    uom = "Meter"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Electrical Wire",
                                    spec = "4 sqmm",
                                    quantity = 1500.0,
                                    unitPrice = 180.0,
                                    uom = "Meter"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "MCB",
                                    spec = "16A",
                                    quantity = 50.0,
                                    unitPrice = 800.0,
                                    uom = "Nos"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Switch & Socket",
                                    spec = "Modular",
                                    quantity = 100.0,
                                    unitPrice = 150.0,
                                    uom = "Nos"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Conduit Pipe",
                                    spec = "20mm",
                                    quantity = 500.0,
                                    unitPrice = 45.0,
                                    uom = "Meter"
                                )
                            )
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Finishing & Interior",
                    start = run {
                        val cal = Calendar.getInstance()
                        cal.set(2026, Calendar.MARCH, 6)
                        cal.time
                    },
                    end = run {
                        val cal = Calendar.getInstance()
                        cal.set(2026, Calendar.JUNE, 3)
                        cal.time
                    },
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Civil",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf(
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Tiles",
                                    spec = "Ceramic 600x600",
                                    quantity = 500.0,
                                    unitPrice = 45.0,
                                    uom = "Sqft"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Tiles",
                                    spec = "Vitrified 600x600",
                                    quantity = 300.0,
                                    unitPrice = 65.0,
                                    uom = "Sqft"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Paint",
                                    spec = "Emulsion",
                                    quantity = 200.0,
                                    unitPrice = 350.0,
                                    uom = "Liter"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Paint",
                                    spec = "Primer",
                                    quantity = 100.0,
                                    unitPrice = 280.0,
                                    uom = "Liter"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Putty",
                                    spec = "Wall Putty",
                                    quantity = 150.0,
                                    unitPrice = 180.0,
                                    uom = "Kg"
                                )
                            )
                        ),
                        DepartmentDraft(
                            departmentName = "Plumbing",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf(
                                LineItem(
                                    itemType = "Raw material",
                                    item = "CPVC Pipe",
                                    spec = "1/2 inch",
                                    quantity = 200.0,
                                    unitPrice = 120.0,
                                    uom = "Meter"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "CPVC Pipe",
                                    spec = "3/4 inch",
                                    quantity = 150.0,
                                    unitPrice = 180.0,
                                    uom = "Meter"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Fitting",
                                    spec = "Elbow",
                                    quantity = 100.0,
                                    unitPrice = 25.0,
                                    uom = "Nos"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Fitting",
                                    spec = "Tee",
                                    quantity = 50.0,
                                    unitPrice = 35.0,
                                    uom = "Nos"
                                )
                            )
                        ),
                        DepartmentDraft(
                            departmentName = "Electrical",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf(
                                LineItem(
                                    itemType = "Raw material",
                                    item = "LED Light",
                                    spec = "9W",
                                    quantity = 50.0,
                                    unitPrice = 450.0,
                                    uom = "Nos"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "LED Light",
                                    spec = "12W",
                                    quantity = 30.0,
                                    unitPrice = 650.0,
                                    uom = "Nos"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Fan",
                                    spec = "Ceiling Fan",
                                    quantity = 20.0,
                                    unitPrice = 2500.0,
                                    uom = "Nos"
                                ),
                                LineItem(
                                    itemType = "Raw material",
                                    item = "Switch & Socket",
                                    spec = "Modular",
                                    quantity = 80.0,
                                    unitPrice = 150.0,
                                    uom = "Nos"
                                )
                            )
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "renovation",
            name = "Renovation",
            description = "Template for building renovation and remodeling projects",
            icon = Icons.Default.Hardware,
            businessType = "Construction",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Demolition & Prep",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Demolition",
                            contractorMode = ContractorMode.LABOUR_ONLY,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Waste Removal",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Site Preparation",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Renovation Work",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Structural Repair",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Electrical Upgrade",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Plumbing Upgrade",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Interior Finishing",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "commercial_office",
            name = "Commercial Office",
            description = "Template for commercial office space construction and fit-out",
            icon = Icons.Default.Apartment,
            businessType = "Construction",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Core & Shell",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Structure",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "MEP Services",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Elevator",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Fire Safety",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "HVAC",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Security Systems",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Interior Fit-out",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Partitioning",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "False Ceiling",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Flooring",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Lighting",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Furniture",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "IT Infrastructure",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "road_infrastructure",
            name = "Road Infrastructure",
            description = "Template for road construction and infrastructure projects",
            icon = Icons.Default.Route,
            businessType = "Construction",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Earthwork & Preparation",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Excavation",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Grading",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Drainage",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Pavement & Finishing",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Base Course",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Asphalt/Concrete",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Markings & Signage",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Landscaping",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        // Interior Design Templates - Updated with correct IDs
        ProjectTemplate(
            id = "interior_design_residential",
            name = "Residential Interior Design",
            description = "Comprehensive interior design template for 3BHK residential apartment including woodwork, false ceiling, and finishing.",
            icon = Icons.Default.Home,
            businessType = "Interior Design",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Design & Civil Changes",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Design Studio",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Civil Work",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Woodwork & False Ceiling",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Carpentry",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "False Ceiling",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Finishing & Handover",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Paint & Polish",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Electrical & Decor",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "interior_design_commercial_office",
            name = "Commercial Office Interior",
            description = "Complete interior design solution for modern commercial office spaces including workstations, meeting rooms, and common areas.",
            icon = Icons.Default.Apartment,
            businessType = "Interior Design",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Design & Planning",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Space Planning",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Design Development",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Execution",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Furniture",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Lighting & Electrical",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Flooring & Wall Coverings",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "interior_design_restaurant",
            name = "Restaurant Interior Design",
            description = "Complete interior design for restaurant/cafe including dining area, kitchen layout, and ambiance creation.",
            icon = Icons.Default.Restaurant,
            businessType = "Interior Design",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Design & Planning",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Design",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Material Selection",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Execution",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Furniture & Fixtures",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Lighting",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Accessories",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "interior_design_luxury_villa",
            name = "Luxury Villa Interior",
            description = "Premium interior design for luxury villas including high-end finishes, custom furniture, and smart home integration.",
            icon = Icons.Default.Home,
            businessType = "Interior Design",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Design & Planning",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Design",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Material Selection",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Execution",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Furniture & Fixtures",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Lighting",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Accessories",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        // Interior Design Templates - Updated with correct IDs
        ProjectTemplate(
            id = "interior_design_residential",
            name = "Residential Interior Design",
            description = "Comprehensive interior design template for 3BHK residential apartment including woodwork, false ceiling, and finishing.",
            icon = Icons.Default.Home,
            businessType = "Interior Design",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Design & Civil Changes",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Design Studio",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Civil Work",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Woodwork & False Ceiling",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Carpentry",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "False Ceiling",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Finishing & Handover",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Paint & Polish",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Electrical & Decor",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "interior_design_commercial_office",
            name = "Commercial Office Interior",
            description = "Complete interior design solution for modern commercial office spaces including workstations, meeting rooms, and common areas.",
            icon = Icons.Default.Apartment,
            businessType = "Interior Design",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Design & Planning",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Space Planning",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Design Development",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Execution",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Furniture",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Lighting & Electrical",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Flooring & Wall Coverings",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "interior_design_restaurant",
            name = "Restaurant Interior Design",
            description = "Complete interior design for restaurant/cafe including dining area, kitchen layout, and ambiance creation.",
            icon = Icons.Default.Restaurant,
            businessType = "Interior Design",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Design & Planning",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Design",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Material Selection",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Execution",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Furniture & Fixtures",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Lighting",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Accessories",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "interior_design_luxury_villa",
            name = "Luxury Villa Interior",
            description = "Premium interior design for luxury villas including high-end finishes, custom furniture, and smart home integration.",
            icon = Icons.Default.Home,
            businessType = "Interior Design",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Design & Planning",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Design",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Material Selection",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Execution",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Furniture & Fixtures",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Lighting",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Accessories",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        // Media Templates - Updated with correct IDs
        ProjectTemplate(
            id = "media_production_ad_film",
            name = "Ad Film Production",
            description = "Template for TV Commercial/ Digital Ad Film production including pre-production, shoot, and post-production.",
            icon = Icons.Default.Movie,
            businessType = "Media",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Pre-Production",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Creative & Planning",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Production (Shoot)",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Camera & Lighting",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Crew & Talent",
                            contractorMode = ContractorMode.LABOUR_ONLY,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Art Department",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Post-Production",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Edit & DI",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Sound Design",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "media_corporate_video",
            name = "Corporate Video Production",
            description = "Professional corporate video production including company profile, product showcase, and testimonial videos.",
            icon = Icons.Default.Videocam,
            businessType = "Media",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Pre-Production",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Creative & Scripting",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Production",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Camera & Equipment",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Crew",
                            contractorMode = ContractorMode.LABOUR_ONLY,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Post-Production",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Editing",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Audio & Finalization",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        ),
        ProjectTemplate(
            id = "media_event_coverage",
            name = "Event Photography & Videography",
            description = "Complete event coverage package including photography, videography, and live streaming for corporate events, weddings, and conferences.",
            icon = Icons.Default.Camera,
            businessType = "Media",
            phases = listOf(
                PhaseDraft(
                    phaseName = "Pre-Event Planning",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Planning & Coordination",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Event Coverage",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Photography Team",
                            contractorMode = ContractorMode.LABOUR_ONLY,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Videography Team",
                            contractorMode = ContractorMode.LABOUR_ONLY,
                            lineItems = mutableListOf()
                        ),
                        DepartmentDraft(
                            departmentName = "Live Streaming",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                ),
                PhaseDraft(
                    phaseName = "Post-Event Delivery",
                    departments = mutableListOf(
                        DepartmentDraft(
                            departmentName = "Photo & Video Editing",
                            contractorMode = ContractorMode.SELF_EXECUTION,
                            lineItems = mutableListOf()
                        )
                    )
                )
            )
        )
    )
}

object ProjectTemplateMetadataService {
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    suspend fun getTemplates(businessType: String?): List<ProjectTemplate> {
        return try {
            val cloudTemplates = fetchTemplates(businessType)
            cloudTemplates.ifEmpty { localTemplates(businessType) }
        } catch (error: Exception) {
            android.util.Log.w(
                "TemplateMetadataService",
                "Falling back to bundled templates: ${error.message}",
                error
            )
            localTemplates(businessType)
        }
    }

    suspend fun getTemplate(templateId: String): ProjectTemplate? {
        return try {
            val document = firestore.collection("metadata")
                .document("projectTemplates")
                .collection("templates")
                .document(templateId)
                .get()
                .await()

            document.data?.takeIf { it["isActive"] as? Boolean != false }?.toProjectTemplate(document.id)
                ?: ProjectTemplates.templates.find { it.id == templateId }
        } catch (error: Exception) {
            android.util.Log.w(
                "TemplateMetadataService",
                "Falling back to bundled template $templateId: ${error.message}",
                error
            )
            ProjectTemplates.templates.find { it.id == templateId }
        }
    }

    private suspend fun fetchTemplates(businessType: String?): List<ProjectTemplate> {
        val snapshot = firestore.collection("metadata")
            .document("projectTemplates")
            .collection("templates")
            .whereEqualTo("isActive", true)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.data?.toProjectTemplate(document.id)
        }.filter { template ->
            businessType.isNullOrBlank() || normalizeBusinessType(template.businessType) == normalizeBusinessType(businessType)
        }.sortedWith(compareBy<ProjectTemplate> { it.businessType.orEmpty() }.thenBy { it.name.lowercase() })
    }

    private fun localTemplates(businessType: String?): List<ProjectTemplate> {
        return if (businessType.isNullOrBlank()) {
            ProjectTemplates.templates
        } else {
            ProjectTemplates.templates.filter {
                normalizeBusinessType(it.businessType) == normalizeBusinessType(businessType)
            }
        }
    }

    private fun Map<String, Any>.toProjectTemplate(documentId: String): ProjectTemplate? {
        val id = stringValue("id").ifBlank { documentId }
        val title = stringValue("title").ifBlank { return null }
        val description = stringValue("description")
        val phasesData = this["phases"] as? List<*> ?: return null
        val phases = phasesData.mapNotNull { (it as? Map<*, *>)?.toPhaseDraft() }

        return ProjectTemplate(
            id = id,
            name = title,
            description = description,
            icon = iconValue(stringValue("icon")),
            phases = phases,
            businessType = this["businessType"] as? String
        )
    }

    private fun Map<*, *>.toPhaseDraft(): PhaseDraft? {
        val phaseName = this["phaseName"] as? String ?: return null
        val departmentsData = this["departments"] as? List<*> ?: return null
        val departments = departmentsData.mapNotNull { (it as? Map<*, *>)?.toDepartmentDraft() }.toMutableList()

        return PhaseDraft(
            phaseName = phaseName,
            start = dateFromOffset(numberValue("startDateDays").toInt()),
            end = dateFromOffset(numberValue("endDateDays").toInt()),
            departments = departments
        )
    }

    private fun Map<*, *>.toDepartmentDraft(): DepartmentDraft? {
        val name = this["name"] as? String ?: return null
        val lineItemsData = this["lineItems"] as? List<*> ?: emptyList<Any>()
        val lineItems = lineItemsData.mapNotNull { (it as? Map<*, *>)?.toLineItem() }.toMutableList()

        return DepartmentDraft(
            departmentName = name,
            contractorMode = contractorModeValue(this["contractorMode"] as? String),
            lineItems = lineItems
        )
    }

    private fun Map<*, *>.toLineItem(): LineItem? {
        val itemType = stringValue("itemType")
        val item = stringValue("item")
        if (itemType.isBlank() || item.isBlank()) return null

        return LineItem(
            itemType = itemType,
            item = item,
            spec = stringValue("spec"),
            quantity = numberValue("quantity"),
            unitPrice = numberValue("unitPrice"),
            uom = stringValue("uom")
        )
    }

    private fun Map<*, *>.stringValue(key: String): String {
        return when (val value = this[key]) {
            is String -> value
            is Number -> value.toString()
            null -> ""
            else -> value.toString()
        }
    }

    private fun Map<*, *>.numberValue(key: String): Double {
        return when (val value = this[key]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun dateFromOffset(days: Int): Date {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days)
        }.time
    }

    private fun contractorModeValue(value: String?): ContractorMode {
        return when (value?.trim()?.lowercase()) {
            "self execution", "self-execution" -> ContractorMode.SELF_EXECUTION
            "turnkey", "turnkey amount only", "turnkey_amount_only" -> ContractorMode.TURNKEY_AMOUNT_ONLY
            else -> ContractorMode.LABOUR_ONLY
        }
    }

    private fun iconValue(value: String): ImageVector {
        return when (value.trim().lowercase()) {
            "house.fill", "home", "house" -> Icons.Default.Home
            "building.2.fill", "building", "business" -> Icons.Default.Business
            "road.lanes", "road" -> Icons.Default.Route
            "paintbrush.fill", "paintbrush" -> Icons.Default.Brush
            "video.fill", "video", "movie" -> Icons.Default.Movie
            "camera.fill", "camera" -> Icons.Default.PhotoCamera
            else -> Icons.Default.Description
        }
    }

    private fun normalizeBusinessType(value: String?): String {
        return value.orEmpty()
            .trim()
            .lowercase()
            .replace("-", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }
}
