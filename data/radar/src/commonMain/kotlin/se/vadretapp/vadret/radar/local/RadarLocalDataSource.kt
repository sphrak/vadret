package se.vadretapp.vadret.radar.local

import kotlinx.coroutines.flow.Flow

internal interface RadarLocalDataSource {

    suspend fun getRadar(): RadarCacheEntity?
    fun observeRadar(): Flow<RadarCacheEntity?>
    suspend fun setRadar(radarCacheEntity: RadarCacheEntity)
}