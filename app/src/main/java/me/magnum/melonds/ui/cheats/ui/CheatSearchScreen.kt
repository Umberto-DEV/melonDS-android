package me.magnum.melonds.ui.cheats.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.domain.model.CheatInFolder
import me.magnum.melonds.domain.model.CheatFolder
import me.magnum.melonds.ui.cheats.model.CheatFormDialogState
import me.magnum.melonds.ui.cheats.model.CheatSubmissionForm
import me.magnum.melonds.ui.cheats.model.CheatSearchRow
import me.magnum.melonds.ui.cheats.model.CheatsScreenUiState
import me.magnum.melonds.ui.cheats.ui.cheatform.CheatFormDialog
import me.magnum.melonds.ui.cheats.ui.item.HighlightedText
import me.magnum.melonds.ui.cheats.ui.item.CheatInFolderItem

@Composable
fun CheatSearchScreen(
    modifier: Modifier,
    contentPadding: PaddingValues,
    searchResults: CheatsScreenUiState<List<CheatSearchRow.Match>>,
    query: String,
    onBack: () -> Unit,
    onCheatClick: (Cheat) -> Unit,
    onUpdateCheat: (Cheat, CheatSubmissionForm) -> Unit,
    onDeleteCheatClick: (CheatFolder, Cheat) -> Unit,
    onSearchResultClick: (CheatSearchRow.Match) -> Unit,
) {
    var cheatFormDialogState by rememberSaveable(stateSaver = CheatFormDialogState.Saver) {
        mutableStateOf(CheatFormDialogState.Hidden)
    }

    when (searchResults) {
        is CheatsScreenUiState.Loading -> LoadingScreen(modifier.padding(contentPadding))
        is CheatsScreenUiState.Ready -> {
            if (searchResults.data.isEmpty()) {
                Box(modifier.fillMaxSize().padding(contentPadding)) {
                    Text(
                        modifier = Modifier.padding(24.dp).align(Alignment.Center),
                        text = stringResource(R.string.no_cheats_found_from_search),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                        top = contentPadding.calculateTopPadding(),
                        end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                    ),
                ) {
                    items(
                        items = searchResults.data,
                        key = { it.key },
                    ) { result ->
                        val cheat = result.cheat
                        if (cheat != null) CheatInFolderItem(
                            modifier = Modifier.fillMaxWidth(),
                            cheatInFolder = CheatInFolder(cheat, result.folder.name),
                            highlightQuery = query.trim(),
                            onEditClick = { cheatFormDialogState = CheatFormDialogState.EditCheat(cheat) },
                            onDeleteClick = { onDeleteCheatClick(result.folder, cheat) },
                            onClick = { onCheatClick(cheat) },
                        )
                        else Column(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .clickable { onSearchResultClick(result) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            HighlightedText(text = result.folder.name, query = query.trim())
                            HighlightedText(text = result.label, query = query.trim())
                            result.familyTitle?.let { Text(it) }
                        }
                    }
                }
            }
        }
    }

    CheatFormDialog(
        state = cheatFormDialogState,
        onDismiss = { cheatFormDialogState = CheatFormDialogState.Hidden },
        onSaveCheat = { form ->
            val dialogState = cheatFormDialogState
            if (dialogState is CheatFormDialogState.EditCheat) {
                onUpdateCheat(dialogState.cheat, form)
            }
            cheatFormDialogState = CheatFormDialogState.Hidden
        },
    )

    BackHandler {
        onBack()
    }
}
