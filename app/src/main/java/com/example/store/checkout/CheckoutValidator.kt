package com.example.store.checkout

import com.example.data.model.CustomerDeliveryInfo

data class CheckoutValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

object CheckoutValidator {

    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun validateDeliveryInfo(info: CustomerDeliveryInfo): CheckoutValidationResult {
        val errors = mutableListOf<String>()

        if (info.customerName.trim().length < 2) {
            errors.add("Please enter your full name.")
        }

        if (!EMAIL_REGEX.matches(info.customerEmail.trim())) {
            errors.add("Please enter a valid email address.")
        }

        val cleanedPhone = info.customerPhone.trim().replace("\\s".toRegex(), "").replace("-", "")
        if (cleanedPhone.length < 9) {
            errors.add("Please enter a valid phone number.")
        }

        if (info.country.trim().isBlank()) {
            errors.add("Please select or enter your country.")
        }

        if (info.city.trim().length < 2) {
            errors.add("Please enter your city.")
        }

        if (info.deliveryAddress.trim().length < 5) {
            errors.add("Please provide a complete delivery address or Ghana digital address (e.g. GA-183-9321).")
        }

        return CheckoutValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}
