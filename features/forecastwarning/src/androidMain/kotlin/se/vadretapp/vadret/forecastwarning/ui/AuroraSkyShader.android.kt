package se.vadretapp.vadret.forecastwarning.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Shader

internal actual fun auroraSkyShaderOrNull(): AuroraSkyShader? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        DefaultAuroraSkyShader()
    } else {
        null
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class DefaultAuroraSkyShader : AuroraSkyShader {

    private val runtimeShader = RuntimeShader(AURORA_SKY_SHADER)

    override fun shader(
        width: Float,
        height: Float,
        seconds: Float
    ): Shader {
        runtimeShader.setFloatUniform(RESOLUTION_UNIFORM, width, height)
        runtimeShader.setFloatUniform(TIME_UNIFORM, seconds)
        return runtimeShader
    }
}