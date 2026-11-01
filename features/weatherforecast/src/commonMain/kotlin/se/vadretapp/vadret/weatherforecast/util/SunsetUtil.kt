package se.vadretapp.vadret.weatherforecast.util

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.time.Instant

internal object SunsetUtil {

    private const val SUN_ALTITUDE_SUNRISE_SUNSET: Double = -0.833
    private const val EARTH_AXIAL_TILT_DEGREES: Double = 23.439
    private const val JULIAN_DATE_2000_01_01: Double = 2451545.0
    private const val JULIAN_DATE_UNIX_EPOCH: Double = 2440587.5
    private const val LEAP_SECONDS_DAYS: Double = 0.0009
    private const val DEGREES_IN_CIRCLE: Double = 360.0
    private const val DEGREES_IN_HALF_CIRCLE: Double = 180.0
    private const val MILLISECONDS_IN_DAY: Double = 86_400_000.0
    private const val HALF_DAY: Double = 0.5
    private const val MEAN_ANOMALY_AT_J2000_DEGREES: Double = 357.5291
    private const val MEAN_ANOMALY_DEGREES_PER_DAY: Double = 0.98560028
    private const val EQUATION_OF_CENTER_FIRST_HARMONIC_DEGREES: Double = 1.9148
    private const val EQUATION_OF_CENTER_SECOND_HARMONIC_DEGREES: Double = 0.0200
    private const val EQUATION_OF_CENTER_THIRD_HARMONIC_DEGREES: Double = 0.0003
    private const val PERIHELION_ARGUMENT_DEGREES: Double = 102.9372
    private const val EQUATION_OF_TIME_ECCENTRICITY_DAYS: Double = 0.0053
    private const val EQUATION_OF_TIME_OBLIQUITY_DAYS: Double = 0.0069
    private const val SECOND_HARMONIC: Double = 2.0
    private const val THIRD_HARMONIC: Double = 3.0
    private const val MINIMUM_COSINE: Double = -1.0

    fun getSunriseSunset(
        day: Instant,
        latitude: Double,
        longitude: Double
    ): SunriseSunset? {
        val solarNoon: SolarNoon = solarNoon(
            day = day,
            longitude = longitude
        )
        val hourAngle: Double = acos(
            hourAngleCosine(
                latitude = latitude,
                declination = solarNoon.declination
            )
        )
        if (hourAngle.isNaN()) {
            return null
        }
        val sunset: Double = solarNoon.transit + hourAngle.toDegrees() / DEGREES_IN_CIRCLE
        val sunrise: Double = solarNoon.transit - hourAngle.toDegrees() / DEGREES_IN_CIRCLE
        return SunriseSunset(
            sunrise = sunrise.julianDateToInstant(),
            sunset = sunset.julianDateToInstant()
        )
    }

    fun isSunUp(
        at: Instant,
        latitude: Double,
        longitude: Double
    ): Boolean {
        val sunriseSunset: SunriseSunset? = getSunriseSunset(
            day = at,
            latitude = latitude,
            longitude = longitude
        )
        return if (sunriseSunset != null) {
            at >= sunriseSunset.sunrise && at < sunriseSunset.sunset
        } else {
            hourAngleCosine(
                latitude = latitude,
                declination = solarNoon(
                    day = at,
                    longitude = longitude
                ).declination
            ) < MINIMUM_COSINE
        }
    }

    private fun solarNoon(
        day: Instant,
        longitude: Double
    ): SolarNoon {
        val westLongitude: Double = -longitude
        val daysSinceJ2000: Double = day.toJulianDate() - JULIAN_DATE_2000_01_01 - LEAP_SECONDS_DAYS - westLongitude / DEGREES_IN_CIRCLE
        val cycle: Double = floor(daysSinceJ2000 + HALF_DAY)
        val approximateSolarNoon: Double = JULIAN_DATE_2000_01_01 + LEAP_SECONDS_DAYS + westLongitude / DEGREES_IN_CIRCLE + cycle
        val meanAnomalyDegrees: Double = (MEAN_ANOMALY_AT_J2000_DEGREES + MEAN_ANOMALY_DEGREES_PER_DAY * (approximateSolarNoon - JULIAN_DATE_2000_01_01)) % DEGREES_IN_CIRCLE
        val meanAnomaly: Double = meanAnomalyDegrees.toRadians()
        val equationOfCenter: Double = EQUATION_OF_CENTER_FIRST_HARMONIC_DEGREES * sin(meanAnomaly) +
            EQUATION_OF_CENTER_SECOND_HARMONIC_DEGREES * sin(SECOND_HARMONIC * meanAnomaly) +
            EQUATION_OF_CENTER_THIRD_HARMONIC_DEGREES * sin(THIRD_HARMONIC * meanAnomaly)
        val eclipticLongitudeDegrees: Double = (meanAnomaly.toDegrees() + PERIHELION_ARGUMENT_DEGREES + equationOfCenter + DEGREES_IN_HALF_CIRCLE) % DEGREES_IN_CIRCLE
        val eclipticLongitude: Double = eclipticLongitudeDegrees.toRadians()
        val transit: Double = approximateSolarNoon +
            EQUATION_OF_TIME_ECCENTRICITY_DAYS * sin(meanAnomaly) -
            EQUATION_OF_TIME_OBLIQUITY_DAYS * sin(SECOND_HARMONIC * eclipticLongitude)
        return SolarNoon(
            transit = transit,
            declination = asin(sin(eclipticLongitude) * sin(EARTH_AXIAL_TILT_DEGREES.toRadians()))
        )
    }

    private fun hourAngleCosine(
        latitude: Double,
        declination: Double
    ): Double {
        val latitudeRadians: Double = latitude.toRadians()
        return (sin(SUN_ALTITUDE_SUNRISE_SUNSET.toRadians()) - sin(latitudeRadians) * sin(declination)) /
            (cos(latitudeRadians) * cos(declination))
    }

    private fun Instant.toJulianDate(): Double = toEpochMilliseconds() / MILLISECONDS_IN_DAY + JULIAN_DATE_UNIX_EPOCH

    private fun Double.julianDateToInstant(): Instant = Instant.fromEpochMilliseconds(((this - JULIAN_DATE_UNIX_EPOCH) * MILLISECONDS_IN_DAY).roundToLong())

    private fun Double.toRadians(): Double = this * PI / DEGREES_IN_HALF_CIRCLE

    private fun Double.toDegrees(): Double = this * DEGREES_IN_HALF_CIRCLE / PI
}