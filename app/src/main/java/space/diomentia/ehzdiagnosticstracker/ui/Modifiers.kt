package space.diomentia.ehzdiagnosticstracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize

@Composable
fun Modifier.getActualSize(
    updater: (DpSize) -> Unit
): Modifier {
    val density = LocalDensity.current
    fun toDpSize(size: IntSize) = with(density) {
        DpSize(
            size.width.toDp(),
            size.height.toDp()
        )
    }

    var currentSize: DpSize = DpSize.Zero
    return this then Modifier
        .onGloballyPositioned { coordinates ->
            currentSize = toDpSize(coordinates.size)
            updater(currentSize)
        }
        .onSizeChanged { size ->
            val newSize = toDpSize(size)
            if (newSize != currentSize) {
                currentSize = newSize
                updater(currentSize)
            }
        }
}