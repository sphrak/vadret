package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.asComposeShader
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal actual fun auroraSkyShaderOrNull(): AuroraSkyShader? = DefaultAuroraSkyShader()

internal class DefaultAuroraSkyShader : AuroraSkyShader {

    private val runtimeShaderBuilder = RuntimeShaderBuilder(effect = RuntimeEffect.makeForShader(sksl = AURORA_SKY_SHADER))

    override fun shader(
        width: Float,
        height: Float,
        seconds: Float
    ): Shader {
        runtimeShaderBuilder.uniform(
            name = RESOLUTION_UNIFORM,
            value1 = width,
            value2 = height
        )
        runtimeShaderBuilder.uniform(name = TIME_UNIFORM, value = seconds)
        return runtimeShaderBuilder.makeShader().asComposeShader()
    }
}