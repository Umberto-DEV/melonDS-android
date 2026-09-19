package me.magnum.melonds.ui.cheats.ui.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.magnum.melonds.R
import me.magnum.melonds.common.cheats.ModifierFamily
import me.magnum.melonds.ui.common.component.text.CaptionText
import me.magnum.melonds.ui.common.melonTextButtonColors

/** One row for a whole modifier family: title, the active choice, and a button that expands its members. */
@Composable
fun FamilyItem(
    modifier: Modifier,
    family: ModifierFamily,
    expanded: Boolean,
    onClick: () -> Unit,
    onToggleExpanded: () -> Unit,
) {
    val current = family.current
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            modifier = Modifier.padding(top = 4.dp),
            checked = current != null,
            onCheckedChange = null,
        )
        Column(
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(family.title)
            CaptionText(
                style = MaterialTheme.typography.body2,
                text = when {
                    current == null -> stringResource(R.string.modifier_family_inactive)
                    family.currentLevel != null -> "${current.label} · Lv ${family.currentLevel}"
                    else -> current.label
                },
            )
        }
        TextButton(onClick = onToggleExpanded, colors = melonTextButtonColors()) {
            Text(
                when {
                    expanded -> stringResource(R.string.modifier_family_hide)
                    family.members.size == 1 -> stringResource(R.string.modifier_family_show_code)
                    else -> stringResource(R.string.modifier_family_show_all, family.members.size)
                },
            )
        }
    }
}
