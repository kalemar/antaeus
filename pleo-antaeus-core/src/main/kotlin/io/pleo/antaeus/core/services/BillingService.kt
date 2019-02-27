package io.pleo.antaeus.core.services

import arrow.core.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.PAID
import mu.KotlinLogging


class BillingService(

    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal) {

    private val logger = KotlinLogging.logger {}

    fun bill(invoice: Invoice): Either<BillingError, Invoice> {
        if (invoice.status == PAID) return Right(invoice)

        return Try { paymentProvider.charge(invoice) }
            .toEither { handleExceptions(it, invoice) }
            .fold(::identity) { response ->
                processResponse(response, invoice)
            }
    }

    private fun processResponse(response: Boolean, invoice: Invoice): Either<InsufficientFunds, Invoice> {
        return when (response) {
            true -> persistInvoiceStatus(invoice).right()
            false -> InsufficientFunds(invoice).left()
        }
    }

    private fun persistInvoiceStatus(invoice: Invoice): Invoice {
        return dal.updateInvoice(invoice.copy(status = PAID))
            ?: tryToRecreateInvoice(invoice)
            ?: throw IllegalStateException("Could not persist billed invoice [$invoice]")
    }

    private fun tryToRecreateInvoice(invoice: Invoice): Invoice? {
        return dal
            .fetchCustomer(invoice.customerId)
            ?.let { invoiceCustomer ->
                dal.createInvoice(invoice.amount, invoiceCustomer, PAID)
            }
    }

    private fun handleExceptions(exception: Throwable, invoice: Invoice): Either<BillingError, Invoice> {
        logger.error(exception) { "An error occurred when billing invoice '${invoice.id}'" }
        return exception.toBillingError(failedInvoice = invoice).left()
    }

    private fun Throwable.toBillingError(failedInvoice: Invoice): BillingError {
        return when (this) {
            is CustomerNotFoundException -> CustomerNotFound(failedInvoice)
            is CurrencyMismatchException -> CurrencyMismatch(failedInvoice)
            is NetworkException -> NetworkFailure(failedInvoice)
            else -> throw this
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
