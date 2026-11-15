package com.example.privatbankcurrencies

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import java.io.IOException

class ExchangeRatesSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val requestedDate = inputData.getString(KEY_DATE)
            ?.takeIf { it.isNotBlank() }
            ?: DateUtils.todayString()

        if (!DateUtils.isValidDate(requestedDate)) {
            return Result.failure(
                workDataOf(KEY_MESSAGE to "Некоректний формат дати: $requestedDate")
            )
        }

        val api = PrivatBankNetworkModule.api
        val dao = ExchangeDatabase.getDatabase(applicationContext).exchangeRateDao()

        return try {
            val response = api.getArchiveRates(date = requestedDate)

            if (!response.isSuccessful) {
                val code = response.code()
                return if (code in 500..599 || code == 429) {
                    Result.retry()
                } else {
                    Result.failure(
                        workDataOf(KEY_MESSAGE to "Помилка сервера: HTTP $code")
                    )
                }
            }

            val body = response.body()
                ?: return Result.failure(
                    workDataOf(KEY_MESSAGE to "Сервер повернув порожнє тіло відповіді")
                )

            val responseDate = body.date?.trim()
            if (responseDate != requestedDate) {
                return Result.failure(
                    workDataOf(
                        KEY_MESSAGE to "Дата відповіді ($responseDate) не збігається із запитаною ($requestedDate)"
                    )
                )
            }

            val defaultBase = body.baseCurrencyLit?.takeIf { it.isNotBlank() } ?: "UAH"

            val entities = body.exchangeRate.orEmpty()
                .filter { !it.currency.isNullOrBlank() }
                .map { dto ->
                    ExchangeRateEntity(
                        date = responseDate,
                        currency = dto.currency!!.trim(),
                        baseCurrency = dto.baseCurrency?.takeIf { it.isNotBlank() } ?: defaultBase,
                        purchaseRate = dto.purchaseRate,
                        saleRate = dto.saleRate
                    )
                }

            if (entities.isEmpty()) {
                return Result.success(
                    workDataOf(
                        KEY_MESSAGE to "Сервер не повернув курсів",
                        KEY_EMPTY_RESPONSE to true
                    )
                )
            }

            dao.upsertRates(entities)

            Result.success(
                workDataOf(
                    KEY_MESSAGE to "Курси за $responseDate оновлено",
                    KEY_EMPTY_RESPONSE to false
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.failure(
                workDataOf(KEY_MESSAGE to "Помилка синхронізації: ${e.message ?: "невідома помилка"}")
            )
        }
    }

    companion object {
        const val KEY_DATE = "date"
        const val KEY_MESSAGE = "message"
        const val KEY_EMPTY_RESPONSE = "empty_response"
    }
}