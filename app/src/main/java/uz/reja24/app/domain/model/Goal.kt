package uz.reja24.app.domain.model

/** Uzoq muddatli (yillik) maqsad — "Yillik" ekrani uchun. */
data class Goal(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val year: Int,
    val progressPercent: Int = 0,     // 0–100
    val colorHex: String = Category.OTHER.defaultColorHex,
    val isAchieved: Boolean = false
)
