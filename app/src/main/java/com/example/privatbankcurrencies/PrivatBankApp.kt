package com.example.privatbankcurrencies

import android.app.Application

class PrivatBankApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ExchangeRatesWorkScheduler.schedulePeriodicSync(this)
    }
}