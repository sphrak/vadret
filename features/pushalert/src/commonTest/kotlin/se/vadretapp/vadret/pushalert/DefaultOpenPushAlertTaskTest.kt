package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
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
                    notificationId = "n-1"
                ).asRight(),
                PushAlertMessage(
                    title = "Björkpollen",
                    body = "Höga halter",
                    kind = PushAlertKind.POLLEN,
                    notificationId = null
                ).asRight(),
                PushAlertMessage(
                    title = "Kallare",
                    body = "3° kallare i morgon",
                    kind = PushAlertKind.TEMPERATURE,
                    notificationId = "n-3"
                ).asRight()
            ),
            actual = listOf(SEALED_WARNING, SEALED_POLLEN, SEALED_TEMPERATURE).map { sealed ->
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
            expected = List(size = 3) { Failure(errorCode = ErrorCode.PUSH_ALERT_NOT_CHOSEN).asLeft() },
            actual = listOf(
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(warningLowestLevel = null))(sealed = Base64.encode(source = SEALED_WARNING)),
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(pollenChoices = emptyList()))(sealed = Base64.encode(source = SEALED_POLLEN)),
                openPushAlert(pushAlertChoices = EVERYTHING_CHOSEN.copy(temperatureTrend = false))(sealed = Base64.encode(source = SEALED_TEMPERATURE))
            )
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

private fun openPushAlert(pushAlertChoices: PushAlertChoices): DefaultOpenPushAlertTask = DefaultOpenPushAlertTask(
    sealedMessageKey = FAKE_SEALED_MESSAGE_KEY,
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

private val FAKE_SEALED_MESSAGE_KEY = FakeSealedMessageKey(
    openedBySealed = mapOf(
        SEALED_WARNING.toList() to """{"title":"Gul varning: Stockholms län","body":"Kuling","data":{"kind":"warning","notification_id":"n-1"}}""".encodeToByteArray(),
        SEALED_POLLEN.toList() to """{"title":"Björkpollen","body":"Höga halter","data":{"kind":"pollen"}}""".encodeToByteArray(),
        SEALED_TEMPERATURE.toList() to """{"title":"Kallare","body":"3° kallare i morgon","data":{"kind":"temperature","notification_id":"n-3"}}""".encodeToByteArray(),
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
    temperatureTrend = true
)