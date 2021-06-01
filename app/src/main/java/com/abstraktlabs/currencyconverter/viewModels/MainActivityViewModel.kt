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

package com.abstraktlabs.currencyconverter.viewModels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.abstraktlabs.currencyconverter.repository.MainActivityRepository
import com.abstraktlabs.currencyconverter.utils.CurrencyEvent
import com.abstraktlabs.currencyconverter.utils.DispatcherProvider
import com.abstraktlabs.currencyconverter.utils.Resource

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val repo: MainActivityRepository,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    // StateFlows
    private val _currConversion = MutableStateFlow<CurrencyEvent>(CurrencyEvent.Empty())
    val currencyEvent: StateFlow<CurrencyEvent> = _currConversion


    fun convert(
        amountStr: String, currFrom: String,
        currTo: String, scope: CoroutineScope = viewModelScope
    ) {
        val amountFromAsDouble = amountStr.toDoubleOrNull()

        if (amountFromAsDouble == null) {
            _currConversion.value = CurrencyEvent.Failure("Invalid amount.")
            return
        }

        if (currFrom == currTo) {
            val rate = formatNumber(1.0)
            val amountFromFormatted = formatCurr(currFrom, amountFromAsDouble)
            val amountToAsString = amountFromAsDouble.toString()
            val amountToFormatted = formatCurr(currTo, amountFromAsDouble)

            _currConversion.value = CurrencyEvent.Success(
                rate, amountStr, amountFromFormatted,
                amountToAsString, amountToFormatted,
            )
            return
        }

        scope.launch(dispatcher.io) {
            _currConversion.value = CurrencyEvent.Loading()

            when (val res = repo.convert(amountFromAsDouble, currFrom, currTo)) {
                is Resource.Error -> {
                    val message = res.message ?: "An error occurred."
                    _currConversion.value =
                        CurrencyEvent.Failure(message)
                }

                is Resource.Fallback -> {
                    val message = res.message ?: "An error occurred."
                    val rate = formatNumber(repo.getRate(currFrom, currTo))
                    val amountFromFormatted = formatCurr(currFrom, amountFromAsDouble)
                    val amountTo = res.data ?: 0.0
                    val amountToAsString = amountTo.toString()
                    val amountToFormatted = formatCurr(currTo, amountTo)

                    _currConversion.value =
                        CurrencyEvent.Fallback(
                            rate, amountStr, amountFromFormatted,
                            amountToAsString, amountToFormatted, message
                        )
                }

                is Resource.Success -> {
                    val rate = formatNumber(repo.getRate(currFrom, currTo))
                    val amountFromFormatted = formatCurr(currFrom, amountFromAsDouble)
                    val amountTo = res.data ?: 0.0
                    val amountToAsString = amountTo.toString()
                    val amountToFormatted = formatCurr(currTo, amountTo)

                    _currConversion.value =
                        CurrencyEvent.Success(
                            rate, amountStr, amountFromFormatted,
                            amountToAsString, amountToFormatted,
                        )
                }
            }
        }
    }

    /**
     * Formats a number to a currency string representation.
     * Rounds on the 6th decimal point.
     */
    private fun formatCurr(curr: String, amount: Double): String {
        val format = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 6
        format.currency = Currency.getInstance(curr)
        return format.format(amount)
    }

    /**
     * Formats a number according to the locale.
     * Rounds on the 6th decimal point.
     */
    private fun formatNumber(number: Double): String {
        val format = NumberFormat.getInstance()
        format.maximumFractionDigits = 6
        return format.format(number)
    }
}