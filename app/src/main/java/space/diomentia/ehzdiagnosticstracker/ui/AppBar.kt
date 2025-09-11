package space.diomentia.ehzdiagnosticstracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun slanted(size: Size, offset: Offset = Offset(0f, 0f)): Path {
    val o = offset
    return Path().apply {
        moveTo(o.x, o.y)
        lineTo(o.x + size.width + size.height / 3f, o.y)
        lineTo(o.x + size.width, o.y + size.height)
        lineTo(o.x, o.y + size.height)
    }
}

private val slantedShape = GenericShape { size, _ -> addPath(slanted(size)) }

@Composable
private fun Modifier.slantedBorder(width: Dp, color: Color): Modifier {
    val w = with(LocalDensity.current) { width.toPx() }
    return Modifier
        .drawWithCache {
            onDrawBehind {
                drawPath(
                    slanted(
                        this.size.let { Size(it.width + w, it.height + w) },
                        Offset(0f, -w)
                    ),
                    color
                )
            }
        }
}

private fun backSlanted(size: Size, offset: Offset = Offset(0f, 0f)): Path {
    val o = offset
    return Path().apply {
        moveTo(o.x + size.width, o.y)
        lineTo(o.x + size.width, o.y + size.height)
        lineTo(o.x - size.height / 3f, o.y + size.height)
        lineTo(o.x, o.y)
    }
}

private val backSlantedShape = GenericShape { size, _ -> addPath(backSlanted(size)) }

@Composable
private fun Modifier.backSlantedBorder(width: Dp, color: Color): Modifier {
    val w = with(LocalDensity.current) { width.toPx() }
    return Modifier
        .drawWithCache {
            onDrawBehind {
                drawPath(
                    backSlanted(
                        this.size.let { Size(it.width + w, it.height + w) },
                        Offset(0f, -w)
                    ),
                    color
                )
            }
        }
}

@Composable
fun PtmTopBar(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
) {
    var currentHeight by remember { mutableStateOf(0.dp) }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .getActualSize {
                currentHeight = it.height
            }
            .then(modifier)
    ) {
        if (title != null) {
            Surface(
                shape = slantedShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.defaultMinSize(minHeight = currentHeight)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(8.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    title.let {
                        Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                            ProvideTextStyle(MaterialTheme.typography.headlineMedium) {
                                it()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PtmBottomBar(
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    var currentHeight by remember { mutableStateOf(0.dp) }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .getActualSize {
                currentHeight = it.height
            }
            .then(modifier)
    ) {
        if (navigation != null) {
            Surface(
                shape = slantedShape,
                color = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .defaultMinSize(minHeight = currentHeight)
                    .slantedBorder(8.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(8.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    navigation.let {
                        Box {
                            it()
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (actions != null) {
            Surface(
                shape = backSlantedShape,
                color = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .defaultMinSize(minHeight = currentHeight)
                    .backSlantedBorder(8.dp, MaterialTheme.colorScheme.primary)
            ) {
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(8.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    actions()
                }
            }
        }
    }
}