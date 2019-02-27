package io.pleo.antaeus.core.services

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.time.Duration
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.time.temporal.TemporalAdjusters.firstDayOfNextMonth
import java.util.concurrent.Executors

class ExecutionService {

    private val schedulingDispatcher = Executors.newWorkStealingPool().asCoroutineDispatcher()

    fun execute(
        schedule: Sequence<ZonedDateTime>,
        startingFrom: ZonedDateTime = now(),
        action: suspend () -> Unit
    ): Job {
        val delays = schedule.toDelays(startingFrom)
        return GlobalScope.launch(schedulingDispatcher) {
            for (delay in delays) {
                delay(delay)
                launchNext(action)
            }
        }
    }

    private suspend fun launchNext(action: suspend () -> Unit) {
        GlobalScope
            .launch(IO) { action() }
            .join()
    }

    private fun Sequence<ZonedDateTime>.toDelays(startingFrom: ZonedDateTime): Sequence<Long> {
        return sequenceOf(startingFrom).plus(this)
            .zip(this)
            .map { (from, to) -> Duration.between(from, to).toMillis() }
            .filter { it >= 0 }
    }
}

object Schedules {

    fun everyFirstDayOfMonth(
        startingFrom: ZonedDateTime = now()
    ): Sequence<ZonedDateTime> {
        return generateSequence(startingFrom.atStartOfDay()) { previous ->
            previous.with(firstDayOfNextMonth())
        }
            .adjustStartingDate(startingFrom)
    }

    private fun Sequence<ZonedDateTime>.adjustStartingDate(startingFrom: ZonedDateTime): Sequence<ZonedDateTime> {
        return if (startingFrom.dayOfMonth != 1) drop(1) else this
    }

    private fun ZonedDateTime.atStartOfDay(): ZonedDateTime {
        return toLocalDate().atStartOfDay(zone)
    }
}
