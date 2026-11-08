package se.vadretapp.vadret.forecastwarning.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.aggregatedfeed.model.AuroraChance
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessRisk
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.icons.VadretIconGraphic
import se.vadretapp.vadret.ui.model.ElapsedTime

@Immutable
internal data class WarningItem(
    val identifier: String,
    val headline: String,
    val headlineResource: StringResource?,
    val headlineArguments: ImmutableList<String>,
    val icon: VadretIconGraphic?,
    val areaName: String?,
    val warningLevel: WarningLevel?,
    val slipperinessRisk: SlipperinessRisk?,
    val auroraChance: AuroraChance?,
    val source: FeedSource,
    val isPublicAlert: Boolean,
    val publisher: String,
    val byline: String,
    val elapsedTime: ElapsedTime,
    val startsAtText: String?,
    val endsAtText: String?,
    val details: ImmutableList<WarningDetail>,
    val mapArea: WarningMapArea,
    val followedPlaceNames: String?
)