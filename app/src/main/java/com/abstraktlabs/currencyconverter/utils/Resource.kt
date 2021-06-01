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

package com.abstraktlabs.currencyconverter.utils

sealed class Resource<T>(val data: T?, val message: String?) {
    class Success<T>(data: T) : Resource<T>(data, null) {
        override fun toString(): String {
            return "{Instance of Resource.Success, data= ${super.data.toString()}}"
        }
    }

    class Error<T>(message: String) : Resource<T>(null, message) {
        override fun toString(): String {
            return "{Instance of Resource.Error, message= ${super.message.toString()}}"
        }
    }

    class Fallback<T>(data: T, message: String) : Resource<T>(data, message) {
        override fun toString(): String {
            return "{Instance of Resource.Fallback, data= ${super.data.toString()} , message= ${super.message.toString()}}"
        }
    }
}

sealed class CurrencyEvent(
    val rate: String?, val amountFromStr: String?,
    val amountFromFormatted: String?, val amountToStr: String?,
    val amountToFormatted: String?, val error: String?
) {

    class Success(
        rate: String, amountFromStr: String,
        amountFromFormatted: String, amountToStr: String,
        amountToFormatted: String
    ) : CurrencyEvent(
        rate, amountFromStr,
        amountFromFormatted, amountToStr,
        amountToFormatted, null
    ) {
        override fun toString(): String {
            return "Instance of CurrencyEvent.Success, " + super.toString()
        }
    }

    class Failure(error: String) : CurrencyEvent(null, null, null, null, null, error) {
        override fun toString(): String {
            return "Instance of CurrencyEvent.Failure, " + super.toString()
        }
    }

    class Fallback(
        rate: String, amountFromStr: String, amountFromFormatted: String,
        amountToStr: String, amountToFormatted: String, error: String
    ) : CurrencyEvent(
        rate, amountFromStr, amountFromFormatted, amountToStr,
        amountToFormatted, error
    ) {
        override fun toString(): String {
            return "Instance of CurrencyEvent.Fallback, " + super.toString()
        }
    }

    class Loading : CurrencyEvent(null, null, null, null, null, null) {
        override fun toString(): String {
            return "Instance of CurrencyEvent.Loading, " + super.toString()
        }
    }

    class Empty : CurrencyEvent(null, null, null, null, null, null) {
        override fun toString(): String {
            return "Instance of CurrencyEvent.Empty, " + super.toString()
        }
    }

    override fun toString(): String {
        return "rate= $rate, amountFromStr= $amountFromStr, amountFromFormatted= $amountFromFormatted, amountToStr= $amountToStr, amountToFormatted= $amountToFormatted, error= $error"
    }
}
