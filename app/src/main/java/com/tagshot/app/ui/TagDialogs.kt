package com.tagshot.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tagshot.app.data.TagEntity
import com.tagshot.app.data.TagWithCount
import com.tagshot.app.domain.Limits
import com.tagshot.app.domain.ParseResult
import com.tagshot.app.domain.TagParser
import com.tagshot.app.ui.components.TagBadge
import com.tagshot.app.ui.components.springDialogEntrance

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagInputDialog(
    title: String,
    initialText: String = "",
    allExistingTags: List<TagWithCount> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (rawTags: String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    val parseResult = remember(text) { TagParser.parse(text) }
    val isValid = parseResult is ParseResult.Success

    // Extract the active prefix being typed to suggest tags
    val currentPrefix = remember(text) {
        val lastPart = text.split(',', '\n').lastOrNull()?.trim() ?: ""
        lastPart.removePrefix("#").lowercase()
    }

    val suggestedTags = remember(currentPrefix, allExistingTags) {
        if (currentPrefix.isBlank()) {
            allExistingTags.take(6)
        } else {
            allExistingTags.filter { it.normalizedName.startsWith(currentPrefix) }.take(6)
        }
    }

    AlertDialog(
        modifier = Modifier.springDialogEntrance(),
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Tags (comma or newline separated)") },
                    placeholder = { Text("receipt, office, 2026") },
                    isError = !isValid && text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error message
                when (parseResult) {
                    is ParseResult.Failure.TooLong -> {
                        Text(
                            text = "Tag too long (max ${Limits.MAX_TAG_CODEPOINTS} chars): ${parseResult.rawText}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    is ParseResult.Failure.TooMany -> {
                        Text(
                            text = "Too many tags (max ${Limits.MAX_TAGS_PER_PHOTO})",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live Preview Chips
                if (parseResult is ParseResult.Success && parseResult.tags.isNotEmpty()) {
                    Text("Preview:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (tag in parseResult.tags) {
                            TagBadge(normalizedName = tag.normalized, displayName = tag.display)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Quick-suggest Chips from Existing Tags
                if (suggestedTags.isNotEmpty()) {
                    Text("Suggestions:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (suggestion in suggestedTags) {
                            AssistChip(
                                onClick = {
                                    val parts = text.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }
                                    val newParts = if (currentPrefix.isNotBlank()) {
                                        parts.dropLast(1) + suggestion.name
                                    } else {
                                        parts + suggestion.name
                                    }
                                    text = newParts.joinToString(", ") + ", "
                                },
                                label = { Text("#${suggestion.name}") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = isValid
            ) {
                Text("Confirm")
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
fun ConfirmDeleteDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.springDialogEntrance(),
        onDismissRequest = onDismiss,
        title = { Text("Delete Photos") },
        text = {
            Text(
                if (count == 1) "Delete this photo? You will have 8 seconds to undo."
                else "Delete $count photos? You will have 8 seconds to undo."
            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemoveTagFromSelectedDialog(
    distinctTags: List<TagEntity>,
    onDismiss: () -> Unit,
    onTagSelected: (tagId: Long) -> Unit
) {
    AlertDialog(
        modifier = Modifier.springDialogEntrance(),
        onDismissRequest = onDismiss,
        title = { Text("Remove Tag from Selected") },
        text = {
            if (distinctTags.isEmpty()) {
                Text("Selected photos have no tags.")
            } else {
                Column {
                    Text(
                        "Select a tag to remove from all selected photos:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (tag in distinctTags) {
                            AssistChip(
                                onClick = { onTagSelected(tag.id) },
                                label = { Text("#${tag.name}") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
