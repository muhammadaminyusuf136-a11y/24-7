package uz.reja24.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.presentation.theme.RejaTheme

/**
 * Orqa fon — har doim tanlangan ranglar to'plamining tekis foni:
 * Qaymoq (PURE), To'q ko'k (POWER), To'q yashil (GREEN), To'q pushti (GRACE).
 *
 * Avval "Kuch" va "Nafosat" gradient va chiziqlar bilan chizilardi, lekin kartalar, pastki
 * menyu, lenta chetlari va dialoglar tekis rangda edi — fon bilan mos tushmay, telefonda
 * chiziq-chiziq bo'lib ko'rinardi. Endi fon va hamma qismlar bitta to'plamdan olinadi
 * (rang to'plami RejaTheme'da uslubga qarab tanlanadi, shuning uchun style bu yerda kerak emas).
 */
@Composable
fun RejaBackground(
    @Suppress("UNUSED_PARAMETER") style: BackgroundStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RejaTheme.colors.background),
        content = content
    )
}

/** Fonning eng yuqori qismidagi rang — ustidagi rasm/video chetlarini fonga silliq qo'shish uchun. */
@Suppress("UNUSED_PARAMETER")
fun backgroundTopColor(style: BackgroundStyle, plain: Color): Color = plain
