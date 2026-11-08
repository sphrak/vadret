package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import se.vadretapp.vadret.pushalert.model.DeviceRegistration
import se.vadretapp.vadret.pushalert.model.DeviceToken
import se.vadretapp.vadret.pushalert.model.InstallationToken
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertSubscription
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.pushalert.usecase.DefaultGetCurrentGeohashTask
import se.vadretapp.vadret.pushalert.usecase.DefaultGetPushAlertGeohashesTask
import se.vadretapp.vadret.pushalert.usecase.DefaultSyncPushAlertsTask
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Language
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
                "subscribe installation-1 u6sce YELLOW false false"
            ),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun onlyStrongWindChosenInstallsRegistersAndSubscribesForIt() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = STRONG_WIND_ONLY)

        val synced = syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()

        assertEquals(
            expected = Unit.asRight(),
            actual = synced
        )
        assertEquals(
            expected = listOf(
                "install",
                "register installation-1 fcm-1 replacing null key 65 at $NOW_MILLIS",
                "subscribe installation-1 u6sce null false true"
            ),
            actual = pushAlertRepository.calls
        )
    }

    @Test
    fun theSubscriptionSaysWhetherStrongWindIsFollowed() = runTest {
        val subscribed = listOf(
            WARNINGS_FROM_YELLOW,
            WARNINGS_FROM_YELLOW.copy(strongWind = true)
        ).map { pushAlertChoices: PushAlertChoices ->
            val pushAlertRepository = FakePushAlertRepository(choices = pushAlertChoices)
            syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()
            pushAlertRepository.calls.last()
        }

        assertEquals(
            expected = listOf(
                "subscribe installation-1 u6sce YELLOW false false",
                "subscribe installation-1 u6sce YELLOW false true"
            ),
            actual = subscribed
        )
    }

    @Test
    fun theSubscriptionSaysWhetherSlipperinessIsFollowed() = runTest {
        val subscribed = listOf(
            WARNINGS_FROM_YELLOW,
            WARNINGS_FROM_YELLOW.copy(slipperiness = true),
            PushAlertChoices.NOTHING_CHOSEN.copy(slipperiness = true)
        ).map { pushAlertChoices: PushAlertChoices ->
            val pushAlertRepository = FakePushAlertRepository(choices = pushAlertChoices)
            syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()
            pushAlertRepository.subscriptions.last()?.slipperiness
        }

        assertEquals(
            expected = listOf(false, true, true),
            actual = subscribed
        )
    }

    @Test
    fun thePollenLevelIsAskedForEveryAllergenPickedForPollenAndForNoneWhenPollenIsTurnedOff() = runTest {
        val subscribed = listOf(
            WARNINGS_FROM_YELLOW to TWO_ALLERGENS,
            WARNINGS_FROM_YELLOW.copy(pollenLowestLevel = PollenAlertLevel.HIGH) to TWO_ALLERGENS,
            WARNINGS_FROM_YELLOW.copy(pollenLowestLevel = PollenAlertLevel.HIGH) to TWO_ALLERGENS.copy(isPollenEnabled = false),
            WARNINGS_FROM_YELLOW.copy(pollenLowestLevel = PollenAlertLevel.HIGH) to NO_ALLERGENS
        ).map { (pushAlertChoices: PushAlertChoices, pollenPreferences: PollenPreferences) ->
            val pushAlertRepository = FakePushAlertRepository(choices = pushAlertChoices)
            syncPushAlertsTask(
                pushAlertRepository = pushAlertRepository,
                pollenPreferenceRepository = FakePollenPreferenceRepository(pollenPreferences = pollenPreferences.asRight())
            )()
            pushAlertRepository.subscriptions.last()?.pollenChoices
        }

        assertEquals(
            expected = listOf(
                emptyList(),
                listOf(
                    PollenChoice(
                        pollenTypeId = BIRCH_ID,
                        lowestLevel = PollenAlertLevel.HIGH
                    ),
                    PollenChoice(
                        pollenTypeId = GRASS_ID,
                        lowestLevel = PollenAlertLevel.HIGH
                    )
                ),
                emptyList(),
                emptyList()
            ),
            actual = subscribed
        )
    }

    @Test
    fun pollenPreferencesThatCannotBeReadSubscribeNothingAndAreTriedAgainLater() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW.copy(pollenLowestLevel = PollenAlertLevel.HIGH))

        val synced = syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            pollenPreferenceRepository = FakePollenPreferenceRepository(pollenPreferences = STORAGE_FAILED.asLeft())
        )()

        assertEquals(
            expected = STORAGE_FAILED.asLeft(),
            actual = synced
        )
        assertEquals(
            expected = false to true,
            actual = pushAlertRepository.calls.any { call -> call.startsWith(prefix = "subscribe") } to pushAlertRepository.isSyncPending.value
        )
    }

    @Test
    fun theSubscriptionSaysTheClockAndTheLanguageTheAppShows() = runTest {
        val subscribed = listOf(
            HourFormat.TWENTY_FOUR_HOUR to Language.SWEDISH,
            HourFormat.TWELVE_HOUR to Language.POLISH
        ).map { (hourFormat: HourFormat, language: Language) ->
            val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)
            syncPushAlertsTask(
                pushAlertRepository = pushAlertRepository,
                getHourFormatTask = FakeGetHourFormatTask(result = hourFormat.asRight()),
                getShownLanguageTask = FakeGetShownLanguageTask(result = language.asRight())
            )()
            val subscription: PushAlertSubscription? = pushAlertRepository.subscriptions.last()
            subscription?.hourFormat to subscription?.language
        }

        assertEquals(
            expected = listOf<Pair<HourFormat?, Language?>>(
                HourFormat.TWENTY_FOUR_HOUR to Language.SWEDISH,
                HourFormat.TWELVE_HOUR to Language.POLISH
            ),
            actual = subscribed
        )
    }

    @Test
    fun aClockOrLanguageThatCannotBeReadSubscribesNothingAndIsTriedAgainLater() = runTest {
        val pushAlertRepositories = listOf(FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW), FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW))

        val synced = listOf(
            syncPushAlertsTask(
                pushAlertRepository = pushAlertRepositories[0],
                getHourFormatTask = FakeGetHourFormatTask(result = STORAGE_FAILED.asLeft())
            )(),
            syncPushAlertsTask(
                pushAlertRepository = pushAlertRepositories[1],
                getShownLanguageTask = FakeGetShownLanguageTask(result = STORAGE_FAILED.asLeft())
            )()
        )

        assertEquals(
            expected = List(size = 2) { STORAGE_FAILED.asLeft() },
            actual = synced
        )
        assertEquals(
            expected = List(size = 2) { false to true },
            actual = pushAlertRepositories.map { pushAlertRepository: FakePushAlertRepository ->
                pushAlertRepository.calls.any { call -> call.startsWith(prefix = "subscribe") } to pushAlertRepository.isSyncPending.value
            }
        )
    }

    @Test
    fun turningOffStrongWindWhenNothingElseIsChosenMakesTheServerForgetThePhone() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = STRONG_WIND_ONLY,
            installationToken = InstallationToken(value = "installation-1")
        )
        val syncPushAlertsTask = syncPushAlertsTask(pushAlertRepository = pushAlertRepository)
        syncPushAlertsTask()

        pushAlertRepository.saveChoices(pushAlertChoices = STRONG_WIND_ONLY.copy(strongWind = false))
        syncPushAlertsTask()

        assertEquals(
            expected = listOf(
                "register installation-1 fcm-1 replacing null key 65 at $NOW_MILLIS",
                "subscribe installation-1 u6sce null false true",
                "forget installation-1"
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
                "subscribe installation-1 u6sce YELLOW false false"
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
            expected = "subscribe installation-1 u3chr YELLOW false false",
            actual = pushAlertRepository.calls.last()
        )
    }

    @Test
    fun markedSavedPlacesInSwedenAreFollowedAfterWhereThePhoneIs() = runTest {
        val favorites = listOf(
            GOTEBORG_FAVORITE,
            UPPSALA_FAVORITE,
            BERLIN_FAVORITE,
            MALMO_FAVORITE
        )
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW.notifying(favorites = favorites))

        syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            favoriteRepository = FakeFavoriteRepository(favorites = favorites)
        )()

        assertEquals(
            expected = "subscribe installation-1 u6sce,u6282,u6ss9,u3chr YELLOW false false",
            actual = pushAlertRepository.calls.last()
        )
    }

    @Test
    fun everyAlertFollowsOnlyTheMarkedPlaces() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = STRONG_WIND_ONLY.copy(
                isChosenPlaceNotified = false,
                notifiedFavoriteIds = setOf(UPPSALA_FAVORITE.favoriteId)
            )
        )

        syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            favoriteRepository = FakeFavoriteRepository(
                favorites = listOf(
                    GOTEBORG_FAVORITE,
                    UPPSALA_FAVORITE,
                    MALMO_FAVORITE
                )
            )
        )()

        assertEquals(
            expected = "subscribe installation-1 u6ss9 null false true",
            actual = pushAlertRepository.calls.last()
        )
    }

    @Test
    fun noMarkedPlaceMakesTheServerForgetThePhone() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW.copy(isChosenPlaceNotified = false),
            installationToken = InstallationToken(value = "installation-1")
        )

        val synced = syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE))
        )()

        assertEquals(
            expected = Unit.asRight() to "forget installation-1",
            actual = synced to pushAlertRepository.calls.last()
        )
        assertEquals(
            expected = false,
            actual = pushAlertRepository.calls.any { call -> call.startsWith(prefix = "subscribe") }
        )
    }

    @Test
    fun placesSharingAGeohashWithEachOtherOrWithWhereThePhoneIsAreSentOnce() = runTest {
        val sharingPlaces = listOf(
            STOCKHOLM_NORRMALM_FAVORITE,
            MALMO_FAVORITE,
            MALMO_CENTRAL_FAVORITE,
            LIMHAMN_FAVORITE
        )
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW.notifying(favorites = sharingPlaces))

        syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            favoriteRepository = FakeFavoriteRepository(favorites = sharingPlaces)
        )()

        assertEquals(
            expected = "subscribe installation-1 u6sce,u3chr,u3chn YELLOW false false",
            actual = pushAlertRepository.calls.last()
        )
    }

    @Test
    fun noMoreThanFivePlacesAreSentAndWhereThePhoneIsIsAlwaysOne() = runTest {
        val manyPlaces = listOf(
            MALMO_FAVORITE,
            GOTEBORG_FAVORITE,
            UPPSALA_FAVORITE,
            UMEA_FAVORITE,
            KIRUNA_FAVORITE,
            VISBY_FAVORITE
        )
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW.notifying(favorites = manyPlaces))

        syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            favoriteRepository = FakeFavoriteRepository(favorites = manyPlaces)
        )()

        assertEquals(
            expected = "subscribe installation-1 u6sce,u3chr,u6282,u6ss9,u7q7j YELLOW false false",
            actual = pushAlertRepository.calls.last()
        )
    }

    @Test
    fun aSavedPlaceRemovedElsewhereInTheAppIsLeftOutOfTheNextSync() = runTest {
        val favorites = listOf(
            MALMO_FAVORITE,
            GOTEBORG_FAVORITE
        )
        val favoriteRepository = FakeFavoriteRepository(favorites = favorites)
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW.notifying(favorites = favorites))
        val syncPushAlertsTask = syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            favoriteRepository = favoriteRepository
        )

        syncPushAlertsTask()
        favoriteRepository.removeFavorite(favoriteId = MALMO_FAVORITE.favoriteId)
        syncPushAlertsTask()

        assertEquals(
            expected = listOf(
                "subscribe installation-1 u6sce,u3chr,u6282 YELLOW false false",
                "subscribe installation-1 u6sce,u6282 YELLOW false false"
            ),
            actual = pushAlertRepository.calls.filter { call -> call.startsWith(prefix = "subscribe") }
        )
    }

    @Test
    fun savedPlacesAloneInstallNothingAndMakeTheServerForgetThePhone() = runTest {
        val newPhone = FakePushAlertRepository(choices = PLACES_ONLY)
        val installedPhone = FakePushAlertRepository(
            choices = PLACES_ONLY,
            installationToken = InstallationToken(value = "installation-1")
        )

        val synced = listOf(
            newPhone,
            installedPhone
        ).map { pushAlertRepository: FakePushAlertRepository ->
            syncPushAlertsTask(
                pushAlertRepository = pushAlertRepository,
                favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE))
            )()
        }

        assertEquals(
            expected = List(size = 2) { Unit.asRight() },
            actual = synced
        )
        assertEquals(
            expected = listOf(
                emptyList<String>(),
                listOf("forget installation-1")
            ),
            actual = listOf(
                newPhone.calls,
                installedPhone.calls
            )
        )
    }

    @Test
    fun savedPlacesThatCannotBeReadSubscribeNothingAndAreTriedAgainLater() = runTest {
        val pushAlertRepository = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)

        val synced = syncPushAlertsTask(
            pushAlertRepository = pushAlertRepository,
            favoriteRepository = FakeFavoriteRepository(
                favorites = listOf(MALMO_FAVORITE),
                failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            )
        )()

        assertEquals(
            expected = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft(),
            actual = synced
        )
        assertEquals(
            expected = false to true,
            actual = pushAlertRepository.calls.any { call -> call.startsWith(prefix = "subscribe") } to pushAlertRepository.isSyncPending.value
        )
    }

    @Test
    fun aSyncTheServerTakesKeepsWhereThePhoneIsAsItWasSentButNotTheSavedPlaces() = runTest {
        val followingTheGps = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)
        val followingASavedPlace = FakePushAlertRepository(choices = WARNINGS_FROM_YELLOW)

        syncPushAlertsTask(
            pushAlertRepository = followingTheGps,
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE))
        )()
        syncPushAlertsTask(
            pushAlertRepository = followingASavedPlace,
            locationRepository = FakeLocationRepository(isAutomatic = false)
        )()

        assertEquals(
            expected = listOf(
                Geohash(value = "u6sce"),
                Geohash(value = "u3chr")
            ),
            actual = listOf(
                followingTheGps.syncedCurrentGeohash,
                followingASavedPlace.syncedCurrentGeohash
            )
        )
    }

    @Test
    fun aSyncThatFailsLeavesWhereThePhoneWasAtTheLastSyncThatWorked() = runTest {
        val refusedSubscription = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            installationToken = InstallationToken(value = "installation-1"),
            syncedCurrentGeohash = Geohash(value = "u3chr"),
            subscriptionFailures = mutableListOf(Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE))
        )
        val noPosition = FakePushAlertRepository(
            choices = WARNINGS_FROM_YELLOW,
            installationToken = InstallationToken(value = "installation-1"),
            syncedCurrentGeohash = Geohash(value = "u3chr")
        )

        val synced = listOf(
            syncPushAlertsTask(pushAlertRepository = refusedSubscription)(),
            syncPushAlertsTask(
                pushAlertRepository = noPosition,
                locationRepository = FakeLocationRepository(deviceLocation = Failure(errorCode = ErrorCode.LOCATION_PERMISSION_MISSING).asLeft())
            )()
        )

        assertEquals(
            expected = listOf(
                Failure(errorCode = ErrorCode.HTTP_SERVICE_UNAVAILABLE).asLeft(),
                Failure(errorCode = ErrorCode.LOCATION_PERMISSION_MISSING).asLeft()
            ),
            actual = synced
        )
        assertEquals(
            expected = List(size = 2) { Geohash(value = "u3chr") },
            actual = listOf(
                refusedSubscription.syncedCurrentGeohash,
                noPosition.syncedCurrentGeohash
            )
        )
    }

    @Test
    fun turningEverythingOffForgetsWhereThePhoneWasAtTheLastSync() = runTest {
        val pushAlertRepository = FakePushAlertRepository(
            installationToken = InstallationToken(value = "installation-1"),
            syncedCurrentGeohash = Geohash(value = "u6sce")
        )

        syncPushAlertsTask(pushAlertRepository = pushAlertRepository)()

        assertEquals(
            expected = listOf("forget installation-1"),
            actual = pushAlertRepository.calls
        )
        assertEquals(
            expected = null,
            actual = pushAlertRepository.syncedCurrentGeohash
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
            expected = listOf("subscribe installation-1 u6sce YELLOW false false"),
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
                "subscribe installation-1 u6sce YELLOW false false"
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
                "subscribe installation-0 u6sce YELLOW false false",
                "clear",
                "install",
                "register installation-1 fcm-1 replacing null key 65 at $NOW_MILLIS",
                "subscribe installation-1 u6sce YELLOW false false"
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
    locationRepository: FakeLocationRepository = FakeLocationRepository(),
    favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository(),
    pollenPreferenceRepository: FakePollenPreferenceRepository = FakePollenPreferenceRepository(),
    getHourFormatTask: FakeGetHourFormatTask = FakeGetHourFormatTask(),
    getShownLanguageTask: FakeGetShownLanguageTask = FakeGetShownLanguageTask()
): DefaultSyncPushAlertsTask = DefaultSyncPushAlertsTask(
    pushAlertRepository = pushAlertRepository,
    pollenPreferenceRepository = pollenPreferenceRepository,
    getHourFormatTask = getHourFormatTask,
    getShownLanguageTask = getShownLanguageTask,
    deviceTokenSource = deviceTokenSource,
    notificationPermissionSource = notificationPermissionSource,
    sealedMessageKey = sealedMessageKey,
    getCurrentGeohashTask = DefaultGetCurrentGeohashTask(getChosenPlaceTask = FakeGetChosenPlaceTask(locationRepository = locationRepository)),
    getPushAlertGeohashesTask = DefaultGetPushAlertGeohashesTask(
        favoriteRepository = favoriteRepository,
        locationRepository = locationRepository
    ),
    clock = FIXED_CLOCK
)

private val FIXED_CLOCK: Clock = object : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(epochMilliseconds = NOW_MILLIS)
}

