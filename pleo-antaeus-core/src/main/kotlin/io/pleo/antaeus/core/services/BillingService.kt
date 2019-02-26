package io.pleo.antaeus.core.services

import arrow.core.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.PAID
import mu.KotlinLogging


class BillingService(private val paymentProvider: PaymentProvider) {

    private val logger = KotlinLogging.logger {}

    fun bill(invoice: Invoice): Option<BillingError> {
        if (invoice.status == PAID) return None

        return Try { charge(invoice) }
            .fold(
                ifFailure = { processError(it, invoice) },
                ifSuccess = ::identity
            )
    }

    private fun charge(invoice: Invoice): Option<InsufficientFunds> {
        return when (paymentProvider.charge(invoice)) {
            true -> None
            false -> Some(InsufficientFunds(invoice))
        }
    }

    private fun processError(exception: Throwable, invoice: Invoice): Some<BillingError> {
        logger.error(exception) { "An error occurred when billing invoice '${invoice.id}'" }
        return Some(exception.toBillingError(failedInvoice = invoice))
    }

    private fun Throwable.toBillingError(failedInvoice: Invoice): BillingError {
        return when (this) {
            is CustomerNotFoundException -> CustomerNotFound(failedInvoice)
            is CurrencyMismatchException -> CurrencyMismatch(failedInvoice)
            is NetworkException -> NetworkFailure(failedInvoice)
            else -> Unknown(failedInvoice)
        }
    }
}

sealed class BillingError {
    abstract val failedInvoice: Invoice
}

data class InsufficientFunds(override val failedInvoice: Invoice) : BillingError()
data class CurrencyMismatch(override val failedInvoice: Invoice) : BillingError()
data class CustomerNotFound(override val failedInvoice: Invoice) : BillingError()
data class NetworkFailure(override val failedInvoice: Invoice) : BillingError()
data class Unknown(override val failedInvoice: Invoice) : BillingError()