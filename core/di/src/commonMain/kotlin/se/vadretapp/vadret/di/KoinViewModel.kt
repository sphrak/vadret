package se.vadretapp.vadret.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.core.Koin
import org.koin.core.qualifier.Qualifier

val LocalKoin: ProvidableCompositionLocal<Koin> = staticCompositionLocalOf {
    error("No Koin instance provided, wrap the content in CompositionLocalProvider(LocalKoin provides koin)")
}

@Composable
inline fun <reified T : Any> koinAll(): List<T> {
    val koin: Koin = LocalKoin.current
    return remember(koin) { koin.getAll<T>() }
}

@Composable
inline fun <reified T : ViewModel> koinViewModel(): T {
    val koin: Koin = LocalKoin.current
    return viewModel { koin.get<T>() }
}

@Composable
inline fun <reified T : ViewModel> koinViewModel(qualifier: Qualifier): T {
    val koin: Koin = LocalKoin.current
    return viewModel { koin.get<T>(qualifier = qualifier) }
}