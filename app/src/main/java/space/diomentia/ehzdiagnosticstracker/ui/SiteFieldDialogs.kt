package space.diomentia.ehzdiagnosticstracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.data.FieldSpec
import space.diomentia.ehzdiagnosticstracker.data.FieldType
import space.diomentia.ehzdiagnosticstracker.data.ReportTemplate
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun FieldSpecChooserDialog(
    allSpecs: List<FieldSpec>,
    addedSpecIds: Set<String>,
    onSpecsChosen: (List<FieldSpec>) -> Unit,
    onCancel: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember {
        allSpecs.map { it.id to false }.toMutableStateMap()
    }

    val sortedSpecs = remember(allSpecs, addedSpecIds) {
        allSpecs.sortedBy { if (it.id in addedSpecIds) 1 else 0 }
    }
    val filteredSpecs = remember(searchQuery, sortedSpecs) {
        if (searchQuery.isBlank()) sortedSpecs
        else sortedSpecs.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val hasSelection = selectedIds.any { it.value }

    // TODO: Fix first height, so it won't wobble when searching
    ActionCancelDialog(
        onAction = {
            val chosen = allSpecs.filter { selectedIds[it.id] == true }
            onSpecsChosen(chosen)
            onCancel()
        },
        actionLabel = { Text(stringResource(R.string.add)) },
        actionEnabled = hasSelection,
        onCancel = onCancel,
        label = { Text(stringResource(R.string.add_field)) }
    ) {
        Column {
            // TODO: default look of this text field doesn't match searching
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        stringResource(R.string.search_field),
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.alpha(.5f)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(filteredSpecs) { spec ->
                    val alreadyAdded = spec.id in addedSpecIds
                    val checked = selectedIds[spec.id] == true
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (alreadyAdded) .4f else 1f)
                            .clickable(enabled = !alreadyAdded) {
                                selectedIds[spec.id] = !checked
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = checked || alreadyAdded,
                            onCheckedChange = null,
                            enabled = !alreadyAdded,
                            modifier = Modifier
                                .padding(end = 8.dp)
                        )
                        Text(
                            spec.name + (spec.unit?.let { " ($it)" } ?: ""),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateChooserDialog(
    templates: List<ReportTemplate>,
    onTemplateChosen: (ReportTemplate) -> Unit,
    onCancel: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(-1) }

    ActionCancelDialog(
        onAction = {
            templates.getOrNull(selectedIndex)?.let {
                onTemplateChosen(it)
            }
            onCancel()
        },
        actionLabel = { Text(stringResource(R.string.add)) },
        actionEnabled = selectedIndex >= 0,
        onCancel = onCancel,
        label = { Text(stringResource(R.string.choose_template)) }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            items(templates.size) { i ->
                val selected = selectedIndex == i
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedIndex = i }
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        painterResource(
                            if (selected) R.drawable.ic_radio_button_checked
                            else R.drawable.ic_radio_button_unchecked
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .height(18.dp)
                            .padding(end = 12.dp)
                    )
                    Text(
                        templates[i].name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FieldSpecChooserDialogPreview() = PtmTheme {
    FieldSpecChooserDialog(
        allSpecs = listOf(
            FieldSpec("1", "Напряжение", FieldType.Decimal, "В"),
            FieldSpec("2", "Ток", FieldType.Decimal, "А"),
            FieldSpec("3", "Примечание", FieldType.Text)
        ),
        addedSpecIds = setOf("2"),
        onSpecsChosen = {},
        onCancel = {}
    )
}

@Preview
@Composable
private fun TemplateChooserDialogPreview() = PtmTheme {
    TemplateChooserDialog(
        templates = listOf(
            ReportTemplate("1", "СКЗ"),
            ReportTemplate("2", "УДЗ"),
            ReportTemplate("3", "КИП")
        ),
        onTemplateChosen = {},
        onCancel = {}
    )
}