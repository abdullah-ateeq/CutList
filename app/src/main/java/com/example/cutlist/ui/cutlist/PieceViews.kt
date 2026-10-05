package com.example.cutlist.ui.cutlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cutlist.domain.CutListRow
import com.example.cutlist.domain.GrainDirection
import com.example.cutlist.domain.LengthUnit
import com.example.cutlist.domain.MeasurementFormatter
import com.example.cutlist.ui.theme.overline
import com.example.cutlist.ui.theme.tabular

class RowActions(
    val onEdit: (CutListRow) -> Unit,
    val onDuplicate: (CutListRow) -> Unit,
    val onDelete: (CutListRow) -> Unit,
)

private fun pieceDescription(row: CutListRow, unit: LengthUnit, decimals: Int): String {
    val piece = row.piece
    return buildString {
        append("Piece ${piece.name}, ")
        append(MeasurementFormatter.spokenDimensions(piece.lengthMm, piece.widthMm, unit, decimals))
        append(", quantity ${piece.quantity}")
        piece.thicknessMm?.let { append(", thickness ${MeasurementFormatter.spokenLength(it, unit, decimals)}") }
        piece.material?.let { append(", $it") }
        if (piece.grainDirection != GrainDirection.NONE) append(", grain along ${piece.grainDirection.label.lowercase()}")
        if (!piece.rotationAllowed) append(", rotation not allowed")
        piece.notes?.let { append(". Note: $it") }
    }
}

private fun Modifier.pieceSemantics(row: CutListRow, description: String, actions: RowActions): Modifier =
    semantics(mergeDescendants = true) {
        contentDescription = description
        onClick(label = "Edit piece") { actions.onEdit(row); true }
        customActions = listOf(
            CustomAccessibilityAction("Duplicate piece") { actions.onDuplicate(row); true },
            CustomAccessibilityAction("Delete piece") { actions.onDelete(row); true },
        )
    }

/** Secondary details shown under the dimensions; null when there are none. */
private fun detailsLine(row: CutListRow, unit: LengthUnit, decimals: Int, includeThicknessAndMaterial: Boolean): String? {
    val piece = row.piece
    val parts = buildList {
        if (includeThicknessAndMaterial) {
            piece.thicknessMm?.let { add(MeasurementFormatter.formatLength(it, unit, decimals) + " thick") }
            piece.material?.let { add(it) }
        }
        if (piece.grainDirection != GrainDirection.NONE) add("Grain ↔ ${piece.grainDirection.label.lowercase()}")
        if (!piece.rotationAllowed) add("No rotation")
        if (row.isMerged) add("${row.sourceIds.size} entries combined")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

@Composable
private fun OrderBadge(number: Int) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelMedium.tabular(),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun RowMenu(row: CutListRow, actions: RowActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "More actions for ${row.piece.name}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                onClick = { expanded = false; actions.onEdit(row) },
            )
            DropdownMenuItem(
                text = { Text("Duplicate") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                onClick = { expanded = false; actions.onDuplicate(row) },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                onClick = { expanded = false; actions.onDelete(row) },
            )
        }
    }
}

