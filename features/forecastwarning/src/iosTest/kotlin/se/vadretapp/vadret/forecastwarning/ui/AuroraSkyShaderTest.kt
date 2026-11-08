package se.vadretapp.vadret.forecastwarning.ui

import kotlin.test.Test
import kotlin.test.assertNotNull

class AuroraSkyShaderTest {

    @Test
    fun `the aurora sky compiles and takes its size and time`() {
        val auroraSkyShader: AuroraSkyShader? = auroraSkyShaderOrNull()

        assertNotNull(actual = auroraSkyShader)
        assertNotNull(
            actual = auroraSkyShader.shader(
                width = 1080f,
                height = 667f,
                seconds = 12.5f
            )
        )
    }
}