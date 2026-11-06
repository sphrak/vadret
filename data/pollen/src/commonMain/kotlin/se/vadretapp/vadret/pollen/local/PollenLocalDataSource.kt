package se.vadretapp.vadret.pollen.local

import kotlinx.coroutines.flow.Flow

internal interface PollenLocalDataSource {

    suspend fun getPollenTypes(): PollenTypesCacheEntity?
    fun observePollenTypes(): Flow<PollenTypesCacheEntity?>
    suspend fun setPollenTypes(pollenTypesCacheEntity: PollenTypesCacheEntity)
}