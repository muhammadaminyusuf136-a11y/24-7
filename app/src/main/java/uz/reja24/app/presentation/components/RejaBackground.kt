package uz.reja24.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.presentation.theme.GraceBackgroundBottom
import uz.reja24.app.presentation.theme.GraceBackgroundTop
import uz.reja24.app.presentation.theme.GraceSurface
import uz.reja24.app.presentation.theme.RejaTheme
import kotlin.math.PI
import kotlin.math.sin

// Orqa fon ranglari (dizayn tizimidan)
private val PowerTop = Color(0xFF17202B)
private val PowerBottom = Color(0xFF1F2B3A)

/**
 * Orqa fon: rasm (PNG) ishlatilmaydi — hammasi kod bilan chiziladi, ilova hajmi kichik.
 *  - Kuch     — to'q ko'k-grafit gradient + keskin diagonal chiziqlar
 *  - Nafosat  — chang-pushti → xira binafsha gradient + silliq to'lqinlar (yorqin emas)
 *  - Sof      — faqat tekis rang
 */
@Composable
fun RejaBackground(
    style: BackgroundStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val plain = RejaTheme.colors.background
    val accent = RejaTheme.colors.accent
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                when (style) {
                    BackgroundStyle.POWER -> drawPower(accent)
                    BackgroundStyle.GRACE -> drawGrace()
                    BackgroundStyle.PURE -> drawRect(plain)
                }
            },
        content = content
    )
}

/** Fonning eng yuqori qismidagi rang — ustidagi rasm/video chetlarini fonga silliq qo'shish uchun. */
fun backgroundTopColor(style: BackgroundStyle, plain: Color): Color = when (style) {
    BackgroundStyle.POWER -> PowerTop
    BackgroundStyle.GRACE -> GraceBackgroundTop
    BackgroundStyle.PURE -> plain
}

private fun DrawScope.drawPower(accent: Color) {
    drawRect(Brush.verticalGradient(listOf(PowerTop, PowerBottom)))
    // Nozik diagonal chiziqlar
    val step = 56.dp.toPx()
    val lineColor = Color.White.copy(alpha = 0.045f)
    var x = -size.height
    while (x < size.width) {
        drawLine(
            color = lineColor,
            start = Offset(x, size.height),
            end = Offset(x + size.height, 0f),
            strokeWidth = 1.5.dp.toPx()
        )
        x += step
    }
    // Ikkita keskin, qalin "intizom" chizig'i
    drawLine(
        color = accent.copy(alpha = 0.16f),
        start = Offset(size.width * 0.55f, 0f),
        end = Offset(size.width * 1.1f, size.height * 0.32f),
        strokeWidth = 10.dp.toPx()
    )
    drawLine(
        color = Color.White.copy(alpha = 0.06f),
        start = Offset(-size.width * 0.1f, size.height * 0.78f),
        end = Offset(size.width * 0.6f, size.height * 1.05f),
        strokeWidth = 22.dp.toPx()
    )
}

private fun DrawScope.drawGrace() {
    drawRect(Brush.verticalGradient(listOf(GraceBackgroundTop, GraceBackgroundBottom)))
    // Uchta silliq to'lqin, pastga tushgan sari quyuqroq
    listOf(0.58f to 0.22f, 0.72f to 0.18f, 0.86f to 0.14f).forEachIndexed { index, (level, alpha) ->
        val path = Path()
        val baseY = size.height * level
        val amplitude = 18.dp.toPx() + index * 6.dp.toPx()
        val waveLength = size.width * (0.9f + index * 0.25f)
        path.moveTo(0f, baseY)
        var x = 0f
        val stepX = 8.dp.toPx()
        while (x <= size.width) {
            val y = baseY + amplitude * sin((2 * PI * x / waveLength) + index).toFloat()
            path.lineTo(x, y)
            x += stepX
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()
        drawPath(path, GraceSurface.copy(alpha = alpha))
    }
}

