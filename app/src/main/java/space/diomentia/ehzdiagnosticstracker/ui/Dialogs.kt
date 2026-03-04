package space.diomentia.ehzdiagnosticstracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.util.UUID

@Composable
fun PtmDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest,
        DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(Modifier.padding(horizontal = 32.dp)) {
            BorderedDialogContainer(
                modifier = modifier,
                content = content
            )
        }
    }
}

@Composable
fun ActionCancelDialog(
    onCancel: () -> Unit,
    onAction: () -> Unit = {},
    actionLabel: (@Composable () -> Unit)? = null,
    actionEnabled: Boolean = true,
    label: @Composable () -> Unit = {},
    onSecondaryAction: () -> Unit = {},
    secondaryActionLabel: (@Composable () -> Unit)? = null,
    secondaryActionEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    PtmDialog(onCancel) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .padding(16.dp)
        ) {
            ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                label()
            }

            Box(Modifier.padding(8.dp)) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    content()
                }
            }

            Spacer(Modifier.height(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.End,
                itemVerticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                actionLabel?.let {
                    PtmFilledButton(
                        enabled = actionEnabled,
                        onClick = {
                            onCancel()
                            onAction()
                        },
                        content = actionLabel,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                secondaryActionLabel?.let {
                    PtmFilledButton(
                        enabled = secondaryActionEnabled,
                        onClick = {
                            onCancel()
                            onSecondaryAction()
                        },
                        content = secondaryActionLabel,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                PtmFilledIconButton(
                    onClick = onCancel,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteDialog(
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    label: @Composable () -> Unit,
    name: String,
    details: String? = null
) = ActionCancelDialog(
    onAction = onDelete,
    onCancel = onCancel,
    actionLabel = { Text(stringResource(R.string.delete)) },
    label = label
) {
    Column {
        Text(
            name,
            style = MaterialTheme.typography.displaySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        details?.let {
            Text(
                details,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .alpha(.5f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class DialogField(
    val name: String,
    val checker: (String) -> Boolean = { true },
    val supportingText: String? = null,
    val mutable: Boolean = true,
    val initValue: String? = null,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val allowUnchecked: Boolean = true,
    val nullable: Boolean = false
)

@Composable
fun EditFieldsDialog(
    fields: List<Pair<Int, DialogField>>,
    onAction: (Map<Int, String>) -> Unit,
    actionLabel: @Composable () -> Unit,
    label: @Composable () -> Unit = {},
    onSecondaryAction: (Map<Int, String>) -> Unit = {},
    secondaryActionLabel: (@Composable () -> Unit)? = null,
    onCancel: () -> Unit
) {
    val fieldMap = fields.toMap()
    val valuesErrors: Map<Int, Pair<MutableState<String>, MutableState<Boolean>>> = remember {
        fields.associate { (k, field) ->
            k to (mutableStateOf(field.initValue ?: "") to mutableStateOf(false))
        }
    }

    val focusRequester = remember { FocusRequester() }
    var focusAttached = false

    ActionCancelDialog(
        onAction = { onAction(valuesErrors.mapValues { (_, v) -> v.first.value }) },
        actionLabel = actionLabel,
        actionEnabled = valuesErrors.all { (k, v) -> (fieldMap[k]!!.nullable || !v.first.value.isBlank()) && !v.second.value },
        onSecondaryAction = { onSecondaryAction(valuesErrors.mapValues { (_, v) -> v.first.value }) },
        secondaryActionLabel = secondaryActionLabel,
        label = label,
        onCancel = onCancel
    ) {
        LazyColumn {
            itemsIndexed(fields) { i, (k, field) ->
                val ve = remember { valuesErrors[k]!! }
                if (field.mutable) {
                    var v by ve.first
                    var e by ve.second
                    OutlinedTextField(
                        v,
                        onValueChange = { input: String ->
                            if (field.allowUnchecked || field.checker(input)) {
                                v = input
                                e = !field.checker(input)
                            }
                        },
                        label = { Text(field.name) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = field.keyboardType,
                            imeAction = if (i == fields.lastIndex) ImeAction.Done else ImeAction.Next
                        ),
                        isError = e,
                        supportingText = { field.supportingText?.let { s -> if (e) Text(s) } },
                        maxLines = 1,
                        modifier = if (!focusAttached && field.mutable) {
                            focusAttached = true
                            Modifier.focusRequester(focusRequester)
                        } else Modifier
                    )
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                } else {
                    val v by ve.first
                    Text(
                        "${field.name}:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        v,
                        modifier = Modifier
                            .padding(start = 16.dp, top = 4.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ActionCancelDialogPreview() = PtmTheme {
    ActionCancelDialog(
        {},
        {},
        { Text("Принять") },
        onSecondaryAction = {},
        secondaryActionLabel = { Text("Удалить") },
        label = { Text("Label") }
    ) {
        Text(stringResource(R.string.lipsum), maxLines = 10)
    }
}

@Preview
@Composable
private fun EditFieldsDialogPreview() = PtmTheme {
    EditFieldsDialog(
        fields = listOf(
            0 to DialogField(
                "Id",
                { true },
                mutable = false,
                initValue = UUID.randomUUID().toString()
            )
        ),
        actionLabel = { Text(stringResource(R.string.save)) },
        label = { Text(stringResource(R.string.edit_object)) },
        onAction = {},
        onCancel = {}
    )
}

@Preview
@Composable
private fun DeleteDialogPreview() = PtmTheme {
    DeleteDialog({}, {}, { Text(stringResource(R.string.delete_object)) }, "САЦ-III", "Трубопровод")
}