package se.vadretapp.vadret.forecastwarning.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.ui.model.ElapsedTime

@Immutable
internal data class WarningItem(
    val identifier: String,
    val headline: String,
    val areaName: String?,
    val warningLevel: WarningLevel?,
    val source: FeedSource,
    val isPublicAlert: Boolean,
    val publisher: String,
    val byline: String,
    val elapsedTime: ElapsedTime,
    val startsAtText: String?,
    val endsAtText: String?,
    val details: ImmutableList<WarningDetail>,
    val mapArea: WarningMapArea
)