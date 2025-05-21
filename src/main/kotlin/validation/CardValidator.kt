package com.shimuli.validation

import com.shimuli.model.CardRequest
import com.shimuli.model.ValidationResult
import java.time.YearMonth

object CardValidator {
    fun validate(request: CardRequest): ValidationResult {
        if (!Luhn.isValid(request.cardNumber))
            return ValidationResult(false, "Invalid card number (Luhn check failed)")

        if (request.expiryMonth !in 1..12)
            return ValidationResult(false, "Invalid expiry month")

        val now = YearMonth.now()
        val expiry = YearMonth.of(request.expiryYear, request.expiryMonth)
        if (expiry.isBefore(now))
            return ValidationResult(false, "Card is expired")

        if (!request.cvv.all { it.isDigit() } || request.cvv.length !in 3..4)
            return ValidationResult(false, "Invalid CVV format")

        return ValidationResult(true, "Card is valid")
    }
}