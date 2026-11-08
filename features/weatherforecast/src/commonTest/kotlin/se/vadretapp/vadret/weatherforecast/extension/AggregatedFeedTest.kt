package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.Aurora
import se.vadretapp.vadret.aggregatedfeed.model.AuroraChance
import se.vadretapp.vadret.aggregatedfeed.model.Slipperiness
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessRisk
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.icons.Aurora
import se.vadretapp.vadret.icons.Slipperiness
import se.vadretapp.vadret.icons.VadretIconGraphic
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.weatherforecast.model.LatestWarning
import se.vadretapp.vadret.weatherforecast.warning
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AggregatedFeedTest {

    @Test
    fun aSlipperinessEstimateShowsTheSlipperinessIconAndAnyOtherWarningItsSourcesIcon() {
        val icons: List<VadretIconGraphic?> = listOf(
            warning(identifier = "smhi-1"),
            warning(identifier = "vadret-slipperiness-u6sce-2026-11-07-POSSIBLE").copy(
                slipperiness = Slipperiness(
                    risk = SlipperinessRisk.POSSIBLE,
                    causes = emptyList()
                )
            )
        ).map { aggregatedFeed ->
            listOf(aggregatedFeed).toLatestWarning(
                now = Instant.parse(input = "2026-11-07T18:00:00Z"),
                hourFormat = HourFormat.TWENTY_FOUR_HOUR,
                timeZone = TimeZone.of(zoneId = "Europe/Stockholm"),
                languageTag = "sv-SE"
            )?.icon
        }

        assertEquals(
            expected = listOf(null, VadretIcons.Alert.Slipperiness),
            actual = icons
        )
    }

    @Test
    fun anAuroraEstimateShowsTheAuroraIconAndItsChance() {
        val latestWarning: LatestWarning? = listOf(
            warning(identifier = "vadret-aurora-u6sce-2026-11-07-HIGH").copy(
                aurora = Aurora(
                    chance = AuroraChance.HIGH,
                    score = 72,
                    peakAt = Instant.parse(input = "2026-11-07T21:00:00Z"),
                    geohash = Geohash(value = "u6sce"),
                    auroraNight = "2026-11-07"
                )
            )
        ).toLatestWarning(
            now = Instant.parse(input = "2026-11-07T18:00:00Z"),
            hourFormat = HourFormat.TWENTY_FOUR_HOUR,
            timeZone = TimeZone.of(zoneId = "Europe/Stockholm"),
            languageTag = "sv-SE"
        )

        assertEquals(
            expected = VadretIcons.Alert.Aurora to AuroraChance.HIGH,
            actual = latestWarning?.icon to latestWarning?.auroraChance
        )
    }
}