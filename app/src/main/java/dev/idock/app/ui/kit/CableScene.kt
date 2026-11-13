package dev.idock.app.ui.kit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * The app's one illustration: this phone on the left, the iPhone on the right, and the cable
 * between them.
 *
 * @param plug 0 – the cable hangs loose and the iPhone is only an outline; 1 – plugged in.
 *   Read while drawing, so animating it does not recompose anything.
 * @param live draws data pulses travelling along the cable (only meaningful when plugged in).
 * @param ring 0..1 progress of the single ring that marks the moment of contact; 1 – no ring.
 */
@Composable
fun CableScene(
    plug: () -> Float,
    live: Boolean,
    modifier: Modifier = Modifier,
    ring: () -> Float = { 1f },
) {
    val scheme = MaterialTheme.colorScheme
    val copper = scheme.primary
    val brass = scheme.tertiary
    val ink = scheme.onSurface
    val faint = scheme.outline
    val screen = scheme.surfaceContainerHigh
    val ok = scheme.secondary

    val pulse: State<Float>? = if (live) {
        rememberInfiniteTransition(label = "cable").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "pulse",
        )
    } else {
        null
    }
    val path = remember { Path() }
    val measure = remember { PathMeasure() }

    Canvas(modifier) {
        val p = plug()
        val pc = p.coerceIn(0f, 1f)
        val phoneW = 48.dp.toPx()
        val phoneH = 86.dp.toPx()
        val top = 4.dp.toPx()
        val bottom = top + phoneH
        val leftCx = size.width * 0.2f
        val rightCx = size.width * 0.8f
        val plugH = 9.dp.toPx()
        val plugW = 12.dp.toPx()

        // This phone, with the app's tile on its screen.
        phone(leftCx, top, phoneW, phoneH, screen, ink, pill = false, dashed = false)
        drawRoundRect(
            color = copper,
            topLeft = Offset(leftCx - 9.dp.toPx(), top + phoneH * 0.38f),
            size = Size(18.dp.toPx(), 18.dp.toPx()),
            cornerRadius = CornerRadius(5.dp.toPx()),
        )

        // The iPhone: a dashed outline until it is plugged in.
        phone(
            cx = rightCx,
            top = top,
            w = phoneW,
            h = phoneH,
            screen = screen.copy(alpha = screen.alpha * lerp(0.35f, 1f, pc)),
            stroke = lerp(faint, ink, pc),
            pill = true,
            dashed = pc < 0.5f,
        )
        if (pc > 0f) {
            drawCircle(ok, radius = 6.dp.toPx() * pc, center = Offset(rightCx, top + phoneH * 0.5f), alpha = pc)
        }

        // The cable sags between the two ports.
        val start = Offset(leftCx, bottom + plugH)
        val endX = lerp(rightCx - 36.dp.toPx(), rightCx, p)
        val endY = lerp(bottom + plugH + 22.dp.toPx(), bottom + plugH, p)
        val sag = 44.dp.toPx()
        path.rewind()
        path.moveTo(start.x, start.y)
        path.cubicTo(start.x, start.y + sag, endX, endY + sag * lerp(0.5f, 1f, pc), endX, endY)
        drawPath(
            path = path,
            brush = Brush.linearGradient(listOf(copper, brass), start = start, end = Offset(endX, endY)),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
        drawRoundRect(
            color = copper,
            topLeft = Offset(start.x - plugW / 2, bottom + 1.dp.toPx()),
            size = Size(plugW, plugH),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
        drawRoundRect(
            color = brass,
            topLeft = Offset(endX - plugW / 2, endY - plugH + 1.dp.toPx()),
            size = Size(plugW, plugH),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )

        val r = ring()
        if (r < 1f) {
            drawCircle(
                color = copper,
                radius = lerp(6.dp.toPx(), 36.dp.toPx(), r),
                center = Offset(rightCx, bottom),
                alpha = (1f - r) * 0.7f,
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        if (pulse != null && pc > 0.98f) {
            measure.setPath(path, false)
            val length = measure.length
            for (i in 0 until 3) {
                val at = measure.getPosition(length * ((pulse.value + i / 3f) % 1f))
                drawCircle(copper, radius = 7.dp.toPx(), center = at, alpha = 0.22f)
                drawCircle(Color.White, radius = 2.6.dp.toPx(), center = at, alpha = 0.95f)
            }
        }
    }
}

private fun DrawScope.phone(
    cx: Float,
    top: Float,
    w: Float,
    h: Float,
    screen: Color,
    stroke: Color,
    pill: Boolean,
    dashed: Boolean,
) {
    val left = cx - w / 2
    val radius = CornerRadius(w * 0.24f)
    drawRoundRect(color = screen, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = radius)
    drawRoundRect(
        color = stroke,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = radius,
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())) else null,
        ),
    )
    if (pill) {
        drawRoundRect(
            color = stroke,
            topLeft = Offset(cx - w * 0.18f, top + 7.dp.toPx()),
            size = Size(w * 0.36f, 5.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
    } else {
        drawCircle(stroke, radius = 2.5.dp.toPx(), center = Offset(cx, top + 9.dp.toPx()))
    }
}
