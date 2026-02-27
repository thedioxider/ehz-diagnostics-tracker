package space.diomentia.ehzdiagnosticstracker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import space.diomentia.ehzdiagnosticstracker.ui.theme.blue_oxford

val PtmOutlinedButtonColors
    @Composable
    get() = ButtonColors(
        contentColor = MaterialTheme.colorScheme.primary,
        containerColor = Color.Transparent,
        disabledContentColor = blue_oxford,
        disabledContainerColor = Color.Transparent
    )

@Composable
fun PtmOutlinedButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = PtmOutlinedButtonColors,
    roundedCorners: Boolean = false,
    content: @Composable () -> Unit
) {
    val buttonShape: Shape = RoundedCornerShape(if (roundedCorners) 25 else 0)
    var buttonMinSize by remember { mutableStateOf(0.dp) }
    val contentColor by animateColorAsState(if (enabled) colors.contentColor else colors.disabledContentColor)
    val containerColor by animateColorAsState(if (enabled) colors.containerColor else colors.containerColor)
    Surface(
        modifier = Modifier
            .clip(buttonShape)
            .getActualSize {
                buttonMinSize = min(it.width, it.height)
            }
            .then(modifier),
        shape = buttonShape,
        border = BorderStroke(
            width = buttonMinSize / 16,
            color = contentColor
        ),
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(Modifier.padding(buttonMinSize / 8)) {
            content()
        }
    }
}

@Composable
fun PtmFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = blue_oxford,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContentColor = MaterialTheme.colorScheme.onPrimary
    ),
    content: @Composable () -> Unit
) {
    FilledTonalButton(
        onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RectangleShape,
        colors = colors
    ) {
        content()
    }
}

@Composable
fun PtmFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = blue_oxford,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContentColor = MaterialTheme.colorScheme.onPrimary
    ),
    content: @Composable () -> Unit
) {
    FilledTonalIconButton(
        onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RectangleShape,
        colors = colors
    ) {
        content()
    }
}

@Composable
fun ActionButton(
    icon: Painter,
    modifier: Modifier = Modifier,
    label: String? = null,
    tint: Color = LocalContentColor.current,
    action: () -> Unit
) {
    Icon(
        icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier
            .size(48.dp)
            .clickableWithLabel(
                label = label,
                action = action,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false)
            )
            .padding(6.dp)
            .then(modifier)
    )
}