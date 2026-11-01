package se.vadretapp.vadret.wind.local

import kotlin.concurrent.atomics.AtomicReference

internal class DefaultWindLocalDataSource : WindLocalDataSource {

    private val windForecastEntity: AtomicReference<WindForecastEntity?> = AtomicReference(value = null)

    override fun getWindForecast(): WindForecastEntity? = windForecastEntity.load()

    override fun setWindForecast(windForecastEntity: WindForecastEntity) {
        this.windForecastEntity.store(newValue = windForecastEntity)
    }
}