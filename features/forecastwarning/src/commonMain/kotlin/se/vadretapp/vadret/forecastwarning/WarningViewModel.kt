package se.vadretapp.vadret.forecastwarning

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastwarning.extension.toWarningItems
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.ui.extension.launchReplacing
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.format.currentLanguageTag
import se.vadretapp.vadret.warning.ObserveChosenPlaceWarningsTask
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock

@Immutable
class WarningViewModel internal constructor(
    private val observeChosenPlaceWarningsTask: ObserveChosenPlaceWarningsTask,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<WarningState>
        field = MutableStateFlow(value = WarningState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val languageTag: AtomicReference<String> = AtomicReference(value = currentLanguageTag())
    private val warnings: AtomicReference<List<AggregatedFeed>> = AtomicReference(value = emptyList())
    private val loadJob: AtomicReference<Job?> = AtomicReference(value = null)

    internal fun onEvent(event: WarningEvent) {
        when (event) {
            WarningEvent.OnStart -> onStart()
            WarningEvent.OnRefresh -> onRefresh()
            WarningEvent.OnErrorShown -> state.update { state: WarningState ->
                state.copy(errorMessageResource = null)
            }
            is WarningEvent.OnLocaleChanged -> onLocaleChanged(languageTag = event.languageTag)
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            load(isRefresh = false)
        }
    }

    private fun onRefresh() {
        state.update { state: WarningState ->
            state.copy(isRefreshing = true)
        }
        load(isRefresh = true)
    }

    private fun onLocaleChanged(languageTag: String) {
        if (this.languageTag.exchange(newValue = languageTag) == languageTag) {
            return
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            state.update { state: WarningState ->
                state.copy(warnings = warnings.load().toItems())
            }
        }
    }

    private fun load(isRefresh: Boolean) {
        viewModelScope.launchReplacing(
            currentJob = loadJob,
            context = coroutineDispatcher
        ) {
            observeChosenPlaceWarningsTask(isRefresh = isRefresh).collect { result: Either<Failure, List<AggregatedFeed>> ->
                result.onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = { aggregatedFeeds: List<AggregatedFeed> ->
                        warnings.store(newValue = aggregatedFeeds)
                        displayWarnings(warningItems = aggregatedFeeds.toItems())
                    }
                )
            }
        }
    }

    private fun List<AggregatedFeed>.toItems(): ImmutableList<WarningItem> =
        toWarningItems(
            now = clock.now(),
            timeZone = TimeZone.currentSystemDefault(),
            languageTag = languageTag.load()
        )

    private fun displayWarnings(warningItems: ImmutableList<WarningItem>) {
        state.update { state: WarningState ->
            state.copy(
                warnings = warningItems,
                isLoading = false,
                isRefreshing = false,
                isEmpty = warningItems.isEmpty()
            )
        }
    }

    private fun displayError(failure: Failure) {
        state.update { state: WarningState ->
            state.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessageResource = failure.messageResource()
            )
        }
    }
}