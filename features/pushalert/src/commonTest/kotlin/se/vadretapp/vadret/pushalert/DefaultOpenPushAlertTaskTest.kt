package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.pushalert.usecase.DefaultAddPlaceNameTask
import se.vadretapp.vadret.pushalert.usecase.DefaultGetCurrentGeohashTask
import se.vadretapp.vadret.pushalert.usecase.DefaultOpenPushAlertTask
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultOpenPushAlertTaskTest {
    @Test
    fun aSealedAlertOpensToWhatTheServerSent() = runTest {
        assertEquals(
            expected = listOf(
                PushAlertMessage(
                    title = "Gul varning: Stockholms län",
                    body = "Kuling",
                    kind = PushAlertKind.WARNING,
                    notificationId = "n-1",
                    cell = null
                ).asRight(),
                PushAlertMessage(
                    title = "Björkpollen",
                    body = "Höga halter",
                    kind = PushAlertKind.POLLEN,
                    notificationId = null,
                    cell = null
                ).asRight(),
                PushAlertMessage(
                    title = "Kallare",
                    body = "3° kallare i morgon",
                    kind = PushAlertKind.TEMPERATURE,
                    notificationId = "n-3",
                    cell = null
                ).asRight(),
                WIND_MESSAGE.asRight()
            ),
            actual = listOf(SEALED_WARNING, SEALED_POLLEN, SEALED_TEMPERATURE, SEALED_WIND).map { sealed ->
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN)(sealed = Base64.encode(source = sealed))
            }
        )
    }

    @Test
    fun anythingThatIsNotAWholeAlertOfAKnownKindForThisPhoneIsDropped() = runTest {
        assertEquals(
            expected = List(size = 8) { Failure(errorCode = ErrorCode.INVALID_RESPONSE).asLeft() },
            actual = listOf(
                "not base64 !",
                Base64.encode(source = byteArrayOf(9, 9, 9)),
                Base64.encode(source = SEALED_WITHOUT_TITLE),
                Base64.encode(source = SEALED_WITHOUT_BODY),
                Base64.encode(source = SEALED_WITHOUT_DATA),
                Base64.encode(source = SEALED_NONSENSE),
                Base64.encode(source = SEALED_WITHOUT_KIND),
                Base64.encode(source = SEALED_UNKNOWN_KIND)
            ).map { sealed -> openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN)(sealed = sealed) }
        )
    }

    @Test
    fun aKindThatIsTurnedOffHereIsDroppedEvenWhenTheServerStillSendsIt() = runTest {
        assertEquals(
            expected = List(size = 4) { Failure(errorCode = ErrorCode.PUSH_ALERT_NOT_CHOSEN).asLeft() },
            actual = listOf(
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(warningLowestLevel = null))(sealed = Base64.encode(source = SEALED_WARNING)),
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(pollenChoices = emptyList()))(sealed = Base64.encode(source = SEALED_POLLEN)),
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(temperatureTrend = false))(sealed = Base64.encode(source = SEALED_TEMPERATURE)),
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(strongWind = false))(sealed = Base64.encode(source = SEALED_WIND))
            )
        )
    }

    @Test
    fun aWindAlertIsShownWhenStrongWindIsTheOnlyAlertFollowed() = runTest {
        val openPushAlert = openPushAlert(pushAlertChoices = PushAlertChoices.NOTHING_CHOSEN.copy(strongWind = true))

        assertEquals(
            expected = listOf(
                WIND_MESSAGE.asRight(),
                Failure(errorCode = ErrorCode.PUSH_ALERT_NOT_CHOSEN).asLeft()
            ),
            actual = listOf(SEALED_WIND, SEALED_TEMPERATURE).map { sealed -> openPushAlert(sealed = Base64.encode(source = sealed)) }
        )
    }

    @Test
    fun aWindOrTemperatureAlertForAChosenSavedPlaceIsNamedAfterItAndOtherKindsAreNot() = runTest {
        val openPushAlert = openPushAlert(
            pushAlertChoices = EVERYTHING_CHOSEN.copy(favoriteIds = setOf(MALMO_FAVORITE.favoriteId)),
            favoriteRepository = FakeFavoriteRepository(favorites = listOf(MALMO_FAVORITE))
        )

        assertEquals(
            expected = listOf(
                "Storm i dag - Malmö",
                "Varmare - Malmö",
                "Gul varning: Skåne län",
                "Björkpollen i Malmö"
            ).map { title -> title.asRight() },
            actual = listOf(
                SEALED_WIND_IN_MALMO,
                SEALED_TEMPERATURE_IN_MALMO,
                SEALED_WARNING_WITH_CELL,
                SEALED_POLLEN_WITH_CELL
            ).map { sealed -> openPushAlert(sealed = Base64.encode(source = sealed)).map { pushAlertMessage -> pushAlertMessage.title } }
        )
    }

    @Test
    fun aCellFromTheServerIsKeptWithTheAlert() = runTest {
        assertEquals(
            expected = PushAlertMessage(
                title = "Storm i dag",
                body = "Upp till 25 m/s i medelvind.",
                kind = PushAlertKind.WIND,
                notificationId = "n-5",
                cell = "u3ch"
            ).asRight(),
            actual = openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN)(sealed = Base64.encode(source = SEALED_WIND_IN_MALMO))
        )
    }

    @Test
    fun choicesThatCannotBeReadDropTheAlert() = runTest {
        val openPushAlert = DefaultOpenPushAlertTask(
            sealedMessageKey = FAKE_SEALED_MESSAGE_KEY,
            pushAlertRepository = FakePushAlertRepository(choicesFailure = Failure(errorCode = ErrorCode.STORAGE_FAILED)),
            addPlaceNameTask = addPlaceNameTask(favoriteRepository = FakeFavoriteRepository())
        )

        assertEquals(
            expected = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft(),
            actual = openPushAlert(sealed = Base64.encode(source = SEALED_WARNING))
        )
    }
}

