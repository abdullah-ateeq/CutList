package com.example.cutlist.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cutlist.CutListApplication
import com.example.cutlist.data.AppSettings
import com.example.cutlist.data.ProjectRepository
import com.example.cutlist.data.SettingsRepository
import com.example.cutlist.domain.CutListCalculator
import com.example.cutlist.domain.CutListProject
import com.example.cutlist.domain.CutListRow
import com.example.cutlist.domain.CutListSummary
import com.example.cutlist.domain.CutListTextFormatter
import com.example.cutlist.domain.CutPiece
import com.example.cutlist.domain.LengthUnit
import com.example.cutlist.domain.MaterialGroup
import com.example.cutlist.domain.PieceErrors
import com.example.cutlist.domain.PieceInput
import com.example.cutlist.domain.PieceValidationResult
import com.example.cutlist.domain.PieceValidator
import com.example.cutlist.domain.SortOption
import com.example.cutlist.domain.ThicknessGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.math.BigDecimal
import java.util.UUID

const val MAX_PIECES_PER_PROJECT = 200

data class CutListUiState(
    val project: CutListProject,
    val rows: List<CutListRow>,
    val summary: CutListSummary,
    val thicknessGroups: List<ThicknessGroup>,
    val materialGroups: List<MaterialGroup>,
    val sortOption: SortOption,
    val settings: AppSettings,
    val hasUnsavedChanges: Boolean,
    val materialSuggestions: List<String>,
) {
    val hasThickness: Boolean get() = thicknessGroups.any { it.thicknessMm != null }
    val hasMaterial: Boolean get() = materialGroups.any { it.material != null }
}

data class HistoryItem(val project: CutListProject, val summary: CutListSummary)

data class EditorState(
    val isEditing: Boolean,
    val sourceIds: List<String>,
    val originalPiece: CutPiece?,
    val initialInput: PieceInput,
    val input: PieceInput,
    val errors: PieceErrors = PieceErrors(),
)

sealed interface PendingConfirmation {
    data object NewCutList : PendingConfirmation
    data class DeleteRow(val row: CutListRow) : PendingConfirmation
    data class OpenProject(val project: CutListProject) : PendingConfirmation
    data class DeleteHistory(val project: CutListProject) : PendingConfirmation
    data object ClearHistory : PendingConfirmation
    data object ResetSettings : PendingConfirmation
}

sealed interface UiEvent {
    data class Message(
        val text: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
    ) : UiEvent

    data object Haptic : UiEvent
    data object ShowCutList : UiEvent
}

