package me.magnum.melonds.ui.cheats.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.magnum.melonds.R
import me.magnum.melonds.common.cheats.ModifierFamily
import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.ui.cheats.model.CheatFormDialogState
import me.magnum.melonds.ui.cheats.model.CheatListItem
import me.magnum.melonds.ui.cheats.model.CheatSubmissionForm
import me.magnum.melonds.ui.cheats.model.CheatsScreenUiState
import me.magnum.melonds.ui.cheats.ui.cheatform.CheatFormDialog
import me.magnum.melonds.ui.cheats.ui.item.CheatItem
import me.magnum.melonds.ui.cheats.ui.item.FamilyItem

@Composable
fun CheatListScreen(
    modifier: Modifier,
    contentPadding: PaddingValues,
    items: CheatsScreenUiState<List<CheatListItem>>,
    onSelectFamilyOption: (ModifierFamily, Int, Int?) -> Unit,
    onDisableFamily: (ModifierFamily) -> Unit,
    onCheatClick: (Cheat) -> Unit,
    onAddNewCheat: (CheatSubmissionForm) -> Unit,
    onUpdateCheat: (Cheat, CheatSubmissionForm) -> Unit,
    onDeleteCheatClick: (Cheat) -> Unit,
) {
    when (items) {
        is CheatsScreenUiState.Loading -> LoadingScreen(modifier.padding(contentPadding))
        is CheatsScreenUiState.Ready -> List(
            modifier = modifier,
            contentPadding = contentPadding,
            items = items.data,
            onSelectFamilyOption = onSelectFamilyOption,
            onDisableFamily = onDisableFamily,
            onCheatClick = onCheatClick,
            onAddNewCheat = onAddNewCheat,
            onUpdateCheat = onUpdateCheat,
            onDeleteCheatClick = onDeleteCheatClick,
        )
    }
}

/** What the lazy list actually renders: a family row, or a cheat row (plain, or an expanded family member). */
private sealed class ListRow(val key: String) {
    class CheatRow(val cheat: Cheat, key: String) : ListRow(key)
    class FamilyRow(val item: CheatListItem.Family) : ListRow(item.key.toString())
}

@Composable
private fun List(
    modifier: Modifier,
    contentPadding: PaddingValues,
    items: List<CheatListItem>,
    onSelectFamilyOption: (ModifierFamily, Int, Int?) -> Unit,
    onDisableFamily: (ModifierFamily) -> Unit,
    onCheatClick: (Cheat) -> Unit,
    onAddNewCheat: (CheatSubmissionForm) -> Unit,
    onUpdateCheat: (Cheat, CheatSubmissionForm) -> Unit,
    onDeleteCheatClick: (Cheat) -> Unit,
) {
    var cheatFormDialogState by rememberSaveable(stateSaver = CheatFormDialogState.Saver) { mutableStateOf(CheatFormDialogState.Hidden) }
    var expandedFamilies by rememberSaveable { mutableStateOf(ArrayList<String>()) }
    var dialogFamilyKey by rememberSaveable { mutableStateOf<String?>(null) }

    val dialogFamily = items.filterIsInstance<CheatListItem.Family>().firstOrNull { it.key == dialogFamilyKey }?.family
    if (dialogFamily != null) {
        ModifierFamilyDialog(
            family = dialogFamily,
            onDismiss = { dialogFamilyKey = null },
            onDisable = {
                onDisableFamily(dialogFamily)
                dialogFamilyKey = null
            },
            onConfirm = { value, level ->
                onSelectFamilyOption(dialogFamily, value, level)
                dialogFamilyKey = null
            },
        )
    }

    val rows = buildList {
        items.forEach { item ->
            when (item) {
                is CheatListItem.Single -> add(ListRow.CheatRow(item.cheat, item.key.toString()))
                is CheatListItem.Family -> {
                    add(ListRow.FamilyRow(item))
                    if (item.key in expandedFamilies) {
                        item.family.members.forEach { add(ListRow.CheatRow(it, "${item.key}/${it.id ?: it.code}")) }
                    }
                }
            }
        }
    }

    Box(modifier) {
        if (items.isEmpty()) {
            Text(
                modifier = Modifier.padding(contentPadding).padding(24.dp).align(Alignment.Center),
                text = stringResource(R.string.folder_is_empty),
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(
                modifier = modifier.consumeWindowInsets(contentPadding),
                contentPadding = PaddingValues(
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    top = contentPadding.calculateTopPadding(),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = contentPadding.calculateBottomPadding() + 16.dp + 56.dp + 16.dp, // Take FAB into consideration
                ),
            ) {
                itemsIndexed(
                    items = rows,
                    key = { _, row -> row.key },
                ) { index, row ->
                    if (index > 0) {
                        Divider()
                    }

                    when (row) {
                        is ListRow.FamilyRow -> FamilyItem(
                            modifier = Modifier.fillMaxWidth(),
                            family = row.item.family,
                            expanded = row.item.key in expandedFamilies,
                            onClick = { dialogFamilyKey = row.item.key.toString() },
                            onToggleExpanded = {
                                val key = row.item.key.toString()
                                expandedFamilies = ArrayList(if (key in expandedFamilies) expandedFamilies - key else expandedFamilies + key)
                            },
                        )
                        is ListRow.CheatRow -> CheatItem(
                            modifier = Modifier.fillMaxWidth(),
                            cheat = row.cheat,
                            onClick = { onCheatClick(row.cheat) },
                            onEditClick = { cheatFormDialogState = CheatFormDialogState.EditCheat(row.cheat) },
                            onDeleteClick = { onDeleteCheatClick(row.cheat) },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current) + 16.dp,
                ),
            onClick = { cheatFormDialogState = CheatFormDialogState.NewCheat },
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.AutoMirrored.Filled.PlaylistAdd),
                contentDescription = stringResource(R.string.add_cheat_folder),
            )
        }
    }

    CheatFormDialog(
        state = cheatFormDialogState,
        onDismiss = { cheatFormDialogState = CheatFormDialogState.Hidden },
        onSaveCheat = {
            val dialogState = cheatFormDialogState
            if (dialogState == CheatFormDialogState.NewCheat) {
                onAddNewCheat(it)
            } else if (dialogState is CheatFormDialogState.EditCheat) {
                onUpdateCheat(dialogState.cheat, it)
            }
            cheatFormDialogState = CheatFormDialogState.Hidden
        },
    )
}
