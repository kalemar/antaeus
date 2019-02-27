package io.pleo.antaeus.core.services

import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters.firstDayOfNextMonth

class ExecutionService {
    fun execute(schedule: Sequence<ZonedDateTime>, action: suspend () -> Unit) {
        TODO()
    }
}

object Schedules {

    fun everyFirstDayOfMonth(
        startingFrom: ZonedDateTime = ZonedDateTime.now()
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
