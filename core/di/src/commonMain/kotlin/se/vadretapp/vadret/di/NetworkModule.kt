package se.vadretapp.vadret.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.Module
import org.koin.dsl.module
import se.vadretapp.vadret.common.defaultJson
import se.vadretapp.vadret.di.model.AppInfo

val networkModule: Module = module {
    single<HttpClient> {
        httpClient(
            httpClientEngine = get(),
            appInfo = get()
        )
    }
}

private fun httpClient(
    httpClientEngine: HttpClientEngine,
    appInfo: AppInfo
): HttpClient =
    HttpClient(engine = httpClientEngine) {
        install(plugin = UserAgent) {
            agent = "${appInfo.applicationId}/${appInfo.versionName}"
        }
        install(plugin = ContentNegotiation) {
            json(json = defaultJson)
        }
    }