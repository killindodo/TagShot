package com.tagshot.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tagshot.app.data.TagWithCount
import com.tagshot.app.domain.Limits
import com.tagshot.app.domain.TagNormalizer
import com.tagshot.app.domain.TagResult
import com.tagshot.app.ui.components.TagBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerScreen(
    viewModel: PhotoViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val allTags = (uiState as? MainUiState.Success)?.allTags ?: emptyList()
    val snackbarHostState = remember { SnackbarHostState() }

    var tagToRename by remember { mutableStateOf<TagWithCount?>(null) }
    var tagToDelete by remember { mutableStateOf<TagWithCount?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is UiEvent.ShowMessage) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tag Manager") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (allTags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No tags yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tags you add to photos will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = allTags,
                    key = { it.id }
                ) { tag ->
                    TagManagerItem(
                        tag = tag,
                        onRenameClick = { tagToRename = tag },
                        onDeleteClick = { tagToDelete = tag }
                    )
                }
            }
        }
    }

    // Rename dialog
    tagToRename?.let { tag ->
        RenameTagDialog(
            tag = tag,
            allTags = allTags,
            onDismiss = { tagToRename = null },
            onConfirm = { newName ->
                viewModel.renameTag(tag.id, newName)
                tagToRename = null
            }
        )
    }

    // Delete confirmation dialog
    tagToDelete?.let { tag ->
        DeleteTagDialog(
            tag = tag,
            onDismiss = { tagToDelete = null },
            onConfirm = {
                viewModel.deleteTag(tag.id)
                tagToDelete = null
            }
        )
    }
}

@Composable
private fun TagManagerItem(
    tag: TagWithCount,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TagBadge(
                normalizedName = tag.normalizedName,
                displayName = tag.name
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${tag.photoCount} photo${if (tag.photoCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRenameClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename tag ${tag.name}"
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete tag ${tag.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RenameTagDialog(
    tag: TagWithCount,
    allTags: List<TagWithCount>,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var text by remember { mutableStateOf(tag.name) }
    val normResult = remember(text) { TagNormalizer.normalize(text) }
    val isValid = normResult is TagResult.Valid

    val mergeTarget = remember(normResult, allTags) {
        if (normResult is TagResult.Valid) {
            allTags.find { it.id != tag.id && it.normalizedName == normResult.normalized }
        } else null
    }

    val isChanged = if (normResult is TagResult.Valid) {
        normResult.display != tag.name || normResult.normalized != tag.normalizedName
    } else false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Tag") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= Limits.MAX_TAG_CODEPOINTS) {
                            text = it
                        }
                    },
                    label = { Text("Tag Name") },
                    singleLine = true,
                    isError = !isValid && text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (mergeTarget != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tag #${mergeTarget.name} already exists. Renaming will merge this tag into #${mergeTarget.name}.",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (!isValid && text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Invalid tag name (must be 1–${Limits.MAX_TAG_CODEPOINTS} characters)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = isValid && isChanged
            ) {
                Text(if (mergeTarget != null) "Merge" else "Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteTagDialog(
    tag: TagWithCount,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Tag") },
        text = {
            Text("Remove #${tag.name} from ${tag.photoCount} photo${if (tag.photoCount == 1) "" else "s"}? Photos are not deleted.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
