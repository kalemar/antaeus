package io.pleo.antaeus.core.services

import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

internal class ExecutionServiceTest {

    private val executionService = ExecutionService()

    private val zoneId = ZoneId.of("Europe/Prague")
    private val startingFrom = ZonedDateTime.ofInstant(Instant.EPOCH, zoneId)
    private val schedule = sequenceOf(
        startingFrom.plus(10, ChronoUnit.MILLIS),
        startingFrom.plus(20, ChronoUnit.MILLIS),
        startingFrom.plus(30, ChronoUnit.MILLIS)
    )

    @Test
    fun `ExecutionService must execute amount of actions based on schedule`() {
        val action = {}
        val spy = spyk(action)

        runBlocking {
            executionService.execute(schedule, startingFrom = startingFrom) {
                spy.invoke()
            }.join()
        }

        verify(exactly = 3) { spy.invoke() }
    }

    @Test
    fun `ExecutionService must not execute any actions if empty schedule`() {
        val action = {}
        val spy = spyk(action)

        runBlocking {
            executionService.execute(sequenceOf()) {
                spy.invoke()
            }.join()
        }

        verify(exactly = 0) { spy.invoke() }
    }

    @Test
    fun `ExecutionService must not execute only actions after starting point in schedule`() {
        val action = {}
        val spy = spyk(action)

        runBlocking {
            executionService.execute(schedule, startingFrom.plus(15, ChronoUnit.MILLIS)) {
                spy.invoke()
            }.join()
        }

        verify(exactly = 2) { spy.invoke() }
    }
}
