package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.model.AndroidAttestation
import se.vadretapp.vadret.pushalert.model.Attestation
import se.vadretapp.vadret.pushalert.model.DeviceRegistration
import se.vadretapp.vadret.pushalert.model.DeviceToken
import se.vadretapp.vadret.pushalert.model.InstallationToken
import se.vadretapp.vadret.pushalert.model.IosAttestation
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertStateEntity
import se.vadretapp.vadret.pushalert.model.PushAlertSubscription
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.pushalert.remote.DefaultPushAlertRemoteDataSource
import se.vadretapp.vadret.testing.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultPushAlertRepositoryTest {
    private val requests = mutableListOf<String>()

    @Test
    fun installingSendsTheAttestedChallengeAndKeepsTheToken() = runTest {
        val attestationLocalDataSource = FakeAttestationLocalDataSource(attestation = AndroidAttestation(certificateChain = listOf(byteArrayOf(1, 2, 3))))
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = PushAlertStateEntity(registeredDeviceToken = "fcm-old"))

        val installed = repository(
            pushAlertLocalDataSource = pushAlertLocalDataSource,
            attestationLocalDataSource = attestationLocalDataSource
        ).install()

        assertEquals(
            expected = InstallationToken(value = INSTALLATION_TOKEN).asRight(),
            actual = installed
        )
        assertEquals(
            expected = listOf(CHALLENGE_BYTES),
            actual = attestationLocalDataSource.challenges
        )
        assertEquals(
            expected = listOf(
                "POST /api/v1/installations/challenges",
                """POST /api/v1/installations {"challenge":"$CHALLENGE","android":{"certificate_chain":["AQID"]}}"""
            ),
            actual = requests
        )
        assertEquals(
            expected = PushAlertStateEntity(installationToken = INSTALLATION_TOKEN),
            actual = pushAlertLocalDataSource.state.value
        )
    }

    @Test
    fun aServerAnswerWithoutAChallengeOrATokenInstallsNothing() = runTest {
        val noChallenge = FakePushAlertLocalDataSource()
        val noToken = FakePushAlertLocalDataSource()

        val installed = listOf(
            repository(
                pushAlertLocalDataSource = noChallenge,
                responses = mapOf("POST /api/v1/installations/challenges" to (HttpStatusCode.Created to "{}"))
            ).install(),
            repository(
                pushAlertLocalDataSource = noToken,
                responses = mapOf("POST /api/v1/installations" to (HttpStatusCode.Created to "{}"))
            ).install()
        )

        assertEquals(
            expected = List(size = 2) { Failure(errorCode = ErrorCode.UNKNOWN).asLeft() },
            actual = installed
        )
        assertEquals(
            expected = listOf(PushAlertStateEntity(), PushAlertStateEntity()),
            actual = listOf(noChallenge.state.value, noToken.state.value)
        )
    }

    @Test
    fun anIosAttestationSendsItsKeyAndObject() = runTest {
        repository(attestation = IosAttestation(keyId = "key-1", attestationObject = byteArrayOf(4, 5, 6))).install()

        assertEquals(
            expected = """POST /api/v1/installations {"challenge":"$CHALLENGE","ios":{"key_id":"key-1","attestation":"BAUG"}}""",
            actual = requests.last()
        )
    }

    @Test
    fun aDeviceThatCannotAttestInstallsNothing() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource()

        val installed = repository(
            pushAlertLocalDataSource = pushAlertLocalDataSource,
            attestation = null
        ).install()

        assertEquals(
            expected = Failure(errorCode = ErrorCode.ATTESTATION_FAILED).asLeft(),
            actual = installed
        )
        assertEquals(
            expected = listOf("POST /api/v1/installations/challenges"),
            actual = requests
        )
        assertEquals(
            expected = PushAlertStateEntity(),
            actual = pushAlertLocalDataSource.state.value
        )
    }

    @Test
    fun anAttestationTheServerRefusesIsRefusedAndInstallsNothing() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource()

        val installed = repository(
            pushAlertLocalDataSource = pushAlertLocalDataSource,
            responses = mapOf("POST /api/v1/installations" to (HttpStatusCode.Forbidden to """{"code":"ATTESTATION_INVALID"}"""))
        ).install()

        assertEquals(
            expected = Failure(errorCode = ErrorCode.ATTESTATION_REFUSED).asLeft(),
            actual = installed
        )
        assertEquals(
            expected = PushAlertStateEntity(),
            actual = pushAlertLocalDataSource.state.value
        )
    }

    @Test
    fun onlyTheInstallationRefusingTheAttestationMeansThePhoneIsRefused() = runTest {
        val installed = listOf(
            repository(
                responses = mapOf("POST /api/v1/installations" to (HttpStatusCode.ServiceUnavailable to """{"code":"ATTESTATION_UNAVAILABLE"}"""))
            ).install(),
            repository(
                responses = mapOf("POST /api/v1/installations/challenges" to (HttpStatusCode.Forbidden to "{}"))
            ).install()
        )

        assertEquals(
            expected = listOf(
                Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE).asLeft(),
                Failure(errorCode = ErrorCode.HTTP_FORBIDDEN).asLeft()
            ),
            actual = installed
        )
    }

    @Test
    fun registeringADeviceNamesTheTokenItReplacesAndKeepsTheNewOne() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = PushAlertStateEntity(installationToken = INSTALLATION_TOKEN, registeredDeviceToken = "fcm-1"))

        val registered = repository(pushAlertLocalDataSource = pushAlertLocalDataSource).registerDevice(
            installationToken = InstallationToken(value = INSTALLATION_TOKEN),
            deviceToken = DeviceToken(value = "fcm-2"),
            replacesDeviceToken = DeviceToken(value = "fcm-1"),
            devicePublicKey = byteArrayOf(4, 1, 2),
            registeredAtMillis = REGISTERED_AT_MILLIS
        )

        assertEquals(
            expected = Unit.asRight(),
            actual = registered
        )
        assertEquals(
            expected = listOf("""POST /api/v1/notification/devices Bearer $INSTALLATION_TOKEN {"token":"fcm-2","public_key":"BAEC","replaces_token":"fcm-1"}"""),
            actual = requests
        )
        assertEquals(
            expected = Triple("fcm-2", "BAEC", REGISTERED_AT_MILLIS),
            actual = Triple(
                pushAlertLocalDataSource.state.value.registeredDeviceToken,
                pushAlertLocalDataSource.state.value.registeredDevicePublicKey,
                pushAlertLocalDataSource.state.value.registeredAtMillis
            )
        )
    }

    @Test
    fun aDeviceTheServerDoesNotTakeIsNotKeptSoItIsSentAgain() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = PushAlertStateEntity(installationToken = INSTALLATION_TOKEN, registeredDeviceToken = "fcm-1"))

        val registered = repository(
            pushAlertLocalDataSource = pushAlertLocalDataSource,
            responses = mapOf("POST /api/v1/notification/devices" to (HttpStatusCode.InternalServerError to "{}"))
        ).registerDevice(
            installationToken = InstallationToken(value = INSTALLATION_TOKEN),
            deviceToken = DeviceToken(value = "fcm-2"),
            replacesDeviceToken = DeviceToken(value = "fcm-1"),
            devicePublicKey = byteArrayOf(4, 1, 2),
            registeredAtMillis = REGISTERED_AT_MILLIS
        )

        assertEquals(
            expected = Failure(errorCode = ErrorCode.HTTP_INTERNAL_SERVER_ERROR).asLeft(),
            actual = registered
        )
        assertEquals(
            expected = "fcm-1",
            actual = pushAlertLocalDataSource.state.value.registeredDeviceToken
        )
    }

    @Test
    fun anInstallationTheServerForgotIsUnauthorized() = runTest {
        val replaced = repository(
            responses = mapOf("PUT /api/v1/subscription" to (HttpStatusCode.Unauthorized to """{"code":"UNAUTHORIZED"}"""))
        ).replaceSubscription(
            installationToken = InstallationToken(value = INSTALLATION_TOKEN),
            pushAlertSubscription = null
        )

        assertEquals(
            expected = Failure(errorCode = ErrorCode.HTTP_UNAUTHORIZED).asLeft(),
            actual = replaced
        )
    }

    @Test
    fun aPlaceOutsideSwedenIsAnUnsupportedAreaAndAnyOtherRefusalABadRequest() = runTest {
        val replaced = listOf(
            """{"code":"UNSUPPORTED_AREA"}""",
            """{"code":"SUBSCRIPTION_INVALID"}""",
            "<html>Bad Request</html>"
        ).map { body ->
            repository(
                responses = mapOf("PUT /api/v1/subscription" to (HttpStatusCode.BadRequest to body))
            ).replaceSubscription(
                installationToken = InstallationToken(value = INSTALLATION_TOKEN),
                pushAlertSubscription = null
            )
        }

        assertEquals(
            expected = listOf(
                Failure(errorCode = ErrorCode.UNSUPPORTED_AREA).asLeft(),
                Failure(errorCode = ErrorCode.HTTP_BAD_REQUEST).asLeft(),
                Failure(errorCode = ErrorCode.HTTP_BAD_REQUEST).asLeft()
            ),
            actual = replaced
        )
    }

    @Test
    fun aSubscriptionIsSentWithTheInstallationsTokenAndNoneTurnsEverythingOff() = runTest {
        val pushAlertRepository = repository()

        pushAlertRepository.replaceSubscription(
            installationToken = InstallationToken(value = INSTALLATION_TOKEN),
            pushAlertSubscription = PushAlertSubscription(
                geohashes = listOf(
                    Geohash(value = "u6sce"),
                    Geohash(value = "u3chr")
                ),
                warningLowestLevel = WarningAlertLevel.YELLOW,
                pollenChoices = listOf(PollenChoice(pollenTypeId = BIRCH, lowestLevel = PollenAlertLevel.HIGH)),
                temperatureTrend = true,
                strongWind = true
            )
        )
        pushAlertRepository.replaceSubscription(
            installationToken = InstallationToken(value = INSTALLATION_TOKEN),
            pushAlertSubscription = null
        )

        assertEquals(
            expected = listOf(
                """PUT /api/v1/subscription Bearer $INSTALLATION_TOKEN {"geohashes":["u6sce","u3chr"],"warning_lowest_level":"YELLOW","pollen":[{"pollen_type_id":"$BIRCH","lowest_level":5}],"temperature_trend":true,"strong_wind":true}""",
                "PUT /api/v1/subscription Bearer $INSTALLATION_TOKEN {}"
            ),
            actual = requests
        )
    }

    @Test
    fun aSubscriptionWithoutStrongWindSaysSoInsteadOfLeavingItOut() = runTest {
        repository().replaceSubscription(
            installationToken = InstallationToken(value = INSTALLATION_TOKEN),
            pushAlertSubscription = PushAlertSubscription(
                geohashes = listOf(Geohash(value = "u6sce")),
                warningLowestLevel = null,
                pollenChoices = emptyList(),
                temperatureTrend = false,
                strongWind = false
            )
        )

        assertEquals(
            expected = listOf("""PUT /api/v1/subscription Bearer $INSTALLATION_TOKEN {"geohashes":["u6sce"],"pollen":[],"temperature_trend":false,"strong_wind":false}"""),
            actual = requests
        )
    }

    @Test
    fun forgettingDeletesOnTheServerAndThenHere() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = PushAlertStateEntity(installationToken = INSTALLATION_TOKEN, registeredDeviceToken = "fcm-1"))

        repository(pushAlertLocalDataSource = pushAlertLocalDataSource).forgetInstallation(installationToken = InstallationToken(value = INSTALLATION_TOKEN))

        assertEquals(
            expected = listOf("DELETE /api/v1/installations/me Bearer $INSTALLATION_TOKEN"),
            actual = requests
        )
        assertEquals(
            expected = PushAlertStateEntity(),
            actual = pushAlertLocalDataSource.state.value
        )
    }

    @Test
    fun aServerThatCannotForgetLeavesTheInstallationHere() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = PushAlertStateEntity(installationToken = INSTALLATION_TOKEN))

        val forgotten = repository(
            pushAlertLocalDataSource = pushAlertLocalDataSource,
            responses = mapOf("DELETE /api/v1/installations/me" to (HttpStatusCode.ServiceUnavailable to "{}"))
        ).forgetInstallation(installationToken = InstallationToken(value = INSTALLATION_TOKEN))

        assertEquals(
            expected = Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE).asLeft(),
            actual = forgotten
        )
        assertEquals(
            expected = INSTALLATION_TOKEN,
            actual = pushAlertLocalDataSource.state.value.installationToken
        )
    }

    @Test
    fun storedTokensReadBackAndAStoreThatFailsSaysSo() = runTest {
        val stored = repository(pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = PushAlertStateEntity(installationToken = INSTALLATION_TOKEN, registeredDeviceToken = "fcm-1", registeredDevicePublicKey = "BAEC", registeredAtMillis = REGISTERED_AT_MILLIS)))
        val broken = repository(pushAlertLocalDataSource = FakePushAlertLocalDataSource(isBroken = true))

        assertEquals(
            expected = listOf(
                InstallationToken(value = INSTALLATION_TOKEN).asRight(),
                DeviceRegistration(deviceToken = DeviceToken(value = "fcm-1"), devicePublicKey = "BAEC", registeredAtMillis = REGISTERED_AT_MILLIS).asRight()
            ),
            actual = listOf(stored.installationToken(), stored.registeredDevice())
        )
        assertEquals(
            expected = List(size = 2) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
            actual = listOf(broken.installationToken(), broken.clearInstallation())
        )
    }

    @Test
    fun choicesAreKeptAndReadBackAndOutliveTheInstallation() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = PushAlertStateEntity(installationToken = INSTALLATION_TOKEN))
        val pushAlertRepository = repository(pushAlertLocalDataSource = pushAlertLocalDataSource)

        pushAlertRepository.saveChoices(pushAlertChoices = CHOICES)
        pushAlertRepository.forgetInstallation(installationToken = InstallationToken(value = INSTALLATION_TOKEN))
        pushAlertRepository.clearInstallation()

        assertEquals(
            expected = listOf(CHOICES.asRight(), CHOICES.asRight()),
            actual = listOf(pushAlertRepository.choices(), pushAlertRepository.observeChoices().first())
        )
        assertEquals(
            expected = null,
            actual = pushAlertLocalDataSource.state.value.installationToken
        )
    }

    @Test
    fun strongWindAndPlacesAreStoredWithTheOtherChoices() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource()

        repository(pushAlertLocalDataSource = pushAlertLocalDataSource).saveChoices(pushAlertChoices = CHOICES)

        assertEquals(
            expected = """{"choices":{"warning_lowest_level":"ORANGE","pollen_choices":[{"pollen_type_id":"$BIRCH","lowest_level":4}],"temperature_trend":true,"strong_wind":true,"favorite_ids":["$MALMO_FAVORITE_ID","$GOTEBORG_FAVORITE_ID"]}}""",
            actual = Json.encodeToString(
                serializer = PushAlertStateEntity.serializer(),
                value = pushAlertLocalDataSource.state.value
            )
        )
    }

    @Test
    fun choicesStoredBeforeStrongWindExistedReadAsStrongWindTurnedOff() = runTest {
        val storedBeforeStrongWind = Json.decodeFromString(
            deserializer = PushAlertStateEntity.serializer(),
            string = """{"installation_token":"$INSTALLATION_TOKEN","choices":{"warning_lowest_level":"ORANGE","pollen_choices":[{"pollen_type_id":"$BIRCH","lowest_level":4}],"temperature_trend":true}}"""
        )

        assertEquals(
            expected = CHOICES.copy(
                strongWind = false,
                favoriteIds = emptySet()
            ).asRight(),
            actual = repository(pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = storedBeforeStrongWind)).choices()
        )
    }

    @Test
    fun choicesStoredBeforePlacesExistedReadAsNoPlaceChosen() = runTest {
        val storedBeforePlaces = Json.decodeFromString(
            deserializer = PushAlertStateEntity.serializer(),
            string = """{"installation_token":"$INSTALLATION_TOKEN","choices":{"warning_lowest_level":"ORANGE","pollen_choices":[{"pollen_type_id":"$BIRCH","lowest_level":4}],"temperature_trend":true,"strong_wind":true}}"""
        )

        assertEquals(
            expected = CHOICES.copy(favoriteIds = emptySet()).asRight(),
            actual = repository(pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = storedBeforePlaces)).choices()
        )
    }

    @Test
    fun choicesThatCannotBeReadSaySo() = runTest {
        val broken = repository(pushAlertLocalDataSource = FakePushAlertLocalDataSource(isBroken = true))

        assertEquals(
            expected = List(size = 3) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
            actual = listOf(
                broken.observeChoices().first(),
                broken.observeIsSyncPending().first(),
                broken.saveIsSyncPending(isSyncPending = true)
            )
        )
    }

    @Test
    fun whereThePhoneWasAtTheLastSyncIsKeptAndReadBack() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = PushAlertStateEntity(installationToken = INSTALLATION_TOKEN))
        val pushAlertRepository = repository(pushAlertLocalDataSource = pushAlertLocalDataSource)

        val before = pushAlertRepository.syncedCurrentGeohash()
        val saved = pushAlertRepository.saveSyncedCurrentGeohash(geohash = Geohash(value = "u6sce"))

        assertEquals(
            expected = listOf(
                null.asRight(),
                Unit.asRight(),
                Geohash(value = "u6sce").asRight()
            ),
            actual = listOf(
                before,
                saved,
                pushAlertRepository.syncedCurrentGeohash()
            )
        )
        assertEquals(
            expected = """{"installation_token":"$INSTALLATION_TOKEN","synced_current_geohash":"u6sce"}""",
            actual = Json.encodeToString(
                serializer = PushAlertStateEntity.serializer(),
                value = pushAlertLocalDataSource.state.value
            )
        )
    }

    @Test
    fun installingForgettingOrClearingTheInstallationForgetsWhereThePhoneWasAtTheLastSync() = runTest {
        val synced = PushAlertStateEntity(
            installationToken = INSTALLATION_TOKEN,
            syncedCurrentGeohash = "u6sce"
        )
        val forgotten = FakePushAlertLocalDataSource(initialState = synced)
        val cleared = FakePushAlertLocalDataSource(initialState = synced)
        val installedAgain = FakePushAlertLocalDataSource(initialState = synced)

        repository(pushAlertLocalDataSource = forgotten).forgetInstallation(installationToken = InstallationToken(value = INSTALLATION_TOKEN))
        repository(pushAlertLocalDataSource = cleared).clearInstallation()
        repository(pushAlertLocalDataSource = installedAgain).install()

        assertEquals(
            expected = List(size = 3) { null.asRight() },
            actual = listOf(
                forgotten,
                cleared,
                installedAgain
            ).map { pushAlertLocalDataSource: FakePushAlertLocalDataSource -> repository(pushAlertLocalDataSource = pushAlertLocalDataSource).syncedCurrentGeohash() }
        )
    }

    @Test
    fun aServerThatCannotForgetLeavesWhereThePhoneWasAtTheLastSync() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource(
            initialState = PushAlertStateEntity(
                installationToken = INSTALLATION_TOKEN,
                syncedCurrentGeohash = "u6sce"
            )
        )

        repository(
            pushAlertLocalDataSource = pushAlertLocalDataSource,
            responses = mapOf("DELETE /api/v1/installations/me" to (HttpStatusCode.ServiceUnavailable to "{}"))
        ).forgetInstallation(installationToken = InstallationToken(value = INSTALLATION_TOKEN))

        assertEquals(
            expected = Geohash(value = "u6sce").asRight(),
            actual = repository(pushAlertLocalDataSource = pushAlertLocalDataSource).syncedCurrentGeohash()
        )
    }

    @Test
    fun stateStoredBeforeTheLastSyncWasKeptReadsAsNoPlace() = runTest {
        val storedBefore = Json.decodeFromString(
            deserializer = PushAlertStateEntity.serializer(),
            string = """{"installation_token":"$INSTALLATION_TOKEN","registered_device_token":"fcm-1","registered_device_public_key":"BAEC","registered_at_millis":$REGISTERED_AT_MILLIS}"""
        )

        assertEquals(
            expected = null.asRight(),
            actual = repository(pushAlertLocalDataSource = FakePushAlertLocalDataSource(initialState = storedBefore)).syncedCurrentGeohash()
        )
    }

    @Test
    fun whereThePhoneWasThatCannotBeReadOrWrittenSaysSo() = runTest {
        val broken = repository(pushAlertLocalDataSource = FakePushAlertLocalDataSource(isBroken = true))

        assertEquals(
            expected = List(size = 2) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
            actual = listOf(
                broken.syncedCurrentGeohash(),
                broken.saveSyncedCurrentGeohash(geohash = Geohash(value = "u6sce"))
            )
        )
    }

    @Test
    fun aPendingSyncIsKeptAndReadBackAndOutlivesTheInstallation() = runTest {
        val pushAlertLocalDataSource = FakePushAlertLocalDataSource()
        val pushAlertRepository = repository(pushAlertLocalDataSource = pushAlertLocalDataSource)

        val pendingBefore = pushAlertRepository.observeIsSyncPending().first()
        pushAlertRepository.saveIsSyncPending(isSyncPending = true)
        pushAlertRepository.install()
        pushAlertRepository.clearInstallation()
        val pendingAfter = pushAlertRepository.observeIsSyncPending().first()
        pushAlertRepository.saveIsSyncPending(isSyncPending = false)

        assertEquals(
            expected = listOf(false.asRight(), true.asRight(), false.asRight()),
            actual = listOf(pendingBefore, pendingAfter, pushAlertRepository.observeIsSyncPending().first())
        )
    }

    @Test
    fun aChangedSyncStatusDoesNotRepeatTheChoices() = runTest {
        val pushAlertRepository = repository(pushAlertLocalDataSource = FakePushAlertLocalDataSource())
        val observedChoices = mutableListOf<Either<Failure, PushAlertChoices>>()
        backgroundScope.launch(context = UnconfinedTestDispatcher(scheduler = testScheduler)) {
            pushAlertRepository.observeChoices().toList(destination = observedChoices)
        }

        pushAlertRepository.saveIsSyncPending(isSyncPending = true)
        pushAlertRepository.saveChoices(pushAlertChoices = CHOICES)

        assertEquals(
            expected = listOf<Either<Failure, PushAlertChoices>>(PushAlertChoices.NOTHING_CHOSEN.asRight(), CHOICES.asRight()),
            actual = observedChoices
        )
    }

    private fun repository(
        pushAlertLocalDataSource: FakePushAlertLocalDataSource = FakePushAlertLocalDataSource(),
        attestation: Attestation? = AndroidAttestation(certificateChain = listOf(byteArrayOf(1, 2, 3))),
        attestationLocalDataSource: FakeAttestationLocalDataSource = FakeAttestationLocalDataSource(attestation = attestation),
        responses: Map<String, Pair<HttpStatusCode, String>> = emptyMap()
    ): PushAlertRepository = DefaultPushAlertRepository(
        pushAlertRemoteDataSource = DefaultPushAlertRemoteDataSource(
            httpClient = lazy {
                mockHttpClient { request ->
                    val route = "${request.method.value} ${request.url.encodedPath}"
                    requests += request.describe(route = route)
                    val (status, body) = responses[route] ?: defaultResponses.getValue(key = route)
                    respondJson(status = status, body = body)
                }
            }
        ),
        pushAlertLocalDataSource = pushAlertLocalDataSource,
        attestationLocalDataSource = attestationLocalDataSource
    )
}

