package fi.kroon.vadret.pushalert

import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.extension.toFailure
import fi.kroon.vadret.pushalert.error.DeviceTokenUnavailableException
import fi.kroon.vadret.pushalert.model.DeviceToken
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AndroidDeviceTokenSource : DeviceTokenSource {
    override suspend fun deviceToken(): Either<Failure, DeviceToken> =
        try {
            DeviceToken(value = FirebaseMessaging.getInstance().token.awaitResult()).asRight()
        } catch (exception: Exception) {
            exception.toFailure().asLeft()
        }
}

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task: Task<T> ->
            val exception = task.exception
            if (task.isSuccessful) {
                continuation.resume(value = task.result)
            } else if (exception != null) {
                continuation.resumeWithException(exception = exception)
            } else {
                continuation.resumeWithException(exception = DeviceTokenUnavailableException(message = "Firebase gave neither a token nor a reason"))
            }
        }
    }