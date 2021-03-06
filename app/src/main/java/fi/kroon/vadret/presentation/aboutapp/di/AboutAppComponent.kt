package fi.kroon.vadret.presentation.aboutapp.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import fi.kroon.vadret.presentation.aboutapp.AboutAppViewModel
import fi.kroon.vadret.presentation.aboutapp.about.di.AboutAppAboutScope
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@AboutAppAboutScope
@Component(
    modules = [
        AboutAppModule::class
    ]
)
interface AboutAppComponent {

    fun provideViewModel(): AboutAppViewModel

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            context: Context
        ): AboutAppComponent
    }
}