package com.example.cutlist.ui.cutlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cutlist.domain.MeasurementFormatter
import com.example.cutlist.ui.CutListUiState
import com.example.cutlist.ui.components.InfoRow
import com.example.cutlist.ui.components.SectionLabel
import com.example.cutlist.ui.theme.overline
import com.example.cutlist.ui.theme.tabular

private fun pluralPieces(count: Int) = if (count == 1) "1 piece" else "$count pieces"

/** Compact strip of headline totals shown above the list. */
@Composable
fun SummaryStrip(state: CutListUiState, modifier: Modifier = Modifier) {
    val summary = state.summary
    val unit = state.project.unit
    val decimals = state.settings.decimalPlaces
    val area = MeasurementFormatter.formatArea(summary.totalAreaMm2, unit, decimals)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Total pieces", summary.totalPieces.toString(), "Total ${pluralPieces(summary.totalPieces)}", Modifier.weight(1f))
            Stat("Unique items", summary.uniqueItems.toString(), "${summary.uniqueItems} unique items", Modifier.weight(1f))
            if (state.settings.showAreaSummary) {
                Stat("Total area", area.text, "Total area ${area.spoken}", Modifier.weight(1.3f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, spoken: String, modifier: Modifier = Modifier) {
    Column(modifier.clearAndSetSemantics { contentDescription = spoken }) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.overline, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleLarge.tabular(), maxLines = 1)
    }
}

/** Full project summary: totals, thickness and material breakdowns, and the informational disclaimer. */
@Composable
fun ProjectSummaryPanel(state: CutListUiState, modifier: Modifier = Modifier) {
    val summary = state.summary
    val unit = state.project.unit
    val decimals = state.settings.decimalPlaces
    OutlinedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            SectionLabel("Project summary")
            Spacer(Modifier.height(8.dp))
            InfoRow("Project", state.project.displayName)
            InfoRow("Unique items", summary.uniqueItems.toString())
            InfoRow("Total pieces", summary.totalPieces.toString())
            if (state.settings.showAreaSummary) {
                InfoRow("Total panel area", MeasurementFormatter.formatArea(summary.totalAreaMm2, unit, decimals).text)
            }
            InfoRow("Total piece length", MeasurementFormatter.formatLength(summary.totalPieceLengthMm, unit, decimals))

            if (state.settings.showThicknessSummary && state.hasThickness) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                SectionLabel("By thickness")
                Spacer(Modifier.height(4.dp))
                state.thicknessGroups.forEach { group ->
                    InfoRow(
                        group.thicknessMm?.let { MeasurementFormatter.formatLength(it, unit, decimals) } ?: "Not specified",
                        pluralPieces(group.pieceCount),
                    )
                }
            }
            if (state.settings.showMaterialSummary && state.hasMaterial) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                SectionLabel("By material")
                Spacer(Modifier.height(4.dp))
                state.materialGroups.forEach { group ->
                    InfoRow(group.material ?: "Not specified", pluralPieces(group.pieceCount))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Text(
                text = "Area is the sum of piece faces and total piece length is the sum of piece lengths. " +
                    "Neither tells you how many sheets or boards to buy — that depends on layout, " +
                    "grain, kerf, waste and available stock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
