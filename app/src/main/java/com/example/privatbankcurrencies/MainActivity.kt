package com.example.privatbankcurrencies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ExchangeRatesScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRatesScreen(
    viewModel: ExchangeRatesViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val rates by viewModel.rates.collectAsStateWithLifecycle()
    val savedDates by viewModel.savedDates.collectAsStateWithLifecycle()
    val workInfo by viewModel.currentWorkInfo.collectAsStateWithLifecycle()

    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        val initialMillis = DateUtils.dateStringToMillis(selectedDate)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.selectDate(DateUtils.millisToDateString(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Обрати")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Скасувати")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ПриватБанк — валюти") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Дата: $selectedDate")
                }

                Button(
                    onClick = { viewModel.refreshSelectedDate() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Оновити")
                }
            }

            if (savedDates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Збережені дати у базі:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = savedDates,
                            key = { it }
                        ) { date ->
                            FilterChip(
                                selected = date == selectedDate,
                                onClick = { viewModel.selectDate(date) },
                                label = { Text(date) }
                            )
                        }
                    }
                }
            }

            WorkStatusBanner(
                workInfo = workInfo,
                hasCachedRates = rates.isNotEmpty()
            )

            if (rates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Курси за цю дату ще не завантажені",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = rates,
                        key = { "${it.date}_${it.currency}" }
                    ) { rate ->
                        ExchangeRateCard(rate = rate)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkStatusBanner(
    workInfo: WorkInfo?,
    hasCachedRates: Boolean
) {
    if (workInfo == null) return

    val statusText: String?
    val isInProgress: Boolean
    val isError: Boolean

    when (workInfo.state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
            statusText = "Очікує мережі / запуску"
            isInProgress = true
            isError = false
        }
        WorkInfo.State.RUNNING -> {
            statusText = "Оновлюється..."
            isInProgress = true
            isError = false
        }
        WorkInfo.State.SUCCEEDED -> {
            val message = workInfo.outputData.getString(ExchangeRatesSyncWorker.KEY_MESSAGE)
            val isEmptyResponse = workInfo.outputData.getBoolean(
                ExchangeRatesSyncWorker.KEY_EMPTY_RESPONSE,
                false
            )
            statusText = when {
                isEmptyResponse && hasCachedRates -> "$message (показано раніше збережені курси)"
                !message.isNullOrBlank() -> message
                else -> null
            }
            isInProgress = false
            isError = isEmptyResponse
        }
        WorkInfo.State.FAILED -> {
            val errorMessage = workInfo.outputData.getString(ExchangeRatesSyncWorker.KEY_MESSAGE)
                ?: "Помилка оновлення"
            statusText = if (hasCachedRates) {
                "$errorMessage (показано раніше збережені курси)"
            } else {
                errorMessage
            }
            isInProgress = false
            isError = true
        }
        WorkInfo.State.CANCELLED -> {
            statusText = null
            isInProgress = false
            isError = false
        }
    }

    if (statusText != null) {
        val containerColor = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
        val contentColor = if (isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun ExchangeRateCard(rate: ExchangeRateEntity) {
    val purchaseText = rate.purchaseRate?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
    val saleText = rate.saleRate?.let { String.format(Locale.US, "%.2f", it) } ?: "—"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${rate.currency} / ${rate.baseCurrency}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = rate.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Купівля",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = purchaseText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Продаж",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = saleText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}