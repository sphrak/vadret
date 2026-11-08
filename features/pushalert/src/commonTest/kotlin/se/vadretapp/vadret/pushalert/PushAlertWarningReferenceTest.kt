package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.geohash.model.Geohash
import kotlin.test.Test
import kotlin.test.assertEquals

class PushAlertWarningReferenceTest {

    @Test
    fun aWarningSlipperinessOrAuroraAlertRefersToItsStory() {
        assertEquals(
            expected = listOf(
                WarningReference.SmhiWarningArea(warningAreaId = 12),
                WarningReference.Slipperiness(
                    geohash = Geohash(value = "u6sce"),
                    slipperinessDay = "2026-11-08"
                ),
                WarningReference.Aurora(
                    geohash = Geohash(value = "u6sce"),
                    auroraNight = "2026-11-08"
                )
            ),
            actual = listOf(
                mapOf(
                    "kind" to "warning",
                    "warning_area_id" to "12"
                ),
                mapOf(
                    "kind" to "slipperiness",
                    "cell" to "u6sce",
                    "day" to "2026-11-08"
                ),
                mapOf(
                    "kind" to "aurora",
                    "cell" to "u6sce",
                    "day" to "2026-11-08",
                    "aurora_level" to "POSSIBLE"
                )
            ).map { pushAlertData: Map<String, String> -> pushAlertWarningReferenceOrNull(pushAlertData = pushAlertData) }
        )
    }

    @Test
    fun anAlertWithoutAWholeReferenceOrOfAnotherKindRefersToNoStory() {
        assertEquals(
            expected = List(size = 8) { null },
            actual = listOf(
                mapOf("kind" to "warning"),
                mapOf(
                    "kind" to "warning",
                    "warning_area_id" to "twelve"
                ),
                mapOf(
                    "kind" to "slipperiness",
                    "cell" to "u6sc",
                    "day" to "2026-11-08"
                ),
                mapOf(
                    "kind" to "slipperiness",
                    "cell" to "u6sce"
                ),
                mapOf(
                    "kind" to "aurora",
                    "cell" to "u6sc",
                    "day" to "2026-11-08"
                ),
                mapOf(
                    "kind" to "aurora",
                    "day" to "2026-11-08"
                ),
                mapOf(
                    "kind" to "wind",
                    "cell" to "u6sc",
                    "day" to "2026-11-08"
                ),
                emptyMap()
            ).map { pushAlertData: Map<String, String> -> pushAlertWarningReferenceOrNull(pushAlertData = pushAlertData) }
        )
    }
}