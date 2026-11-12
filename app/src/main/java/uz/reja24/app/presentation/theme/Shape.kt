package uz.reja24.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Burchaklar: kichik kartalar 14–18dp, katta bloklar 20–22dp, tugmalar 13–16dp.
 */
val RejaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),     // tugmalar, chiplar
    medium = RoundedCornerShape(16.dp),    // kichik kartalar
    large = RoundedCornerShape(20.dp),     // katta bloklar
    extraLarge = RoundedCornerShape(22.dp) // dialog, bottom sheet
)
