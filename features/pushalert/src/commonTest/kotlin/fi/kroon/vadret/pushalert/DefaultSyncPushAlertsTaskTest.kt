package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.location.model.Location
import fi.kroon.vadret.pushalert.model.DeviceRegistration
import fi.kroon.vadret.pushalert.model.DeviceToken
import fi.kroon.vadret.pushalert.model.InstallationToken
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.model.WarningAlertLevel
import fi.kroon.vadret.pushalert.usecase.DefaultGetCurrentGeohashTask
import fi.kroon.vadret.pushalert.usecase.DefaultSyncPushAlertsTask
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultSyncPushAlertsTaskTest {
    @Test
    fun nothingChosenOnANewPhoneAsksTheServerNothing() = runTest {
        val pushAlertRepository = FakePushAlertRepository()

        assertEquals(
            expected = Unit.asRight(),
            actual = syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()
        )
        assertEquals(
            expected = emptyList(),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun turningEverythingOffMakesTheServerForgetThePhone() = runTest {
        val pushAlertRepository = FakePushAlertRepository(installationToken = InstallationToken(value = "installation-1"))

        syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()

        assertEquals(
            expected = listOf("forget installation-1"),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun theFirstChoiceInstallsRegistersAndSubscribesForWhereThePhoneIs() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)

        val synced = syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()

        assertEquals(
            expected = Unit.asRight(),
            actual = synced
        )
        assertEquals(
            expected = listOf(
                "install",
                "register installation-1 fcm-1 replacing null key 65 at $NOW_MILLIS",
                "subscribe installation-1 u6sce YELLOW false"
            ),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun aChoiceWithoutNotificationPermissionNeverInstallsNorRegisters() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)

        val synced = syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            notificationPermissionSource = FakeNotificationPermissionSource(isNotificationAllowed = false)
        )()

        assertEquals(
            expected = Unit.asRight(),
            actual = synced
        )
        assertEquals(
            expected = emptyList(),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun withdrawingNotificationPermissionMakesTheServerForgetThePhone() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            installationToken = InstallationToken(value = "installation-1")
        )

        syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            notificationPermissionSource = FakeNotificationPermissionSource(isNotificationAllowed = false)
        )()

        assertEquals(
            expected = listOf("forget installation-1"),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun allowingNotificationsAgainInstallsOnTheNextSync() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)
        val notificationPermissionSource = FakeNotificationPermissionSource(isNotificationAllowed = false)
        val syncPushAlertsTask = syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            notificationPermissionSource = notificationPermissionSource
        )
        syncPushAlertsTask()

        notificationPermissionSource.isNotificationAllowed = true
        syncPushAlertsTask()

        assertEquals(
            expected = listOf(
                "install",
                "register installation-1 fcm-1 replacing null key 65 at $NOW_MILLIS",
                "subscribe installation-1 u6sce YELLOW false"
            ),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun theSavedPlaceIsUsedWhenTheAppDoesNotFollowTheGps() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)

        syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            locationRepository = FakeLocationRepository(isAutomatic = false)
        )()

        assertEquals(
            expected = "subscribe installation-1 u3chr YELLOW false",
            actual = pushAlertRepository.calls.last()
        )
    }

    @Test
    fun aTokenTheServerAlreadyHasIsNotSentAgain() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            installationToken = InstallationToken(value = "installation-1"),
            deviceRegistration = DeviceRegistration(
                deviceToken = DeviceToken(value = "fcm-1"),
                devicePublicKey = FAKE_PUBLIC_KEY,
                registeredAtMillis = NOW_MILLIS - DAY_MILLIS
            )
        )

        syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()

        assertEquals(
            expected = listOf("subscribe installation-1 u6sce YELLOW false"),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun aNewTokenReplacesTheOneTheServerHas() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            installationToken = InstallationToken(value = "installation-1"),
            deviceRegistration = DeviceRegistration(
                deviceToken = DeviceToken(value = "fcm-1"),
                devicePublicKey = FAKE_PUBLIC_KEY,
                registeredAtMillis = NOW_MILLIS - DAY_MILLIS
            )
        )

        syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            deviceTokenSource = FakeDeviceTokenSource(deviceToken = DeviceToken(value = "fcm-2").asRight())
        )()

        assertEquals(
            expected = "register installation-1 fcm-2 replacing fcm-1 key 65 at $NOW_MILLIS",
            actual = pushAlertRepository.calls.first()
        )
    }

    @Test
    fun aNewDeviceKeyIsSentEvenWhenTheTokenIsTheSame() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            installationToken = InstallationToken(value = "installation-1"),
            deviceRegistration = DeviceRegistration(
                deviceToken = DeviceToken(value = "fcm-1"),
                devicePublicKey = "an-old-key",
                registeredAtMillis = NOW_MILLIS - DAY_MILLIS
            )
        )

        syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()

        assertEquals(
            expected = "register installation-1 fcm-1 replacing fcm-1 key 65 at $NOW_MILLIS",
            actual = pushAlertRepository.calls.first()
        )
    }

    @Test
    fun aPositionThatIsNoPlaceOnEarthSubscribesNothing() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)

        val synced = syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            locationRepository = FakeLocationRepository(deviceLocation = Location(latitude = Double.NaN, longitude = 18.0).asRight())
        )()

        assertEquals(
            expected = Failure(errorCode = ErrorCode.LOCATION_NOT_AVAILABLE).asLeft(),
            actual = synced
        )
        assertEquals(
            expected = false,
            actual = pushAlertRepository.calls.any { call -> call.startsWith(prefix = "subscribe") }
        )
    }

    @Test
    fun aRegistrationThirtyDaysOldIsRefreshedWithTheSameToken() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            installationToken = InstallationToken(value = "installation-1"),
            deviceRegistration = DeviceRegistration(
                deviceToken = DeviceToken(value = "fcm-1"),
                devicePublicKey = FAKE_PUBLIC_KEY,
                registeredAtMillis = NOW_MILLIS - THIRTY_DAYS_MILLIS
            )
        )

        syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()

        assertEquals(
            expected = "register installation-1 fcm-1 replacing fcm-1 key 65 at $NOW_MILLIS",
            actual = pushAlertRepository.calls.first()
        )
    }

    @Test
    fun aTokenTheServerDidNotTakeIsSentAgainAsItIsAndTheOldOneIsKeptUntilThen() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            installationToken = InstallationToken(value = "installation-1"),
            deviceRegistration = DeviceRegistration(
                deviceToken = DeviceToken(value = "fcm-1"),
                devicePublicKey = FAKE_PUBLIC_KEY,
                registeredAtMillis = NOW_MILLIS - DAY_MILLIS
            ),
            registrationFailures = mutableListOf(Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE))
        )
        val syncPushAlertsTask = syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            deviceTokenSource = FakeDeviceTokenSource(deviceToken = DeviceToken(value = "fcm-2").asRight())
        )

        val synced = listOf(syncPushAlertsTask(), syncPushAlertsTask())

        assertEquals(
            expected = listOf(Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE).asLeft(), Unit.asRight()),
            actual = synced
        )
        assertEquals(
            expected = listOf(
                "register installation-1 fcm-2 replacing fcm-1 key 65 at $NOW_MILLIS",
                "register installation-1 fcm-2 replacing fcm-1 key 65 at $NOW_MILLIS",
                "subscribe installation-1 u6sce YELLOW false"
            ),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun aSyncThatFailsIsKeptAsNotSentAndOneThatWorksClearsIt() = runTest {
        val deviceTokenSource = FakeDeviceTokenSource(deviceToken = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft())
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)
        val syncPushAlertsTask = syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            deviceTokenSource = deviceTokenSource
        )

        syncPushAlertsTask()
        val isPendingAfterFailure = pushAlertRepository.isSyncPending.value
        deviceTokenSource.deviceToken = DeviceToken(value = "fcm-1").asRight()
        syncPushAlertsTask()

        assertEquals(
            expected = true to false,
            actual = isPendingAfterFailure to pushAlertRepository.isSyncPending.value
        )
        assertEquals(
            expected = WARNINGS_FROM_YELLOW,
            actual = pushAlertRepository.savedChoices.value
        )
    }

    @Test
    fun turningEverythingOffClearsAPendingSync() = runTest {
        val pushAlertRepository = FakePushAlertRepository(installationToken = InstallationToken(value = "installation-1"))
        pushAlertRepository.isSyncPending.value = true

        syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()

        assertEquals(
            expected = false,
            actual = pushAlertRepository.isSyncPending.value
        )
    }

    @Test
    fun aPhoneTheServerCannotConfirmHasEverythingTurnedOffSoTheNextStartDoesNotAttestAgain() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)
        pushAlertRepository.installFailure = Failure(errorCode = ErrorCode.ATTESTATION_REFUSED)
        pushAlertRepository.isSyncPending.value = true
        val syncPushAlertsTask = syncPushAlertsTask(pushAlertRepository = pushAlertRepository)

        val refused = syncPushAlertsTask()
        val nextStart = syncPushAlertsTask()

        assertEquals(
            expected = Failure(errorCode = ErrorCode.ATTESTATION_REFUSED).asLeft() to Unit.asRight(),
            actual = refused to nextStart
        )
        assertEquals(
            expected = Triple(PushAlertChoices.NOTHING_CHOSEN, false, listOf("install")),
            actual = Triple(
                pushAlertRepository.savedChoices.value,
                pushAlertRepository.isSyncPending.value,
                pushAlertRepository.calls
            )
        )
    }

    @Test
    fun anAttestationThatFailsHereOrAServerThatCannotCheckItKeepsTheChoicesToTryAgain() = runTest {
        val results = listOf(ErrorCode.ATTESTATION_FAILED, ErrorCode.HTTP_SERVICE_UNAVAILABLE, ErrorCode.NETWORK_UNAVAILABLE).map { errorCode: ErrorCode ->
            val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)
            pushAlertRepository.installFailure = Failure(errorCode = errorCode)
            syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()
            pushAlertRepository.savedChoices.value to pushAlertRepository.isSyncPending.value
        }

        assertEquals(
            expected = List(size = 3) { WARNINGS_FROM_YELLOW to true },
            actual = results
        )
    }

    @Test
    fun anInstallationTheServerForgotIsInstalledAgainOnce() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            installationToken = InstallationToken(value = "installation-0"),
            deviceRegistration = DeviceRegistration(
                deviceToken = DeviceToken(value = "fcm-1"),
                devicePublicKey = FAKE_PUBLIC_KEY,
                registeredAtMillis = NOW_MILLIS - DAY_MILLIS
            ),
            subscriptionFailures = mutableListOf(UNAUTHORIZED)
        )

        val synced = syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()

        assertEquals(
            expected = Unit.asRight(),
            actual = synced
        )
        assertEquals(
            expected = listOf(
                "subscribe installation-0 u6sce YELLOW false",
                "clear",
                "install",
                "register installation-1 fcm-1 replacing null key 65 at $NOW_MILLIS",
                "subscribe installation-1 u6sce YELLOW false"
            ),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun aServerThatKeepsForgettingIsNotAskedForeverAndSaysSo() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            subscriptionFailures = mutableListOf(UNAUTHORIZED, UNAUTHORIZED, UNAUTHORIZED)
        )

        assertEquals(
            expected = UNAUTHORIZED.asLeft(),
            actual = syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()
        )
        assertEquals(
            expected = 2,
            actual = pushAlertRepository.calls.count { call -> call == "install" }
        )
    }

    @Test
    fun aStepThatFailsStopsTheRestAndSaysWhy() = runTest {
        val noToken = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)
        val noKey = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)
        val noPlace = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)
        val notGenuine = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)
        notGenuine.installFailure = Failure(errorCode = ErrorCode.ATTESTATION_FAILED)

        val results = listOf(
            syncPushAlertsTask(
                pushAlertRepository = noToken,
                deviceTokenSource = FakeDeviceTokenSource(deviceToken = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft())
            )(),
            syncPushAlertsTask(
                pushAlertRepository = noKey,
                sealedMessageKey = FakeSealedMessageKey(isUnavailable = true)
            )(),
            syncPushAlertsTask(
                pushAlertRepository = noPlace,
                locationRepository = FakeLocationRepository(deviceLocation = Failure(errorCode = ErrorCode.LOCATION_PERMISSION_MISSING).asLeft())
            )(),
            syncPushAlertsTask(pushAlertRepository = notGenuine)()
        )

        assertEquals(
            expected = listOf(
                Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft(),
                Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft(),
                Failure(errorCode = ErrorCode.LOCATION_PERMISSION_MISSING).asLeft(),
                Failure(errorCode = ErrorCode.ATTESTATION_FAILED).asLeft()
            ),
            actual = results
        )
        assertEquals(
            expected = listOf(false, false, false, false),
            actual = listOf(noToken, noKey, noPlace, notGenuine).map { fakePushAlertRepository -> fakePushAlertRepository.calls.any { call -> call.startsWith(prefix = "subscribe") } }
        )
    }

    @Test
    fun syncsThatStartTogetherInstallOnlyOnce() = runTest {
        val installGate = CompletableDeferred<Unit>()
        val pushAlertRepository = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            installGate = installGate
        )
        val syncPushAlertsTask = syncPushAlertsTask(pushAlertRepository = pushAlertRepository)

        val syncs = List(size = 3) { async { syncPushAlertsTask() } }
        runCurrent()
        installGate.complete(value = Unit)
        syncs.awaitAll()

        assertEquals(
            expected = 1,
            actual = pushAlertRepository.calls.count { call -> call == "install" }
        )
    }
}

