package io.pleo.antaeus.core.services

import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class NotificationService {

    private val logger = KotlinLogging.logger {}

    fun notifyBilled(invoice: Invoice) {
        logger.info { "Invoice [$invoice] was successfully billed" }
    }

    fun notifyBillingFailed(error: BillingError) {
        logger.info { "There was an billing error [$error]" }
    }
}
