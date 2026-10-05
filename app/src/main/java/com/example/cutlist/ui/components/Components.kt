package com.example.cutlist.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cutlist.ui.theme.overline

/** Small uppercase label used to title sections. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.overline,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

/** Generic Material 3 single-choice segmented control. */
@Composable
fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {},
                label = { Text(label(option), maxLines = 1) },
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Centered empty state with a dimensioned-panel drawing. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DimensionedPanelGlyph(Modifier.size(width = 120.dp, height = 84.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium.overline,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}

/** Decorative rectangle with width/height dimension lines — the app's visual motif. */
@Composable
fun DimensionedPanelGlyph(modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    Canvas(modifier) {
        val gap = 14.dp.toPx()
        val panelTopLeft = Offset(gap, 0f)
        val panelSize = Size(size.width - gap, size.height - gap)
        drawRect(fill, panelTopLeft, panelSize)
        drawRect(accent, panelTopLeft, panelSize, style = Stroke(width = 1.5.dp.toPx()))
        val y = size.height - gap / 3
        drawDimensionLine(line, Offset(panelTopLeft.x, y), Offset(size.width, y))
        val x = gap / 3
        drawDimensionLine(line, Offset(x, 0f), Offset(x, panelSize.height))
    }
}

/** Thin line with end ticks, like a drawing's dimension line. */
fun DrawScope.drawDimensionLine(color: Color, start: Offset, end: Offset, alpha: Float = 1f) {
    val stroke = 1.dp.toPx()
    val tick = 4.dp.toPx()
    drawLine(color, start, end, strokeWidth = stroke, alpha = alpha)
    val horizontal = kotlin.math.abs(end.x - start.x) >= kotlin.math.abs(end.y - start.y)
    listOf(start, end).forEach { point ->
        if (horizontal) {
            drawLine(color, Offset(point.x, point.y - tick), Offset(point.x, point.y + tick), stroke, alpha = alpha)
        } else {
            drawLine(color, Offset(point.x - tick, point.y), Offset(point.x + tick, point.y), stroke, alpha = alpha)
        }
    }
}

/** A label/value pair laid out in one row. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
