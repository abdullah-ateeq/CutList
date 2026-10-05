package com.example.cutlist.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cutlist.data.ProjectCodec
import com.example.cutlist.domain.MeasurementFormatter
import com.example.cutlist.ui.CutListViewModel
import com.example.cutlist.ui.HistoryItem
import com.example.cutlist.ui.components.EmptyState
import com.example.cutlist.ui.theme.overline
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: CutListViewModel, snackbarHostState: SnackbarHostState) {
    val items by viewModel.historyItems.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentId = state.project.id
    val decimals = state.settings.decimalPlaces
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("History")
                        Text(
                            "${items.size} of ${ProjectCodec.MAX_HISTORY} saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::requestClearHistory, enabled = items.isNotEmpty()) {
                        Text("Clear all")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "No saved cut lists",
                    message = "Saved cut lists appear here. Up to ${ProjectCodec.MAX_HISTORY} are kept; " +
                        "the oldest is removed when the limit is reached.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(items, key = { it.project.id }) { item ->
                    HistoryCard(
                        item = item,
                        isCurrent = item.project.id == currentId,
                        decimals = decimals,
                        dateText = dateFormat.format(Date(item.project.updatedAt)),
                        onOpen = { viewModel.requestOpenProject(item.project) },
                        onDuplicate = { viewModel.duplicateHistoryProject(item.project) },
                        onDelete = { viewModel.requestDeleteHistoryProject(item.project) },
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItem,
    isCurrent: Boolean,
    decimals: Int,
    dateText: String,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val project = item.project
    val summary = item.summary
    val area = MeasurementFormatter.formatArea(summary.totalAreaMm2, project.unit, decimals)
    val pieces = if (summary.totalPieces == 1) "1 piece" else "${summary.totalPieces} pieces"
    val types = if (summary.uniqueItems == 1) "1 item type" else "${summary.uniqueItems} item types"
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${project.displayName}, $pieces, $types, ${area.spoken}, saved $dateText"
                onClick(label = "Open cut list") { onOpen(); true }
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        project.displayName.uppercase(),
                        style = MaterialTheme.typography.titleMedium.overline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isCurrent) {
                        Text(
                            "  •  OPEN",
                            style = MaterialTheme.typography.labelSmall.overline,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("$pieces  ·  $types  ·  ${area.text}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "$dateText  ·  ${project.unit.symbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More actions for ${project.displayName}")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
                        onClick = { menuOpen = false; onOpen() },
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Outlined.FileCopy, null) },
                        onClick = { menuOpen = false; onDuplicate() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}
