package se.vadretapp.vadret.aggregatedfeed

import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.geohash.model.Geohash
import kotlin.test.Test
import kotlin.test.assertEquals

class WarningReferenceTest {

    @Test
    fun `an smhi warning area refers to its area in any warning and to nothing else`() {
        val warningReference = WarningReference.SmhiWarningArea(warningAreaId = 12)

        assertEquals(
            expected = listOf(true, true, false, false, false),
            actual = listOf(
                "smhi-1-12",
                "smhi-7-12",
                "smhi-1-112",
                "smhi-12-1",
                "krisinformation-12"
            ).map { warningIdentifier: String -> warningReference.isReferenceTo(warningIdentifier = warningIdentifier) }
        )
    }

    @Test
    fun `slipperiness refers to its cell and day at every level and to nothing else`() {
        val warningReference = WarningReference.Slipperiness(
            geohash = Geohash(value = "u6sce"),
            slipperinessDay = "2026-11-08"
        )

        assertEquals(
            expected = listOf(true, true, false, false, false),
            actual = listOf(
                "vadret-slipperiness-u6sce-2026-11-08-POSSIBLE",
                "vadret-slipperiness-u6sce-2026-11-08-HIGH",
                "vadret-slipperiness-u6sce-2026-11-09-HIGH",
                "vadret-slipperiness-u6scf-2026-11-08-HIGH",
                "smhi-1-2"
            ).map { warningIdentifier: String -> warningReference.isReferenceTo(warningIdentifier = warningIdentifier) }
        )
    }

    @Test
    fun `aurora refers to its cell and night at every level and to nothing else`() {
        val warningReference = WarningReference.Aurora(
            geohash = Geohash(value = "u6sce"),
            auroraNight = "2026-11-08"
        )

        assertEquals(
            expected = listOf(true, true, false, false, false),
            actual = listOf(
                "vadret-aurora-u6sce-2026-11-08-POSSIBLE",
                "vadret-aurora-u6sce-2026-11-08-HIGH",
                "vadret-aurora-u6sce-2026-11-09-HIGH",
                "vadret-slipperiness-u6sce-2026-11-08-HIGH",
                "vadret-aurora-u6scf-2026-11-08-HIGH"
            ).map { warningIdentifier: String -> warningReference.isReferenceTo(warningIdentifier = warningIdentifier) }
        )
    }
}