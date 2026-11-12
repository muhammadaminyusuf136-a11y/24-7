package uz.reja24.app.presentation.block

import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.reja24.app.R
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.domain.model.BackgroundStyle
import uz.reja24.app.domain.model.ThemeMode
import uz.reja24.app.presentation.components.RejaBackground
import uz.reja24.app.presentation.components.backgroundTopColor
import uz.reja24.app.presentation.theme.RejaTheme
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Motivatsion xabar qancha vaqtda almashadi. */
private const val MESSAGE_ROTATE_MS = 10_000L

/** Video tomonlari nisbati (720×1280). Video hech qayeridan kesilmaydi — to'liq kadr, asl ranglarida. */
private const val VIDEO_ASPECT = 9f / 16f

/** Blok ekrani holati. Mavzu va fon foydalanuvchi ilovada tanlaganidek bo'ladi. */
data class BlockOverlayState(
    val unlockTime: LocalTime,
    val streakDays: Int,
    val messageSeed: Int,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.PURE
)

/**
 * Blok ekrani — ikki oyna (pastga suriladi):
 *  1) video (ijtimoiy tarmoqlar va o'yinlar vaqtni tortib olmoqda), soat va motivatsion gap;
 *  2) ochilish vaqti, streak, "Bosh ekranga qaytish" va kam ko'zga tashlanadigan "Favqulodda kirish".
 * Ekranni tepaga surish ikkinchi oynani ochadi, pastga surish qaytaradi — video hech narsa bilan yopilmaydi.
 */
@Composable
fun BlockScreen(
    state: BlockOverlayState,
    onEmergency: () -> Unit,
    onClose: () -> Unit
) {
    RejaTheme(themeMode = state.themeMode, backgroundStyle = state.backgroundStyle) {
        val now by produceState(initialValue = LocalDateTime.now()) {
            while (true) {
                value = LocalDateTime.now()
                delay(1_000)
            }
        }
        val messages = stringArrayResource(R.array.block_messages)
        // Xabar tasodifiy tanlanadi va har 10 soniyada keyingisiga silliq almashadi
        var messageIndex by remember(state.messageSeed) {
            mutableIntStateOf(Math.floorMod(state.messageSeed, messages.size))
        }
        LaunchedEffect(messages.size) {
            while (true) {
                delay(MESSAGE_ROTATE_MS)
                messageIndex = (messageIndex + 1) % messages.size
            }
        }
        val pager = rememberPagerState { 2 }
        val scope = rememberCoroutineScope()

        RejaBackground(style = state.backgroundStyle) {
            VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                if (page == 0) {
                    VideoPage(
                        state = state,
                        now = now,
                        message = messages[messageIndex.coerceIn(0, messages.lastIndex)],
                        onMore = { scope.launch { pager.animateScrollToPage(1) } }
                    )
                } else {
                    DetailsPage(
                        state = state,
                        now = now,
                        onEmergency = onEmergency,
                        onClose = onClose
                    )
                }
            }
        }
    }
}

/** 1-oyna: video tepaga tegib turadi (eni ekranga to'liq), ostida soat, gap va qimirlab turadigan chiziqcha. */
@Composable
private fun VideoPage(state: BlockOverlayState, now: LocalDateTime, message: String, onMore: () -> Unit) {
    val colors = RejaTheme.colors
    // Video ostidagi joy va matn foni shu rangga silliq o'tadi
    val blend = backgroundTopColor(state.backgroundStyle, colors.background)
    Box(Modifier.fillMaxSize()) {
        LoopingVideo(
            resId = R.raw.block_background,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(VIDEO_ASPECT)
                .align(Alignment.TopCenter)
        )

        // Pastki qism: video tugagan joydan fonga o'tadi; soat va gap shu yerda
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to blend.copy(alpha = 0.85f),
                        1f to blend
                    )
                )
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = TimeFormat.time(now.toLocalTime()),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 52.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textPrimary
            )
            Crossfade(
                targetState = message,
                animationSpec = tween(durationMillis = 300),
                label = "block-message"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.copy(lineHeight = 26.sp),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            SwipeUpHint(
                description = stringResource(R.string.cd_block_more),
                onClick = onMore
            )
        }
    }
}

