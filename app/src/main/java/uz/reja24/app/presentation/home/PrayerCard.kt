package uz.reja24.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.reja24.app.R
import uz.reja24.app.core.util.DurationText
import uz.reja24.app.core.util.LocaleHelper
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.prayer.Prayer
import uz.reja24.app.domain.prayer.ResolvedLocation
import uz.reja24.app.presentation.components.RejaCard
import uz.reja24.app.presentation.components.SectionLabel
import uz.reja24.app.presentation.components.prayerNameRes
import uz.reja24.app.presentation.theme.PrayerGreen
import uz.reja24.app.presentation.theme.RejaTheme
import java.time.Duration
import java.util.Locale

/** Bosh ekrandagi namoz vaqtlari kartasi. Bosilsa — namoz sozlamalari ochiladi. */
@Composable
fun PrayerCard(state: HomeUiState, onOpen: () -> Unit) {
    val overview = state.prayer
    if (!overview.settings.isEnabled) return
    val colors = RejaTheme.colors
    val today = overview.days[state.today]

    RejaCard(onClick = onOpen, modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Mosque, contentDescription = null, tint = PrayerGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            SectionLabel(stringResource(R.string.home_prayer_title), modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_open), tint = colors.textMuted)
        }
        if (today == null || overview.location == null) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.home_prayer_setup), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            return@RejaCard
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = locationName(overview.location),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        state.nextPrayer?.let { (prayer, at) ->
            // Yuqoriga yaxlitlanadi: 34 daqiqa 20 soniya → 35, aniq 34:00 → 34
            val minutes = ((Duration.between(state.now, at).seconds + 59) / 60).toInt()
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(
                    R.string.home_prayer_next,
                    stringResource(prayerNameRes(prayer)),
                    DurationText.format(LocalContext.current.resources, minutes)
                ),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }

        Spacer(Modifier.height(12.dp))
        val nextPrayer = state.nextPrayer?.takeIf { it.second.toLocalDate() == state.today }?.first
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            Prayer.entries.forEach { prayer ->
                PrayerCell(
                    // Tor kataklarga sig'ishi uchun "Quyosh chiqishi" o'rniga "Quyosh"
                    name = stringResource(if (prayer == Prayer.SUNRISE) R.string.prayer_sunrise_short else prayerNameRes(prayer)),
                    time = TimeFormat.time(today.time(prayer).toLocalTime()),
                    highlighted = prayer == nextPrayer,
                    passed = today.time(prayer).isBefore(state.now),
                    isSunrise = !prayer.isObligatory,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PrayerCell(
    name: String,
    time: String,
    highlighted: Boolean,
    passed: Boolean,
    isSunrise: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = RejaTheme.colors
    val content = when {
        highlighted -> Color.White
        passed -> colors.textMuted
        else -> colors.textPrimary
    }
    Column(
        modifier = modifier
            .background(if (highlighted) PrayerGreen else Color.Transparent, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) Color.White else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = time,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium),
            color = content
        )
        if (isSunrise) {
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .background(colors.textMuted.copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

/** "Toshkent" yoki (shahar yaqin bo'lmasa) "Joriy joylashuv (41.30°, 69.24°)". */
@Composable
fun locationName(location: ResolvedLocation): String {
    val context = LocalContext.current
    val city = location.city
    return if (city != null) {
        city.name(LocaleHelper.currentLanguage(context))
    } else if (!location.placeName.isNullOrBlank()) {
        location.placeName
    } else {
        stringResource(
            R.string.prayer_location_coordinates,
            String.format(Locale.ROOT, "%.2f°", location.latitude),
            String.format(Locale.ROOT, "%.2f°", location.longitude)
        )
    }
}
