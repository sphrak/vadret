package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.DrawScope
import se.vadretapp.vadret.radar.ui.map.RasterProjection
import se.vadretapp.vadret.radar.wind.RasterArea
import se.vadretapp.vadret.radar.wind.TRAIL_LENGTH
import se.vadretapp.vadret.radar.wind.WindField
import se.vadretapp.vadret.radar.wind.WindParticles
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

private const val PARTICLES_PER_SQUARE_DP = 900f / (360f * 645f)
private const val MAXIMUM_PARTICLE_COUNT = 1800
private const val TRAIL_FADE = 0.88f
private const val SCREEN_DP_PER_METER_PER_SECOND = 0.12f
private const val LINE_WIDTH_DP = 1f
private const val VERTICES_PER_SEGMENT = 6
private const val FLOATS_PER_VERTEX = 2
private const val ALPHA_SHIFT = 24
private const val RGB_MASK = 0x00FFFFFF
private const val HALF = 2f
private const val NO_LENGTH = 0f
private const val NO_NORMAL = 0f
private const val NO_ADVANCES = 0

internal class WindParticleLayer {

    private var windParticles: WindParticles? = null
    private var advanceCount: Int by mutableIntStateOf(value = NO_ADVANCES)
    private var screenXs: FloatArray = FloatArray(size = 0)
    private var screenYs: FloatArray = FloatArray(size = 0)
    private var vertices: FloatArray = FloatArray(size = 0)
    private var vertexColors: IntArray = IntArray(size = 0)

    fun advance(
        windField: WindField,
        rasterProjection: RasterProjection,
        screenWidth: Float,
        screenHeight: Float,
        density: Float
    ) {
        if (screenWidth <= 0f || screenHeight <= 0f) {
            return
        }
        val particles: WindParticles = particlesFor(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            density = density
        )
        val visibleArea: RasterArea = rasterProjection.visibleArea(
            screenWidth = screenWidth.toInt(),
            screenHeight = screenHeight.toInt(),
            rasterWidth = windField.values.width,
            rasterHeight = windField.values.height
        )
        if (visibleArea.isEmpty) {
            return
        }
        particles.advance(
            windFieldValues = windField.values,
            visibleArea = visibleArea,
            rasterPixelsPerMeterPerSecond = SCREEN_DP_PER_METER_PER_SECOND * density / rasterProjection.screenPixelsPerRasterPixel
        )
        advanceCount++
    }

    fun DrawScope.drawParticles(
        rasterProjection: RasterProjection,
        particleColor: Int,
        opacity: Float
    ) {
        val particles: WindParticles = windParticles ?: return
        if (advanceCount == NO_ADVANCES || !particles.isSeeded) {
            return
        }
        projectTrails(
            rasterProjection = rasterProjection,
            particles = particles
        )
        val halfLineWidth: Float = LINE_WIDTH_DP * density / HALF
        var vertex = 0
        for (age in 0 until TRAIL_LENGTH - 1) {
            val headColor: Int = trailColor(age = age, particleColor = particleColor, opacity = opacity)
            val tailColor: Int = trailColor(age = age + 1, particleColor = particleColor, opacity = opacity)
            for (particle in 0 until particles.particleCount) {
                val head: Int = particle * TRAIL_LENGTH + age
                val tailX: Float = screenXs[head + 1]
                val tailY: Float = screenYs[head + 1]
                val headX: Float = screenXs[head]
                val headY: Float = screenYs[head]
                val deltaX: Float = headX - tailX
                val deltaY: Float = headY - tailY
                val length: Float = sqrt(deltaX * deltaX + deltaY * deltaY)
                val normalX: Float = if (length > NO_LENGTH) {
                    -deltaY / length * halfLineWidth
                } else {
                    NO_NORMAL
                }
                val normalY: Float = if (length > NO_LENGTH) {
                    deltaX / length * halfLineWidth
                } else {
                    NO_NORMAL
                }
                vertex = putVertex(vertex = vertex, x = tailX + normalX, y = tailY + normalY, color = tailColor)
                vertex = putVertex(vertex = vertex, x = tailX - normalX, y = tailY - normalY, color = tailColor)
                vertex = putVertex(vertex = vertex, x = headX + normalX, y = headY + normalY, color = headColor)
                vertex = putVertex(vertex = vertex, x = tailX - normalX, y = tailY - normalY, color = tailColor)
                vertex = putVertex(vertex = vertex, x = headX - normalX, y = headY - normalY, color = headColor)
                vertex = putVertex(vertex = vertex, x = headX + normalX, y = headY + normalY, color = headColor)
            }
        }
        drawColoredTriangles(
            positions = vertices,
            colors = vertexColors
        )
    }

    private fun projectTrails(
        rasterProjection: RasterProjection,
        particles: WindParticles
    ) {
        for (particle in 0 until particles.particleCount) {
            for (age in 0 until TRAIL_LENGTH) {
                val point: Int = particle * TRAIL_LENGTH + age
                screenXs[point] = rasterProjection.screenX(rasterX = particles.x(particle = particle, age = age))
                screenYs[point] = rasterProjection.screenY(rasterY = particles.y(particle = particle, age = age))
            }
        }
    }

    private fun putVertex(
        vertex: Int,
        x: Float,
        y: Float,
        color: Int
    ): Int {
        vertices[vertex * FLOATS_PER_VERTEX] = x
        vertices[vertex * FLOATS_PER_VERTEX + 1] = y
        vertexColors[vertex] = color
        return vertex + 1
    }

    private fun trailColor(
        age: Int,
        particleColor: Int,
        opacity: Float
    ): Int {
        val alpha: Int = ((particleColor ushr ALPHA_SHIFT) * TRAIL_FADE.pow(age) * opacity).toInt()
        return (alpha shl ALPHA_SHIFT) or (particleColor and RGB_MASK)
    }

    private fun particlesFor(
        screenWidth: Float,
        screenHeight: Float,
        density: Float
    ): WindParticles {
        val particleCount: Int = (screenWidth * screenHeight / (density * density) * PARTICLES_PER_SQUARE_DP)
            .toInt()
            .coerceAtMost(maximumValue = MAXIMUM_PARTICLE_COUNT)
        val current: WindParticles? = windParticles
        return if (current != null && current.particleCount == particleCount) {
            current
        } else {
            val particles = WindParticles(
                particleCount = particleCount,
                random = Random.Default
            )
            windParticles = particles
            screenXs = FloatArray(size = particleCount * TRAIL_LENGTH)
            screenYs = FloatArray(size = particleCount * TRAIL_LENGTH)
            vertices = FloatArray(size = particleCount * (TRAIL_LENGTH - 1) * VERTICES_PER_SEGMENT * FLOATS_PER_VERTEX)
            vertexColors = IntArray(size = particleCount * (TRAIL_LENGTH - 1) * VERTICES_PER_SEGMENT)
            particles
        }
    }
}