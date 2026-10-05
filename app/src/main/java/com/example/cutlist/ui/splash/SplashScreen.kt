package com.example.cutlist.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cutlist.ui.components.drawDimensionLine
import com.example.cutlist.ui.theme.overline
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PIECE_WIDTHS = floatArrayOf(1f, 0.72f, 0.86f, 0.52f)
private val SCATTER = arrayOf(
    Offset(-46f, 34f), Offset(52f, -26f), Offset(-30f, -44f), Offset(60f, 40f),
)

/** Lightweight branded intro (~1.4 s): pieces settle into a list, dimension lines appear, title fades in. */
@Composable
fun SplashScreen(isReady: Boolean, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    var animationDone by remember { mutableStateOf(false) }
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        launch {
            delay(550)
            titleAlpha.animateTo(1f, tween(durationMillis = 450))
        }
        progress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        delay(250)
        animationDone = true
    }
    LaunchedEffect(animationDone, isReady) {
        if (animationDone && isReady) finish()
    }

    val pieceFill = MaterialTheme.colorScheme.primaryContainer
    val pieceStroke = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics(mergeDescendants = true) { contentDescription = "CutList. Build a clean cutting list" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(width = 200.dp, height = 140.dp)) {
            val p = progress.value
            val rowHeight = 16.dp.toPx()
            val gap = 16.dp.toPx()
            val top = (size.height - (rowHeight * 4 + gap * 3)) / 2 + 4.dp.toPx()
            PIECE_WIDTHS.forEachIndexed { index, widthFraction ->
                // Staggered local progress for each piece.
                val local = ((p - index * 0.1f) / 0.6f).coerceIn(0f, 1f)
                if (local <= 0f) return@forEachIndexed
                val remaining = 1f - local
                val offset = Offset(SCATTER[index].x.dp.toPx() * remaining, SCATTER[index].y.dp.toPx() * remaining)
                val topLeft = Offset(0f, top + index * (rowHeight + gap)) + offset
                val pieceSize = Size(size.width * widthFraction, rowHeight)
                val corner = CornerRadius(3.dp.toPx())
                drawRoundRect(pieceFill, topLeft, pieceSize, corner, alpha = local)
                drawRoundRect(pieceStroke, topLeft, pieceSize, corner, style = Stroke(1.5.dp.toPx()), alpha = local)
                val lineAlpha = ((local - 0.55f) / 0.45f).coerceIn(0f, 1f)
                if (lineAlpha > 0f) {
                    val y = topLeft.y - 6.dp.toPx()
                    drawDimensionLine(lineColor, Offset(topLeft.x, y), Offset(topLeft.x + pieceSize.width, y), lineAlpha)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "CUTLIST",
            style = MaterialTheme.typography.headlineMedium.overline,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.alpha(titleAlpha.value),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Build a clean cutting list",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(titleAlpha.value),
        )
    }
}
