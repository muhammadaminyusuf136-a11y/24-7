package uz.reja24.app.domain.model

import kotlin.random.Random

/** Favqulodda kirish uchun matematik misol, masalan 47 × 8 = ? */
data class MathProblem(val left: Int, val right: Int) {
    val answer: Int get() = left * right

    fun isCorrect(input: String): Boolean = input.trim().toIntOrNull() == answer

    companion object {
        /** Chap son 12–99, o'ng son 3–9: boshda hisoblash uchun ~10 soniya diqqat talab qiladi. */
        fun generate(random: Random = Random.Default): MathProblem =
            MathProblem(left = random.nextInt(12, 100), right = random.nextInt(3, 10))
    }
}
