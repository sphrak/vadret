package fi.kroon.vadret.weatherforecast.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fi.kroon.vadret.ui.VadretTheme
import fi.kroon.vadret.weatherforecast.WeatherForecastEvent
import fi.kroon.vadret.weatherforecast.model.ComingDay
import fi.kroon.vadret.weatherforecast.model.ForecastDay
import fi.kroon.vadret.weatherforecast.model.ForecastView
import fi.kroon.vadret.weatherforecast.model.WeatherForecast
import kotlinx.collections.immutable.ImmutableList

private const val SLIDE_MILLISECONDS = 300
private const val TOWARDS_START = -1
private const val TOWARDS_END = 1

private val slideGutter = 13.dp
private val forecastDaySpacing = 13.dp
private val forecastDayHeaderSpacing = 8.dp

private val slideSpec: TweenSpec<IntOffset> = tween(
    durationMillis = SLIDE_MILLISECONDS,
    easing = FastOutSlowInEasing
)

@Composable
internal fun ForecastContent(
    forecastView: ForecastView,
    forecast: WeatherForecast,
    onEvent: (WeatherForecastEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val gutter: Int = with(receiver = LocalDensity.current) {
        slideGutter.roundToPx()
    }
    AnimatedContent(
        targetState = forecastView,
        transitionSpec = {
            val direction: Int = if (targetState.ordinal > initialState.ordinal) {
                TOWARDS_START
            } else {
                TOWARDS_END
            }
            slideInHorizontally(animationSpec = slideSpec) { width: Int ->
                -direction * (width + gutter)
            }.togetherWith(
                exit = slideOutHorizontally(animationSpec = slideSpec) { width: Int ->
                    direction * (width + gutter)
                }
            ).using(
                sizeTransform = SizeTransform(
                    clip = false,
                    sizeAnimationSpec = { _, _ -> snap() }
                )
            )
        },
        modifier = modifier
    ) { shownForecastView: ForecastView ->
        when (shownForecastView) {
            ForecastView.DAYS -> ComingDaysContent(
                comingDays = forecast.comingDays,
                onEvent = onEvent
            )
            ForecastView.HOURS -> ForecastDaysContent(forecastDays = forecast.days)
        }
    }
}

@Composable
private fun ComingDaysContent(
    comingDays: ImmutableList<ComingDay>,
    onEvent: (WeatherForecastEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (comingDays.isEmpty()) {
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.medium),
        modifier = modifier
    ) {
        // ComingDaysHeader(dayCount = comingDays.size)
        ComingDaysCard(
            comingDays = comingDays,
            onDayToggled = { dayKey: String ->
                onEvent(WeatherForecastEvent.OnDayToggled(dayKey = dayKey))
            }
        )
    }
}

@Composable
private fun ForecastDaysContent(
    forecastDays: ImmutableList<ForecastDay>,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(space = forecastDaySpacing),
        modifier = modifier
    ) {
        forecastDays.forEach { forecastDay: ForecastDay ->
            key(forecastDay.key) {
                Column(verticalArrangement = Arrangement.spacedBy(space = forecastDayHeaderSpacing)) {
                    ForecastDayHeader(forecastDay = forecastDay)
                    ForecastDayCard(forecastDay = forecastDay)
                }
            }
        }
    }
}