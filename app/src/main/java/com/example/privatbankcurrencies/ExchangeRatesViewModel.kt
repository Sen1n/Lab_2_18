package com.example.privatbankcurrencies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeRatesViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ExchangeDatabase.getDatabase(application).exchangeRateDao()
    private val workManager = WorkManager.getInstance(application)

    private val _selectedDate = MutableStateFlow(DateUtils.todayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val rates: StateFlow<List<ExchangeRateEntity>> = _selectedDate
        .flatMapLatest { date ->
            dao.observeRates(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList()
        )

    val savedDates: StateFlow<List<String>> = dao.observeSavedDates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList()
        )

    val currentWorkInfo: StateFlow<WorkInfo?> = _selectedDate
        .flatMapLatest { date ->
            val workName = ExchangeRatesWorkScheduler.uniqueOneTimeWorkName(date)
            workManager.getWorkInfosForUniqueWorkFlow(workName)
                .map { workInfos -> workInfos.firstOrNull() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null
        )

    fun selectDate(date: String) {
        if (DateUtils.isValidDate(date)) {
            _selectedDate.value = date
        }
    }

    fun refreshSelectedDate() {
        ExchangeRatesWorkScheduler.enqueueOneTimeSync(
            context = getApplication(),
            date = _selectedDate.value
        )
    }
}