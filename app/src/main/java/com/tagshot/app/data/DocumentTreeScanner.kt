package com.tagshot.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log

object DocumentTreeScanner {

    private const val TAG = "DocumentTreeScanner"

    data class FolderScanResult(
        val folderName: String,
        val imageUris: List<Uri>
    )

    fun scanFolder(context: Context, treeUri: Uri): FolderScanResult {
        val contentResolver = context.contentResolver
        val documentId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tree document ID", e)
            return FolderScanResult("Folder", emptyList())
        }

        // 1. Resolve human-readable folder name
        var folderName = documentId.substringAfterLast(':').substringAfterLast('/').ifBlank { "Folder" }
        try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
            contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (nameCol != -1) {
                        val name = cursor.getString(nameCol)
                        if (!name.isNullOrBlank()) {
                            folderName = name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not query folder display name, falling back to parsed name: $folderName", e)
        }

        // 2. Query child documents in folder
        val imageUris = mutableListOf<Uri>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )

        try {
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val childDocId = cursor.getString(idCol)
                    val mimeType = cursor.getString(mimeCol) ?: ""
                    val name = if (nameCol != -1) cursor.getString(nameCol) ?: "" else ""

                    val isImage = mimeType.startsWith("image/") ||
                            name.endsWith(".jpg", ignoreCase = true) ||
                            name.endsWith(".jpeg", ignoreCase = true) ||
                            name.endsWith(".png", ignoreCase = true) ||
                            name.endsWith(".webp", ignoreCase = true) ||
                            name.endsWith(".heic", ignoreCase = true) ||
                            name.endsWith(".heif", ignoreCase = true)

                    if (isImage) {
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                        imageUris.add(childUri)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query child documents in tree: $treeUri", e)
        }

        return FolderScanResult(folderName = folderName, imageUris = imageUris)
    }
}
