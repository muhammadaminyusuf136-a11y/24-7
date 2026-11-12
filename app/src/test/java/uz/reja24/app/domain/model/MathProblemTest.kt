package uz.reja24.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathProblemTest {

    @Test
    fun `answer check accepts spaces and rejects garbage`() {
        val problem = MathProblem(47, 8)
        assertEquals(376, problem.answer)
        assertTrue(problem.isCorrect("376"))
        assertTrue(problem.isCorrect(" 376 "))
        assertFalse(problem.isCorrect("375"))
        assertFalse(problem.isCorrect(""))
        assertFalse(problem.isCorrect("abc"))
    }

    @Test
    fun `generated numbers stay in range`() {
        val random = Random(42)
        repeat(1_000) {
            val p = MathProblem.generate(random)
            assertTrue(p.left in 12..99)
            assertTrue(p.right in 3..9)
        }
    }
}
