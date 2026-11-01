package se.vadretapp.vadret.radar.ui.overlay

import se.vadretapp.vadret.radar.image.alphaImageBitmap
import se.vadretapp.vadret.radar.image.argbImageBitmap
import kotlin.test.Test
import kotlin.test.assertNotNull

class PrecipitationShaderTest {

    @Test
    fun `the precipitation shader compiles and binds its inputs`() {
        val precipitationShader: PrecipitationShader? = precipitationShaderOrNull()

        assertNotNull(actual = precipitationShader)
        assertNotNull(
            actual = precipitationShader.shader(
                echoes = alphaImageBitmap(levels = byteArrayOf(0, 64, -128, -1), width = 2, height = 2),
                nextEchoes = alphaImageBitmap(levels = byteArrayOf(-1, -128, 64, 0), width = 2, height = 2),
                motionField = argbImageBitmap(pixels = intArrayOf(0xFF808000.toInt()), width = 1, height = 1),
                progress = 0.25f
            )
        )
    }
}