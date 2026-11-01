package se.vadretapp.vadret.ui.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicReference
import kotlin.coroutines.CoroutineContext

fun CoroutineScope.launchReplacing(
    currentJob: AtomicReference<Job?>,
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val job: Job = launch(
        context = context,
        start = CoroutineStart.LAZY,
        block = block
    )
    currentJob.exchange(newValue = job)?.cancel()
    job.start()
    return job
}