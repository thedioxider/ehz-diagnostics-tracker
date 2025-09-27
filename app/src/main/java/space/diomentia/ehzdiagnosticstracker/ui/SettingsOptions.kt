package space.diomentia.ehzdiagnosticstracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme

@Composable
fun SettingsOption(
    label: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    icon: Painter? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .defaultMinSize(minHeight = 36.dp)
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(32.dp)
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium
            )
            if (secondaryLabel != null) {
                Text(
                    secondaryLabel,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .alpha(.7f)
                )
            }
        }
    }
}

@Composable
fun SettingsOptionsDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        thickness = 2.dp,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .then(modifier)
    )
}


@Preview(showBackground = true)
@Composable
private fun SettingsOptionPreview() {
    PtmTheme {
        SettingsOption(
            label = "Some settings option",
            secondaryLabel = "Description of this option",
            icon = rememberVectorPainter(Icons.Outlined.Settings)
        ) {}
    }
}