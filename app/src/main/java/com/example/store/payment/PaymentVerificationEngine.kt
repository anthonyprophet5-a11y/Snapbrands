package com.example.store.payment

import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.service.AnalyticsService
import com.example.service.SnapBrandEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Trusted Payment Verification and Transactional Order State Engine.
 *
 * CRITICAL ZERO-TRUST MANDATE:
 * - A payment is ONLY marked PAID when trusted backend/provider verification succeeds.
 * - Amount and currency MUST match expected order values exactly.
 * - Inventory is decremented safely and atomically upon verified payment.
 * - Duplicate callbacks/references are processed idempotently without duplicate side-effects.
 */
class PaymentVerificationEngine(
    private val paymentProvider: PaymentProvider,
    private val analytics: AnalyticsService? = null
) {
    private val mutex = Mutex()
    private val processedReferences = mutableSetOf<String>()

    data class VerificationOutcome(
        val isSuccessful: Boolean,
        val updatedOrder: Order,
        val message: String,
        val isDuplicate: Boolean = false
    )

    /**
     * Executes the trusted payment verification sequence for an order.
     */
    suspend fun verifyAndCommitOrder(
        order: Order,
        paymentReference: String,
        currentProductsProvider: (List<String>) -> List<Product>,
        onInventoryDeducted: (productId: String, quantity: Int) -> Unit
    ): Result<VerificationOutcome> = mutex.withLock {
        // 1. Idempotency Check:
        if (processedReferences.contains(paymentReference) || order.paymentStatus == PaymentStatus.PAID) {
            return@withLock Result.success(
                VerificationOutcome(
                    isSuccessful = true,
                    updatedOrder = order.copy(
                        paymentStatus = PaymentStatus.PAID,
                        orderStatus = if (order.orderStatus == OrderStatus.PENDING_PAYMENT) OrderStatus.PAID else order.orderStatus
                    ),
                    message = "Payment reference was previously verified and committed.",
                    isDuplicate = true
                )
            )
        }

        analytics?.logEvent(
            SnapBrandEvent.PAYMENT_VERIFICATION_PENDING,
            mapOf("orderId" to order.id, "reference" to paymentReference)
        )

        // 2. Query provider for authoritative verification
        val verificationResult = paymentProvider.verifyPayment(
            reference = paymentReference,
            expectedAmountMinorUnits = order.total,
            expectedCurrency = order.currency
        ).getOrElse { error ->
            val failedOrder = order.copy(
                paymentStatus = PaymentStatus.FAILED,
                orderStatus = OrderStatus.PAYMENT_FAILED,
                updatedAt = System.currentTimeMillis()
            )
            analytics?.logEvent(
                SnapBrandEvent.PAYMENT_FAILED,
                mapOf("orderId" to order.id, "reason" to (error.message ?: "Verification failed"))
            )
            return@withLock Result.failure(error)
        }

        // 3. Confirm provider validation
        if (!verificationResult.isVerified || verificationResult.status != PaymentStatus.PAID) {
            val failedOrder = order.copy(
                paymentStatus = PaymentStatus.FAILED,
                orderStatus = OrderStatus.PAYMENT_FAILED,
                paymentReference = paymentReference,
                updatedAt = System.currentTimeMillis()
            )
            analytics?.logEvent(
                SnapBrandEvent.PAYMENT_FAILED,
                mapOf("orderId" to order.id, "reason" to (verificationResult.failureReason ?: "Payment incomplete"))
            )
            return@withLock Result.success(
                VerificationOutcome(
                    isSuccessful = false,
                    updatedOrder = failedOrder,
                    message = verificationResult.failureReason ?: "Payment verification failed."
                )
            )
        }

        // 4. Validate Amount Match
        if (verificationResult.amountMinorUnits != order.total) {
            val mismatchOrder = order.copy(
                paymentStatus = PaymentStatus.FAILED,
                orderStatus = OrderStatus.PAYMENT_FAILED,
                paymentReference = paymentReference,
                updatedAt = System.currentTimeMillis()
            )
            analytics?.logEvent(
                SnapBrandEvent.PAYMENT_FAILED,
                mapOf("orderId" to order.id, "reason" to "AMOUNT_MISMATCH")
            )
            return@withLock Result.success(
                VerificationOutcome(
                    isSuccessful = false,
                    updatedOrder = mismatchOrder,
                    message = "Amount mismatch: expected ${order.total}, provider confirmed ${verificationResult.amountMinorUnits}."
                )
            )
        }

        // 5. Validate Currency Match
        if (!verificationResult.currency.equals(order.currency, ignoreCase = true)) {
            val currencyMismatchOrder = order.copy(
                paymentStatus = PaymentStatus.FAILED,
                orderStatus = OrderStatus.PAYMENT_FAILED,
                paymentReference = paymentReference,
                updatedAt = System.currentTimeMillis()
            )
            analytics?.logEvent(
                SnapBrandEvent.PAYMENT_FAILED,
                mapOf("orderId" to order.id, "reason" to "CURRENCY_MISMATCH")
            )
            return@withLock Result.success(
                VerificationOutcome(
                    isSuccessful = false,
                    updatedOrder = currencyMismatchOrder,
                    message = "Currency mismatch: expected ${order.currency}, provider confirmed ${verificationResult.currency}."
                )
            )
        }

        // 6. Transactional Inventory Re-validation & Safe Commitment
        val productIds = order.items.map { it.productId }
        val currentProducts = currentProductsProvider(productIds).associateBy { it.id }

        for (item in order.items) {
            val product = currentProducts[item.productId]
                ?: return@withLock Result.failure(IllegalStateException("Product ${item.productId} no longer exists."))

            if (product.inventory < item.quantity) {
                // Stock exhausted between checkout and payment verification!
                val outOfStockOrder = order.copy(
                    paymentStatus = PaymentStatus.FAILED,
                    orderStatus = OrderStatus.PAYMENT_FAILED,
                    paymentReference = paymentReference,
                    updatedAt = System.currentTimeMillis()
                )
                return@withLock Result.success(
                    VerificationOutcome(
                        isSuccessful = false,
                        updatedOrder = outOfStockOrder,
                        message = "Inventory depleted: only ${product.inventory} available for ${product.title}."
                    )
                )
            }
        }

        // 7. Commit Inventory Deductions
        for (item in order.items) {
            onInventoryDeducted(item.productId, item.quantity)
        }

        // 8. Transition Order to PAID
        val now = System.currentTimeMillis()
        val paidOrder = order.copy(
            paymentReference = paymentReference,
            paymentStatus = PaymentStatus.PAID,
            orderStatus = OrderStatus.PAID,
            paidAt = now,
            updatedAt = now
        )

        // 9. Register processed reference for idempotency
        processedReferences.add(paymentReference)

        analytics?.logEvent(
            SnapBrandEvent.PAYMENT_VERIFIED,
            mapOf("orderId" to order.id, "reference" to paymentReference, "amount" to order.total)
        )
        analytics?.logEvent(
            SnapBrandEvent.ORDER_CREATED,
            mapOf("orderId" to order.id, "orderNumber" to order.orderNumber)
        )

        return@withLock Result.success(
            VerificationOutcome(
                isSuccessful = true,
                updatedOrder = paidOrder,
                message = "Order successfully verified and confirmed."
            )
        )
    }

    /**
     * Checks if a reference has already been verified and processed.
     */
    fun isReferenceProcessed(reference: String): Boolean {
        return processedReferences.contains(reference)
    }
}
