package se.vadretapp.vadret.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.lifecycle.AppStartHook
import se.vadretapp.vadret.theme.model.Theme
import kotlin.concurrent.atomics.AtomicBoolean

internal class MainViewModel(
    private val observeThemeTask: ObserveThemeTask,
    private val observeLanguageTask: ObserveLanguageTask,
    private val appStartHooks: List<AppStartHook>,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    val theme: StateFlow<Theme?>
        field = MutableStateFlow<Theme?>(value = null)

    val language: StateFlow<AppLanguage?>
        field = MutableStateFlow<AppLanguage?>(value = null)

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
                observeLanguageTask().collect { result: Either<Failure, AppLanguage> ->
                    language.value = result.onResult(
                        onError = { _: Failure -> AppLanguage.SYSTEM },
                        onSuccess = { observedLanguage: AppLanguage -> observedLanguage }
                    )
                }
            }
        }
    }
}