private fun HttpRequestData.describe(route: String): String {
    val authorization = headers[HttpHeaders.Authorization]
    val body = (body as? TextContent)?.text
    return listOfNotNull(route, authorization, body).joinToString(separator = " ")
}

private fun MockRequestHandleScope.respondJson(
    status: HttpStatusCode,
    body: String
): HttpResponseData = respond(
    content = body,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, "application/json")
)

private val defaultResponses = mapOf(
    "POST /api/v1/installations/challenges" to (HttpStatusCode.Created to """{"challenge":"AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="}"""),
    "POST /api/v1/installations" to (HttpStatusCode.Created to """{"installation_token":"vadret_secret"}"""),
    "POST /api/v1/notification/devices" to (HttpStatusCode.NoContent to ""),
    "PUT /api/v1/subscription" to (HttpStatusCode.NoContent to ""),
    "DELETE /api/v1/installations/me" to (HttpStatusCode.NoContent to "")
)

private val CHALLENGE_BYTES = List(size = 32) { index -> index.toByte() }
private const val CHALLENGE = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
private const val INSTALLATION_TOKEN = "vadret_secret"
private const val BIRCH = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323236"
private const val REGISTERED_AT_MILLIS = 1_793_000_000_000L
private const val MALMO_FAVORITE_ID = "favorite-malmo"
private const val GOTEBORG_FAVORITE_ID = "favorite-goteborg"
private val CHOICES = PushAlertChoices(
    warningLowestLevel = WarningAlertLevel.ORANGE,
    pollenChoices = listOf(PollenChoice(pollenTypeId = BIRCH, lowestLevel = PollenAlertLevel.MODERATE_TO_HIGH)),
    temperatureTrend = true,
    strongWind = true,
    favoriteIds = setOf(
        MALMO_FAVORITE_ID,
        GOTEBORG_FAVORITE_ID
    )
)