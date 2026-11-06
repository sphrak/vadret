package se.vadretapp.vadret.pushalert.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PushAlertChoicesEntity(
    @SerialName(value = "warning_lowest_level")
    val warningLowestLevel: String? = null,
    @SerialName(value = "pollen_choices")
    val pollenChoices: List<PollenChoiceEntity> = emptyList(),
    @SerialName(value = "temperature_trend")
    val temperatureTrend: Boolean = false,
    @SerialName(value = "strong_wind")
    val strongWind: Boolean = false,
    @SerialName(value = "is_chosen_place_notified")
    val isChosenPlaceNotified: Boolean = true,
    @SerialName(value = "notified_favorite_ids")
    val notifiedFavoriteIds: List<String> = emptyList()
)