private val WARNINGS_FROM_YELLOW = PushAlertChoices(
    warningLowestLevel = WarningAlertLevel.YELLOW,
    pollenLowestLevel = null,
    temperatureTrend = false,
    strongWind = false,
    slipperiness = false,
    isChosenPlaceNotified = true,
    notifiedFavoriteIds = emptySet()
)

private val STRONG_WIND_ONLY = PushAlertChoices.NOTHING_CHOSEN.copy(strongWind = true)

private val PLACES_ONLY = PushAlertChoices.NOTHING_CHOSEN.copy(notifiedFavoriteIds = setOf(MALMO_FAVORITE.favoriteId))

private fun PushAlertChoices.notifying(favorites: List<Favorite>): PushAlertChoices =
    copy(notifiedFavoriteIds = favorites.map { favorite: Favorite -> favorite.favoriteId }.toSet())

private val UNAUTHORIZED = Failure(errorCode = ErrorCode.HTTP_UNAUTHORIZED)

private val STORAGE_FAILED = Failure(errorCode = ErrorCode.STORAGE_FAILED)

private const val BIRCH_ID = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323236"

private const val GRASS_ID = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323237"

private val TWO_ALLERGENS = NO_ALLERGENS.copy(allergenPollenIds = setOf(GRASS_ID, BIRCH_ID))

private const val NOW_MILLIS = 1_793_000_000_000L
private const val DAY_MILLIS = 86_400_000L
private const val THIRTY_DAYS_MILLIS = 2_592_000_000L
private val FAKE_PUBLIC_KEY = Base64.encode(source = ByteArray(size = 65) { 4 })