package se.vadretapp.vadret.weatherforecast.model

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.forecast_days
import se.vadretapp.vadret.ui.resources.forecast_hours
import se.vadretapp.vadret.ui.resources.ic_calendar_view_day
import se.vadretapp.vadret.ui.resources.ic_schedule

enum class ForecastView(
    val titleResource: StringResource,
    val iconResource: DrawableResource
) {
    DAYS(
        titleResource = Res.string.forecast_days,
        iconResource = Res.drawable.ic_calendar_view_day
    ),
    HOURS(
        titleResource = Res.string.forecast_hours,
        iconResource = Res.drawable.ic_schedule
    )
}