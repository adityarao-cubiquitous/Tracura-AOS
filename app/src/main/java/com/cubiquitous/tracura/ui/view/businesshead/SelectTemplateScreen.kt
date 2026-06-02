package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.ProjectTemplate
import com.cubiquitous.tracura.model.ProjectTemplateMetadataService
import com.cubiquitous.tracura.repository.ProjectRepository
import com.cubiquitous.tracura.viewmodel.ProjectViewModel
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectTemplateScreen(
    onDismiss: () -> Unit,
    onTemplateSelected: (ProjectTemplate) -> Unit,
    onCreateNew: () -> Unit,
    projectRepository: ProjectRepository,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf<String?>(null) }
    var isLoadingBusinessType by remember { mutableStateOf(true) }
    var allTemplates by remember { mutableStateOf<List<ProjectTemplate>>(emptyList()) }
    var isLoadingTemplates by remember { mutableStateOf(true) }
    
    // Fetch customer's businessType
    LaunchedEffect(Unit) {
        try {
            businessType = projectRepository.getCustomerBusinessType()
            Log.d("SelectTemplateScreen", "📊 Customer business type: $businessType")
        } catch (e: Exception) {
            Log.e("SelectTemplateScreen", "❌ Error fetching business type: ${e.message}")
        } finally {
            isLoadingBusinessType = false
        }
    }

    LaunchedEffect(businessType, isLoadingBusinessType) {
        if (isLoadingBusinessType) {
            Log.d("SelectTemplateScreen", "⏳ Loading business type, showing empty list")
            allTemplates = emptyList()
            return@LaunchedEffect
        }

        isLoadingTemplates = true
        allTemplates = ProjectTemplateMetadataService.getTemplates(businessType)
        Log.d("SelectTemplateScreen", "✅ Loaded ${allTemplates.size} templates for business type: $businessType")
        allTemplates.forEach { template ->
            Log.d("SelectTemplateScreen", "  - ${template.name} (${template.id})")
        }
        isLoadingTemplates = false
    }
    
    val filteredTemplates = remember(searchQuery, allTemplates) {
        if (searchQuery.isBlank()) {
            allTemplates
        } else {
            allTemplates.filter { template ->
                template.name.contains(searchQuery, ignoreCase = true) ||
                template.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Cancel",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 17.sp
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onCreateNew) {
                        Text(
                            "Create New",
                            color = colorScheme.primary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        containerColor = colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
        ) {
            // Select Template Header
            Text(
                text = "Select Template",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )

            // Search Bar - merged with background
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search templates...", color = colorScheme.onSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.background,
                    unfocusedContainerColor = colorScheme.background,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            // Template List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoadingBusinessType || isLoadingTemplates) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                items(filteredTemplates) { template ->
                    TemplateCard(
                        template = template,
                        onClick = {
                            onTemplateSelected(template)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: ProjectTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            colorScheme.primary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (template.id == "road_infrastructure") {
                        Image(
                            painter = painterResource(id = R.drawable.ic_road_infrastructure),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            colorFilter = ColorFilter.tint(colorScheme.onPrimary)
                        )
                    } else {
                        Icon(
                            imageVector = template.icon,
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = template.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    Text(
                        text = template.description,
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                // Arrow Head Icon - aligned to top
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Divider line above phases and departments - full width
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colorScheme.outlineVariant)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Details Column - Department and Phase below, completely left-aligned
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Phases
                Row(
                    modifier = Modifier.wrapContentWidth(Alignment.Start),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${template.phaseCount} Phases",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Departments
                Row(
                    modifier = Modifier.wrapContentWidth(Alignment.Start),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${template.departmentCount} Departments",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

