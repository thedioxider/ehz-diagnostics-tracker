package space.diomentia.ehzdiagnosticstracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

@Composable
fun FullScreenImageDialog(
    imageBitmap: ImageBitmap,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    val imgAspect = imageBitmap.width.toFloat() / imageBitmap.height
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange * scale
    }

    // Snap back when gesture ends
    LaunchedEffect(state.isTransformInProgress) {
        if (!state.isTransformInProgress && containerSize != Size.Zero) {
            val targetScale = scale.coerceIn(1f, 5f)
            val targetOffset = coerceOffset(offset, targetScale, containerSize, imgAspect)
            if (targetScale != scale || targetOffset != offset) {
                val scaleAnim = Animatable(scale)
                val offsetAnim = Animatable(offset, Offset.VectorConverter)
                coroutineScope {
                    if (scale == 1f && abs(offset.y) > containerSize.height / 3f) {
                        onDismiss()
                        return@coroutineScope
                    }
                    launch {
                        offsetAnim.animateTo(targetOffset, spring()) { offset = value }
                    }
                    launch {
                        scaleAnim.animateTo(targetScale, spring()) { scale = value }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (scale == 1f)
                        max(1.1f - abs(offset.y) / containerSize.height, 0f)
                    else 1f
                }
                .background(MaterialTheme.colorScheme.background.copy(alpha = .6f))
                .onGloballyPositioned { containerSize = it.size.toSize() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() },
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2f
                            offset = Offset.Zero
                        }
                    )
                }
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Full screen image",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

private fun coerceOffset(
    offset: Offset,
    scale: Float,
    container: Size,
    imgAspect: Float
): Offset {
    val containerAspect = container.width / container.height
    val (imgW, imgH) = if (imgAspect > containerAspect) {
        container.width to container.width / imgAspect
    } else {
        container.height * imgAspect to container.height
    }
    val maxX = ((imgW * scale - container.width) / 2f).coerceAtLeast(0f)
    val maxY = ((imgH * scale - container.height) / 2f).coerceAtLeast(0f)
    return Offset(
        offset.x.coerceIn(-maxX, maxX),
        offset.y.coerceIn(-maxY, maxY)
    )
}

@Composable
fun ImageWithFullscreenView(
    thumbnail: ImageBitmap,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    loadFullRes: (suspend () -> ImageBitmap?)? = null
) {
    var fullView by remember { mutableStateOf(false) }
    var fullResState by remember(thumbnail) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(fullView) {
        if (fullView && loadFullRes != null && fullResState == null) {
            fullResState = withContext(Dispatchers.IO) { loadFullRes() } ?: thumbnail
        }
    }

    if (fullView) FullScreenImageDialog(fullResState ?: thumbnail) { fullView = false }

    @OptIn(ExperimentalFoundationApi::class)
    Image(
        thumbnail,
        contentDescription = "",
        modifier = Modifier
            .combinedClickable(
                onClick = { fullView = true },
                onLongClick = onLongClick
            )
            .then(modifier)
    )
}