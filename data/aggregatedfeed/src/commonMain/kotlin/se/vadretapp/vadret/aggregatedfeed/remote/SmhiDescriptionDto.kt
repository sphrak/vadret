package se.vadretapp.vadret.aggregatedfeed.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SmhiDescriptionDto(
    @SerialName(value = "title")
    val title: SmhiTextDto,
    @SerialName(value = "text")
    val text: SmhiTextDto
)