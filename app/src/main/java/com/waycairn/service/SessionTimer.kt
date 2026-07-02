package com.waycairn.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Phase 5: the AFTER_DELAY timer. Runs a single coroutine on the service scope that fires
 * [onElapsed] after [delayMinutes]. Holds its [Job] so an app switch can [cancel] it before it
 * elapses (AFTER_DELAY resets on app switch, per BUILD_PLAN.md).
 */
class SessionTimer(
    private val scope: CoroutineScope,
    private val delayMinutes: Int,
    private val onElapsed: suspend () -> Unit
) {
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            delay(delayMinutes.toLong() * 60_000L)
            onElapsed()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
