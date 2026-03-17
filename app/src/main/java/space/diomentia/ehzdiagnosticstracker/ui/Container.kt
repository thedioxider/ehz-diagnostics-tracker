package space.diomentia.ehzdiagnosticstracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SideArrowContainer(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    toRight: Boolean = true,
    slantFactor: Int = 8,
    overdraw: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val slant = 1f / slantFactor
    val layoutDir = LocalLayoutDirection.current
    val density = LocalDensity.current
    val startPx = with(density) { overdraw.calculateStartPadding(layoutDir).toPx() }
    val endPx = with(density) { overdraw.calculateEndPadding(layoutDir).toPx() }
    Box(
        Modifier
            .fillMaxWidth()
            .drawWithCache {
                val totalW = startPx + size.width + endPx
                val sideArrow = if (toRight)
                    Path().apply {
                        moveTo(-startPx, 0f)
                        lineTo(totalW * (1f - slant) - startPx, 0f)
                        lineTo(totalW * .99f - startPx, size.height / 2f)
                        lineTo(totalW * (1f - slant) - startPx, size.height)
                        lineTo(-startPx, size.height)
                    }
                else
                    Path().apply {
                        moveTo(size.width + endPx, 0f)
                        lineTo(totalW * slant - startPx, 0f)
                        lineTo(totalW * .01f - startPx, size.height / 2f)
                        lineTo(totalW * slant - startPx, size.height)
                        lineTo(size.width + endPx, size.height)
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
    overdraw: PaddingValues = PaddingValues(0.dp),
    arrowDrop: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    val layoutDir = LocalLayoutDirection.current
    val density = LocalDensity.current
    val startPx = with(density) { overdraw.calculateStartPadding(layoutDir).toPx() }
    val endPx = with(density) { overdraw.calculateEndPadding(layoutDir).toPx() }
    val topPx = with(density) { overdraw.calculateTopPadding().toPx() }
    val arrowDropPx = with(density) { arrowDrop.toPx() }
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .drawWithCache {
                    val downArrow = Path().apply {
                        moveTo(-startPx, -topPx)
                        lineTo(-startPx, size.height)
                        lineTo(size.width / 2f, size.height + arrowDropPx)
                        lineTo(size.width + endPx, size.height)
                        lineTo(size.width + endPx, -topPx)
                    }
                    onDrawBehind {
                        drawPath(downArrow, containerColor)
                    }
                }
                .then(modifier)
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
        Spacer(Modifier.height(arrowDrop))
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