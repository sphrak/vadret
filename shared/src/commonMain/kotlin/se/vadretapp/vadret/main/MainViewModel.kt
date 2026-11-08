package se.vadretapp.vadret.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.lifecycle.AppStartHook
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.warning.usecase.GetReferencedWarningIdentifiersTask
import kotlin.concurrent.atomics.AtomicBoolean

internal class MainViewModel(
    private val observeThemeTask: ObserveThemeTask,
    private val observeLanguageTask: ObserveLanguageTask,
    private val appStartHooks: List<AppStartHook>,
    private val openedWarningReferences: OpenedWarningReferences,
    private val getReferencedWarningIdentifiersTask: GetReferencedWarningIdentifiersTask,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    val theme: StateFlow<Theme?>
        field = MutableStateFlow<Theme?>(value = null)

    val language: StateFlow<AppLanguage?>
        field = MutableStateFlow<AppLanguage?>(value = null)

    private val openedStories: Channel<List<String>> = Channel(capacity = Channel.CONFLATED)

    val openedWarningIdentifiers: Flow<List<String>> = openedStories.receiveAsFlow()

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)

    fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.OnStart -> onStart()
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            appStartHooks.forEach { appStartHook: AppStartHook ->
                viewModelScope.launch(context = coroutineDispatcher) { appStartHook.onAppStart() }
            }
            viewModelScope.launch(context = coroutineDispatcher) {
                observeThemeTask().collect { result: Either<Failure, Theme> ->
                    theme.value = result.onResult(
                        onError = { _: Failure -> Theme.SYSTEM },
                        onSuccess = { observedTheme: Theme -> observedTheme }
                    )
                }
            }
            viewModelScope.launch(context = coroutineDispatcher) {
                openedWarningReferences.warningReferences.collect { warningReference: WarningReference -> openStory(warningReference = warningReference) }
            }
            viewModelScope.launch(context = coroutineDispatcher) {
                observeLanguageTask().collect { result: Either<Failure, AppLanguage> ->
                    language.value = result.onResult(
                        onError = { _: Failure -> AppLanguage.SYSTEM },
                        onSuccess = { observedLanguage: AppLanguage -> observedLanguage }
                    )
                }
            }
        }
    }

    private suspend fun openStory(warningReference: WarningReference) {
        getReferencedWarningIdentifiersTask(warningReference = warningReference).onResult(
            onError = { failure: Failure -> logger.warn { "The story of an opened notification cannot be found: ${failure.errorCode}" } },
            onSuccess = { warningIdentifiers: List<String> ->
                if (warningIdentifiers.isEmpty()) {
                    logger.info { "An opened notification refers to a warning that is gone, so only the app opens" }
                } else {
                    openedStories.trySend(element = warningIdentifiers)
                }
            }
        )
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.main")