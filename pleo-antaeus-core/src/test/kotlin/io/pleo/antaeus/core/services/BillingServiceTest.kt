package io.pleo.antaeus.core.services

import arrow.core.Left
import arrow.core.Right
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal


class BillingServiceTest {
    private val paymentProvider = mockk<PaymentProvider>()
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoices() } returns listOf()
    }

    private val billingService = BillingService(paymentProvider, dal)

    private val someCustomerId = 1
    private val someMoney = Money(BigDecimal.ONE, Currency.DKK)
    private val someInvoice = Invoice(1, someCustomerId, someMoney, InvoiceStatus.PENDING)

    @Test
    fun `when CustomerNotFoundException is thrown CustomerNotFound error should be returned`() {
        //Arrange
        every { paymentProvider.charge(any()) } throws CustomerNotFoundException(someCustomerId)

        //Act
        val response = billingService.bill(someInvoice)

        //Assert
        assertThat(response).isEqualTo(Left(CustomerNotFound(someInvoice)))
    }

    @Test
    fun `when CurrencyMismatchException is thrown CurrencyMismatch error should be returned`() {
        //Arrange
        every { paymentProvider.charge(any()) } throws CurrencyMismatchException(someInvoice.id, someCustomerId)

        //Act
        val response = billingService.bill(someInvoice)

        //Assert
        assertThat(response).isEqualTo(Left(CurrencyMismatch(someInvoice)))
    }

    @Test
    fun `when NetworkException is thrown NetworkFailure error should be returned`() {
        //Arrange
        every { paymentProvider.charge(any()) } throws CurrencyMismatchException(someInvoice.id, someCustomerId)

        //Act
        val response = billingService.bill(someInvoice)

        //Assert
        assertThat(response).isEqualTo(Left(CurrencyMismatch(someInvoice)))
    }

    @Test
    fun `when charge() returns false Insufficient funds error should be returned`() {
        //Arrange
        every { paymentProvider.charge(any()) } returns false

        //Act
        val response = billingService.bill(someInvoice)

        //Assert
        assertThat(response).isEqualTo(Left(InsufficientFunds(someInvoice)))
    }

    @Test
    fun `when PaymentProvider throws unknown exception re-throw it`() {
        //Arrange
        val unknownException = NumberFormatException()
        every { paymentProvider.charge(any()) } throws unknownException

        //Assert
        assertThrows<NumberFormatException> {
            //Act
            billingService.bill(someInvoice)
        }
    }

    @Test
    fun `when charge completes successfully no error should be returned`() {
        //Arrange
        val paidInvoice = someInvoice.copy(status = InvoiceStatus.PAID)
        every { paymentProvider.charge(any()) } returns true
        every { dal.updateInvoice(any()) } returns paidInvoice

        //Act
        val response = billingService.bill(someInvoice)

        //Assert
        assertThat(response).isEqualTo(Right(paidInvoice))
    }

    @Test
    fun `when persisting of billed invoice fails try to re-save it`() {
        //Arrange
        val paidInvoice = someInvoice.copy(status = InvoiceStatus.PAID)
        val customer = Customer(someInvoice.customerId, Currency.DKK)
        every { paymentProvider.charge(any()) } returns true
        every { dal.updateInvoice(any()) } returns null
        every { dal.fetchCustomer(eq(someInvoice.customerId)) } returns customer
        every { dal.createInvoice(eq(paidInvoice.amount), eq(customer), eq(InvoiceStatus.PAID)) } returns paidInvoice

        //Act
        val response = billingService.bill(someInvoice)

        //Assert
        verify(exactly = 1) { dal.createInvoice(paidInvoice.amount, customer, InvoiceStatus.PAID) }
        assertThat(response).isEqualTo(Right(paidInvoice))
    }

    @Test
    fun `when re-saving fails due to missing customer throw IllegalStateException`() {
        //Arrange
        every { paymentProvider.charge(any()) } returns true
        every { dal.updateInvoice(any()) } returns null
        every { dal.fetchCustomer(eq(someInvoice.customerId)) } returns null

        //Assert
        assertThrows<IllegalStateException> {
            //Act
            billingService.bill(someInvoice)
        }
    }

    @Test
    fun `when re-saving fails when creating the invoice throw IllegalStateException`() {
        //Arrange
        val paidInvoice = someInvoice.copy(status = InvoiceStatus.PAID)
        val customer = Customer(someInvoice.customerId, Currency.DKK)
        every { paymentProvider.charge(any()) } returns true
        every { dal.updateInvoice(any()) } returns null
        every { dal.fetchCustomer(eq(someInvoice.customerId)) } returns customer
        every { dal.createInvoice(eq(paidInvoice.amount), eq(customer), eq(InvoiceStatus.PAID)) } returns null

        //Assert
        assertThrows<IllegalStateException> {
            //Act
            billingService.bill(someInvoice)
        }
    }

    @Test
    fun `do not charge when invoice is already paid`() {
        //Arrange
        val paidInvoice = someInvoice.copy(status = InvoiceStatus.PAID)
        every { paymentProvider.charge(eq(paidInvoice)) } returns true

        //Act
        billingService.bill(paidInvoice)

        //Assert
        verify(exactly = 0) { paymentProvider.charge(any()) }
    }
}
