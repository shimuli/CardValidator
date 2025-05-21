package com.shimuli.validation

object Luhn {
    fun isValid(cardNumber: String): Boolean {
        val digits = cardNumber.filter { it.isDigit() }.map { it.toString().toInt() }.reversed()
        val sum = digits.mapIndexed { index, digit ->
            if (index % 2 == 1) {
                val doubled = digit * 2
                if (doubled > 9) doubled - 9 else doubled
            } else digit
        }.sum()
        return sum % 10 == 0
    }
}