package se.vadretapp.vadret.ui.extension

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.concurrent.atomics.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineScopeTest {

    @Test
    fun `the first job runs and becomes the current job`() =
        runTest {
            val dispatcher: TestDispatcher = StandardTestDispatcher(scheduler = testScheduler)
            val currentJob: AtomicReference<Job?> = AtomicReference(value = null)

            val job: Job = launchReplacing(
                currentJob = currentJob,
                context = dispatcher
            ) { }
            advanceUntilIdle()

            assertTrue(actual = job.isCompleted)
            assertFalse(actual = job.isCancelled)
            assertEquals(expected = job, actual = currentJob.load())
        }

    @Test
    fun `a new job cancels the running job it replaces`() =
        runTest {
            val dispatcher: TestDispatcher = StandardTestDispatcher(scheduler = testScheduler)
            val currentJob: AtomicReference<Job?> = AtomicReference(value = null)
            val replacedJob: Job = launchReplacing(
                currentJob = currentJob,
                context = dispatcher
            ) {
                awaitCancellation()
            }
            runCurrent()

            val replacingJob: Job = launchReplacing(
                currentJob = currentJob,
                context = dispatcher
            ) { }
            advanceUntilIdle()

            assertTrue(actual = replacedJob.isCancelled)
            assertTrue(actual = replacingJob.isCompleted)
            assertFalse(actual = replacingJob.isCancelled)
            assertEquals(expected = replacingJob, actual = currentJob.load())
        }
}