private fun openPushAlert(
    pushAlertChoices: PushAlertChoices,
    favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository()
): DefaultOpenPushAlertTask = DefaultOpenPushAlertTask(
    sealedMessageKey = FAKE_SEALED_MESSAGE_KEY,
    pushAlertRepository = FakePushAlertRepository(choices = pushAlertChoices),
    addPlaceNameTask = addPlaceNameTask(favoriteRepository = favoriteRepository)
)

private fun addPlaceNameTask(favoriteRepository: FakeFavoriteRepository): DefaultAddPlaceNameTask = DefaultAddPlaceNameTask(
    favoriteRepository = favoriteRepository,
    getCurrentGeohashTask = DefaultGetCurrentGeohashTask(locationRepository = FakeLocationRepository())
)

private val SEALED_WARNING = byteArrayOf(1, 2, 3)
private val SEALED_WITHOUT_BODY = byteArrayOf(4, 5, 6)
private val SEALED_NONSENSE = byteArrayOf(7, 8)
private val SEALED_POLLEN = byteArrayOf(10, 11)
private val SEALED_TEMPERATURE = byteArrayOf(12, 13)
private val SEALED_WITHOUT_KIND = byteArrayOf(14, 15)
private val SEALED_UNKNOWN_KIND = byteArrayOf(16, 17)
private val SEALED_WITHOUT_TITLE = byteArrayOf(18, 19)
private val SEALED_WITHOUT_DATA = byteArrayOf(20, 21)
private val SEALED_WIND = byteArrayOf(22, 23)
private val SEALED_WIND_IN_MALMO = byteArrayOf(
    24,
    25
)
private val SEALED_TEMPERATURE_IN_MALMO = byteArrayOf(
    26,
    27
)
private val SEALED_WARNING_WITH_CELL = byteArrayOf(
    28,
    29
)
private val SEALED_POLLEN_WITH_CELL = byteArrayOf(
    30,
    31
)

private val FAKE_SEALED_MESSAGE_KEY = FakeSealedMessageKey(
    openedBySealed = mapOf(
        SEALED_WARNING.toList() to """{"title":"Gul varning: Stockholms län","body":"Kuling","data":{"kind":"warning","notification_id":"n-1"}}""".encodeToByteArray(),
        SEALED_POLLEN.toList() to """{"title":"Björkpollen","body":"Höga halter","data":{"kind":"pollen"}}""".encodeToByteArray(),
        SEALED_TEMPERATURE.toList() to """{"title":"Kallare","body":"3° kallare i morgon","data":{"kind":"temperature","notification_id":"n-3"}}""".encodeToByteArray(),
        SEALED_WIND.toList() to """{"title":"Hård vind i morgon","body":"Upp till 16 m/s i medelvind, 24 m/s i byarna.","data":{"kind":"wind","notification_id":"n-4","day":"2026-11-02"}}""".encodeToByteArray(),
        SEALED_WIND_IN_MALMO.toList() to """{"title":"Storm i dag","body":"Upp till 25 m/s i medelvind.","data":{"kind":"wind","notification_id":"n-5","cell":"u3ch"}}""".encodeToByteArray(),
        SEALED_TEMPERATURE_IN_MALMO.toList() to """{"title":"Varmare","body":"3° varmare i morgon","data":{"kind":"temperature","notification_id":"n-6","cell":"u3ch"}}""".encodeToByteArray(),
        SEALED_WARNING_WITH_CELL.toList() to """{"title":"Gul varning: Skåne län","body":"Kuling","data":{"kind":"warning","cell":"u3ch"}}""".encodeToByteArray(),
        SEALED_POLLEN_WITH_CELL.toList() to """{"title":"Björkpollen i Malmö","body":"Höga halter","data":{"kind":"pollen","cell":"u3ch"}}""".encodeToByteArray(),
        SEALED_WITHOUT_BODY.toList() to """{"title":"Gul varning","data":{"kind":"warning"}}""".encodeToByteArray(),
        SEALED_NONSENSE.toList() to "not json".encodeToByteArray(),
        SEALED_WITHOUT_TITLE.toList() to """{"body":"Kuling","data":{"kind":"warning"}}""".encodeToByteArray(),
        SEALED_WITHOUT_DATA.toList() to """{"title":"Gul varning","body":"Kuling"}""".encodeToByteArray(),
        SEALED_WITHOUT_KIND.toList() to """{"title":"Gul varning","body":"Kuling","data":{}}""".encodeToByteArray(),
        SEALED_UNKNOWN_KIND.toList() to """{"title":"Gul varning","body":"Kuling","data":{"kind":"earthquake"}}""".encodeToByteArray()
    )
)

private val EVERYTHING_CHOSEN = PushAlertChoices(
    warningLowestLevel = WarningAlertLevel.YELLOW,
    pollenChoices = listOf(
        PollenChoice(
            pollenTypeId = "2a2a2a2a-2a2a-4a2a-aa2a-2a313a323236",
            lowestLevel = PollenAlertLevel.HIGH
        )
    ),
    temperatureTrend = true,
    strongWind = true,
    favoriteIds = emptySet()
)

private val WIND_MESSAGE = PushAlertMessage(
    title = "Hård vind i morgon",
    body = "Upp till 16 m/s i medelvind, 24 m/s i byarna.",
    kind = PushAlertKind.WIND,
    notificationId = "n-4",
    cell = null
)