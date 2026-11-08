package se.vadretapp.vadret.forecastwarning.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetail
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetailKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.Slipperiness
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessCause
import se.vadretapp.vadret.forecastwarning.model.WarningDetail
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.icons.Slipperiness
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.extension.displayName
import se.vadretapp.vadret.ui.extension.explanationResource
import se.vadretapp.vadret.ui.extension.labelResource
import se.vadretapp.vadret.ui.extension.nameResource
import se.vadretapp.vadret.ui.extension.toElapsedTime
import se.vadretapp.vadret.ui.format.toDayTimeText
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.slipperiness_estimate_explanation
import se.vadretapp.vadret.ui.resources.slipperiness_estimate_label
import se.vadretapp.vadret.ui.resources.warning_detail_advice
import se.vadretapp.vadret.ui.resources.warning_detail_comment
import se.vadretapp.vadret.ui.resources.warning_detail_description
import se.vadretapp.vadret.ui.resources.warning_detail_what_happens
import se.vadretapp.vadret.ui.resources.warning_detail_where
import kotlin.time.Instant

private val commaWithoutSpace = Regex(pattern = ",(?=\\S)")

private const val COMMA_WITH_SPACE = ", "

internal fun List<AggregatedFeed>.toWarningItems(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String,
    hourFormat: HourFormat
): ImmutableList<WarningItem> =
    sortedByDescending { aggregatedFeed: AggregatedFeed -> aggregatedFeed.publishedAt }
        .map { aggregatedFeed: AggregatedFeed ->
            aggregatedFeed.toWarningItem(
                now = now,
                timeZone = timeZone,
                languageTag = languageTag,
                hourFormat = hourFormat
            )
        }.toImmutableList()

internal fun AggregatedFeed.toWarningItem(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String,
    hourFormat: HourFormat,
    followedPlaceNames: String? = null
): WarningItem {
    val spacedAreaName: String? = (areaName ?: regionNames())?.withSpacedCommas()
    val publisher: String = senderName ?: source.displayName()
    val estimatedSlipperiness: Slipperiness? = slipperiness
    return WarningItem(
        identifier = identifier,
        headline = headline,
        headlineResource = estimatedSlipperiness?.headlineResource(),
        icon = if (estimatedSlipperiness == null) {
            null
        } else {
            VadretIcons.Alert.Slipperiness
        },
        areaName = spacedAreaName,
        warningLevel = warningLevel,
        slipperinessRisk = estimatedSlipperiness?.risk,
        source = source,
        isPublicAlert = isPublicAlert,
        publisher = publisher,
        byline = when (source) {
            FeedSource.KRISINFORMATION -> publisher
            FeedSource.SMHI -> spacedAreaName ?: publisher
            FeedSource.VADRET_API -> publisher
        },
        elapsedTime = publishedAt.toElapsedTime(now = now),
        startsAtText = startsAt.toDayTimeTextOrNull(
            now = now,
            timeZone = timeZone,
            languageTag = languageTag,
            hourFormat = hourFormat
        ),
        endsAtText = endsAt.toDayTimeTextOrNull(
            now = now,
            timeZone = timeZone,
            languageTag = languageTag,
            hourFormat = hourFormat
        ),
        details = if (estimatedSlipperiness == null) {
            details
                .filterNot { feedDetail: FeedDetail -> feedDetail.repeatsArea(spacedAreaName = spacedAreaName) }
                .map { feedDetail: FeedDetail -> feedDetail.toWarningDetail() }
        } else {
            estimatedSlipperiness.toWarningDetails()
        }.toImmutableList(),
        mapArea = area.toWarningMapArea(),
        followedPlaceNames = followedPlaceNames
    )
}

private fun Slipperiness.headlineResource(): StringResource = causes.firstOrNull()?.nameResource() ?: risk.labelResource()

private fun Slipperiness.toWarningDetails(): List<WarningDetail> =
    causes.map { slipperinessCause: SlipperinessCause ->
        WarningDetail.Explained(
            labelResource = slipperinessCause.nameResource(),
            textResource = slipperinessCause.explanationResource()
        )
    } + WarningDetail.Explained(
        labelResource = Res.string.slipperiness_estimate_label,
        textResource = Res.string.slipperiness_estimate_explanation
    )

private fun AggregatedFeed.regionNames(): String? =
    area.regions
        .map { feedRegion: FeedRegion -> feedRegion.name }
        .distinct()
        .joinToString(separator = COMMA_WITH_SPACE)
        .ifEmpty { null }

private fun Instant?.toDayTimeTextOrNull(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String,
    hourFormat: HourFormat
): String? =
    if (this == null) {
        null
    } else {
        toDayTimeText(
            now = now,
            timeZone = timeZone,
            languageTag = languageTag,
            hourFormat = hourFormat
        )
    }

private fun FeedDetail.repeatsArea(spacedAreaName: String?): Boolean =
    kind == FeedDetailKind.WHERE &&
        text.withSpacedCommas() == spacedAreaName

private fun FeedDetail.toWarningDetail(): WarningDetail =
    when (kind) {
        FeedDetailKind.WHAT_HAPPENS -> WarningDetail.Labelled(
            labelResource = Res.string.warning_detail_what_happens,
            text = text
        )
        FeedDetailKind.DESCRIPTION -> WarningDetail.Labelled(
            labelResource = Res.string.warning_detail_description,
            text = text
        )
        FeedDetailKind.WHERE -> WarningDetail.Labelled(
            labelResource = Res.string.warning_detail_where,
            text = text
        )
        FeedDetailKind.ADVICE -> WarningDetail.Labelled(
            labelResource = Res.string.warning_detail_advice,
            text = text
        )
        FeedDetailKind.COMMENT -> WarningDetail.Labelled(
            labelResource = Res.string.warning_detail_comment,
            text = text
        )
        FeedDetailKind.SUMMARY -> WarningDetail.Paragraph(text = text.withSpacedCommas())
        FeedDetailKind.BODY -> WarningDetail.Paragraph(text = text)
        FeedDetailKind.OTHER -> WarningDetail.Titled(
            title = title,
            text = text
        )
    }

private fun String.withSpacedCommas(): String =
    replace(
        regex = commaWithoutSpace,
        replacement = COMMA_WITH_SPACE
    )