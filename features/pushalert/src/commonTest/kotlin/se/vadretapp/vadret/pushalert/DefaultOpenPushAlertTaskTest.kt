package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.PushAlertType
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
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
                    pushAlertType = null,
                    notificationId = "n-1",
                    day = null,
                    cell = null,
                    warningReference = null
                ).asRight(),
                PushAlertMessage(
                    title = "Björkpollen",
                    body = "Höga halter",
                    kind = PushAlertKind.POLLEN,
                    pushAlertType = null,
                    notificationId = null,
                    day = null,
                    cell = null,
                    warningReference = null
                ).asRight(),
                PushAlertMessage(
                    title = "Kallare",
                    body = "3° kallare i morgon",
                    kind = PushAlertKind.TEMPERATURE,
                    pushAlertType = null,
                    notificationId = "n-3",
                    day = null,
                    cell = null,
                    warningReference = null
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
            expected = List(size = 6) { Failure(errorCode = ErrorCode.PUSH_ALERT_NOT_CHOSEN).asLeft() },
            actual = listOf(
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(warningLowestLevel = null))(sealed = Base64.encode(source = SEALED_WARNING)),
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(pollenLowestLevel = null))(sealed = Base64.encode(source = SEALED_POLLEN)),
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(temperatureTrend = false))(sealed = Base64.encode(source = SEALED_TEMPERATURE)),
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(strongWind = false))(sealed = Base64.encode(source = SEALED_WIND)),
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(slipperiness = false))(sealed = Base64.encode(source = SEALED_SLIPPERINESS)),
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(aurora = false))(sealed = Base64.encode(source = SEALED_AURORA))
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
    fun everyTitleIsShownAsTheServerWroteIt() = runTest {
        val openPushAlert = openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN)

        assertEquals(
            expected = listOf(
                "Storm i dag - Malmö",
                "Varmare",
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
    fun everyAlertTypeFromTheServerIsKeptWithTheAlertAndAnUnknownOneIsLeftOut() = runTest {
        assertEquals(
            expected = listOf(
                PushAlertType.WARNING,
                PushAlertType.POLLEN,
                PushAlertType.WARMER,
                PushAlertType.COLDER,
                PushAlertType.HARD_WIND,
                PushAlertType.STORM,
                PushAlertType.HURRICANE,
                PushAlertType.SLIPPERINESS,
                PushAlertType.AURORA,
                null
            ).map { pushAlertType -> pushAlertType.asRight() },
            actual = ALERT_TYPES.map { alertType ->
                openPushAlert(
                    pushAlertChoices = EVERYTHING_CHOSEN,
                    sealedMessageKey = FakeSealedMessageKey(
                        openedBySealed = mapOf(
                            SEALED_WIND.toList() to """{"title":"Storm i morgon","body":"Upp till 26 m/s i medelvind.","data":{"kind":"wind","alert_type":"$alertType"}}""".encodeToByteArray()
                        )
                    )
                )(sealed = Base64.encode(source = SEALED_WIND)).map { pushAlertMessage -> pushAlertMessage.pushAlertType }
            }
        )
    }

    @Test
    fun aSlipperinessAlertRefersToItsStory() = runTest {
        val openPushAlert = openPushAlert(pushAlertChoices = PushAlertChoices.NOTHING_CHOSEN.copy(slipperiness = true))

        assertEquals(
            expected = PushAlertMessage(
                title = "Risk för halka i natt - Malmö",
                body = "Frost i natt runt kl. 01:00.",
                kind = PushAlertKind.SLIPPERINESS,
                pushAlertType = PushAlertType.SLIPPERINESS,
                notificationId = "n-7",
                day = "2026-11-08",
                cell = "u3chr",
                warningReference = WarningReference.Slipperiness(
                    geohash = Geohash(value = "u3chr"),
                    slipperinessDay = "2026-11-08"
                )
            ).asRight(),
            actual = openPushAlert(sealed = Base64.encode(source = SEALED_SLIPPERINESS))
        )
    }

    @Test
    fun anAuroraAlertRefersToItsStory() = runTest {
        val openPushAlert = openPushAlert(pushAlertChoices = PushAlertChoices.NOTHING_CHOSEN.copy(aurora = true))

        assertEquals(
            expected = PushAlertMessage(
                title = "Stor chans för norrsken i kväll - Malmö",
                body = "Norrsken kan förekomma mellan 20:00-01:00. Störst chans kl. 22:00.",
                kind = PushAlertKind.AURORA,
                pushAlertType = PushAlertType.AURORA,
                notificationId = "n-9",
                day = "2026-11-08",
                cell = "u3chr",
                warningReference = WarningReference.Aurora(
                    geohash = Geohash(value = "u3chr"),
                    auroraNight = "2026-11-08"
                )
            ).asRight(),
            actual = openPushAlert(sealed = Base64.encode(source = SEALED_AURORA))
        )
    }

    @Test
    fun aWarningAlertRefersToTheStoryOfItsWarningArea() = runTest {
        assertEquals(
            expected = WarningReference.SmhiWarningArea(warningAreaId = 12).asRight(),
            actual = openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN)(sealed = Base64.encode(source = SEALED_WARNING_WITH_AREA)).map { pushAlertMessage -> pushAlertMessage.warningReference }
        )
    }

    @Test
    fun aCellFromTheServerIsKeptWithTheAlert() = runTest {
        assertEquals(
            expected = PushAlertMessage(
                title = "Storm i dag - Malmö",
                body = "Upp till 25 m/s i medelvind.",
                kind = PushAlertKind.WIND,
                pushAlertType = null,
                notificationId = "n-5",
                day = null,
                cell = "u3ch",
                warningReference = null
            ).asRight(),
            actual = openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN)(sealed = Base64.encode(source = SEALED_WIND_IN_MALMO))
        )
    }

    @Test
    fun choicesThatCannotBeReadDropTheAlert() = runTest {
        val openPushAlert = DefaultOpenPushAlertTask(
            sealedMessageKey = FAKE_SEALED_MESSAGE_KEY,
            pushAlertRepository = FakePushAlertRepository(choicesFailure = Failure(errorCode = ErrorCode.STORAGE_FAILED))
        )

        assertEquals(
            expected = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft(),
            actual = openPushAlert(sealed = Base64.encode(source = SEALED_WARNING))
        )
    }
}

