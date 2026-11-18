package uz.reja24.app.service.blocking

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import uz.reja24.app.core.util.LocaleHelper
import uz.reja24.app.domain.model.AppLanguage
import uz.reja24.app.presentation.block.BlockOverlayState
import uz.reja24.app.presentation.block.BlockScreen
import uz.reja24.app.presentation.block.DayControlScreen
import uz.reja24.app.presentation.block.DayControlState

/**
 * Blok ekranini boshqa ilovalar ustida ko'rsatadi.
 *  - "Ekran ustida ko'rsatish" ruxsati bo'lsa — TYPE_APPLICATION_OVERLAY
 *  - bo'lmasa — TYPE_ACCESSIBILITY_OVERLAY (Accessibility xizmati o'zi chiza oladi)
 *
 * Ikki xil oyna bor: tungi blok ekrani va kunduzgi nazorat ("o'ylab ko'r" pauzasi, "bugungi vaqt tugadi").
 */
class BlockOverlayManager(private val serviceContext: Context) {

    /** Hozir nima ko'rsatilmoqda. */
    sealed interface Shown {
        data object Night : Shown
        data class Day(val state: DayControlState) : Shown
    }

    private val windowManager: WindowManager? = serviceContext.getSystemService(WindowManager::class.java)
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val content = mutableStateOf<(@Composable () -> Unit)?>(null)

    var shown: Shown? = null
        private set

    val isShowing: Boolean get() = composeView != null

    /** Tungi blok ekrani. */
    fun show(
        overlayState: BlockOverlayState,
        language: AppLanguage?,
        onEmergency: () -> Unit,
        onClose: () -> Unit
    ) {
        shown = Shown.Night
        content.value = { BlockScreen(state = overlayState, onEmergency = onEmergency, onClose = onClose) }
        attach(language)
    }

    /** Kunduzgi nazorat oynasi. */
    fun showDay(
        state: DayControlState,
        language: AppLanguage?,
        onOpen: () -> Unit,
        onClose: () -> Unit
    ) {
        shown = Shown.Day(state)
        content.value = { DayControlScreen(state = state, onOpen = onOpen, onClose = onClose) }
        attach(language)
    }

    private fun attach(language: AppLanguage?) {
        if (composeView != null) return
        val manager = windowManager ?: return

        val type = if (Settings.canDrawOverlays(serviceContext)) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                // Xizmat ochgan oynada tezlashtirish o'zi yoqilmaydi — video va animatsiya uchun kerak
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.OPAQUE
        )

        val owner = OverlayLifecycleOwner().also { it.start() }
        val localized = LocaleHelper.localizedContext(serviceContext, language)
        val view = ComposeView(localized).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { content.value?.invoke() }
        }
        try {
            manager.addView(view, params)
            composeView = view
            lifecycleOwner = owner
        } catch (e: Exception) {
            // Ruxsat yo'q yoki oyna qo'shib bo'lmadi — bloklash "Bosh ekran" orqali baribir ishlaydi
            owner.destroy()
            shown = null
        }
    }

    fun hide() {
        shown = null
        val view = composeView ?: return
        try {
            windowManager?.removeViewImmediate(view)
        } catch (e: Exception) {
            // oyna allaqachon olib tashlangan
        }
        lifecycleOwner?.destroy()
        composeView = null
        lifecycleOwner = null
    }
}