private fun syncPushAlertsTask(
    pushAlertRepository: FakePushAlertRepository,
    deviceTokenSource: FakeDeviceTokenSource = FakeDeviceTokenSource(),
    notificationPermissionSource: FakeNotificationPermissionSource = FakeNotificationPermissionSource(),
    sealedMessageKey: FakeSealedMessageKey = FakeSealedMessageKey(),
    locationRepository: FakeLocationRepository = FakeLocationRepository()
): DefaultSyncPushAlertsTask = DefaultSyncPushAlertsTask(
    pushAlertRepository = pushAlertRepository,
    deviceTokenSource = deviceTokenSource,
    notificationPermissionSource = notificationPermissionSource,
    sealedMessageKey = sealedMessageKey,
    getCurrentGeohashTask = DefaultGetCurrentGeohashTask(locationRepository = locationRepository),
    clock = FIXED_CLOCK
)

private val FIXED_CLOCK: Clock = object : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(epochMilliseconds = NOW_MILLIS)
}

private val WARNINGS_FROM_YELLOW = PushAlertChoices(
    warningLowestLevel = WarningAlertLevel.YELLOW,
    pollenChoices = emptyList(),
    temperatureTrend = false
)

private val UNAUTHORIZED = Failure(errorCode = ErrorCode.HTTP_UNAUTHORIZED)

private const val NOW_MILLIS = 1_793_000_000_000L
private const val DAY_MILLIS = 86_400_000L
private const val THIRTY_DAYS_MILLIS = 2_592_000_000L
private val FAKE_PUBLIC_KEY = Base64.encode(source = ByteArray(size = 65) { 4 })