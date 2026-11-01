package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import se.vadretapp.vadret.weatherforecast.model.ComingDay

internal fun List<ComingDay>.withDayToggled(
    dayKey: String,
    isExpandOnceEnabled: Boolean
): ImmutableList<ComingDay> =
    map { comingDay: ComingDay ->
        when {
            comingDay.key == dayKey -> comingDay.copy(isExpanded = !comingDay.isExpanded)
            isExpandOnceEnabled -> comingDay.copy(isExpanded = false)
            else -> comingDay
        }
    }.toImmutableList()

internal fun List<ComingDay>.withExpansionOf(previousComingDays: List<ComingDay>): ImmutableList<ComingDay> {
    val expandedDayKeys: Set<String> = previousComingDays
        .filter { comingDay: ComingDay -> comingDay.isExpanded }
        .mapTo(destination = HashSet()) { comingDay: ComingDay -> comingDay.key }
    return map { comingDay: ComingDay ->
        comingDay.copy(isExpanded = comingDay.key in expandedDayKeys)
    }.toImmutableList()
}