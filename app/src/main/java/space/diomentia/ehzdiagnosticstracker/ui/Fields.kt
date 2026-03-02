package space.diomentia.ehzdiagnosticstracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun TextFieldWithLabel(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholderText: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next
    ),
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null
) {
    val textStyle = LocalTextStyle.current
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
    ) {
        label?.let {
            Box(
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleSmall) {
                    label()
                }
            }
        }
        OutlinedTextField(
            value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholderText,
                    style = textStyle,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    modifier = Modifier.alpha(.5f)
                )
            },
            keyboardOptions = keyboardOptions,
            isError = isError,
            supportingText = supportingText,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (label != null) Modifier.padding(start = 12.dp) else Modifier)
        )
    }
}

@Preview
@Composable
private fun TextFieldWithLabelPreview() = PtmTheme {
    TextFieldWithLabel(
        label = { Text("Label") },
        value = "",
        onValueChange = {},
        placeholderText = "Введите значение"
    )
}