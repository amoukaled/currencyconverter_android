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

package com.abstraktlabs.currencyconverter.api

import com.abstraktlabs.currencyconverter.models.CurrencyResponse
import com.abstraktlabs.currencyconverter.testResources.sampleResponse

import com.google.gson.Gson

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

import retrofit2.Response

class FakeCurrencyApi : CurrencyApi {

    var apiResult: ApiResult? = null

    override suspend fun getRates(accessKey: String): Response<CurrencyResponse> {
        return when (apiResult!!) {
            ApiResult.SUCCESS -> {
                val gson = Gson()
                val data = gson.fromJson(sampleResponse, CurrencyResponse::class.java)

                Response.success(data)
            }
            ApiResult.EXCEPTION -> {
                throw  Exception("Some exception")
            }
            ApiResult.FAILURE -> {
                Response.error(400, "Error".toResponseBody("application/json".toMediaType()))
            }
        }
    }
}

enum class ApiResult {
    SUCCESS,
    EXCEPTION,
    FAILURE
}