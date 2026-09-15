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
import me.magnum.melonds.common.cheats.PokemonSpecies
import me.magnum.melonds.common.cheats.WildEncounterCheat
import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.ui.cheats.model.CheatSubmissionForm

@Composable
fun WildEncounterDialog(cheat: Cheat, onDismiss: () -> Unit, onConfirm: (CheatSubmissionForm) -> Unit) {
    val initial = remember(cheat.code) { WildEncounterCheat.selection(cheat.code) }
    var species by rememberSaveable(cheat.id) { mutableStateOf(initial?.species) }
    var level by rememberSaveable(cheat.id) { mutableStateOf((initial?.level ?: 5).toString()) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(cheat.id) { mutableStateOf("") }
    val matches = remember(query) { PokemonSpecies.search(query) }
    val selected = species?.let { PokemonSpecies.all[it - 1] }
    val levelValue = level.toIntOrNull()?.takeIf { it in 1..100 }
    val name = stringResource(R.string.wild_encounter_selection, selected?.name.orEmpty(), levelValue ?: 0)
    val description = stringResource(R.string.wild_encounter_help)

    if (showHelp) {
        AlertDialog(onDismissRequest = { showHelp = false }, title = { Text(stringResource(R.string.wild_encounter_title)) },
            text = { Text(description) }, confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text(stringResource(android.R.string.ok)) }
            })
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.surface) {
            Column(Modifier.safeDrawingPadding().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.wild_encounter_title), Modifier.weight(1f), style = MaterialTheme.typography.h6)
                    TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                    Button(enabled = selected != null && levelValue != null, onClick = {
                        val chosen = species ?: return@Button
                        val chosenLevel = levelValue ?: return@Button
                        onConfirm(CheatSubmissionForm(name, description, WildEncounterCheat.code(chosen, chosenLevel)))
                    }) { Text(stringResource(R.string.wild_encounter_enable)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.wild_encounter_summary), Modifier.weight(1f), style = MaterialTheme.typography.caption)
                    TextButton(onClick = { showHelp = true }) { Text(stringResource(R.string.wild_encounter_details)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f), value = query, onValueChange = { query = it },
                        label = { Text(stringResource(R.string.wild_encounter_search)) }, singleLine = true,
                    )
                    OutlinedTextField(
                        modifier = Modifier.width(124.dp),
                        value = level,
                        onValueChange = { value -> if (value.length <= 3 && value.all { it in '0'..'9' }) level = value },
                        label = { Text(stringResource(R.string.wild_encounter_level)) },
                        isError = levelValue == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Text(selected?.label ?: stringResource(R.string.wild_encounter_choose))
                if (species == 201) Text(stringResource(R.string.wild_encounter_unown), style = MaterialTheme.typography.caption)
                if (matches.isEmpty()) Text(stringResource(R.string.wild_encounter_no_results))
                LazyColumn(Modifier.weight(1f)) {
                    items(matches, key = { it.number }) { pokemon ->
                        Row(
                            Modifier.fillMaxWidth().clickable { species = pokemon.number }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = species == pokemon.number, onClick = { species = pokemon.number })
                            Text(pokemon.label)
                        }
                    }
                }

            }
        }
    }
}
