package me.magnum.melonds.ui.cheats.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.magnum.melonds.R
import me.magnum.melonds.common.cheats.ModifierFamily
import me.magnum.melonds.common.cheats.ModifierKind
import me.magnum.melonds.common.cheats.ModifierOption
import me.magnum.melonds.common.cheats.ModifierSource
import me.magnum.melonds.common.cheats.PokemonSpecies
import me.magnum.melonds.ui.common.melonTextButtonColors

/**
 * Picker for a modifier family: a searchable list of options (species, natures, levels, anything enumerated)
 * and, when the family carries a level parameter, a numeric level field. [onConfirm] receives the chosen
 * option value and the level (null when the family has no level parameter).
 */
@Composable
fun ModifierFamilyDialog(family: ModifierFamily, onDismiss: () -> Unit, onDisable: () -> Unit, onConfirm: (value: Int, level: Int?) -> Unit) {
    val key = "${family.folderName}/${family.title}"
    val levelOnly = family.source == ModifierSource.ITEM_LOAD && family.kind == ModifierKind.LEVEL
    val showsLevelField = family.requiresLevel || levelOnly
    var value by rememberSaveable(key) { mutableStateOf(family.current?.value) }
    var level by rememberSaveable(key) { mutableStateOf((if (levelOnly) family.current?.value else family.currentLevel)?.toString() ?: "5") }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(key) { mutableStateOf("") }
    val matches = remember(query, family) { search(family.options, query) }
    val selected = family.options.firstOrNull { it.value == value }
    val levelValue = level.toIntOrNull()?.takeIf { it in 1..100 }
    val canConfirm = (levelOnly || selected != null) && (!showsLevelField || levelValue != null)
    val help = family.instructions ?: stringResource(R.string.wild_encounter_help)

    if (showHelp) {
        AlertDialog(onDismissRequest = { showHelp = false }, title = { Text(family.title) },
            text = { Text(help) }, confirmButton = {
                TextButton(onClick = { showHelp = false }, colors = melonTextButtonColors()) { Text(stringResource(android.R.string.ok)) }
            })
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.surface) {
            Column(Modifier.safeDrawingPadding().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(family.title, Modifier.weight(1f), style = MaterialTheme.typography.h6)
                    TextButton(onClick = onDismiss, colors = melonTextButtonColors()) { Text(stringResource(android.R.string.cancel)) }
                    Button(enabled = canConfirm, onClick = {
                        if (levelOnly) onConfirm(levelValue ?: return@Button, null)
                        else onConfirm(selected?.value ?: return@Button, if (family.requiresLevel) levelValue else null)
                    }) { Text(stringResource(R.string.wild_encounter_enable)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(family.folderName, Modifier.weight(1f), style = MaterialTheme.typography.caption)
                    if (family.active) {
                        TextButton(onClick = onDisable, colors = melonTextButtonColors()) { Text(stringResource(R.string.modifier_family_disable)) }
                    }
                    TextButton(onClick = { showHelp = true }, colors = melonTextButtonColors()) { Text(stringResource(R.string.wild_encounter_details)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!levelOnly) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f), value = query, onValueChange = { query = it },
                            label = { Text(stringResource(R.string.wild_encounter_search)) }, singleLine = true,
                        )
                    }
                    if (showsLevelField) {
                        OutlinedTextField(
                            modifier = if (levelOnly) Modifier.weight(1f) else Modifier.width(124.dp),
                            value = level,
                            onValueChange = { new -> if (new.length <= 3 && new.all { it in '0'..'9' }) level = new },
                            label = { Text(stringResource(R.string.wild_encounter_level)) },
                            isError = levelValue == null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
                if (!levelOnly) {
                    Text(selected?.label ?: stringResource(R.string.wild_encounter_choose))
                    if (family.kind == ModifierKind.SPECIES && value == 201) Text(stringResource(R.string.wild_encounter_unown), style = MaterialTheme.typography.caption)
                    if (matches.isEmpty()) Text(stringResource(R.string.wild_encounter_no_results))
                    LazyColumn(Modifier.weight(1f)) {
                        items(matches, key = { it.value }) { option ->
                            Row(
                                Modifier.fillMaxWidth().clickable { value = option.value }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = value == option.value, onClick = { value = option.value })
                                Text(option.label)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A number matches an option value ("#025" → Pikachu); when no value matches — level and nature families
 * carry raw code words as values — it falls back to a text match on the label, so "7" still finds "Level 7".
 */
private fun search(options: List<ModifierOption>, query: String): List<ModifierOption> {
    val text = query.trim()
    if (text.isEmpty()) return options
    val byValue = text.removePrefix("#").toIntOrNull()?.let { number -> options.filter { it.value == number } }
    if (!byValue.isNullOrEmpty()) return byValue
    val needle = PokemonSpecies.normalize(text)
    return options.filter { PokemonSpecies.normalize(it.label).contains(needle) }
}
