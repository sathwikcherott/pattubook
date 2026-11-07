package com.pattubook.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.ProfilePhotoManager
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun CropPhotoDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onPhotoCropped: (photoPath: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var measuredViewportSizePx by remember { mutableFloatStateOf(0f) }
    var userScale by rememberSaveable { mutableFloatStateOf(1.0f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }

    LaunchedEffect(imageUri) {
        isLoading = true
        sourceBitmap = ProfilePhotoManager.decodeFullBitmapFromUri(context, imageUri, maxDimension = 2048)
        isLoading = false
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Crop Profile Photo",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Drag to position • Pinch to zoom",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Crop Viewport Box
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val boxWidthPx = with(density) { maxWidth.toPx() }
                val boxHeightPx = with(density) { maxHeight.toPx() }
                val viewportSizePx = min(boxWidthPx, boxHeightPx) * 0.85f
                measuredViewportSizePx = viewportSizePx

                when {
                    isLoading || isSaving -> {
                        CircularProgressIndicator(color = AccentGreen)
                    }

                    sourceBitmap != null -> {
                        val bitmap = sourceBitmap!!
                        val imgWidth = bitmap.width.toFloat()
                        val imgHeight = bitmap.height.toFloat()

                        val baseScale = viewportSizePx / min(imgWidth, imgHeight)
                        val totalScale = baseScale * kotlin.math.max(1.0f, userScale)

                        val dispWidth = imgWidth * totalScale
                        val dispHeight = imgHeight * totalScale

                        val maxPanX = kotlin.math.max(0f, (dispWidth - viewportSizePx) / 2f)
                        val maxPanY = kotlin.math.max(0f, (dispHeight - viewportSizePx) / 2f)

                        // Constrain pan within image boundaries
                        val currentPanX = panX.coerceIn(-maxPanX, maxPanX)
                        val currentPanY = panY.coerceIn(-maxPanY, maxPanY)

                        val viewportSizeDp = with(density) { viewportSizePx.toDp() }

                        Box(
                            modifier = Modifier
                                .size(viewportSizeDp)
                                .pointerInput(bitmap, userScale) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (userScale * zoom).coerceIn(1.0f, 5.0f)
                                        val newTotalScale = baseScale * newScale
                                        val newDispWidth = imgWidth * newTotalScale
                                        val newDispHeight = imgHeight * newTotalScale
                                        val newMaxPanX = kotlin.math.max(0f, (newDispWidth - viewportSizePx) / 2f)
                                        val newMaxPanY = kotlin.math.max(0f, (newDispHeight - viewportSizePx) / 2f)

                                        userScale = newScale
                                        panX = (panX + pan.x).coerceIn(-newMaxPanX, newMaxPanX)
                                        panY = (panY + pan.y).coerceIn(-newMaxPanY, newMaxPanY)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val dstLeft = (viewportSizePx - dispWidth) / 2f + currentPanX
                                val dstTop = (viewportSizePx - dispHeight) / 2f + currentPanY

                                // 1. Draw scaled/panned source image
                                drawImage(
                                    image = bitmap.asImageBitmap(),
                                    dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt()),
                                    dstSize = IntSize(dispWidth.toInt(), dispHeight.toInt())
                                )

                                // 2. Dimmed overlay outside the circular guide
                                val circleRadius = viewportSizePx / 2f
                                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                                val circlePath = Path().apply {
                                    addOval(Rect(center = centerOffset, radius = circleRadius))
                                }

                                clipPath(circlePath, clipOp = ClipOp.Difference) {
                                    drawRect(Color.Black.copy(alpha = 0.65f))
                                }

                                // 3. Accent circular boundary ring guide
                                drawCircle(
                                    color = AccentGreen,
                                    radius = circleRadius,
                                    center = centerOffset,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = "Failed to load photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        userScale = 1.0f
                        panX = 0f
                        panY = 0f
                    },
                    enabled = sourceBitmap != null && !isSaving && !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Crop",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val bitmap = sourceBitmap ?: return@Button
                            isSaving = true
                            coroutineScope.launch {
                                val viewportSizePx = if (measuredViewportSizePx > 0f) {
                                    measuredViewportSizePx
                                } else {
                                    with(density) { 320.dp.toPx() } * 0.85f
                                }

                                val cropped = ProfilePhotoManager.cropSquareBitmap(
                                    sourceBitmap = bitmap,
                                    panX = panX,
                                    panY = panY,
                                    userScale = userScale,
                                    viewportSizePx = viewportSizePx,
                                    targetSizePx = 512
                                )
                                val savedPath = ProfilePhotoManager.saveSquareBitmap(context, cropped)
                                isSaving = false
                                if (savedPath != null) {
                                    onPhotoCropped(savedPath)
                                } else {
                                    onDismiss()
                                }
                            }
                        },
                        enabled = sourceBitmap != null && !isSaving && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Photo", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
