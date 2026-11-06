package se.vadretapp.vadret.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.ObserveLocationChoicesTask
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.settings.extension.toPlacesSummary
import se.vadretapp.vadret.settings.usecase.GetSettingsTask
import se.vadretapp.vadret.settings.usecase.ObserveDeveloperModeTask
import se.vadretapp.vadret.settings.usecase.SetHourlyForecastTask
import se.vadretapp.vadret.settings.usecase.SetLanguageTask
import se.vadretapp.vadret.settings.usecase.SetThemeTask
import se.vadretapp.vadret.theme.model.Theme
import kotlin.concurrent.atomics.AtomicBoolean

class SettingsViewModel internal constructor(
    private val getSettingsTask: GetSettingsTask,
    private val setThemeTask: SetThemeTask,
    private val setLanguageTask: SetLanguageTask,
    private val setHourlyForecastTask: SetHourlyForecastTask,
    private val observeLocationChoicesTask: ObserveLocationChoicesTask,
    private val observeDeveloperModeTask: ObserveDeveloperModeTask,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<SettingsState>
        field = MutableStateFlow(value = SettingsState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)

    internal fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.OnStart -> onStart()
            is SettingsEvent.OnHourlyForecastToggled -> onHourlyForecastToggled(isHourlyForecastEnabled = event.isHourlyForecastEnabled)
            SettingsEvent.OnThemeRowClicked -> state.update { state: SettingsState -> state.copy(isThemeDialogVisible = true) }
            SettingsEvent.OnThemeDialogDismissed -> state.update { state: SettingsState -> state.copy(isThemeDialogVisible = false) }
            is SettingsEvent.OnThemeSelected -> onThemeSelected(theme = event.theme)
            SettingsEvent.OnLanguageRowClicked -> state.update { state: SettingsState -> state.copy(isLanguageDialogVisible = true) }
            SettingsEvent.OnLanguageDialogDismissed -> state.update { state: SettingsState -> state.copy(isLanguageDialogVisible = false) }
            is SettingsEvent.OnLanguageSelected -> onLanguageSelected(language = event.language)
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            viewModelScope.launch(context = coroutineDispatcher) { loadSettings() }
            viewModelScope.launch(context = coroutineDispatcher) { observeLocationChoices() }
            viewModelScope.launch(context = coroutineDispatcher) { observeDeveloperMode() }
        }
    }

    private suspend fun loadSettings() {
        getSettingsTask().onResult(
            onError = { _: Failure -> },
            onSuccess = { settings: Settings -> state.update { state: SettingsState -> state.copy(settings = settings) } }
        )
    }

    private suspend fun observeLocationChoices() {
        observeLocationChoicesTask().collect { result: Either<Failure, LocationChoices> ->
            result.onResult(
                onError = { _: Failure -> },
                onSuccess = { locationChoices: LocationChoices ->
                    state.update { state: SettingsState -> state.copy(placesSummary = locationChoices.toPlacesSummary()) }
                }
            )
        }
    }

    private suspend fun observeDeveloperMode() {
        observeDeveloperModeTask().collect { result: Either<Failure, Boolean> ->
            result.onResult(
                onError = { _: Failure -> },
                onSuccess = { isDeveloperMode: Boolean ->
                    state.update { state: SettingsState -> state.copy(isDeveloperRowVisible = isDeveloperMode) }
                }
            )
        }
    }

    private fun onHourlyForecastToggled(isHourlyForecastEnabled: Boolean) {
        viewModelScope.launch(context = coroutineDispatcher) {
            setHourlyForecastTask(isHourlyForecastEnabled = isHourlyForecastEnabled).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }

    private fun onThemeSelected(theme: Theme) {
        state.update { state: SettingsState -> state.copy(isThemeDialogVisible = false) }
        viewModelScope.launch(context = coroutineDispatcher) {
            setThemeTask(theme = theme).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }

    private fun onLanguageSelected(language: AppLanguage) {
        state.update { state: SettingsState -> state.copy(isLanguageDialogVisible = false) }
        viewModelScope.launch(context = coroutineDispatcher) {
            setLanguageTask(language = language).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }
}