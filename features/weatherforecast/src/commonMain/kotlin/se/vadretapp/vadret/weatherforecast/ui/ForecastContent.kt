package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
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
import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.weatherforecast.WeatherForecastEvent
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import se.vadretapp.vadret.weatherforecast.model.DayToShow
import se.vadretapp.vadret.weatherforecast.model.ForecastDay
import se.vadretapp.vadret.weatherforecast.model.ForecastView
import se.vadretapp.vadret.weatherforecast.model.WeatherForecast

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
    dayToShow: DayToShow?,
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
                dayToShow = dayToShow,
                isInPlace = transition.currentState == EnterExitState.Visible && transition.targetState == EnterExitState.Visible,
                onEvent = onEvent
            )
            ForecastView.HOURS -> ForecastDaysContent(forecastDays = forecast.days)
        }
    }
}

@Composable
private fun ComingDaysContent(
    comingDays: ImmutableList<ComingDay>,
    dayToShow: DayToShow?,
    isInPlace: Boolean,
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
            dayToShow = dayToShow,
            isInPlace = isInPlace,
            onDayToggled = { dayKey: String ->
                onEvent(WeatherForecastEvent.OnDayToggled(dayKey = dayKey))
            },
            onDayScrolledTo = { dayKey: String ->
                onEvent(WeatherForecastEvent.OnDayScrolledTo(dayKey = dayKey))
            },
            onDayShown = { dayKey: String ->
                onEvent(WeatherForecastEvent.OnDayShown(dayKey = dayKey))
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