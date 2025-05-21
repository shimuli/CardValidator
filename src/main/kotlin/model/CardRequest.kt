package com.shimuli.model

data class CardRequest (
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String
)