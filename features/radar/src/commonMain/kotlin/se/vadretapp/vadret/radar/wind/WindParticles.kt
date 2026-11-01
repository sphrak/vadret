package se.vadretapp.vadret.radar.wind

import kotlin.math.roundToInt
import kotlin.random.Random

internal const val TRAIL_LENGTH = 10

private const val MINIMUM_LIFESPAN = 30
private const val MAXIMUM_LIFESPAN = 80
private const val SPAWN_ATTEMPTS = 8
private const val NO_PARTICLES = 0
private const val RELOCATION_ATTEMPTS_PER_PARTICLE = 2

internal class WindParticles(
    val particleCount: Int,
    private val random: Random
) {

    private val historyX = FloatArray(size = particleCount * TRAIL_LENGTH)
    private val historyY = FloatArray(size = particleCount * TRAIL_LENGTH)
    private val ages = IntArray(size = particleCount)
    private val lifespans = IntArray(size = particleCount)
    private var head: Int = 0
    private var shownArea: RasterArea? = null
    var isSeeded: Boolean = false
        private set

    fun advance(
        windFieldValues: WindFieldValues,
        visibleArea: RasterArea,
        rasterPixelsPerMeterPerSecond: Float
    ) {
        if (!isSeeded) {
            repeat(times = particleCount) { particle: Int ->
                respawn(
                    particle = particle,
                    windFieldValues = windFieldValues,
                    spawnArea = visibleArea
                )
            }
            shownArea = visibleArea
            isSeeded = true
            return
        }
        val previousArea: RasterArea? = shownArea
        val revealedAreas: List<RasterArea> = if (previousArea == null) {
            emptyList()
        } else {
            visibleArea.uncoveredBy(other = previousArea)
        }
        shownArea = visibleArea
        val revealedParticleCount: Int = revealedParticleCount(
            revealedAreas = revealedAreas,
            visibleArea = visibleArea
        )
        var revealedSpawnCount = 0
        val previous: Int = head
        head = (head + 1) % TRAIL_LENGTH
        for (particle in 0 until particleCount) {
            val x: Float = historyX[slot(particle = particle, index = previous)]
            val y: Float = historyY[slot(particle = particle, index = previous)]
            val pixel: Int = windFieldValues.pixelAt(x = x, y = y)
            ages[particle] = ages[particle] + 1
            val eastward: Float = windFieldValues.valueAt(values = windFieldValues.eastward, pixel = pixel)
            val northward: Float = windFieldValues.valueAt(values = windFieldValues.northward, pixel = pixel)
            val hasLeftScreen: Boolean = !visibleArea.contains(x = x, y = y)
            val isExpired: Boolean = ages[particle] > lifespans[particle] || eastward.isNaN() || northward.isNaN() || hasLeftScreen
            if (isExpired) {
                val isRevealedSpawn: Boolean = hasLeftScreen && revealedSpawnCount < revealedParticleCount
                if (isRevealedSpawn) {
                    revealedSpawnCount++
                }
                respawn(
                    particle = particle,
                    windFieldValues = windFieldValues,
                    spawnArea = if (isRevealedSpawn) {
                        revealedAreas.randomByArea()
                    } else {
                        visibleArea
                    }
                )
            } else {
                historyX[slot(particle = particle, index = head)] = x + eastward * rasterPixelsPerMeterPerSecond
                historyY[slot(particle = particle, index = head)] = y - northward * rasterPixelsPerMeterPerSecond
            }
        }
        var missingCount: Int = revealedParticleCount - revealedSpawnCount
        var attempt = 0
        while (missingCount > 0 && attempt < particleCount * RELOCATION_ATTEMPTS_PER_PARTICLE) {
            attempt++
            val particle: Int = random.nextInt(until = particleCount)
            val isRevealed: Boolean = revealedAreas.any { rasterArea: RasterArea ->
                rasterArea.contains(
                    x = x(particle = particle, age = 0),
                    y = y(particle = particle, age = 0)
                )
            }
            if (!isRevealed) {
                respawn(
                    particle = particle,
                    windFieldValues = windFieldValues,
                    spawnArea = revealedAreas.randomByArea()
                )
                missingCount--
            }
        }
    }

    private fun revealedParticleCount(
        revealedAreas: List<RasterArea>,
        visibleArea: RasterArea
    ): Int {
        if (revealedAreas.isEmpty() || visibleArea.isEmpty) {
            return NO_PARTICLES
        }
        val revealedArea: Double = revealedAreas.sumOf { rasterArea: RasterArea -> rasterArea.area.toDouble() }
        return (particleCount * revealedArea / visibleArea.area).roundToInt()
    }

    fun x(
        particle: Int,
        age: Int
    ): Float = historyX[slot(particle = particle, index = historyIndex(age = age))]

    fun y(
        particle: Int,
        age: Int
    ): Float = historyY[slot(particle = particle, index = historyIndex(age = age))]

    fun reset() {
        isSeeded = false
        shownArea = null
    }

    private fun respawn(
        particle: Int,
        windFieldValues: WindFieldValues,
        spawnArea: RasterArea
    ) {
        var x: Float = spawnArea.randomX(random = random)
        var y: Float = spawnArea.randomY(random = random)
        var attempt = 1
        while (attempt < SPAWN_ATTEMPTS && windFieldValues.isEmptyAt(x = x, y = y)) {
            x = spawnArea.randomX(random = random)
            y = spawnArea.randomY(random = random)
            attempt++
        }
        for (index in 0 until TRAIL_LENGTH) {
            historyX[slot(particle = particle, index = index)] = x
            historyY[slot(particle = particle, index = index)] = y
        }
        ages[particle] = 0
        lifespans[particle] = random.nextInt(from = MINIMUM_LIFESPAN, until = MAXIMUM_LIFESPAN + 1)
    }

    private fun List<RasterArea>.randomByArea(): RasterArea {
        var remainingArea: Float = random.nextFloat() * sumOf { rasterArea: RasterArea -> rasterArea.area.toDouble() }.toFloat()
        for (rasterArea in this) {
            if (remainingArea < rasterArea.area) {
                return rasterArea
            }
            remainingArea -= rasterArea.area
        }
        return last()
    }

    private fun historyIndex(age: Int): Int = (head - age).mod(other = TRAIL_LENGTH)

    private fun slot(
        particle: Int,
        index: Int
    ): Int = particle * TRAIL_LENGTH + index

    private fun WindFieldValues.isEmptyAt(
        x: Float,
        y: Float
    ): Boolean {
        val pixel: Int = pixelAt(x = x, y = y)
        return pixel == OUTSIDE_FIELD || speeds[pixel].isNaN()
    }
}