/** Phone layout: one stacked card per cut-list row. */
@Composable
fun PieceCard(
    row: CutListRow,
    number: Int,
    showNumber: Boolean,
    unit: LengthUnit,
    decimals: Int,
    actions: RowActions,
    modifier: Modifier = Modifier,
) {
    val piece = row.piece
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pieceSemantics(row, pieceDescription(row, unit, decimals), actions),
        onClick = { actions.onEdit(row) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 14.dp, end = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showNumber) {
                        OrderBadge(number)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = piece.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${MeasurementFormatter.formatValue(piece.lengthMm, unit, decimals)} × " +
                            MeasurementFormatter.formatValue(piece.widthMm, unit, decimals),
                        style = MaterialTheme.typography.headlineSmall.tabular(),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unit.symbol,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                detailsLine(row, unit, decimals, includeThicknessAndMaterial = true)?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                piece.notes?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "“$it”",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                RowMenu(row, actions)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 12.dp).widthIn(min = 40.dp),
                ) {
                    Text("QTY", style = MaterialTheme.typography.labelSmall.overline, color = MaterialTheme.colorScheme.primary)
                    Text(
                        piece.quantity.toString(),
                        style = MaterialTheme.typography.headlineSmall.tabular(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Column visibility for the wide table layout. */
data class TableColumns(val showNumber: Boolean, val showThickness: Boolean, val showMaterial: Boolean)

@Composable
private fun TableCells(
    columns: TableColumns,
    number: @Composable () -> Unit,
    piece: @Composable (Modifier) -> Unit,
    length: @Composable (Modifier) -> Unit,
    width: @Composable (Modifier) -> Unit,
    thickness: @Composable (Modifier) -> Unit,
    material: @Composable (Modifier) -> Unit,
    quantity: @Composable (Modifier) -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (columns.showNumber) Box(Modifier.width(36.dp)) { number() }
        piece(Modifier.weight(2.2f))
        length(Modifier.weight(1f))
        width(Modifier.weight(1f))
        if (columns.showThickness) thickness(Modifier.weight(0.9f))
        if (columns.showMaterial) material(Modifier.weight(1.2f))
        quantity(Modifier.weight(0.6f))
        Box(Modifier.width(48.dp)) { trailing() }
    }
}

@Composable
fun PieceTableHeader(columns: TableColumns, unit: LengthUnit, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelSmall.overline
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.padding(horizontal = 16.dp)) {
        TableCells(
            columns = columns,
            number = { Text("#", style = style, color = color) },
            piece = { Text("PIECE", it, style = style, color = color) },
            length = { Text("LENGTH (${unit.symbol})", it, style = style, color = color, textAlign = TextAlign.End) },
            width = { Text("WIDTH (${unit.symbol})", it, style = style, color = color, textAlign = TextAlign.End) },
            thickness = { Text("THK", it, style = style, color = color, textAlign = TextAlign.End) },
            material = { Text("MATERIAL", it, style = style, color = color) },
            quantity = { Text("QTY", it, style = style, color = color, textAlign = TextAlign.End) },
            trailing = {},
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
    }
}

/** Tablet / landscape layout: one compact table row. */
@Composable
fun PieceTableRow(
    row: CutListRow,
    number: Int,
    columns: TableColumns,
    unit: LengthUnit,
    decimals: Int,
    actions: RowActions,
    modifier: Modifier = Modifier,
) {
    val piece = row.piece
    val numberStyle = MaterialTheme.typography.bodyLarge.tabular()
    Column(
        modifier
            .fillMaxWidth()
            .clickable { actions.onEdit(row) }
            .pieceSemantics(row, pieceDescription(row, unit, decimals), actions)
    ) {
        Box(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp)) {
            TableCells(
                columns = columns,
                number = { OrderBadge(number) },
                piece = { mod ->
                    Column(mod) {
                        Text(piece.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val details = detailsLine(row, unit, decimals, includeThicknessAndMaterial = false)
                        val secondary = listOfNotNull(details, piece.notes?.let { "“$it”" }).joinToString("  ·  ")
                        if (secondary.isNotEmpty()) {
                            Text(
                                secondary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                length = { Text(MeasurementFormatter.formatValue(piece.lengthMm, unit, decimals), it, style = numberStyle, textAlign = TextAlign.End) },
                width = { Text(MeasurementFormatter.formatValue(piece.widthMm, unit, decimals), it, style = numberStyle, textAlign = TextAlign.End) },
                thickness = {
                    Text(
                        piece.thicknessMm?.let { t -> MeasurementFormatter.formatValue(t, unit, decimals) } ?: "—",
                        it, style = numberStyle, textAlign = TextAlign.End,
                    )
                },
                material = { Text(piece.material ?: "—", it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                quantity = {
                    Text(
                        piece.quantity.toString(), it,
                        style = numberStyle, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End,
                    )
                },
                trailing = { RowMenu(row, actions) },
            )
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
