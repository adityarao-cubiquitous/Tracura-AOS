package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.ProjectDraft
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftProjectsScreen(
    onNavigateBack: () -> Unit,
    onDraftSelected: (ProjectDraft) -> Unit = {},
    viewModel: BusinessHeadViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val drafts by viewModel.firestoreDrafts.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFirestoreDrafts()
    }

    fun deleteDraft(draftId: String) {
        viewModel.deleteFirestoreDraft(draftId)
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    Text(
                        text = "Draft Projects",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (drafts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No draft projects",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(drafts, key = { it.id }) { draft ->
                    DraftProjectCard(
                        draft = draft,
                        onDraftClick = {
                            android.util.Log.d("DraftProjectsScreen", "🖱️ Draft clicked: ${draft.id}, Name: ${draft.projectName}")
                            onDraftSelected(draft)
                        },
                        onDeleteClick = {
                            deleteDraft(draft.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftProjectCard(
    draft: ProjectDraft,
    onDraftClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDraftClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document icon
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Draft details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = draft.projectName.ifEmpty { "Draft project name" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (draft.client.isNotEmpty()) {
                    Text(
                        text = "Client: ${draft.client}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val locationText = if (draft.location.isNotEmpty()) draft.location else "Not specified"
                val phaseCount = draft.phases.size
                val phaseText = if (phaseCount == 1) "1 phase" else "$phaseCount phases"
                Text(
                    text = "Location: $locationText • $phaseText",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Last updated time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatTimeAgo(draft.lastSaved),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete icon
            IconButton(
                onClick = onDeleteClick
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "${diff / 1000} seconds ago"
        diff < 3600000 -> "${diff / 60000} minutes ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        diff < 604800000 -> "${diff / 86400000} days ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
