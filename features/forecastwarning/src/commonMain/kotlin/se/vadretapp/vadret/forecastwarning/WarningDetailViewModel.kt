package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastwarning.extension.toWarningItem
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.format.currentLanguageTag
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.no_alert_available
import se.vadretapp.vadret.warning.usecase.GetWarningsTask
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock

@Immutable
class WarningDetailViewModel internal constructor(
    private val getWarningsTask: GetWarningsTask,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<WarningDetailState>
        field = MutableStateFlow(value = WarningDetailState())

    private val languageTag: AtomicReference<String> = AtomicReference(value = currentLanguageTag())
    private val hourFormat: AtomicReference<HourFormat> = AtomicReference(value = HourFormat.TWENTY_FOUR_HOUR)
    private val warning: AtomicReference<AggregatedFeed?> = AtomicReference(value = null)
    private val startedWarningIdentifier: AtomicReference<String?> = AtomicReference(value = null)

    internal fun onEvent(event: WarningDetailEvent) {
        when (event) {
            is WarningDetailEvent.OnStart -> onStart(warningIdentifier = event.warningIdentifier)
            is WarningDetailEvent.OnLocaleChanged -> onLocaleChanged(
                languageTag = event.languageTag,
                hourFormat = event.hourFormat
            )
        }
    }

    private fun onStart(warningIdentifier: String) {
        if (startedWarningIdentifier.exchange(newValue = warningIdentifier) == warningIdentifier) {
            return
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            getWarningsTask()
                .onResult(
                    onError = { failure: Failure -> displayMessage(failure = failure) },
                    onSuccess = { aggregatedFeeds: List<AggregatedFeed> ->
                        val aggregatedFeed: AggregatedFeed? = aggregatedFeeds.firstOrNull { aggregatedFeed: AggregatedFeed -> aggregatedFeed.identifier == warningIdentifier }
                        warning.store(newValue = aggregatedFeed)
                        displayWarning()
                    }
                )
        }
    }

    private fun onLocaleChanged(
        languageTag: String,
        hourFormat: HourFormat
    ) {
        val isLanguageSame: Boolean = this.languageTag.exchange(newValue = languageTag) == languageTag
        val isHourFormatSame: Boolean = this.hourFormat.exchange(newValue = hourFormat) == hourFormat
        if (isLanguageSame && isHourFormatSame) {
            return
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            if (warning.load() != null) {
                displayWarning()
            }
        }
    }

    private fun displayWarning() {
        val aggregatedFeed: AggregatedFeed? = warning.load()
        state.update { state: WarningDetailState ->
            state.copy(
                warning = aggregatedFeed?.toWarningItem(
                    now = clock.now(),
                    timeZone = TimeZone.currentSystemDefault(),
                    languageTag = languageTag.load(),
                    hourFormat = hourFormat.load()
                ),
                isLoading = false,
                messageResource = if (aggregatedFeed == null) {
                    Res.string.no_alert_available
                } else {
                    null
                }
            )
        }
    }

    private fun displayMessage(failure: Failure) {
        state.update { state: WarningDetailState ->
            state.copy(
                isLoading = false,
                messageResource = failure.messageResource()
            )
        }
    }
}