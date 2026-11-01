package se.vadretapp.vadret.common

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class EitherTest {

    @Test
    fun `a value is kept on the right`() {
        assertEquals(
            expected = 1.asRight(),
            actual = either { 1 }
        )
    }

    @Test
    fun `an exception is kept on the left`() {
        val exception = IOException("broken")

        val result: Either<Exception, Int> = either { throw exception }

        assertEquals(
            expected = exception.asLeft(),
            actual = result
        )
    }

    @Test
    fun `cancellation is rethrown`() {
        assertFailsWith<CancellationException> {
            either<Int> { throw CancellationException("cancelled") }
        }
    }

    @Test
    fun `catch turns the exception into an error value`() {
        val exception = IOException("broken")

        val result: Either<String, Int> = either<Int> { throw exception }
            .catch { caught: Exception -> caught.message.orEmpty() }

        assertEquals(
            expected = "broken".asLeft(),
            actual = result
        )
    }

    @Test
    fun `catch leaves a value alone`() {
        var isTransformed = false

        val result: Either<String, Int> = either { 1 }
            .catch {
                isTransformed = true
                "unused"
            }

        assertEquals(
            expected = 1.asRight(),
            actual = result
        )
        assertSame(expected = false, actual = isTransformed)
    }
}