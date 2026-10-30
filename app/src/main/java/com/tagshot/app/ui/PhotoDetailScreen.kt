package com.tagshot.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.tagshot.app.data.PhotoWithTags
import com.tagshot.app.domain.Limits
import com.tagshot.app.ui.components.ZoomableImage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoDetailScreen(
    initialPhotoId: Long,
    viewModel: PhotoViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val successState = uiState as? MainUiState.Success
    val photos = successState?.photos ?: emptyList()

    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val initialIndex = remember(photos) {
        val found = photos.indexOfFirst { it.photo.id == initialPhotoId }
        if (found >= 0) found else 0
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }
    var isZoomed by remember { mutableStateOf(false) }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos[0]

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 80.dp,
        sheetContent = {
            PhotoDetailBottomSheetContent(
                photo = currentPhoto,
                onAddTagClick = { showAddTagDialog = true },
                onRemoveTag = { tagId -> viewModel.removeTagFromPhoto(currentPhoto.photo.id, tagId) },
                onNoteChanged = { newNote -> viewModel.updateNote(currentPhoto.photo.id, newNote) }
            )
        },
        topBar = {
            TopAppBar(
                title = { Text(text = "${pagerState.currentPage + 1} of ${photos.size}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        shareSinglePhoto(context, currentPhoto.photo.originalPath)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                key = { photos[it].photo.id },
                userScrollEnabled = !isZoomed,
                beyondBoundsPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photoItem = photos[page]
                val origFile = File(context.filesDir, photoItem.photo.originalPath)
                ZoomableImage(
                    imageFile = origFile,
                    contentDescription = "Photo ${photoItem.photo.id}",
                    onZoomChanged = { zoomed -> isZoomed = zoomed }
                )
            }
        }
    }

    if (showAddTagDialog) {
        TagInputDialog(
            title = "Add Tags",
            allExistingTags = successState?.allTags ?: emptyList(),
            onDismiss = { showAddTagDialog = false },
            onConfirm = { raw ->
                viewModel.addTagsToPhotos(listOf(currentPhoto.photo.id), raw)
                showAddTagDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            count = 1,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                viewModel.deletePhotos(listOf(currentPhoto.photo.id))
                showDeleteConfirm = false
                onNavigateBack()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoDetailBottomSheetContent(
    photo: PhotoWithTags,
    onAddTagClick: () -> Unit,
    onRemoveTag: (Long) -> Unit,
    onNoteChanged: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    val addedDateStr = remember(photo.photo.addedAt) { dateFormat.format(Date(photo.photo.addedAt)) }
    val sizeStr = remember(photo.photo.sizeBytes) { formatFileSize(photo.photo.sizeBytes) }

    var noteText by remember(photo.photo.id) { mutableStateOf(photo.photo.note) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tags FlowRow
        Text("Tags", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (tag in photo.tags) {
                InputChip(
                    selected = true,
                    onClick = {},
                    label = { Text("#${tag.name}") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove tag",
                            modifier = Modifier.size(16.dp).clickable { onRemoveTag(tag.id) }
                        )
                    }
                )
            }
            AssistChip(
                onClick = onAddTagClick,
                label = { Text("+ Add Tag") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Note Editor (max 500 chars per Section 4.3)
        OutlinedTextField(
            value = noteText,
            onValueChange = {
                if (it.length <= Limits.MAX_NOTE_CHARS) {
                    noteText = it
                    onNoteChanged(it)
                }
            },
            label = { Text("Note (${noteText.length}/${Limits.MAX_NOTE_CHARS})") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Metadata Info
        Text("Information", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Imported: $addedDateStr", style = MaterialTheme.typography.bodySmall)
        Text("Dimensions: ${photo.photo.width} × ${photo.photo.height}", style = MaterialTheme.typography.bodySmall)
        Text("File Size: $sizeStr", style = MaterialTheme.typography.bodySmall)

        photo.photo.takenAt?.let {
            Text("Captured: ${dateFormat.format(Date(it))}", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.2f MB".format(mb)
}

private fun shareSinglePhoto(context: Context, relativePath: String) {
    val file = File(context.filesDir, relativePath)
    if (!file.exists()) return
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Photo"))
}