private fun openPushAlert(
    pushAlertChoices: PushAlertChoices,
    sealedMessageKey: FakeSealedMessageKey = FAKE_SEALED_MESSAGE_KEY
): DefaultOpenPushAlertTask =
    DefaultOpenPushAlertTask(
        sealedMessageKey = sealedMessageKey,
        pushAlertRepository = FakePushAlertRepository(choices = pushAlertChoices)
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
private val SEALED_SLIPPERINESS = byteArrayOf(
    32,
    33
)
private val SEALED_WARNING_WITH_AREA = byteArrayOf(
    34,
    35
)
private val SEALED_AURORA = byteArrayOf(
    36,
    37
)

private val FAKE_SEALED_MESSAGE_KEY = FakeSealedMessageKey(
    openedBySealed = mapOf(
        SEALED_WARNING.toList() to """{"title":"Gul varning: Stockholms län","body":"Kuling","data":{"kind":"warning","notification_id":"n-1"}}""".encodeToByteArray(),
        SEALED_POLLEN.toList() to """{"title":"Björkpollen","body":"Höga halter","data":{"kind":"pollen"}}""".encodeToByteArray(),
        SEALED_TEMPERATURE.toList() to """{"title":"Kallare","body":"3° kallare i morgon","data":{"kind":"temperature","notification_id":"n-3"}}""".encodeToByteArray(),
        SEALED_WIND.toList() to """{"title":"Hård vind i morgon","body":"Upp till 16 m/s i medelvind runt kl. 14:00, 24 m/s i byarna.","data":{"kind":"wind","notification_id":"n-4","day":"2026-11-02"}}""".encodeToByteArray(),
        SEALED_WIND_IN_MALMO.toList() to """{"title":"Storm i dag - Malmö","body":"Upp till 25 m/s i medelvind.","data":{"kind":"wind","notification_id":"n-5","cell":"u3ch"}}""".encodeToByteArray(),
        SEALED_TEMPERATURE_IN_MALMO.toList() to """{"title":"Varmare","body":"3° varmare i morgon","data":{"kind":"temperature","notification_id":"n-6","cell":"u3ch"}}""".encodeToByteArray(),
        SEALED_WARNING_WITH_CELL.toList() to """{"title":"Gul varning: Skåne län","body":"Kuling","data":{"kind":"warning","cell":"u3ch"}}""".encodeToByteArray(),
        SEALED_POLLEN_WITH_CELL.toList() to """{"title":"Björkpollen i Malmö","body":"Höga halter","data":{"kind":"pollen","cell":"u3ch"}}""".encodeToByteArray(),
        SEALED_SLIPPERINESS.toList() to """{"title":"Risk för halka i natt - Malmö","body":"Frost i natt runt kl. 01:00.","data":{"kind":"slipperiness","alert_type":"slipperiness","notification_id":"n-7","cell":"u3chr","day":"2026-11-08","slipperiness_level":"POSSIBLE"}}""".encodeToByteArray(),
        SEALED_AURORA.toList() to """{"title":"Stor chans för norrsken i kväll - Malmö","body":"Norrsken kan förekomma mellan 20:00-01:00. Störst chans kl. 22:00.","data":{"kind":"aurora","alert_type":"aurora","notification_id":"n-9","cell":"u3chr","day":"2026-11-08","aurora_level":"POSSIBLE"}}""".encodeToByteArray(),
        SEALED_WARNING_WITH_AREA.toList() to """{"title":"Gul varning: Skåne län","body":"Kuling","data":{"kind":"warning","alert_type":"warning","notification_id":"n-8","warning_area_id":"12"}}""".encodeToByteArray(),
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
    pollenLowestLevel = PollenAlertLevel.HIGH,
    temperatureTrend = true,
    strongWind = true,
    slipperiness = true,
    aurora = true,
    dailyForecast = null,
    isChosenPlaceNotified = true,
    notifiedFavoriteIds = emptySet()
)

private val ALERT_TYPES = listOf("warning", "pollen", "warmer", "colder", "hard_wind", "storm", "hurricane", "slipperiness", "aurora", "earthquake")

private val WIND_MESSAGE = PushAlertMessage(
    title = "Hård vind i morgon",
    body = "Upp till 16 m/s i medelvind runt kl. 14:00, 24 m/s i byarna.",
    kind = PushAlertKind.WIND,
    pushAlertType = null,
    notificationId = "n-4",
    day = "2026-11-02",
    cell = null,
    warningReference = null
)