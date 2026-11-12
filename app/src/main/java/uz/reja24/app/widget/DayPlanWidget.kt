package uz.reja24.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import uz.reja24.app.MainActivity

/**
 * Bosh ekran vidjeti: bugungi sana (milodiy va hijriy), keyingi namoz va bugungi
 * hozirgi hamda kelgusi rejalar. Bosilsa — ilova ochiladi.
 * Ma'lumot har eslatma chalinganda, reja o'zgarganda va yarim tunda yangilanadi.
 */
class DayPlanWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataLoader.load(context)
        provideContent { WidgetContent(data) }
    }

    companion object {
        /** Barcha vidjetlarni yangilaydi; xato bo'lsa jim o'tadi (vidjet qo'yilmagan bo'lishi mumkin). */
        suspend fun refresh(context: Context) {
            runCatching { DayPlanWidget().updateAll(context.applicationContext) }
        }
    }
}

class DayPlanWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DayPlanWidget()
}

/** Sarlavha qismi va bitta qator balandligi — nechta qator sig'ishini hisoblash uchun. */
private val HEADER_HEIGHT = 78.dp
private val ROW_HEIGHT = 30.dp
private val MORE_HEIGHT = 18.dp

// Ranglar kun va tun rejimi uchun (telefon mavzusiga qarab)
private val Background = ColorProvider(day = Color(0xF2FBF8F3), night = Color(0xF2171A1F))
private val TextPrimary = ColorProvider(day = Color(0xFF16181C), night = Color(0xFFF2EFE9))
private val TextMuted = ColorProvider(day = Color(0xFF766F63), night = Color(0xFF9B958A))
private val NowBackground = ColorProvider(day = Color(0x1FD9663D), night = Color(0x33E2703A))
private val Accent = ColorProvider(day = Color(0xFFA84B28), night = Color(0xFFE2703A))
private val PrayerColor = ColorProvider(day = Color(0xFF2B7A5E), night = Color(0xFF5FBF96))

@Composable
private fun WidgetContent(data: WidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Background)
            .cornerRadius(22.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        // Sarlavha: hafta kuni va sanalar | keyingi namoz
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = data.weekday,
                    style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                Text(
                    text = data.gregorian,
                    style = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                if (data.hijri != null) {
                    Text(text = data.hijri, style = TextStyle(color = TextMuted, fontSize = 11.sp), maxLines = 1)
                }
            }
            if (data.nextPrayer != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = data.nextPrayerLabel, style = TextStyle(color = TextMuted, fontSize = 11.sp), maxLines = 1)
                    Text(
                        text = data.nextPrayer,
                        style = TextStyle(color = PrayerColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(GlanceModifier.height(6.dp))

        if (data.items.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = data.emptyText, style = TextStyle(color = TextMuted, fontSize = 13.sp))
            }
        } else {
            // Oddiy ro'yxat (aylantirilmaydi) — har qanday telefon va launcher'da ishonchli ishlaydi.
            // Vidjet balandligiga qancha qator sig'sa, shuncha; qolgani "+N yana".
            val height = LocalSize.current.height
            val fitsAll = ((height - HEADER_HEIGHT) / ROW_HEIGHT).toInt()
            val shown = if (data.items.size <= fitsAll) {
                data.items
            } else {
                data.items.take(((height - HEADER_HEIGHT - MORE_HEIGHT) / ROW_HEIGHT).toInt().coerceAtLeast(1))
            }
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                shown.forEach { item -> ItemRow(item) }
                val hidden = data.items.size - shown.size
                if (hidden > 0) {
                    Text(
                        text = data.moreText.format(hidden),
                        style = TextStyle(color = TextMuted, fontSize = 11.sp),
                        modifier = GlanceModifier.padding(start = 6.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: WidgetItem) {
    val titleColor = when {
        item.isPrayer -> PrayerColor
        else -> TextPrimary
    }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .then(if (item.isNow) GlanceModifier.background(NowBackground).cornerRadius(10.dp) else GlanceModifier)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Toifa rangi — kichik chiziq
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(18.dp)
                .background(ColorProvider(Color(item.color)))
                .cornerRadius(2.dp)
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = item.time,
            style = TextStyle(
                color = if (item.isNow) Accent else TextMuted,
                fontSize = 12.sp,
                fontWeight = if (item.isNow) FontWeight.Bold else FontWeight.Normal
            ),
            maxLines = 1
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = item.title,
            style = TextStyle(
                color = titleColor,
                fontSize = 13.sp,
                fontWeight = if (item.isNow) FontWeight.Bold else FontWeight.Medium,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}
