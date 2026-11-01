package se.vadretapp.vadret.radar.projection

import se.vadretapp.vadret.radar.extension.toDegrees
import se.vadretapp.vadret.radar.extension.toRadians
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

internal object WebMercator {

    fun y(latitude: Double): Double = ln(tan(PI / 4.0 + latitude.toRadians() / 2.0))

    fun latitude(y: Double): Double = (2.0 * atan(exp(y)) - PI / 2.0).toDegrees()
}