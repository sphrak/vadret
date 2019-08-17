package fi.kroon.vadret.domain.weatherforecastwidget.shared

import fi.kroon.vadret.data.exception.Failure
import fi.kroon.vadret.data.functional.Either
import fi.kroon.vadret.data.weatherforecastwidget.local.WeatherForecastWidgetLocalKeyValueDataSource
import fi.kroon.vadret.util.THEME_MODE_WIDGET_KEY
import io.reactivex.Single
import javax.inject.Inject

class GetWidgetThemeKeyValueTask @Inject constructor(
    private val localWeatherForecast: WeatherForecastWidgetLocalKeyValueDataSource
) {
    operator fun invoke(appWidgetId: Int): Single<Either<Failure, String>> =
        localWeatherForecast
            .getString(key = THEME_MODE_WIDGET_KEY, appWidgetId = appWidgetId)
}