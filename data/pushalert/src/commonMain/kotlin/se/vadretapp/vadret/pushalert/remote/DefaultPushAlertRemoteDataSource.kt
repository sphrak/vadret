package se.vadretapp.vadret.pushalert.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException
import se.vadretapp.vadret.common.defaultJson
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.pushalert.error.AttestationRefusedException
import se.vadretapp.vadret.pushalert.error.UnsupportedAreaException
import se.vadretapp.vadret.pushalert.model.ChallengeDto
import se.vadretapp.vadret.pushalert.model.DeviceRequestDto
import se.vadretapp.vadret.pushalert.model.InstallationDto
import se.vadretapp.vadret.pushalert.model.InstallationRequestDto
import se.vadretapp.vadret.pushalert.model.SubscriptionDto
import se.vadretapp.vadret.pushalert.model.SubscriptionErrorDto

internal class DefaultPushAlertRemoteDataSource(
    private val httpClient: Lazy<HttpClient>
) : PushAlertRemoteDataSource {
    override suspend fun createChallenge(): ChallengeDto =
        httpClient.value.post(urlString = "https://api.vadretapp.se/api/v1/installations/challenges") {
            expectSuccess = true
        }.body()

    override suspend fun install(installationRequestDto: InstallationRequestDto): InstallationDto =
        try {
            httpClient.value.post(urlString = "https://api.vadretapp.se/api/v1/installations") {
                expectSuccess = true
                contentType(type = ContentType.Application.Json)
                setBody(body = installationRequestDto)
            }.body()
        } catch (exception: ClientRequestException) {
            if (exception.response.status == HttpStatusCode.Forbidden) {
                throw AttestationRefusedException(message = "The server could not confirm this phone from its attestation")
            }
            throw exception
        }

    override suspend fun registerDevice(
        installationToken: String,
        deviceRequestDto: DeviceRequestDto
    ) {
        httpClient.value.post(urlString = "https://api.vadretapp.se/api/v1/notification/devices") {
            expectSuccess = true
            bearerAuth(token = installationToken)
            contentType(type = ContentType.Application.Json)
            setBody(body = deviceRequestDto)
        }
    }

    override suspend fun replaceSubscription(
        installationToken: String,
        subscriptionDto: SubscriptionDto
    ) {
        try {
            httpClient.value.put(urlString = "https://api.vadretapp.se/api/v1/subscription") {
                expectSuccess = true
                bearerAuth(token = installationToken)
                contentType(type = ContentType.Application.Json)
                setBody(body = subscriptionDto)
            }
        } catch (exception: ClientRequestException) {
            if (exception.response.status == HttpStatusCode.BadRequest && exception.response.errorCodeOrNull() == UNSUPPORTED_AREA) {
                throw UnsupportedAreaException(message = "The server sends push alerts for places in Sweden only")
            }
            throw exception
        }
    }

    override suspend fun deleteInstallation(installationToken: String) {
        httpClient.value.delete(urlString = "https://api.vadretapp.se/api/v1/installations/me") {
            expectSuccess = true
            bearerAuth(token = installationToken)
        }
    }
}

private suspend fun HttpResponse.errorCodeOrNull(): String? =
    try {
        defaultJson.decodeFromString(
            deserializer = SubscriptionErrorDto.serializer(),
            string = bodyAsText()
        ).code
    } catch (exception: SerializationException) {
        logger.warn { "The server refused the subscription with a body that cannot be read: ${exception.message}" }
        null
    }

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pushalert")

private const val UNSUPPORTED_AREA = "UNSUPPORTED_AREA"