package se.vadretapp.vadret.radar.ui.map

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import org.maplibre.android.maps.Projection
import se.vadretapp.vadret.radar.wind.RasterArea
import se.vadretapp.vadret.radar.wind.TRAIL_LENGTH
import se.vadretapp.vadret.radar.wind.WindField
import se.vadretapp.vadret.radar.wind.WindParticles
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

private const val PARTICLES_PER_SQUARE_DP = 900f / (360f * 645f)
private const val MAXIMUM_PARTICLE_COUNT = 1800
private const val DAY_PARTICLE_COLOR = 0xD9FFFFFF.toInt()
private const val TRAIL_FADE = 0.88f
private const val SCREEN_DP_PER_METER_PER_SECOND = 0.12f
private const val LINE_WIDTH_DP = 1f
private const val FLOATS_PER_SEGMENT = 4
private const val VERTICES_PER_SEGMENT = 6
private const val FLOATS_PER_VERTEX = 2
private const val ALPHA_SHIFT = 24
private const val RGB_MASK = 0x00FFFFFF
private const val HALF = 2f
private const val FULL_OPACITY = 1f

internal class WindParticleDrawer(
    private val density: Float
) {

    var windField: WindField? = null
    var particleColor: Int = DAY_PARTICLE_COLOR
    var opacity: Float = FULL_OPACITY

    private var windParticles: WindParticles? = null
    private var segments: FloatArray = FloatArray(size = 0)
    private var screenXs: FloatArray = FloatArray(size = 0)
    private var screenYs: FloatArray = FloatArray(size = 0)
    private var vertices: FloatArray = FloatArray(size = 0)
    private var vertexColors: IntArray = IntArray(size = 0)
    private val halfLineWidth: Float = LINE_WIDTH_DP * density / HALF
    private val trianglePaint: Paint = Paint()
    private val linePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = LINE_WIDTH_DP * density
    }

    fun advance(
        projection: Projection,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val field: WindField = windField ?: return
        if (screenWidth == 0 || screenHeight == 0) {
            return
        }
        val particles: WindParticles = particlesFor(
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        val rasterProjection: RasterProjection = projection.toRasterProjection(
            rasterWidth = field.values.width,
            rasterHeight = field.values.height
        )
        val visibleArea: RasterArea = rasterProjection.visibleArea(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            rasterWidth = field.values.width,
            rasterHeight = field.values.height
        )
        if (visibleArea.isEmpty) {
            return
        }
        particles.advance(
            windFieldValues = field.values,
            visibleArea = visibleArea,
            rasterPixelsPerMeterPerSecond = SCREEN_DP_PER_METER_PER_SECOND * density / rasterProjection.screenPixelsPerRasterPixel
        )
    }

    fun draw(
        canvas: Canvas,
        projection: Projection
    ) {
        val field: WindField = windField ?: return
        val particles: WindParticles = windParticles ?: return
        if (!particles.isSeeded) {
            return
        }
        val rasterProjection: RasterProjection = projection.toRasterProjection(
            rasterWidth = field.values.width,
            rasterHeight = field.values.height
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawTriangles(
                canvas = canvas,
                rasterProjection = rasterProjection,
                particles = particles
            )
        } else {
            drawLines(
                canvas = canvas,
                rasterProjection = rasterProjection,
                particles = particles
            )
        }
    }

    private fun drawTriangles(
        canvas: Canvas,
        rasterProjection: RasterProjection,
        particles: WindParticles
    ) {
        projectTrails(
            rasterProjection = rasterProjection,
            particles = particles
        )
        var vertex = 0
        for (age in 0 until TRAIL_LENGTH - 1) {
            val headColor: Int = trailColor(age = age)
            val tailColor: Int = trailColor(age = age + 1)
            for (particle in 0 until particles.particleCount) {
                val head: Int = particle * TRAIL_LENGTH + age
                val tailX: Float = screenXs[head + 1]
                val tailY: Float = screenYs[head + 1]
                val headX: Float = screenXs[head]
                val headY: Float = screenYs[head]
                val deltaX: Float = headX - tailX
                val deltaY: Float = headY - tailY
                val length: Float = sqrt(deltaX * deltaX + deltaY * deltaY)
                val normalX: Float = if (length > 0f) {
                    -deltaY / length * halfLineWidth
                } else {
                    0f
                }
                val normalY: Float = if (length > 0f) {
                    deltaX / length * halfLineWidth
                } else {
                    0f
                }
                vertex = putVertex(vertex = vertex, x = tailX + normalX, y = tailY + normalY, color = tailColor)
                vertex = putVertex(vertex = vertex, x = tailX - normalX, y = tailY - normalY, color = tailColor)
                vertex = putVertex(vertex = vertex, x = headX + normalX, y = headY + normalY, color = headColor)
                vertex = putVertex(vertex = vertex, x = tailX - normalX, y = tailY - normalY, color = tailColor)
                vertex = putVertex(vertex = vertex, x = headX - normalX, y = headY - normalY, color = headColor)
                vertex = putVertex(vertex = vertex, x = headX + normalX, y = headY + normalY, color = headColor)
            }
        }
        canvas.drawVertices(
            Canvas.VertexMode.TRIANGLES,
            vertex * FLOATS_PER_VERTEX,
            vertices,
            0,
            null,
            0,
            vertexColors,
            0,
            null,
            0,
            0,
            trianglePaint
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

    private fun trailColor(age: Int): Int = (trailAlpha(age = age) shl ALPHA_SHIFT) or (particleColor and RGB_MASK)

    private fun trailAlpha(age: Int): Int = ((particleColor ushr ALPHA_SHIFT) * TRAIL_FADE.pow(age) * opacity).toInt()

    private fun drawLines(
        canvas: Canvas,
        rasterProjection: RasterProjection,
        particles: WindParticles
    ) {
        for (age in 0 until TRAIL_LENGTH - 1) {
            for (particle in 0 until particles.particleCount) {
                val offset: Int = particle * FLOATS_PER_SEGMENT
                segments[offset] = rasterProjection.screenX(rasterX = particles.x(particle = particle, age = age + 1))
                segments[offset + 1] = rasterProjection.screenY(rasterY = particles.y(particle = particle, age = age + 1))
                segments[offset + 2] = rasterProjection.screenX(rasterX = particles.x(particle = particle, age = age))
                segments[offset + 3] = rasterProjection.screenY(rasterY = particles.y(particle = particle, age = age))
            }
            linePaint.color = trailColor(age = age)
            canvas.drawLines(segments, linePaint)
        }
    }

    private fun particlesFor(
        screenWidth: Int,
        screenHeight: Int
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
            segments = FloatArray(size = particleCount * FLOATS_PER_SEGMENT)
            screenXs = FloatArray(size = particleCount * TRAIL_LENGTH)
            screenYs = FloatArray(size = particleCount * TRAIL_LENGTH)
            vertices = FloatArray(size = particleCount * (TRAIL_LENGTH - 1) * VERTICES_PER_SEGMENT * FLOATS_PER_VERTEX)
            vertexColors = IntArray(size = particleCount * (TRAIL_LENGTH - 1) * VERTICES_PER_SEGMENT)
            particles
        }
    }
}