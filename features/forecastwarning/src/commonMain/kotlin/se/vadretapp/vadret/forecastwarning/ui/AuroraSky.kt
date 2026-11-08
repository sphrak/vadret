package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import kotlinx.coroutines.isActive

private const val NANOSECONDS_PER_SECOND = 1_000_000_000f

private val nightSky: Color = Color(color = 0xFF01030A)
private val lowSky: Color = Color(color = 0xFF0A1F2A)
private val auroraGlow: Color = Color(color = 0xFF1F7A55)

// Where the phone cannot run the aurora sky, a still sky with the green glow of the lights stands in.
private val stillSky: Brush = Brush.verticalGradient(colors = listOf(nightSky, auroraGlow, lowSky))

// The sky of an aurora story moves the way the northern lights do, for as long as the story shows it.
@Composable
internal fun AuroraSky(modifier: Modifier = Modifier) {
    val auroraSkyShader: AuroraSkyShader? = remember { auroraSkyShaderOrNull() }
    var seconds: Float by remember { mutableFloatStateOf(value = 0f) }
    if (auroraSkyShader != null) {
        LaunchedEffect(auroraSkyShader) {
            val startNanos: Long = withFrameNanos { frameNanos: Long -> frameNanos }
            while (isActive) {
                withFrameNanos { frameNanos: Long -> seconds = (frameNanos - startNanos) / NANOSECONDS_PER_SECOND }
            }
        }
    }
    Spacer(
        modifier = modifier.drawBehind {
            if (auroraSkyShader != null) {
                drawRect(
                    brush = ShaderBrush(
                        shader = auroraSkyShader.shader(
                            width = size.width,
                            height = size.height,
                            seconds = seconds
                        )
                    )
                )
            } else {
                drawRect(brush = stillSky)
            }
        }
    )
}