/** 2-oyna: ochilish vaqti, streak va tugmalar. */
@Composable
private fun DetailsPage(
    state: BlockOverlayState,
    now: LocalDateTime,
    onEmergency: () -> Unit,
    onClose: () -> Unit
) {
    val colors = RejaTheme.colors
    val unlockAt = now.toLocalDate().atTime(state.unlockTime).let { if (it.isAfter(now)) it else it.plusDays(1) }
    val minutesLeft = Duration.between(now, unlockAt).toMinutes()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tepada kichik chiziqcha — pastga sursa, videoga qaytadi
        Box(
            Modifier
                .padding(top = 10.dp)
                .size(width = 36.dp, height = 4.dp)
                .background(colors.textSecondary.copy(alpha = 0.35f), CircleShape)
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.block_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        // Ochilish vaqti va qolgan vaqt
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.block_unlocks_at), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Text(TimeFormat.time(state.unlockTime), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.block_time_left), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Text(TimeFormat.hoursMinutes(minutesLeft), style = MaterialTheme.typography.headlineSmall, color = colors.accentText)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Streak eslatmasi
        if (state.streakDays > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        R.string.block_streak,
                        pluralStringResource(R.plurals.days_count, state.streakDays, state.streakDays.toString())
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
        Spacer(Modifier.weight(1f))

        Button(
            onClick = onClose,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.background),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.block_go_home), style = MaterialTheme.typography.titleMedium)
        }
        // Kam ko'zga tashlanadigan "Favqulodda kirish"
        TextButton(
            onClick = onEmergency,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.block_emergency),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary.copy(alpha = 0.75f)
            )
        }
    }
}

/**
 * "Tepaga suring" belgisi: yozuvsiz kichik chiziqcha, sekin tepaga "suzib" turadi.
 * Strelka yo'q — ekranning istalgan joyini tepaga surish kifoya; bosilsa ham ochiladi.
 */
@Composable
private fun SwipeUpHint(description: String, onClick: () -> Unit) {
    val colors = RejaTheme.colors
    val lift by rememberInfiniteTransition(label = "hint").animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1100), RepeatMode.Reverse),
        label = "hint-lift"
    )
    Column(
        modifier = Modifier
            .padding(top = 10.dp)
            .heightIn(min = 48.dp)
            .offset(y = lift.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(width = 40.dp, height = 5.dp)
                .background(colors.textSecondary.copy(alpha = 0.55f), CircleShape)
        )
    }
}
/**
 * Ovozsiz, cheksiz aylanadigan video (res/raw dan), asl ranglarida.
 *  - Ovoz o'chiq va audio fokus so'ralmaydi — boshqa ilovalardagi musiqa/podkast to'xtamaydi.
 *  - Video ochilmasa, qulf belgisi ko'rsatiladi (ilova yiqilmaydi).
 */
@Composable
private fun LoopingVideo(@RawRes resId: Int, modifier: Modifier = Modifier) {
    var failed by remember { mutableStateOf(false) }
    val description = stringResource(R.string.cd_block_video)
    val player = remember { arrayOfNulls<MediaPlayer>(1) }
    DisposableEffect(Unit) {
        onDispose {
            player[0]?.let { runCatching { it.stop() }; runCatching { it.release() } }
            player[0] = null
        }
    }
    if (failed) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = stringResource(R.string.cd_locked),
                tint = RejaTheme.colors.accent,
                modifier = Modifier.size(56.dp)
            )
        }
        return
    }
    AndroidView(
        modifier = modifier.semantics { contentDescription = description },
        factory = { context ->
            TextureView(context).apply {
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                        val created = try {
                            MediaPlayer().apply {
                                setAudioAttributes(
                                    AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                        .build()
                                )
                                setDataSource(context, Uri.parse("android.resource://${context.packageName}/$resId"))
                                setSurface(Surface(texture))
                                isLooping = true
                                setVolume(0f, 0f)
                                setOnPreparedListener { it.start() }
                                setOnErrorListener { _, _, _ ->
                                    failed = true
                                    true
                                }
                                prepareAsync()
                            }
                        } catch (e: Exception) {
                            failed = true
                            null
                        }
                        player[0] = created
                    }

                    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit

                    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                        player[0]?.let { runCatching { it.stop() }; runCatching { it.release() } }
                        player[0] = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                }
            }
        }
    )
}
