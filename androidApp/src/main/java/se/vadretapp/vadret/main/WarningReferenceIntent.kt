package se.vadretapp.vadret.main

import android.content.Intent
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.geohash.model.geohashOrNull

internal fun Intent.putWarningReference(warningReference: WarningReference): Intent =
    when (warningReference) {
        is WarningReference.SmhiWarningArea -> putExtra(WARNING_REFERENCE_KIND_EXTRA, SMHI_WARNING_AREA_KIND)
            .putExtra(WARNING_AREA_ID_EXTRA, warningReference.warningAreaId)
        is WarningReference.Slipperiness -> putExtra(WARNING_REFERENCE_KIND_EXTRA, SLIPPERINESS_KIND)
            .putExtra(GEOHASH_EXTRA, warningReference.geohash.value)
            .putExtra(SLIPPERINESS_DAY_EXTRA, warningReference.slipperinessDay)
        is WarningReference.Aurora -> putExtra(WARNING_REFERENCE_KIND_EXTRA, AURORA_KIND)
            .putExtra(GEOHASH_EXTRA, warningReference.geohash.value)
            .putExtra(AURORA_NIGHT_EXTRA, warningReference.auroraNight)
    }

internal fun Intent.warningReferenceOrNull(): WarningReference? =
    when (getStringExtra(WARNING_REFERENCE_KIND_EXTRA)) {
        SMHI_WARNING_AREA_KIND -> smhiWarningAreaOrNull()
        SLIPPERINESS_KIND -> slipperinessOrNull()
        AURORA_KIND -> auroraOrNull()
        else -> null
    }

private fun Intent.smhiWarningAreaOrNull(): WarningReference? {
    if (!hasExtra(WARNING_AREA_ID_EXTRA)) {
        return null
    }
    return WarningReference.SmhiWarningArea(warningAreaId = getIntExtra(WARNING_AREA_ID_EXTRA, MISSING_WARNING_AREA_ID))
}

private fun Intent.slipperinessOrNull(): WarningReference? {
    val geohashValue: String = getStringExtra(GEOHASH_EXTRA) ?: return null
    val slipperinessDay: String = getStringExtra(SLIPPERINESS_DAY_EXTRA) ?: return null
    val geohash = geohashOrNull(value = geohashValue) ?: return null
    return WarningReference.Slipperiness(
        geohash = geohash,
        slipperinessDay = slipperinessDay
    )
}

private fun Intent.auroraOrNull(): WarningReference? {
    val geohashValue: String = getStringExtra(GEOHASH_EXTRA) ?: return null
    val auroraNight: String = getStringExtra(AURORA_NIGHT_EXTRA) ?: return null
    val geohash = geohashOrNull(value = geohashValue) ?: return null
    return WarningReference.Aurora(
        geohash = geohash,
        auroraNight = auroraNight
    )
}

private const val WARNING_REFERENCE_KIND_EXTRA = "warning_reference_kind"
private const val WARNING_AREA_ID_EXTRA = "warning_area_id"
private const val GEOHASH_EXTRA = "geohash"
private const val SLIPPERINESS_DAY_EXTRA = "slipperiness_day"
private const val AURORA_NIGHT_EXTRA = "aurora_night"
private const val SMHI_WARNING_AREA_KIND = "smhi_warning_area"
private const val SLIPPERINESS_KIND = "slipperiness"
private const val AURORA_KIND = "aurora"
private const val MISSING_WARNING_AREA_ID = -1