package com.liquidglass.iptv

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Paleta preto e branco, em dois temas. */
data class LG(
    val dark: Boolean,
    val bg: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val glassA: Color,
    val glassB: Color,
    val edge: Color,
    val onBg: Color,
    val onInk: Color,
    val line: Color,
    val thumb: Color
)

val DarkLG = LG(
    dark = true, bg = Color(0xFF050506), ink = Color(0xFFF6F6F8),
    ink2 = Color(0xA3F6F6F8), ink3 = Color(0x61F6F6F8),
    glassA = Color(0x24FFFFFF), glassB = Color(0x09FFFFFF), edge = Color(0x80FFFFFF),
    onBg = Color(0xFFF6F6F8), onInk = Color(0xFF08080A), line = Color(0x1FFFFFFF),
    thumb = Color(0x2EFFFFFF)
)

val LightLG = LG(
    dark = false, bg = Color(0xFFE8EAEF), ink = Color(0xFF0A0A0D),
    ink2 = Color(0x9E0A0A0D), ink3 = Color(0x5C0A0A0D),
    glassA = Color(0xCCFFFFFF), glassB = Color(0x66FFFFFF), edge = Color(0xFFFFFFFF),
    onBg = Color(0xFF0A0A0D), onInk = Color(0xFFFFFFFF), line = Color(0x170A0A0D),
    thumb = Color(0xF2FFFFFF)
)

val LocalLG = compositionLocalOf { DarkLG }

/** Vidro: gradiente translúcido + borda fina com brilho. */
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp)): Modifier {
    val t = LocalLG.current
    return this
        .clip(shape)
        .background(Brush.linearGradient(listOf(t.glassA, t.glassB)))
        .border(
            1.dp,
            Brush.linearGradient(listOf(t.edge, t.edge.copy(alpha = 0.08f), t.edge.copy(alpha = 0.35f))),
            shape
        )
}

/**
 * Efeito de foco: quando [on] e [enabled], o elemento desfoca (Android 12+),
 * perde a cor (vira cinza) e fica mais transparente, com animação suave.
 */
fun Modifier.dim(
    on: Boolean,
    enabled: Boolean = true,
    blurDp: Dp = 6.dp,
    dimAlpha: Float = 0.6f
): Modifier = composed {
    val active = on && enabled
    val sat by animateFloatAsState(if (active) 0f else 1f, tween(600), label = "sat")
    val b by animateDpAsState(if (active) blurDp else 0.dp, tween(600), label = "blur")
    val al by animateFloatAsState(if (active) dimAlpha else 1f, tween(600), label = "alpha")
    this
        .then(if (Build.VERSION.SDK_INT >= 31 && b > 0.dp) Modifier.blur(b) else Modifier)
        .graphicsLayer { alpha = al }
        .drawWithContent {
            if (sat >= 0.99f) {
                drawContent()
            } else {
                val paint = Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(sat) })
                }
                drawIntoCanvas { c ->
                    c.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                    drawContent()
                    c.restore()
                }
            }
        }
}

/** Fundo com luzes suaves em movimento lento. */
@Composable
fun LGBackground() {
    val t = LocalLG.current
    val tr = rememberInfiniteTransition(label = "orbs")
    val a by tr.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse),
        label = "a"
    )
    Box(Modifier.fillMaxSize().background(t.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun orb(cx: Float, cy: Float, r: Float, c: Color) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(c, Color.Transparent), center = Offset(cx, cy), radius = r),
                    radius = r, center = Offset(cx, cy)
                )
            }
            val m = maxOf(w, h)
            if (t.dark) {
                orb(w * 0.10f + a * w * 0.10f, h * 0.02f, m * 0.55f, Color(0x38FFFFFF))
                orb(w * 0.95f - a * w * 0.08f, h * 0.30f + a * h * 0.10f, m * 0.45f, Color(0x2A96A0B9))
                orb(w * 0.50f, h * 1.02f, m * 0.55f, Color(0x1FFFFFFF))
            } else {
                orb(w * 0.10f + a * w * 0.10f, h * 0.02f, m * 0.60f, Color(0xFFFFFFFF))
                orb(w * 0.95f - a * w * 0.08f, h * 0.30f + a * h * 0.10f, m * 0.50f, Color(0xBFB0B8CC))
                orb(w * 0.50f, h * 1.02f, m * 0.60f, Color(0xF2FFFFFF))
            }
        }
    }
}

private val PAL = listOf(
    0xFFFF8A5B to 0xFF2E0B4F, 0xFF3DE0D6 to 0xFF08245E, 0xFFFFD27A to 0xFF6B1238,
    0xFF7FE9A6 to 0xFF05384A, 0xFFC7B8FF to 0xFF150F3D, 0xFFFF9FB0 to 0xFF390824,
    0xFF8EC1FF to 0xFF081634, 0xFFF2F2F2 to 0xFF15151A
)

/** Arte gerada (usada quando o item não tem capa ou enquanto ela carrega). */
@Composable
fun Art(seed: String, modifier: Modifier = Modifier) {
    val h = seed.hashCode() and 0x7fffffff
    val (a, b) = PAL[h % 8]
    val v = (h / 8) % 3
    Canvas(modifier) {
        drawRect(
            Brush.linearGradient(
                listOf(Color(a), Color(b)),
                start = Offset(0f, 0f), end = Offset(size.width, size.height)
            )
        )
        val white = Color.White
        val cx = size.width * (0.35f + (h % 30) / 100f)
        val cy = size.height * 0.34f
        val r = size.width * 0.55f
        when (v) {
            0 -> {
                drawCircle(
                    Brush.radialGradient(listOf(white.copy(alpha = 0.6f), Color.Transparent), Offset(cx, cy), r),
                    r, Offset(cx, cy)
                )
                drawCircle(white.copy(alpha = 0.9f), size.width * 0.06f, Offset(cx, cy))
            }
            1 -> {
                drawCircle(
                    Brush.radialGradient(listOf(white.copy(alpha = 0.45f), Color.Transparent), Offset(size.width * 0.8f, size.height * 0.2f), r),
                    r, Offset(size.width * 0.8f, size.height * 0.2f)
                )
                drawCircle(
                    Brush.radialGradient(listOf(Color(a).copy(alpha = 0.9f), Color.Transparent), Offset(size.width * 0.2f, size.height * 0.6f), r),
                    r, Offset(size.width * 0.2f, size.height * 0.6f)
                )
            }
            else -> {
                drawCircle(
                    Brush.radialGradient(listOf(white.copy(alpha = 0.5f), Color.Transparent), Offset(size.width * 0.5f, size.height), r * 1.2f),
                    r * 1.2f, Offset(size.width * 0.5f, size.height)
                )
            }
        }
        drawRect(
            Brush.verticalGradient(
                0.45f to Color.Transparent,
                1f to Color(0xAA000000)
            )
        )
    }
}
