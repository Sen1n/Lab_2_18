package com.example.privatbankcurrencies

data class ExchangeArchiveResponse(
    val date: String?,
    val bank: String?,
    val baseCurrency: Int?,
    val baseCurrencyLit: String?,
    val exchangeRate: List<ExchangeRateDto>?
)

data class ExchangeRateDto(
    val baseCurrency: String?,
    val currency: String?,
    val saleRateNB: Double?,
    val purchaseRateNB: Double?,
    val saleRate: Double?,
    val purchaseRate: Double?
)