package se.vadretapp.vadret.main

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import platform.UIKit.UIViewController
import se.vadretapp.vadret.di.LocalKoin
import se.vadretapp.vadret.di.koinApplication
import se.vadretapp.vadret.main.navigation.VadretKey
import se.vadretapp.vadret.main.navigation.WarningStoryKey
import se.vadretapp.vadret.main.navigation.screenKind
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.applyLanguage
import kotlin.concurrent.atomics.AtomicBoolean

private const val NO_TITLE = ""

class NativeShell {

    private val mainViewModel: MainViewModel = koinApplication.koin.get()

    private val coroutineScope: CoroutineScope = MainScope()

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)

    fun start(
        onAppearanceChange: (NativeAppearance) -> Unit,
        onStoryOpen: (NativeRoute) -> Unit
    ) {
        if (!isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            return
        }
        mainViewModel.onEvent(event = MainEvent.OnStart)
        coroutineScope.launch {
            combine(
                mainViewModel.theme.filterNotNull(),
                mainViewModel.language.filterNotNull()
            ) { theme: Theme, language: AppLanguage ->
                nativeAppearance(
                    theme = theme,
                    language = language
                )
            }.collect { nativeAppearance: NativeAppearance -> onAppearanceChange(nativeAppearance) }
        }
        coroutineScope.launch {
            mainViewModel.openedWarningIdentifiers.collect { warningIdentifiers: List<String> ->
                val warningStoryKey = WarningStoryKey(warningIdentifiers = warningIdentifiers)
                onStoryOpen(
                    NativeRoute(
                        key = warningStoryKey,
                        title = NO_TITLE,
                        presentation = warningStoryKey.screenKind.toNativePresentation()
                    )
                )
            }
        }
    }

    fun tabViewController(
        tab: NativeTab,
        onNavigate: (NativeRoute) -> Unit,
        onSelectTab: (NativeTab) -> Unit
    ): UIViewController =
        composeViewController(
            key = tab.destination.key,
            onNavigate = onNavigate,
            onGoBack = {},
            onSelectTab = onSelectTab
        )

    fun screenViewController(
        route: NativeRoute,
        onNavigate: (NativeRoute) -> Unit,
        onGoBack: () -> Unit,
        onSelectTab: (NativeTab) -> Unit
    ): UIViewController =
        composeViewController(
            key = route.key,
            onNavigate = onNavigate,
            onGoBack = onGoBack,
            onSelectTab = onSelectTab
        )

    private fun composeViewController(
        key: VadretKey,
        onNavigate: (NativeRoute) -> Unit,
        onGoBack: () -> Unit,
        onSelectTab: (NativeTab) -> Unit
    ): UIViewController =
        ComposeUIViewController {
            CompositionLocalProvider(LocalKoin provides koinApplication.koin) {
                AppEnvironment(
                    mainViewModel = mainViewModel,
                    systemBarsEffect = { _: Theme -> }
                ) {
                    NativeScreen(
                        key = key,
                        onNavigate = onNavigate,
                        onGoBack = onGoBack,
                        onSelectTab = onSelectTab
                    )
                }
            }
        }

    private suspend fun nativeAppearance(
        theme: Theme,
        language: AppLanguage
    ): NativeAppearance {
        applyLanguage(languageTag = language.languageTag)
        val resourceEnvironment: ResourceEnvironment = getSystemResourceEnvironment()
        return NativeAppearance(
            interfaceStyle = theme.toInterfaceStyle(),
            tabTitles = NativeTab.entries.associateWith { tab: NativeTab ->
                getString(
                    environment = resourceEnvironment,
                    resource = tab.destination.labelResource
                )
            }
        )
    }
}