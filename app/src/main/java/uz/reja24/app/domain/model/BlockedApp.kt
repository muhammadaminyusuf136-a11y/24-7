package uz.reja24.app.domain.model

/** Bloklanadigan (yoki ro'yxatda turgan, lekin hozircha o'chirilgan) ilova. */
data class BlockedApp(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true
)
