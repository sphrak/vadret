package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.common.defaultJson
import fi.kroon.vadret.encryption.SealedMessageKey
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import fi.kroon.vadret.pushalert.PushAlertRepository
import fi.kroon.vadret.pushalert.extension.toPushAlertMessageOrNull
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.model.PushAlertKind
import fi.kroon.vadret.pushalert.model.PushAlertMessage
import fi.kroon.vadret.pushalert.model.SealedContentDto
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import kotlinx.serialization.SerializationException
import kotlin.io.encoding.Base64

internal class DefaultOpenPushAlertTask(
    private val sealedMessageKey: SealedMessageKey,
    private val pushAlertRepository: PushAlertRepository
) : OpenPushAlertTask {
    override suspend fun invoke(sealed: String): Either<Failure, PushAlertMessage> =
        open(sealed = sealed).flatMap { pushAlertMessage: PushAlertMessage ->
            pushAlertRepository.choices().flatMap { pushAlertChoices: PushAlertChoices ->
                if (pushAlertChoices.isChosen(pushAlertKind = pushAlertMessage.kind)) {
                    pushAlertMessage.asRight()
                } else {
                    Failure(errorCode = ErrorCode.PUSH_ALERT_NOT_CHOSEN).asLeft()
                }
            }
        }

    private fun open(sealed: String): Either<Failure, PushAlertMessage> {
        val sealedBytes = try {
            Base64.decode(source = sealed)
        } catch (exception: IllegalArgumentException) {
            return unreadable(description = "it is not base64: ${exception::class.simpleName}")
        }
        return when (val opened = sealedMessageKey.open(sealed = sealedBytes)) {
            is Either.Left -> unreadable(description = "this device's key cannot open it: ${opened.a}")
            is Either.Right -> opened.b.toPushAlertMessage()
        }
    }

    private fun ByteArray.toPushAlertMessage(): Either<Failure, PushAlertMessage> {
        val sealedContentDto = try {
            defaultJson.decodeFromString(
                deserializer = SealedContentDto.serializer(),
                string = decodeToString()
            )
        } catch (exception: SerializationException) {
            return unreadable(description = "it holds no push alert: ${exception::class.simpleName}")
        }
        val pushAlertMessage = sealedContentDto.toPushAlertMessageOrNull()
            ?: return unreadable(description = "it has no title, no body or no kind this app knows")
        return pushAlertMessage.asRight()
    }

    private fun unreadable(description: String): Either<Failure, PushAlertMessage> {
        logger.warn { "${ErrorCode.INVALID_RESPONSE}: a push alert was dropped because $description" }
        return Failure(errorCode = ErrorCode.INVALID_RESPONSE).asLeft()
    }
}

private fun PushAlertChoices.isChosen(pushAlertKind: PushAlertKind): Boolean =
    when (pushAlertKind) {
        PushAlertKind.WARNING -> warningLowestLevel != null
        PushAlertKind.POLLEN -> pollenChoices.isNotEmpty()
        PushAlertKind.TEMPERATURE -> temperatureTrend
    }

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.pushalert")