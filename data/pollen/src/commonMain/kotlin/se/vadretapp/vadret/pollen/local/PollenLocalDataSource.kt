package se.vadretapp.vadret.pollen.local

import kotlinx.coroutines.flow.Flow

internal interface PollenLocalDataSource {

    suspend fun getPollenTypes(): List<PollenTypeEntity>?
    fun observePollenTypes(): Flow<List<PollenTypeEntity>?>
    suspend fun setPollenTypes(pollenTypeEntities: List<PollenTypeEntity>)
}