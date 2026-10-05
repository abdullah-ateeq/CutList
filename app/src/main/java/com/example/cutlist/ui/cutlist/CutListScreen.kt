package com.example.cutlist.ui.cutlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cutlist.domain.LengthUnit
import com.example.cutlist.domain.SortOption
import com.example.cutlist.domain.UNTITLED_PROJECT_NAME
import com.example.cutlist.ui.CutListUiState
import com.example.cutlist.ui.CutListViewModel
import com.example.cutlist.ui.components.EmptyState
import com.example.cutlist.ui.components.SectionLabel
import com.example.cutlist.ui.components.SegmentedChoice

private val TABLE_MIN_WIDTH = 600.dp
private val TWO_PANE_MIN_WIDTH = 840.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutListScreen(
    viewModel: CutListViewModel,
    snackbarHostState: SnackbarHostState,
    onHaptic: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) viewModel.exportText(uri, context.contentResolver)
    }

    val rowActions = remember(viewModel) {
        RowActions(
            onEdit = viewModel::openEditRow,
            onDuplicate = viewModel::duplicateRow,
            onDelete = viewModel::requestDeleteRow,
        )
    }
    val hasPieces = state.project.pieces.isNotEmpty()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CutListTopBar(
                state = state,
                hasPieces = hasPieces,
                scrollBehavior = scrollBehavior,
                onCopy = {
                    copyToClipboard(context, viewModel.cutListText())
                    onHaptic()
                    viewModel.showMessage("Cut list copied")
                },
                onShare = { shareText(context, state.project.displayName, viewModel.cutListText()) },
                onSave = viewModel::saveProject,
                onNew = viewModel::requestNewCutList,
                onDuplicateProject = viewModel::duplicateCurrentProject,
                onExport = { exportLauncher.launch(exportFileName(state.project.displayName)) },
            )
        },
        floatingActionButton = {
            if (hasPieces) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::openAddPiece,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add piece") },
                    modifier = Modifier.semantics { contentDescription = "Add piece" },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= TABLE_MIN_WIDTH
            val twoPane = maxWidth >= TWO_PANE_MIN_WIDTH && hasPieces
            if (twoPane) {
                Row(Modifier.fillMaxSize()) {
                    PieceList(state, viewModel, rowActions, wide = true, includeSummaryPanel = false, modifier = Modifier.weight(1.6f))
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp, end = 16.dp, bottom = 96.dp),
                    ) {
                        ProjectSummaryPanel(state)
                    }
                }
            } else {
                PieceList(state, viewModel, rowActions, wide = wide, includeSummaryPanel = true, modifier = Modifier.fillMaxSize())
            }
        }
    }

    editor?.let { editorState ->
        PieceEditorSheet(
            state = editorState,
            unit = state.project.unit,
            materialSuggestions = state.materialSuggestions,
            onChange = viewModel::updateEditor,
            onSave = viewModel::saveEditor,
            onDismiss = viewModel::dismissEditor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CutListTopBar(
    state: CutListUiState,
    hasPieces: Boolean,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onNew: () -> Unit,
    onDuplicateProject: () -> Unit,
    onExport: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Column {
                Text("Cut List")
                Text(
                    text = when {
                        !hasPieces && !state.hasUnsavedChanges -> "Offline cut-list planner"
                        state.hasUnsavedChanges -> "Unsaved changes"
                        else -> "Saved"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            IconButton(onClick = onCopy, enabled = hasPieces) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy cut list")
            }
            IconButton(onClick = onShare, enabled = hasPieces) {
                Icon(Icons.Outlined.Share, contentDescription = "Share cut list")
            }
            IconButton(onClick = onSave, enabled = hasPieces) {
                Icon(Icons.Outlined.Save, contentDescription = "Save cut list")
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("New cut list") },
                    leadingIcon = { Icon(Icons.Outlined.Add, null) },
                    onClick = { menuOpen = false; onNew() },
                )
                DropdownMenuItem(
                    text = { Text("Duplicate project") },
                    leadingIcon = { Icon(Icons.Outlined.FileCopy, null) },
                    enabled = hasPieces,
                    onClick = { menuOpen = false; onDuplicateProject() },
                )
                DropdownMenuItem(
                    text = { Text("Export text file") },
                    leadingIcon = { Icon(Icons.Outlined.FileDownload, null) },
                    enabled = hasPieces,
                    onClick = { menuOpen = false; onExport() },
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun PieceList(
    state: CutListUiState,
    viewModel: CutListViewModel,
    actions: RowActions,
    wide: Boolean,
    includeSummaryPanel: Boolean,
    modifier: Modifier = Modifier,
) {
    val unit = state.project.unit
    val decimals = state.settings.decimalPlaces
    val showNumbers = state.settings.showCutOrder
    val columns = TableColumns(showNumbers, showThickness = state.hasThickness, showMaterial = state.hasMaterial)
    val horizontal = 16.dp

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp),
    ) {
        item(key = "header", contentType = "header") {
            ProjectHeader(
                state = state,
                onNameChange = viewModel::setProjectName,
                onUnitChange = viewModel::setUnit,
                modifier = Modifier.padding(horizontal = horizontal),
            )
        }
        if (state.project.pieces.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                EmptyState(
                    title = "No pieces yet",
                    message = "Add your first piece to build the cut list.",
                    actionLabel = "Add piece",
                    onAction = viewModel::openAddPiece,
                )
            }
            return@LazyColumn
        }
        item(key = "summary", contentType = "summary") {
            SummaryStrip(state, Modifier.padding(horizontal = horizontal, vertical = 8.dp))
        }
        item(key = "controls", contentType = "controls") {
            ListControls(
                sort = state.sortOption,
                consolidate = state.settings.consolidateIdentical,
                onSort = viewModel::setSort,
                onConsolidate = viewModel::setConsolidate,
                modifier = Modifier.padding(horizontal = horizontal),
            )
        }
        pieceRows(state, wide, columns, unit, decimals, showNumbers, actions, horizontal)
        if (includeSummaryPanel) {
            item(key = "details", contentType = "details") {
                ProjectSummaryPanel(state, Modifier.padding(horizontal = horizontal, vertical = 16.dp))
            }
        }
    }
}

private fun LazyListScope.pieceRows(
    state: CutListUiState,
    wide: Boolean,
    columns: TableColumns,
    unit: LengthUnit,
    decimals: Int,
    showNumbers: Boolean,
    actions: RowActions,
    horizontal: androidx.compose.ui.unit.Dp,
) {
    if (wide) {
        item(key = "table-header", contentType = "table-header") {
            PieceTableHeader(columns, unit, Modifier.padding(top = 8.dp))
        }
        itemsIndexed(state.rows, key = { _, row -> row.sourceIds.first() }, contentType = { _, _ -> "table-row" }) { index, row ->
            PieceTableRow(row, index + 1, columns, unit, decimals, actions)
        }
    } else {
        itemsIndexed(state.rows, key = { _, row -> row.sourceIds.first() }, contentType = { _, _ -> "card" }) { index, row ->
            PieceCard(
                row = row,
                number = index + 1,
                showNumber = showNumbers,
                unit = unit,
                decimals = decimals,
                actions = actions,
                modifier = Modifier.padding(horizontal = horizontal, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun ProjectHeader(
    state: CutListUiState,
    onNameChange: (String) -> Unit,
    onUnitChange: (LengthUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().widthIn(max = 720.dp)) {
        OutlinedTextField(
            value = state.project.name,
            onValueChange = onNameChange,
            label = { Text("Project name") },
            placeholder = { Text(UNTITLED_PROJECT_NAME) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            textStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Project name" },
        )
        Spacer(Modifier.height(12.dp))
        SectionLabel("Units")
        Spacer(Modifier.height(6.dp))
        SegmentedChoice(
            options = LengthUnit.entries,
            selected = state.project.unit,
            label = { it.symbol },
            onSelect = onUnitChange,
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ListControls(
    sort: SortOption,
    consolidate: Boolean,
    onSort: (SortOption) -> Unit,
    onConsolidate: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.Box {
            AssistChip(
                onClick = { sortOpen = true },
                label = { Text(if (sort == SortOption.ORIGINAL) "Sort" else "Sort: ${sort.label}") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null) },
            )
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(sortMenuLabel(option)) },
                        leadingIcon = {
                            if (option == sort) Icon(Icons.Filled.Check, contentDescription = "Selected")
                        },
                        onClick = { sortOpen = false; onSort(option) },
                    )
                }
            }
        }
        FilterChip(
            selected = consolidate,
            onClick = { onConsolidate(!consolidate) },
            label = { Text("Combine identical") },
            leadingIcon = if (consolidate) {
                { Icon(Icons.Filled.Check, contentDescription = null) }
            } else {
                null
            },
        )
    }
}

private fun sortMenuLabel(option: SortOption): String = when (option) {
    SortOption.ORIGINAL -> "Original order"
    SortOption.NAME -> "Name A–Z"
    SortOption.LENGTH -> "Length (longest first)"
    SortOption.WIDTH -> "Width (widest first)"
    SortOption.QUANTITY -> "Quantity (most first)"
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Cut list", text))
}

private fun shareText(context: Context, title: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Cut list — $title")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share cut list"))
}

private fun exportFileName(projectName: String): String {
    val safe = projectName.replace(Regex("""[^A-Za-z0-9 _-]"""), "").trim().replace(' ', '_')
    return "${safe.ifEmpty { "Cut_List" }}.txt"
}
