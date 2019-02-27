package io.pleo.antaeus.core.usecases

import arrow.core.Either
import arrow.core.identity
import io.pleo.antaeus.core.services.BillingError
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.ExecutionService
import io.pleo.antaeus.core.services.NotificationService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.*
import java.time.ZonedDateTime

class BillAllPendingInvoicesOnSchedule(
    private val executionService: ExecutionService,
    private val notificationService: NotificationService,
    private val billingService: BillingService,
    private val dal: AntaeusDal,
    private val schedule: Sequence<ZonedDateTime>
) : Usecase {

    fun execute(startAfter: ZonedDateTime = ZonedDateTime.now()): Job {
        return executionService.execute(schedule, startAfter) {
            getPendingInvoices()
                .map { invoice ->
                    val result = billAsync(invoice).await()
                    notifyAsync(result)
                }
                .joinAll()
        }
    }

    private fun notifyAsync(billing: Either<BillingError, Invoice>): Job {
        return billing
            .bimap(
                leftOperation = { notifyFailedAsync(it) },
                rightOperation = { notifySuccessAsync(it) }
            )
            .fold(::identity, ::identity)
    }

    private fun getPendingInvoices(): List<Invoice> {
        return dal
            .fetchInvoices()
            .filter { it.status == InvoiceStatus.PENDING }
    }

    private fun notifySuccessAsync(it: Invoice): Job {
        return GlobalScope.launch(Dispatchers.IO) {
            notificationService.notifyBilled(it)
        }
    }

    private fun notifyFailedAsync(it: BillingError): Job {
        return GlobalScope.launch(Dispatchers.IO) {
            notificationService.notifyBillingFailed(it)
        }
    }

    private fun billAsync(pendingInvoice: Invoice): Deferred<Either<BillingError, Invoice>> {
        return GlobalScope.async(Dispatchers.IO) {
            billingService.bill(pendingInvoice)
        }
    }
}
