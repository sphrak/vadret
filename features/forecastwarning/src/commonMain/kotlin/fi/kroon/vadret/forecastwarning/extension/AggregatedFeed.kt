package fi.kroon.vadret.forecastwarning.extension

import fi.kroon.vadret.aggregatedfeed.model.AggregatedFeed
import fi.kroon.vadret.aggregatedfeed.model.FeedDetail
import fi.kroon.vadret.aggregatedfeed.model.FeedDetailKind
import fi.kroon.vadret.aggregatedfeed.model.FeedRegion
import fi.kroon.vadret.aggregatedfeed.model.FeedSource
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.warning_detail_advice
import fi.kroon.vadret.core.resources.warning_detail_comment
import fi.kroon.vadret.core.resources.warning_detail_description
import fi.kroon.vadret.core.resources.warning_detail_what_happens
import fi.kroon.vadret.core.resources.warning_detail_where
import fi.kroon.vadret.forecastwarning.model.WarningDetail
import fi.kroon.vadret.forecastwarning.model.WarningItem
import fi.kroon.vadret.ui.extension.displayName
import fi.kroon.vadret.ui.extension.toElapsedTime
import fi.kroon.vadret.ui.format.toDayTimeText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
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