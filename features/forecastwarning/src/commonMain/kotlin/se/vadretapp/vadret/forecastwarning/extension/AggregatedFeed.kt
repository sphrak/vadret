package se.vadretapp.vadret.forecastwarning.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetail
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetailKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.warning_detail_advice
import se.vadretapp.vadret.core.resources.warning_detail_comment
import se.vadretapp.vadret.core.resources.warning_detail_description
import se.vadretapp.vadret.core.resources.warning_detail_what_happens
import se.vadretapp.vadret.core.resources.warning_detail_where
import se.vadretapp.vadret.forecastwarning.model.WarningDetail
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.ui.extension.displayName
import se.vadretapp.vadret.ui.extension.toElapsedTime
import se.vadretapp.vadret.ui.format.toDayTimeText
import kotlin.time.Instant

private val commaWithoutSpace = Regex(pattern = ",(?=\\S)")

private const val COMMA_WITH_SPACE = ", "

internal fun List<AggregatedFeed>.toWarningItems(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String
): ImmutableList<WarningItem> =
    sortedByDescending { aggregatedFeed: AggregatedFeed -> aggregatedFeed.publishedAt }
        .map { aggregatedFeed: AggregatedFeed ->
            aggregatedFeed.toWarningItem(
                now = now,
                timeZone = timeZone,
                languageTag = languageTag
            )
        }.toImmutableList()

internal fun AggregatedFeed.toWarningItem(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String
): WarningItem {
    val spacedAreaName: String? = (areaName ?: regionNames())?.withSpacedCommas()
    val publisher: String = senderName ?: source.displayName()
    return WarningItem(
        identifier = identifier,
        headline = headline,
        areaName = spacedAreaName,
        warningLevel = warningLevel,
        source = source,
        isPublicAlert = isPublicAlert,
        publisher = publisher,
        byline = when (source) {
            FeedSource.KRISINFORMATION -> publisher
            FeedSource.SMHI -> spacedAreaName ?: publisher
        },
        elapsedTime = publishedAt.toElapsedTime(now = now),
        startsAtText = startsAt.toDayTimeTextOrNull(
            now = now,
            timeZone = timeZone,
            languageTag = languageTag
        ),
        endsAtText = endsAt.toDayTimeTextOrNull(
            now = now,
            timeZone = timeZone,
            languageTag = languageTag
        ),
        details = details
            .filterNot { feedDetail: FeedDetail -> feedDetail.repeatsArea(spacedAreaName = spacedAreaName) }
            .map { feedDetail: FeedDetail -> feedDetail.toWarningDetail() }
            .toImmutableList(),
        mapArea = area.toWarningMapArea()
    )
}

private fun AggregatedFeed.regionNames(): String? =
    area.regions
        .map { feedRegion: FeedRegion -> feedRegion.name }
        .distinct()
        .joinToString(separator = COMMA_WITH_SPACE)
        .ifEmpty { null }

private fun Instant?.toDayTimeTextOrNull(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String
): String? =
    if (this == null) {
        null
    } else {
        toDayTimeText(
            now = now,
            timeZone = timeZone,
            languageTag = languageTag
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