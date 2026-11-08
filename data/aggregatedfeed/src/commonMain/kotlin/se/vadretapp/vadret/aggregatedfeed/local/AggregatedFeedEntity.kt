package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AggregatedFeedEntity(
    @SerialName(value = "identifier")
    val identifier: String,
    @SerialName(value = "headline")
    val headline: String,
    @SerialName(value = "area_name")
    val areaName: String? = null,
    @SerialName(value = "warning_level")
    val warningLevel: String? = null,
    @SerialName(value = "details")
    val details: List<FeedDetailEntity>,
    @SerialName(value = "source")
    val source: String,
    @SerialName(value = "is_public_alert")
    val isPublicAlert: Boolean = false,
    @SerialName(value = "sender_name")
    val senderName: String? = null,
    @SerialName(value = "published_at_epoch_millis")
    val publishedAtEpochMillis: Long,
    @SerialName(value = "starts_at_epoch_millis")
    val startsAtEpochMillis: Long? = null,
    @SerialName(value = "ends_at_epoch_millis")
    val endsAtEpochMillis: Long? = null,
    @SerialName(value = "polygons")
    val polygons: List<GeoPolygonEntity>,
    @SerialName(value = "lines")
    val lines: List<GeoLineEntity>,
    @SerialName(value = "regions")
    val regions: List<FeedRegionEntity>,
    @SerialName(value = "slipperiness")
    val slipperiness: SlipperinessEntity? = null
)