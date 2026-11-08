package se.vadretapp.vadret.weatherforecast.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessRisk
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel

@Immutable
data class LatestWarning(
    val warningLevel: WarningLevel?,
    val slipperinessRisk: SlipperinessRisk?,
    val source: FeedSource,
    val isPublicAlert: Boolean,
    val headline: String,
    val endsAtText: String?,
    val warningIdentifiers: ImmutableList<String>
)