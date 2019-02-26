package io.pleo.antaeus.core.services

import arrow.core.None
import arrow.core.Some
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal


class BillingServiceTest {
    private val paymentProvider = mockk<PaymentProvider>()

    private val billingService = BillingService(paymentProvider)

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
        assertThat(response).isEqualTo(Some(CustomerNotFound(someInvoice)))
    }

    @Test
    fun `when CurrencyMismatchException is thrown CurrencyMismatch error should be returned`() {
        //Arrange
        every { paymentProvider.charge(any()) } throws CurrencyMismatchException(someInvoice.id, someCustomerId)

        //Act
        val response = billingService.bill(someInvoice)

        //Assert
        assertThat(response).isEqualTo(Some(CurrencyMismatch(someInvoice)))
    }

    @Test
    fun `when NetworkException is thrown NetworkFailure error should be returned`() {
        //Arrange
        every { paymentProvider.charge(any()) } throws CurrencyMismatchException(someInvoice.id, someCustomerId)

        //Act
        val response = billingService.bill(someInvoice)

        //Assert
        assertThat(response).isEqualTo(Some(CurrencyMismatch(someInvoice)))
    }

    @Test
    fun `when charge() returns false Insufficient funds error should be returned`() {
        //Arrange
        every { paymentProvider.charge(any()) } returns false

        //Act
        val response = billingService.bill(someInvoice)

        //Assert
        assertThat(response).isEqualTo(Some(InsufficientFunds(someInvoice)))
    }

    @Test
    fun `when charge completes successfully no error should be returned`() {
        //Arrange
        every { paymentProvider.charge(any()) } returns true

        //Act
        val response = billingService.bill(someInvoice)

        //Assert
        assertThat(response).isEqualTo(None)
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