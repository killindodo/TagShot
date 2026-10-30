package com.tagshot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tagshot.app.domain.Limits
import com.tagshot.app.domain.ZoomMath
import com.tagshot.app.domain.ZoomState
import java.io.File

@Composable
fun ZoomableImage(
    imageFile: File,
    contentDescription: String?,
    imageWidth: Int = 0,
    imageHeight: Int = 0,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!imageFile.exists()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Missing file",
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Photo file not found", color = Color.Gray)
            }
        }
        return
    }

    var zoomState by remember(imageFile.absolutePath) { mutableStateOf(ZoomState()) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(containerSize, imageFile.absolutePath) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        val cw = containerSize.width.toFloat()
                        val ch = containerSize.height.toFloat()
                        val imgW = if (imageWidth > 0) imageWidth.toFloat() else cw
                        val imgH = if (imageHeight > 0) imageHeight.toFloat() else ch
                        if (cw > 0f && ch > 0f) {
                            val nextState = ZoomMath.toggleDoubleTap(
                                current = zoomState,
                                containerWidth = cw,
                                containerHeight = ch,
                                contentWidth = imgW,
                                contentHeight = imgH,
                                tapX = tapOffset.x,
                                tapY = tapOffset.y
                            )
                            zoomState = nextState
                            onZoomChanged(nextState.scale > 1f)
                        }
                    }
                )
            }
            .pointerInput(containerSize, imageFile.absolutePath) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val cw = containerSize.width.toFloat()
                    val ch = containerSize.height.toFloat()
                    val imgW = if (imageWidth > 0) imageWidth.toFloat() else cw
                    val imgH = if (imageHeight > 0) imageHeight.toFloat() else ch
                    if (cw > 0f && ch > 0f) {
                        val nextState = ZoomMath.calculateZoom(
                            current = zoomState,
                            zoomChange = zoom,
                            panDeltaX = pan.x,
                            panDeltaY = pan.y,
                            centroidX = centroid.x,
                            centroidY = centroid.y,
                            containerWidth = cw,
                            containerHeight = ch,
                            contentWidth = imgW,
                            contentHeight = imgH
                        )
                        zoomState = nextState
                        onZoomChanged(nextState.scale > 1f)
                    }
                }
            }
    ) {
        val context = LocalContext.current
        val imageRequest = remember(imageFile) {
            ImageRequest.Builder(context)
                .data(imageFile)
                .size(Limits.DETAIL_MAX_EDGE)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoomState.scale,
                    scaleY = zoomState.scale,
                    translationX = zoomState.offsetX,
                    translationY = zoomState.offsetY
                )
        )
    }
}
