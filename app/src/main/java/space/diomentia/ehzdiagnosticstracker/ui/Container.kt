package space.diomentia.ehzdiagnosticstracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun SideArrowContainer(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    toRight: Boolean = true,
    slantFactor: Int = 8,
    content: @Composable () -> Unit
) {
    val slant = 1f / slantFactor
    val screenSize = getWindowSize()
    var absCoordinates by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .onGloballyPositioned { coordinates ->
                absCoordinates = coordinates.positionInWindow()
            }
            .drawWithCache {
                val sideArrow = if (toRight)
                    Path().apply {
                        moveTo(-absCoordinates.x, 0f)
                        lineTo(screenSize.width * (1f - slant) - absCoordinates.x, 0f)
                        lineTo(
                            screenSize.width * .99f - absCoordinates.x * (1 + slant),
                            size.height / 2f
                        )
                        lineTo(screenSize.width * (1f - slant) - absCoordinates.x, size.height)
                        lineTo(-absCoordinates.x, size.height)
                    }
                else
                    Path().apply {
                        moveTo(screenSize.width - absCoordinates.x, 0f)
                        lineTo(screenSize.width * slant - absCoordinates.x, 0f)
                        lineTo(
                            screenSize.width * .01f - absCoordinates.x * (1f - slant),
                            size.height / 2f
                        )
                        lineTo(screenSize.width * slant - absCoordinates.x, size.height)
                        lineTo(screenSize.width - absCoordinates.x, size.height)
                    }
                onDrawBehind {
                    drawPath(sideArrow, containerColor)
                }
            }
            .then(modifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun DownArrowContainer(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    slantFactor: Int = 8,
    content: @Composable () -> Unit
) {
    val screenSize = getWindowSize()
    var absCoordinates by remember { mutableStateOf(Offset.Zero) }
    val offset = screenSize.height / slantFactor / 3 * 2
    Column {
        Box(
            Modifier
                .onGloballyPositioned { coordinates ->
                    absCoordinates = coordinates.positionInWindow()
                }
                .drawWithCache {
                    val downArrow = Path().apply {
                        moveTo(-absCoordinates.x, -absCoordinates.y)
                        lineTo(-absCoordinates.x, size.height)
                        lineTo(screenSize.width / 2f - absCoordinates.x, size.height + offset)
                        lineTo(screenSize.width - absCoordinates.x, size.height)
                        lineTo(screenSize.width - absCoordinates.x, -absCoordinates.y)
                    }
                    onDrawBehind {
                        drawPath(downArrow, containerColor)
                    }
                }
                .then(
                    modifier
                )
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
        Spacer(
            Modifier
                .height(with(LocalDensity.current) { offset.toDp() })
        )
    }
}

@Composable
fun BorderedDialogContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor: Color = MaterialTheme.colorScheme.primary
    val containerColor: Color = MaterialTheme.colorScheme.background
    //val contentColor: Color = blue_robins_egg
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .drawWithCache {
                val borderShape = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(0f, size.height / 2f)
                    lineTo(size.width, size.height / 5f)
                    lineTo(size.width, 0f)
                }
                onDrawBehind {
                    drawPath(borderShape, borderColor)
                }
            }
            .padding(24.dp)
            .defaultMinSize(minHeight = 150.dp)
            .then(modifier),
        shape = RectangleShape,
        color = containerColor
        //contentColor = contentColor
    ) {
        content()
    }
}