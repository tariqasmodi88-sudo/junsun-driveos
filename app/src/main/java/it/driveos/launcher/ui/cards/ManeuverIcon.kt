package it.driveos.launcher.ui.cards

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import it.driveos.launcher.nav.NavProgress.Turn
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Freccia di manovra disegnata a vettori (niente immagini): nitida a ogni dimensione. */
@Composable
fun ManeuverIcon(turn: Turn, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.minDimension
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float, mirror: Boolean = false) = Offset((if (mirror) 1f - x else x) * w, y * w)

        when (turn) {
            Turn.ARRIVE -> {
                // segnaposto
                val pin = Path().apply {
                    moveTo(w * 0.5f, w * 0.9f)
                    cubicTo(w * 0.28f, w * 0.62f, w * 0.22f, w * 0.5f, w * 0.22f, w * 0.38f)
                    cubicTo(w * 0.22f, w * 0.2f, w * 0.36f, w * 0.1f, w * 0.5f, w * 0.1f)
                    cubicTo(w * 0.64f, w * 0.1f, w * 0.78f, w * 0.2f, w * 0.78f, w * 0.38f)
                    cubicTo(w * 0.78f, w * 0.5f, w * 0.72f, w * 0.62f, w * 0.5f, w * 0.9f)
                    close()
                }
                drawPath(pin, color)
                drawCircle(Color.Black.copy(alpha = 0.55f), radius = w * 0.1f, center = p(0.5f, 0.38f))
            }
            Turn.ROUNDABOUT -> {
                drawCircle(color, radius = w * 0.17f, center = p(0.5f, 0.47f), style = stroke)
                drawLine(color, p(0.5f, 0.92f), p(0.5f, 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                arrow(p(0.62f, 0.35f), p(0.8f, 0.17f), color, stroke, w)
            }
            Turn.UTURN -> {
                val path = Path().apply {
                    moveTo(w * 0.66f, w * 0.9f); lineTo(w * 0.66f, w * 0.42f)
                    cubicTo(w * 0.66f, w * 0.14f, w * 0.34f, w * 0.14f, w * 0.34f, w * 0.42f)
                    lineTo(w * 0.34f, w * 0.56f)
                }
                drawPath(path, color, style = stroke)
                head(p(0.34f, 0.56f), Math.PI.toFloat() / 2, color, w)
            }
            Turn.STRAIGHT -> {
                drawLine(color, p(0.5f, 0.92f), p(0.5f, 0.3f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                head(p(0.5f, 0.3f), -Math.PI.toFloat() / 2, color, w)
            }
            else -> {
                val left = turn == Turn.LEFT || turn == Turn.SLIGHT_LEFT || turn == Turn.SHARP_LEFT
                // punti definiti per la destra, specchiati per la sinistra
                val (start, knee, ctrl, end) = when (turn) {
                    Turn.SLIGHT_RIGHT, Turn.SLIGHT_LEFT -> listOf(p(0.4f, 0.92f, left), p(0.4f, 0.62f, left), p(0.4f, 0.44f, left), p(0.66f, 0.26f, left))
                    Turn.SHARP_RIGHT, Turn.SHARP_LEFT -> listOf(p(0.34f, 0.92f, left), p(0.34f, 0.42f, left), p(0.34f, 0.16f, left), p(0.66f, 0.5f, left))
                    else -> listOf(p(0.34f, 0.92f, left), p(0.34f, 0.5f, left), p(0.34f, 0.3f, left), p(0.7f, 0.3f, left))
                }
                val path = Path().apply {
                    moveTo(start.x, start.y); lineTo(knee.x, knee.y)
                    quadraticBezierTo(ctrl.x, ctrl.y, end.x, end.y)
                }
                drawPath(path, color, style = stroke)
                head(end, atan2(end.y - ctrl.y, end.x - ctrl.x), color, w)
            }
        }
    }
}

private fun DrawScope.arrow(from: Offset, to: Offset, color: Color, stroke: Stroke, w: Float) {
    drawLine(color, from, to, strokeWidth = stroke.width, cap = StrokeCap.Round)
    head(to, atan2(to.y - from.y, to.x - from.x), color, w)
}

/** Punta della freccia: triangolo con la punta oltre [at], orientato secondo [angle] (radianti). */
private fun DrawScope.head(at: Offset, angle: Float, color: Color, w: Float) {
    val len = w * 0.16f
    val half = w * 0.15f
    val dx = cos(angle); val dy = sin(angle)
    val tip = Offset(at.x + dx * len, at.y + dy * len)
    val back = Offset(at.x - dx * len * 0.25f, at.y - dy * len * 0.25f)
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(back.x - dy * half, back.y + dx * half)
        lineTo(back.x + dy * half, back.y - dx * half)
        close()
    }
    drawPath(path, color)
}
