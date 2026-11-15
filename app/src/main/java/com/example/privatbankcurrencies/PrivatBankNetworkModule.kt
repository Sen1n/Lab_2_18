package com.example.privatbankcurrencies

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object PrivatBankNetworkModule {

    private const val BASE_URL = "https://api.privatbank.ua/p24api/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: PrivatBankApi by lazy {
        retrofit.create(PrivatBankApi::class.java)
    }
}