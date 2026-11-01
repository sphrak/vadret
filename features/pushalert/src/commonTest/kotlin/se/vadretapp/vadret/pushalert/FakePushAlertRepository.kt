package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.DeviceRegistration
import se.vadretapp.vadret.pushalert.model.DeviceToken
import se.vadretapp.vadret.pushalert.model.InstallationToken
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertSubscription
import kotlin.io.encoding.Base64

internal class FakePushAlertRepository(
    choices: PushAlertChoices = PushAlertChoices.NOTHING_CHOSEN,
    var installationToken: InstallationToken? = null,
    var deviceRegistration: DeviceRegistration? = null,
    private val subscriptionFailures: MutableList<Failure> = mutableListOf(),
    private val registrationFailures: MutableList<Failure> = mutableListOf(),
    private val choicesFailure: Failure? = null,
    private val installGate: Deferred<Unit> = CompletableDeferred(value = Unit)
) : PushAlertRepository {
    val calls = mutableListOf<String>()
    val savedChoices = MutableStateFlow(value = choices)
    val isSyncPending = MutableStateFlow(value = false)
    var installFailure: Failure? = null

    override suspend fun installationToken(): Either<Failure, InstallationToken?> = installationToken.asRight()

    override suspend fun install(): Either<Failure, InstallationToken> {
        installGate.await()
        calls += "install"
        val failure = installFailure
        if (failure != null) {
            return failure.asLeft()
        }
        val newInstallationToken = InstallationToken(value = "installation-${calls.count { call -> call == "install" }}")
        installationToken = newInstallationToken
        deviceRegistration = null
        return newInstallationToken.asRight()
    }

    override suspend fun registeredDevice(): Either<Failure, DeviceRegistration?> = deviceRegistration.asRight()

    override suspend fun registerDevice(
        installationToken: InstallationToken,
        deviceToken: DeviceToken,
        replacesDeviceToken: DeviceToken?,
        devicePublicKey: ByteArray,
        registeredAtMillis: Long
    ): Either<Failure, Unit> {
        calls += "register ${installationToken.value} ${deviceToken.value} replacing ${replacesDeviceToken?.value} key ${devicePublicKey.size} at $registeredAtMillis"
        if (registrationFailures.isNotEmpty()) {
            return registrationFailures.removeAt(index = 0).asLeft()
        }
        deviceRegistration = DeviceRegistration(
            deviceToken = deviceToken,
            devicePublicKey = Base64.encode(source = devicePublicKey),
            registeredAtMillis = registeredAtMillis
        )
        return Unit.asRight()
    }

    override suspend fun replaceSubscription(
        installationToken: InstallationToken,
        pushAlertSubscription: PushAlertSubscription?
    ): Either<Failure, Unit> {
        calls += "subscribe ${installationToken.value} ${pushAlertSubscription?.geohash?.value} ${pushAlertSubscription?.warningLowestLevel} ${pushAlertSubscription?.temperatureTrend}"
        return if (subscriptionFailures.isEmpty()) {
            Unit.asRight()
        } else {
            subscriptionFailures.removeAt(index = 0).asLeft()
        }
    }

    override suspend fun forgetInstallation(installationToken: InstallationToken): Either<Failure, Unit> {
        calls += "forget ${installationToken.value}"
        this.installationToken = null
        deviceRegistration = null
        return Unit.asRight()
    }

    override suspend fun clearInstallation(): Either<Failure, Unit> {
        calls += "clear"
        installationToken = null
        deviceRegistration = null
        return Unit.asRight()
    }

    override fun observeChoices(): Flow<Either<Failure, PushAlertChoices>> = savedChoices.map { pushAlertChoices -> pushAlertChoices.asRight() }

    override suspend fun choices(): Either<Failure, PushAlertChoices> = choicesFailure?.asLeft() ?: savedChoices.value.asRight()

    override suspend fun saveChoices(pushAlertChoices: PushAlertChoices): Either<Failure, Unit> {
        savedChoices.value = pushAlertChoices
        return Unit.asRight()
    }

    override fun observeIsSyncPending(): Flow<Either<Failure, Boolean>> = isSyncPending.map { isSyncPending -> isSyncPending.asRight() }

    override suspend fun saveIsSyncPending(isSyncPending: Boolean): Either<Failure, Unit> {
        this.isSyncPending.value = isSyncPending
        return Unit.asRight()
    }
}