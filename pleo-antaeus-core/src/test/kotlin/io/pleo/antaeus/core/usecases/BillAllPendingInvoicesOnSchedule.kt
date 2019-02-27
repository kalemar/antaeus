package io.pleo.antaeus.core.usecases

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.ExecutionService
import io.pleo.antaeus.core.services.NotificationService
import io.pleo.antaeus.data.AntaeusDal
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

internal class BillAllPendingInvoicesOnScheduleTest {
    private val executionService = mockk<ExecutionService> {
        every { execute(any(), any(), any()) } returns GlobalScope.launch {}
    }
    private val notificationService = mockk<NotificationService>()
    private val billingService = mockk<BillingService>()
    private val dal = mockk<AntaeusDal>()
    private val zoneId = ZoneId.of("Europe/Prague")
    private val startingFrom = ZonedDateTime.ofInstant(Instant.EPOCH, zoneId)
    private val schedule = sequenceOf(
        startingFrom.plus(10, ChronoUnit.MILLIS),
        startingFrom.plus(20, ChronoUnit.MILLIS),
        startingFrom.plus(30, ChronoUnit.MILLIS)
    )
    private val usecase = BillAllPendingInvoicesOnSchedule(
        executionService,
        notificationService,
        billingService,
        dal,
        schedule
    )

    @Test
    fun `when usecase is executed it should submit a job to execution service`() {
        runBlocking {
            usecase.execute(startingFrom)
        }

        verify { executionService.execute(match { it === schedule }, match { it === startingFrom }, any()) }
    }
}
