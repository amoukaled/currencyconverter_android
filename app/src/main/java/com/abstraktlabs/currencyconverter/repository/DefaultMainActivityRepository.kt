/* Copyright (C) 2021  Ali Moukaled
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.abstraktlabs.currencyconverter.repository

import com.abstraktlabs.currencyconverter.BuildConfig
import com.abstraktlabs.currencyconverter.api.CurrencyApi
import com.abstraktlabs.currencyconverter.room.CurrencyDao
import com.abstraktlabs.currencyconverter.room.RoomRate
import com.abstraktlabs.currencyconverter.utils.Resource
import com.abstraktlabs.currencyconverter.utils.roundTo

import java.lang.Exception
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class DefaultMainActivityRepository
@Inject constructor(private val currencyDao: CurrencyDao, private val api: CurrencyApi) :
    MainActivityRepository {


    /**
     * Converts currency to another.
     * Checks saved cache:
     *   1.a- if data exists:
     *       1.a.a- if timestamp valid -> calc
     *       1.a.b- if timestamp invalid -> Get rates from api and calc; return outdated data on network error.
     *
     *   1.b- if data doesn't exist:
     *       1.b.a- get data from api
     *       1.b.b- return calculated rate or error on exception
     */
    override suspend fun convert(
        amountFrom: Double, currFrom: String, currTo: String
    ): Resource<Double> {
        // Checking the cache
        val fromRate = currencyDao.getRateByCurrencyName(currFrom)
        val toRate = currencyDao.getRateByCurrencyName(currTo)

        if (fromRate != null && toRate != null) {
            // Check the timestamp limit
            return (isTimestampValid(fromRate.timestamp)).run {
                return@run if (this) {
                    Resource.Success(calculateAmount(amountFrom, currFrom, currTo))
                } else {
                    try {
                        getRatesAndSave()
                        Resource.Success(calculateAmount(amountFrom, currFrom, currTo))
                    } catch (e: Exception) {
                        Resource.Fallback(
                            calculateAmount(amountFrom, currFrom, currTo),
                            "An error occurred. Using outdated rates."
                        )
                    }
                }
            }
        } else {
            return try {
                getRatesAndSave()
                val amount = calculateAmount(amountFrom, currFrom, currTo)
                Resource.Success(amount)
            } catch (e: Exception) {
                Resource.Error("An error occurred.")
            }
        }
    }


    /**
     * Checks if the timestamp given is within 48hrs timeframe.
     */
    private fun isTimestampValid(timestamp: Int): Boolean {
        val now = Calendar.getInstance().timeInMillis / 1000
        val elapsed = now - timestamp

        return elapsed < 172800
    }

    /**
     * Gets the rates and saves them to the DB.
     * Throws an exception if the response is not successful.
     */
    private suspend fun getRatesAndSave() {
        val res = api.getRates(BuildConfig.API_KEY)
        if (res.isSuccessful) {
            res.body()?.let {
                for (rate in it.rates.javaClass.declaredFields) {
                    val rateString = rate.toGenericString().split(".").last()
                    currencyDao.insertRoomRate(
                        RoomRate(
                            rateString.toUpperCase(Locale.ROOT),
                            rate.getDouble(it.rates),
                            it.base,
                            it.timestamp
                        )
                    )
                }
            }
        } else {
            throw Exception("Something went wrong.")
        }
    }


    /**
     * Gets the rates from the DB and converts the amount to the desired
     * currency.
     */
    private suspend fun calculateAmount(
        amountFrom: Double, currFrom: String, currTo: String
    ): Double {
        val fromRate = currencyDao.getRateByCurrencyName(currFrom)
        val toRate = currencyDao.getRateByCurrencyName(currTo)

        if (fromRate != null && toRate != null && fromRate.rate != 0.0) {
            val amount = (amountFrom * toRate.rate) / fromRate.rate
            return amount.roundTo(6)
        }
        return 0.0
    }

    /**
     * Gets the rate.
     */
    override suspend fun getRate(currFrom: String, currTo: String): Double {
        val fromRate = currencyDao.getRateByCurrencyName(currFrom)
        val toRate = currencyDao.getRateByCurrencyName(currTo)

        //  -->                               to avoid dividing by zero
        if (fromRate != null && toRate != null && fromRate.rate != 0.0) {
            return (toRate.rate / fromRate.rate).roundTo(6)
        }

        return 0.0
    }
}