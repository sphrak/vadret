package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.geohash.model.geohashOrNull

fun pushAlertWarningReferenceOrNull(pushAlertData: Map<String, String>): WarningReference? =
    when (pushAlertData[KIND_FIELD]) {
        WARNING_KIND -> smhiWarningAreaOrNull(warningAreaId = pushAlertData[WARNING_AREA_ID_FIELD])
        SLIPPERINESS_KIND -> slipperinessOrNull(
            cell = pushAlertData[CELL_FIELD],
            slipperinessDay = pushAlertData[DAY_FIELD]
        )
        AURORA_KIND -> auroraOrNull(
            cell = pushAlertData[CELL_FIELD],
            auroraNight = pushAlertData[DAY_FIELD]
        )
        else -> null
    }

private fun smhiWarningAreaOrNull(warningAreaId: String?): WarningReference? {
    val parsedWarningAreaId: Int = warningAreaId?.toIntOrNull() ?: return null
    return WarningReference.SmhiWarningArea(warningAreaId = parsedWarningAreaId)
}

private fun slipperinessOrNull(
    cell: String?,
    slipperinessDay: String?
): WarningReference? {
    if (cell == null || slipperinessDay == null) {
        return null
    }
    val geohash: Geohash = geohashOrNull(value = cell) ?: return null
    return WarningReference.Slipperiness(
        geohash = geohash,
        slipperinessDay = slipperinessDay
    )
}

private fun auroraOrNull(
    cell: String?,
    auroraNight: String?
): WarningReference? {
    if (cell == null || auroraNight == null) {
        return null
    }
    val geohash: Geohash = geohashOrNull(value = cell) ?: return null
    return WarningReference.Aurora(
        geohash = geohash,
        auroraNight = auroraNight
    )
}

private const val KIND_FIELD = "kind"
private const val WARNING_AREA_ID_FIELD = "warning_area_id"
private const val CELL_FIELD = "cell"
private const val DAY_FIELD = "day"
private const val WARNING_KIND = "warning"
private const val SLIPPERINESS_KIND = "slipperiness"
private const val AURORA_KIND = "aurora"