package com.example.cutlist.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cutlist.data.AppSettings
import com.example.cutlist.data.ThemeMode
import com.example.cutlist.domain.LengthUnit
import com.example.cutlist.domain.MeasurementFormatter
import com.example.cutlist.ui.CutListViewModel
import com.example.cutlist.ui.components.SectionLabel
import com.example.cutlist.ui.components.SegmentedChoice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: CutListViewModel, snackbarHostState: SnackbarHostState) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val update: ((AppSettings) -> AppSettings) -> Unit = viewModel::updateSettings
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SectionLabel("Appearance")
                Text("Theme", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                SegmentedChoice(
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    label = { it.label },
                    onSelect = { mode -> update { it.copy(themeMode = mode) } },
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                SwitchRow(
                    title = "Dynamic color",
                    subtitle = if (dynamicSupported) "Use colors from your wallpaper" else "Requires Android 12 or later",
                    checked = settings.dynamicColor && dynamicSupported,
                    enabled = dynamicSupported,
                    onCheckedChange = { checked -> update { it.copy(dynamicColor = checked) } },
                )

                SettingsDivider()
                SectionLabel("Units & precision")
                Text("Default unit for new cut lists", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                SegmentedChoice(
                    options = LengthUnit.entries,
                    selected = settings.defaultUnit,
                    label = { it.symbol },
                    onSelect = { unit -> update { it.copy(defaultUnit = unit) } },
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                Text("Decimal places", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                SegmentedChoice(
                    options = (MeasurementFormatter.MIN_DECIMALS..MeasurementFormatter.MAX_DECIMALS).toList(),
                    selected = settings.decimalPlaces,
                    label = { it.toString() },
                    onSelect = { places -> update { it.copy(decimalPlaces = places) } },
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                Text(
                    "Whole numbers are shown without decimals (1200 mm, not 1200.00 mm).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingsDivider()
                SectionLabel("Cut list")
                SwitchRow(
                    title = "Consolidate identical pieces",
                    subtitle = "Combine entries with the same name, size, thickness, material and notes",
                    checked = settings.consolidateIdentical,
                    onCheckedChange = { checked -> update { it.copy(consolidateIdentical = checked) } },
                )
                SwitchRow(
                    title = "Show cut order numbers",
                    subtitle = "Number rows in list order",
                    checked = settings.showCutOrder,
                    onCheckedChange = { checked -> update { it.copy(showCutOrder = checked) } },
                )
                SwitchRow(
                    title = "Show area summary",
                    checked = settings.showAreaSummary,
                    onCheckedChange = { checked -> update { it.copy(showAreaSummary = checked) } },
                )
                SwitchRow(
                    title = "Show material summary",
                    checked = settings.showMaterialSummary,
                    onCheckedChange = { checked -> update { it.copy(showMaterialSummary = checked) } },
                )
                SwitchRow(
                    title = "Show thickness summary",
                    checked = settings.showThicknessSummary,
                    onCheckedChange = { checked -> update { it.copy(showThicknessSummary = checked) } },
                )

                SettingsDivider()
                SectionLabel("Behavior")
                SwitchRow(
                    title = "Haptic feedback",
                    checked = settings.hapticFeedback,
                    onCheckedChange = { checked -> update { it.copy(hapticFeedback = checked) } },
                )
                SwitchRow(
                    title = "Confirm delete",
                    subtitle = "When off, deletions can be undone from the snackbar",
                    checked = settings.confirmDelete,
                    onCheckedChange = { checked -> update { it.copy(confirmDelete = checked) } },
                )

                SettingsDivider()
                SectionLabel("Data")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = viewModel::requestResetSettings, modifier = Modifier.weight(1f)) {
                        Text("Reset settings")
                    }
                    OutlinedButton(onClick = viewModel::requestClearHistory, modifier = Modifier.weight(1f)) {
                        Text("Clear history")
                    }
                }
                Text(
                    "All data is stored only on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(vertical = 12.dp))
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
