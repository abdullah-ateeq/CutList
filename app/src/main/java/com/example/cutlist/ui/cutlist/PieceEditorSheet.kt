package com.example.cutlist.ui.cutlist

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cutlist.domain.GrainDirection
import com.example.cutlist.domain.LengthUnit
import com.example.cutlist.domain.NumberParser
import com.example.cutlist.domain.PieceInput
import com.example.cutlist.domain.PieceValidator
import com.example.cutlist.ui.EditorState
import com.example.cutlist.ui.components.SectionLabel
import com.example.cutlist.ui.components.SegmentedChoice
import com.example.cutlist.ui.theme.tabular

private const val MAX_NUMBER_INPUT = 16

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PieceEditorSheet(
    state: EditorState,
    unit: LengthUnit,
    materialSuggestions: List<String>,
    onChange: ((PieceInput) -> PieceInput) -> Unit,
    onSave: (addAnother: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        PieceEditorContent(state, unit, materialSuggestions, onChange, onSave, onDismiss)
    }
}

@Composable
private fun PieceEditorContent(
    state: EditorState,
    unit: LengthUnit,
    materialSuggestions: List<String>,
    onChange: ((PieceInput) -> PieceInput) -> Unit,
    onSave: (addAnother: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val input = state.input
    val errors = state.errors
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(state.isEditing, state.initialInput) {
        if (!state.isEditing) nameFocus.requestFocus()
    }
    val decimalKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.isEditing) "Edit piece" else "Add piece",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Units: ${unit.symbol}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = input.name,
            onValueChange = { v -> onChange { it.copy(name = v.take(PieceValidator.MAX_NAME_LENGTH)) } },
            label = { Text("Piece name") },
            placeholder = { Text("e.g. Top shelf") },
            singleLine = true,
            isError = errors.name != null,
            supportingText = errors.name?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DimensionField(
                label = "Length",
                value = input.length,
                unit = unit,
                error = errors.length,
                keyboardOptions = decimalKeyboard,
                onValueChange = { v -> onChange { it.copy(length = v) } },
                modifier = Modifier.weight(1f),
            )
            DimensionField(
                label = "Width",
                value = input.width,
                unit = unit,
                error = errors.width,
                keyboardOptions = decimalKeyboard,
                onValueChange = { v -> onChange { it.copy(width = v) } },
                modifier = Modifier.weight(1f),
            )
        }

        QuantityField(
            value = input.quantity,
            error = errors.quantity,
            onValueChange = { v -> onChange { it.copy(quantity = v) } },
        )

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionLabel("Optional details")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DimensionField(
                label = "Thickness",
                value = input.thickness,
                unit = unit,
                error = errors.thickness,
                keyboardOptions = decimalKeyboard,
                onValueChange = { v -> onChange { it.copy(thickness = v) } },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = input.material,
                onValueChange = { v -> onChange { it.copy(material = v.take(PieceValidator.MAX_MATERIAL_LENGTH)) } },
                label = { Text("Material") },
                placeholder = { Text("e.g. Plywood") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f),
            )
        }
        val suggestions = materialSuggestions.filterNot { it.equals(input.material.trim(), ignoreCase = true) }
        if (suggestions.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                suggestions.take(4).forEach { material ->
                    SuggestionChip(
                        onClick = { onChange { it.copy(material = material) } },
                        label = { Text(material, maxLines = 1) },
                    )
                }
            }
        }

        OutlinedTextField(
            value = input.notes,
            onValueChange = { v -> onChange { it.copy(notes = v.take(PieceValidator.MAX_NOTES_LENGTH)) } },
            label = { Text("Notes") },
            placeholder = { Text("e.g. Edge band front") },
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Rotation allowed", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Planning note for manual layout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = input.rotationAllowed,
                onCheckedChange = { checked -> onChange { it.copy(rotationAllowed = checked) } },
                modifier = Modifier.semantics { contentDescription = "Rotation allowed" },
            )
        }

        Text("Grain direction", style = MaterialTheme.typography.bodyLarge)
        SegmentedChoice(
            options = GrainDirection.entries,
            selected = input.grainDirection,
            label = { it.label },
            onSelect = { grain -> onChange { it.copy(grainDirection = grain) } },
        )
        Text(
            "Rotation and grain are informational. CutList does not optimize layouts or nesting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            if (!state.isEditing) {
                OutlinedButton(onClick = { onSave(true) }) { Text("Save + next") }
            }
            Button(onClick = { onSave(false) }) { Text("Save piece") }
        }
    }
}

@Composable
private fun DimensionField(
    label: String,
    value: String,
    unit: LengthUnit,
    error: String?,
    keyboardOptions: KeyboardOptions,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= MAX_NUMBER_INPUT) onValueChange(it) },
        label = { Text(label) },
        suffix = { Text(unit.symbol) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyLarge.tabular(),
        modifier = modifier,
    )
}

@Composable
private fun QuantityField(value: String, error: String?, onValueChange: (String) -> Unit) {
    val current = NumberParser.parseWholeNumber(value)
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 5) onValueChange(it.filter(Char::isDigit)) },
        label = { Text("Quantity") },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        textStyle = MaterialTheme.typography.titleMedium.tabular().copy(textAlign = TextAlign.Center),
        leadingIcon = {
            IconButton(
                onClick = {
                    val next = ((current ?: 1) - 1).coerceIn(PieceValidator.MIN_QUANTITY, PieceValidator.MAX_QUANTITY)
                    onValueChange(next.toString())
                },
                enabled = current == null || current > PieceValidator.MIN_QUANTITY,
            ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity") }
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    val next = ((current ?: 0) + 1).coerceIn(PieceValidator.MIN_QUANTITY, PieceValidator.MAX_QUANTITY)
                    onValueChange(next.toString())
                },
                enabled = current == null || current < PieceValidator.MAX_QUANTITY,
            ) { Icon(Icons.Filled.Add, contentDescription = "Increase quantity") }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
