package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.pushalert.usecase.DefaultSavePushAlertChoicesTask
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSavePushAlertChoicesTaskTest {
    @Test
    fun theChoicesAreKeptAndTheServerIsNotAsked() = runTest {
        val pushAlertRepository = FakePushAlertRepository()
        val choices = PushAlertChoices.NOTHING_CHOSEN.copy(warningLowestLevel = WarningAlertLevel.ORANGE)

        val saved = DefaultSavePushAlertChoicesTask(pushAlertRepository = pushAlertRepository)(pushAlertChoices = choices)

        assertEquals(
            expected = Unit.asRight(),
            actual = saved
        )
        assertEquals(
            expected = choices to emptyList<String>(),
            actual = pushAlertRepository.savedChoices.value to pushAlertRepository.calls
        )
    }
}