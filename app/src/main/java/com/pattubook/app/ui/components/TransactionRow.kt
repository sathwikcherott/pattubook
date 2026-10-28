package com.pattubook.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.ui.theme.AccentBlue
import com.pattubook.app.ui.theme.AccentBlueContainer
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.AccentGreenContainer
import com.pattubook.app.ui.theme.AccentRed
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.DarkSurfaceVariant
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.CurrencyFormatter
import com.pattubook.app.ui.util.DateFormatter
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeableTransactionRow(
    entry: LedgerEntry,
    onArchiveSwipe: (LedgerEntry) -> Unit,
    onEditClick: (LedgerEntry) -> Unit,
    onDeleteClick: (LedgerEntry) -> Unit,
    modifier: Modifier = Modifier,
    onSetEntryPaid: (LedgerEntry, Boolean) -> Unit = { _, _ -> },
) {
    val coroutineScope = rememberCoroutineScope()
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    val offsetX = remember { Animatable(0f) }
    val cardShape = RoundedCornerShape(18.dp)

    val archiveText = if (entry.isArchived) "Unarchive" else "Archive"
    val archiveIcon = if (entry.isArchived) Icons.Default.Refresh else Icons.Default.Info

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                rowWidthPx = size.width.toFloat()
            }
    ) {
        // Background Actions Layer (Shown for both left and right swipes)
        if (offsetX.value != 0f) {
            val contentAlignment = if (offsetX.value < 0f) Alignment.CenterEnd else Alignment.CenterStart
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(cardShape)
                    .background(AccentBlue)
                    .padding(horizontal = 20.dp),
                contentAlignment = contentAlignment
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = archiveIcon,
                        contentDescription = archiveText,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = archiveText,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Foreground Transaction Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = offsetX.value
                }
                .pointerInput(rowWidthPx) {
                    if (rowWidthPx <= 0f) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-rowWidthPx, rowWidthPx)
                            coroutineScope.launch {
                                offsetX.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            val currentOffset = offsetX.value
                            val absOffset = abs(currentOffset)
                            val swipeProgress = if (rowWidthPx > 0f) absOffset / rowWidthPx else 0f

                            if (swipeProgress > 0.70f) {
                                // SWIPE IN EITHER DIRECTION > 70%: Archive or Unarchive!
                                val targetX = if (currentOffset < 0f) -rowWidthPx else rowWidthPx
                                coroutineScope.launch {
                                    offsetX.animateTo(targetX, animationSpec = tween(150))
                                    onArchiveSwipe(entry)
                                }
                            } else {
                                // <= 70%: Restore to resting position
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, animationSpec = spring())
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, animationSpec = spring())
                            }
                        }
                    )
                }
        ) {
            TransactionRow(
                entry = entry,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
                onSetEntryPaid = onSetEntryPaid,
            )
        }
    }
}

@Composable
private fun PaidCheckboxGlyph(isPaid: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val inset = strokeWidth / 2
        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = strokeWidth),
        )
        if (isPaid) {
            val checkmark = Path().apply {
                moveTo(size.width * 0.22f, size.height * 0.52f)
                lineTo(size.width * 0.43f, size.height * 0.72f)
                lineTo(size.width * 0.80f, size.height * 0.30f)
            }
            drawPath(
                path = checkmark,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    entry: LedgerEntry,
    modifier: Modifier = Modifier,
    onEditClick: (LedgerEntry) -> Unit = {},
    onDeleteClick: (LedgerEntry) -> Unit = {},
    onSetEntryPaid: (LedgerEntry, Boolean) -> Unit = { _, _ -> },
) {
    val cardShape = RoundedCornerShape(18.dp)
    val isGiven = entry.type == LedgerEntryType.GIVEN
    val accentColor = if (isGiven) AccentGreen else AccentBlue
    val containerColor = if (isGiven) AccentGreenContainer else AccentBlueContainer
    val typeTitle = if (isGiven) "Money Given" else "Money Returned"

    var dropdownExpanded by remember { mutableStateOf(false) }
    val cardAlpha = when {
        entry.isArchived && entry.isPaid -> 0.5f
        entry.isArchived -> 0.65f
        entry.isPaid -> 0.55f
        else -> 1.0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .alpha(cardAlpha)
            .background(DarkSurface)
            .border(1.dp, BorderSubtle, cardShape)
            .combinedClickable(
                onClick = {},
                onLongClick = { dropdownExpanded = true }
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(containerColor)
                            .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape)
                            .then(
                                if (isGiven) {
                                    Modifier
                                        .toggleable(
                                            value = entry.isPaid,
                                            role = Role.Checkbox,
                                            onValueChange = { onSetEntryPaid(entry, it) },
                                        )
                                        .semantics {
                                            contentDescription = if (entry.isPaid) "Paid" else "Mark as paid"
                                        }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGiven) {
                            PaidCheckboxGlyph(isPaid = entry.isPaid, color = accentColor)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = typeTitle,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = typeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = DateFormatter.formatEpochMillis(entry.timestamp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CurrencyFormatter.formatPaiseToRupees(entry.amountPaise),
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.isArchived) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Long-Press Contextual Action Menu
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
            modifier = Modifier.background(DarkSurfaceVariant)
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Transaction",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                onClick = {
                    dropdownExpanded = false
                    onEditClick(entry)
                }
            )

            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Transaction",
                            tint = AccentRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                onClick = {
                    dropdownExpanded = false
                    onDeleteClick(entry)
                }
            )
        }
    }
}

@Preview
@Composable
fun TransactionRowPreview() {
    PattubookTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TransactionRow(
                entry = LedgerEntry(
                    id = 1,
                    personId = 1,
                    amountPaise = 50000L,
                    type = LedgerEntryType.GIVEN,
                    note = "Lunch split for team"
                )
            )
            TransactionRow(
                entry = LedgerEntry(
                    id = 2,
                    personId = 1,
                    amountPaise = 20000L,
                    type = LedgerEntryType.GIVEN_BACK,
                    note = null,
                    isArchived = true
                )
            )
        }
    }
}
