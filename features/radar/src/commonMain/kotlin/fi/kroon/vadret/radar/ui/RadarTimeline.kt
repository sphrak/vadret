package fi.kroon.vadret.radar.ui

import androidx.compose.runtime.Composable
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.day_forecast
import fi.kroon.vadret.core.resources.now
import fi.kroon.vadret.core.resources.source_and_time
import fi.kroon.vadret.radar.RadarState
import fi.kroon.vadret.radar.model.RadarFrame
import fi.kroon.vadret.radar.model.RadarLayer
import fi.kroon.vadret.radar.model.WindTimeFrame
import fi.kroon.vadret.ui.elapsedTimeText
import org.jetbrains.compose.resources.stringResource

internal data class RadarTimeline(
    val timeText: String,
    val caption: String,
    val startLabel: String,
    val middleLabel: String,
    val endLabel: String,
    val index: Int,
    val lastIndex: Int
)

@Composable
internal fun radarTimeline(state: RadarState): RadarTimeline? =
    when (state.layer) {
        RadarLayer.PRECIPITATION -> precipitationTimeline(
            frames = state.precipitationFrames,
            index = state.precipitationIndex
        )
        RadarLayer.WIND -> windTimeline(
            frames = state.windFrames,
            index = state.windIndex
        )
    }

@Composable
private fun precipitationTimeline(
    frames: List<RadarFrame>,
    index: Int
): RadarTimeline? {
    val frame: RadarFrame = frames.getOrNull(index = index) ?: return null
    return RadarTimeline(
        timeText = frame.timeText,
        caption = stringResource(
            resource = Res.string.source_and_time,
            frame.weekday,
            elapsedTimeText(elapsedTime = frame.elapsedTime)
        ).capitalized(),
        startLabel = frames.first().timeText,
        middleLabel = frames[frames.lastIndex / 2].timeText,
        endLabel = stringResource(resource = Res.string.now),
        index = index,
        lastIndex = frames.lastIndex
    )
}

@Composable
private fun windTimeline(
    frames: List<WindTimeFrame>,
    index: Int
): RadarTimeline? {
    val frame: WindTimeFrame = frames.getOrNull(index = index) ?: return null
    return RadarTimeline(
        timeText = frame.timeText,
        caption = stringResource(
            resource = Res.string.day_forecast,
            frame.weekday
        ).capitalized(),
        startLabel = stringResource(resource = Res.string.now),
        middleLabel = frames[frames.lastIndex / 2].timeText,
        endLabel = frames.last().timeText,
        index = index,
        lastIndex = frames.lastIndex
    )
}

private fun String.capitalized(): String = replaceFirstChar { character: Char -> character.titlecase() }