class CutListViewModel(
    private val settingsRepository: SettingsRepository,
    private val projectRepository: ProjectRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _project = MutableStateFlow(newProject(LengthUnit.MM))
    private val _sort = MutableStateFlow(SortOption.ORIGINAL)
    private val _editor = MutableStateFlow<EditorState?>(null)
    private val _pending = MutableStateFlow<PendingConfirmation?>(null)
    private val _isReady = MutableStateFlow(false)
    private val events = Channel<UiEvent>(Channel.BUFFERED)

    val editor: StateFlow<EditorState?> = _editor.asStateFlow()
    val pending: StateFlow<PendingConfirmation?> = _pending.asStateFlow()
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    val uiEvents: Flow<UiEvent> = events.receiveAsFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val history: StateFlow<List<CutListProject>> = projectRepository.history
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val historyItems: StateFlow<List<HistoryItem>> = history
        .map { projects -> projects.map { HistoryItem(it, CutListCalculator.summarize(it.pieces)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<CutListUiState> =
        combine(_project, _sort, settings, history) { project, sort, settings, history ->
            buildUiState(project, sort, settings, history)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            buildUiState(_project.value, _sort.value, AppSettings(), emptyList()),
        )

    private var lastDeletion: Pair<String, List<CutPiece>>? = null

    init {
        viewModelScope.launch {
            val draft = projectRepository.loadDraft()
            _project.value = draft ?: newProject(settingsRepository.settings.first().defaultUnit)
            _isReady.value = true
            // Persist the in-progress project so it survives process death; latest value wins.
            _project.collectLatest { project ->
                delay(DRAFT_SAVE_DELAY_MS)
                projectRepository.saveDraft(project)
            }
        }
    }

    // region Project

    fun setProjectName(name: String) {
        updateProject { it.copy(name = name.take(MAX_PROJECT_NAME_LENGTH)) }
    }

    /** Changes the display/entry unit. Stored millimeter values are untouched, so sizes are preserved. */
    fun setUnit(unit: LengthUnit) {
        updateProject { it.copy(unit = unit) }
    }

    fun setSort(option: SortOption) {
        _sort.value = option
    }

    fun setConsolidate(enabled: Boolean) {
        updateSettings { it.copy(consolidateIdentical = enabled) }
    }

    fun saveProject() {
        val project = _project.value.let { it.copy(name = it.name.trim(), updatedAt = clock()) }
        _project.value = project
        viewModelScope.launch {
            projectRepository.save(project)
            emit(UiEvent.Haptic)
            emit(UiEvent.Message("Saved “${project.displayName}”"))
        }
    }

    fun requestNewCutList() {
        if (uiState.value.hasUnsavedChanges && _project.value.pieces.isNotEmpty()) {
            _pending.value = PendingConfirmation.NewCutList
        } else {
            startNewCutList()
        }
    }

    private fun startNewCutList() {
        _project.value = newProject(settings.value.defaultUnit)
        _sort.value = SortOption.ORIGINAL
        emit(UiEvent.Message("New cut list started"))
    }

    fun duplicateCurrentProject() {
        _project.value = ProjectRepository.duplicateOf(_project.value, clock())
        emit(UiEvent.Message("Working on “${_project.value.displayName}” — not saved yet"))
    }

    fun cutListText(): String {
        val state = uiState.value
        return CutListTextFormatter.build(
            project = state.project,
            rows = state.rows,
            summary = state.summary,
            options = CutListTextFormatter.Options(
                decimals = state.settings.decimalPlaces,
                showArea = state.settings.showAreaSummary,
                showCutOrder = state.settings.showCutOrder,
            ),
        )
    }

    fun exportText(uri: Uri, contentResolver: ContentResolver) {
        val text = cutListText()
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
                } catch (_: IOException) {
                    false
                } catch (_: SecurityException) {
                    false
                }
            }
            emit(UiEvent.Message(if (success) "Cut list exported" else "Could not export the file"))
        }
    }

    fun showMessage(text: String) {
        emit(UiEvent.Message(text))
    }

    // endregion

    // region Pieces

    fun openAddPiece() {
        if (_project.value.pieces.size >= MAX_PIECES_PER_PROJECT) {
            emit(UiEvent.Message("A cut list can hold up to $MAX_PIECES_PER_PROJECT entries."))
            return
        }
        val input = PieceInput()
        _editor.value = EditorState(isEditing = false, sourceIds = emptyList(), originalPiece = null,
            initialInput = input, input = input)
    }

    fun openEditRow(row: CutListRow) {
        val input = PieceValidator.toInput(row.piece, _project.value.unit)
        _editor.value = EditorState(isEditing = true, sourceIds = row.sourceIds, originalPiece = row.piece,
            initialInput = input, input = input)
    }

    fun updateEditor(transform: (PieceInput) -> PieceInput) {
        _editor.update { state ->
            state ?: return@update null
            val updated = transform(state.input)
            val old = state.input
            val errors = state.errors.copy(
                name = state.errors.name.takeIf { updated.name == old.name },
                length = state.errors.length.takeIf { updated.length == old.length },
                width = state.errors.width.takeIf { updated.width == old.width },
                quantity = state.errors.quantity.takeIf { updated.quantity == old.quantity },
                thickness = state.errors.thickness.takeIf { updated.thickness == old.thickness },
            )
            state.copy(input = updated, errors = errors)
        }
    }

    fun dismissEditor() {
        _editor.value = null
    }

    /** Validates and stores the editor contents. With [addAnother], the editor stays open for the next piece. */
    fun saveEditor(addAnother: Boolean = false) {
        val state = _editor.value ?: return
        val project = _project.value
        val id = state.sourceIds.firstOrNull() ?: UUID.randomUUID().toString()
        when (val result = PieceValidator.validate(state.input, project.unit, id)) {
            is PieceValidationResult.Invalid -> _editor.value = state.copy(errors = result.errors)
            is PieceValidationResult.Valid -> {
                val piece = preserveUntouchedDimensions(result.piece, state)
                if (state.isEditing) {
                    updateProject { current ->
                        val firstIndex = current.pieces.indexOfFirst { it.id in state.sourceIds }
                        val remaining = current.pieces.filterNot { it.id in state.sourceIds }
                        val insertAt = if (firstIndex < 0) remaining.size else firstIndex.coerceAtMost(remaining.size)
                        current.copy(pieces = remaining.toMutableList().apply { add(insertAt, piece) })
                    }
                } else {
                    if (project.pieces.size >= MAX_PIECES_PER_PROJECT) {
                        _editor.value = null
                        emit(UiEvent.Message("A cut list can hold up to $MAX_PIECES_PER_PROJECT entries."))
                        return
                    }
                    updateProject { it.copy(pieces = it.pieces + piece) }
                }
                emit(UiEvent.Haptic)
                val canAddMore = _project.value.pieces.size < MAX_PIECES_PER_PROJECT
                if (addAnother && !state.isEditing && canAddMore) {
                    val next = PieceInput(thickness = state.input.thickness, material = state.input.material)
                    _editor.value = EditorState(false, emptyList(), null, next, next)
                    emit(UiEvent.Message("Added ${piece.name}"))
                } else {
                    _editor.value = null
                }
            }
        }
    }

    fun duplicateRow(row: CutListRow) {
        val project = _project.value
        if (project.pieces.size >= MAX_PIECES_PER_PROJECT) {
            emit(UiEvent.Message("A cut list can hold up to $MAX_PIECES_PER_PROJECT entries."))
            return
        }
        val copyName = "${row.piece.name} (copy)".take(PieceValidator.MAX_NAME_LENGTH)
        val copy = row.piece.copy(id = UUID.randomUUID().toString(), name = copyName)
        updateProject { current ->
            val lastIndex = current.pieces.indexOfLast { it.id in row.sourceIds }
            val insertAt = if (lastIndex < 0) current.pieces.size else lastIndex + 1
            current.copy(pieces = current.pieces.toMutableList().apply { add(insertAt, copy) })
        }
        emit(UiEvent.Message("Duplicated ${row.piece.name}"))
    }

    fun requestDeleteRow(row: CutListRow) {
        if (settings.value.confirmDelete) {
            _pending.value = PendingConfirmation.DeleteRow(row)
        } else {
            deleteRow(row, offerUndo = true)
        }
    }

    private fun deleteRow(row: CutListRow, offerUndo: Boolean) {
        val before = _project.value
        lastDeletion = before.id to before.pieces
        updateProject { it.copy(pieces = it.pieces.filterNot { piece -> piece.id in row.sourceIds }) }
        emit(UiEvent.Haptic)
        emit(
            if (offerUndo) {
                UiEvent.Message("Deleted ${row.piece.name}", "Undo", ::undoDelete)
            } else {
                UiEvent.Message("Deleted ${row.piece.name}")
            }
        )
    }

    private fun undoDelete() {
        val (projectId, pieces) = lastDeletion ?: return
        if (_project.value.id == projectId) {
            updateProject { it.copy(pieces = pieces) }
        }
        lastDeletion = null
    }

    // endregion

    // region History

    fun requestOpenProject(project: CutListProject) {
        val current = uiState.value
        if (current.hasUnsavedChanges && current.project.id != project.id && current.project.pieces.isNotEmpty()) {
            _pending.value = PendingConfirmation.OpenProject(project)
        } else {
            openProject(project)
        }
    }

    private fun openProject(project: CutListProject) {
        _project.value = project
        _sort.value = SortOption.ORIGINAL
        _editor.value = null
        emit(UiEvent.ShowCutList)
    }

    fun duplicateHistoryProject(project: CutListProject) {
        viewModelScope.launch {
            val copy = projectRepository.duplicate(project.id, clock())
            emit(UiEvent.Message(if (copy != null) "Created “${copy.displayName}”" else "Could not duplicate"))
        }
    }

    fun requestDeleteHistoryProject(project: CutListProject) {
        if (settings.value.confirmDelete) {
            _pending.value = PendingConfirmation.DeleteHistory(project)
        } else {
            deleteHistoryProject(project, offerUndo = true)
        }
    }

    private fun deleteHistoryProject(project: CutListProject, offerUndo: Boolean) {
        viewModelScope.launch {
            projectRepository.delete(project.id)
            emit(UiEvent.Haptic)
            val restore: (() -> Unit)? = if (offerUndo) {
                { viewModelScope.launch { projectRepository.save(project) } }
            } else {
                null
            }
            emit(UiEvent.Message("Deleted “${project.displayName}”", restore?.let { "Undo" }, restore))
        }
    }

    fun requestClearHistory() {
        if (history.value.isNotEmpty()) _pending.value = PendingConfirmation.ClearHistory
    }

    // endregion

    // region Settings

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun requestResetSettings() {
        _pending.value = PendingConfirmation.ResetSettings
    }

    // endregion

    // region Confirmations

    fun confirmPending() {
        val pending = _pending.value ?: return
        _pending.value = null
        when (pending) {
            PendingConfirmation.NewCutList -> startNewCutList()
            is PendingConfirmation.DeleteRow -> deleteRow(pending.row, offerUndo = false)
            is PendingConfirmation.OpenProject -> openProject(pending.project)
            is PendingConfirmation.DeleteHistory -> deleteHistoryProject(pending.project, offerUndo = false)
            PendingConfirmation.ClearHistory -> viewModelScope.launch {
                projectRepository.clearHistory()
                emit(UiEvent.Message("History cleared"))
            }
            PendingConfirmation.ResetSettings -> viewModelScope.launch {
                settingsRepository.reset()
                emit(UiEvent.Message("Settings reset"))
            }
        }
    }

    fun dismissPending() {
        _pending.value = null
    }

    // endregion

    private fun updateProject(transform: (CutListProject) -> CutListProject) {
        _project.update(transform)
    }

    private fun emit(event: UiEvent) {
        events.trySend(event)
    }

    private fun newProject(unit: LengthUnit): CutListProject {
        val now = clock()
        return CutListProject(UUID.randomUUID().toString(), "", unit, emptyList(), now, now)
    }

    /**
     * If a dimension's text was not changed in the editor, keep the original exact millimeter value
     * instead of the value re-parsed from rounded text (avoids drift for inch/foot conversions).
     */
    private fun preserveUntouchedDimensions(piece: CutPiece, state: EditorState): CutPiece {
        val original = state.originalPiece ?: return piece
        fun keep(field: (PieceInput) -> String, originalValue: BigDecimal?, newValue: BigDecimal?) =
            if (field(state.input) == field(state.initialInput)) originalValue else newValue
        return piece.copy(
            lengthMm = keep({ it.length }, original.lengthMm, piece.lengthMm) ?: piece.lengthMm,
            widthMm = keep({ it.width }, original.widthMm, piece.widthMm) ?: piece.widthMm,
            thicknessMm = keep({ it.thickness }, original.thicknessMm, piece.thicknessMm),
        )
    }

    private fun buildUiState(
        project: CutListProject,
        sort: SortOption,
        settings: AppSettings,
        history: List<CutListProject>,
    ): CutListUiState {
        val pieces = project.pieces
        val rows = CutListCalculator.sortRows(CutListCalculator.buildRows(pieces, settings.consolidateIdentical), sort)
        val saved = history.firstOrNull { it.id == project.id }
        val unsaved = if (saved == null) pieces.isNotEmpty() || project.name.isNotBlank() else !saved.hasSameContentAs(project)
        return CutListUiState(
            project = project,
            rows = rows,
            summary = CutListCalculator.summarize(pieces),
            thicknessGroups = CutListCalculator.groupByThickness(pieces),
            materialGroups = CutListCalculator.groupByMaterial(pieces),
            sortOption = sort,
            settings = settings,
            hasUnsavedChanges = unsaved,
            materialSuggestions = pieces.mapNotNull { it.material }.distinctBy { it.lowercase() }.take(6),
        )
    }

    companion object {
        private const val DRAFT_SAVE_DELAY_MS = 400L
        const val MAX_PROJECT_NAME_LENGTH = 60

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CutListApplication
                CutListViewModel(app.container.settingsRepository, app.container.projectRepository)
            }
        }
    }
}
