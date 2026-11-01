package se.vadretapp.vadret.radar.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.day_forecast
import se.vadretapp.vadret.core.resources.now
import se.vadretapp.vadret.core.resources.source_and_time
import se.vadretapp.vadret.radar.RadarState
import se.vadretapp.vadret.radar.model.RadarFrame
import se.vadretapp.vadret.radar.model.RadarLayer
import se.vadretapp.vadret.radar.model.WindTimeFrame
import se.vadretapp.vadret.ui.elapsedTimeText

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