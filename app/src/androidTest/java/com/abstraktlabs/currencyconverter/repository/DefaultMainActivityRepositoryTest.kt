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

import androidx.test.filters.MediumTest

import com.abstraktlabs.currencyconverter.testResources.AndroidTestData
import com.abstraktlabs.currencyconverter.api.ApiResult
import com.abstraktlabs.currencyconverter.api.FakeCurrencyApi
import com.abstraktlabs.currencyconverter.room.AppDatabase
import com.abstraktlabs.currencyconverter.room.CurrencyDao
import com.abstraktlabs.currencyconverter.testResources.invokeSuspend
import com.abstraktlabs.currencyconverter.testResources.logInstance
import com.abstraktlabs.currencyconverter.utils.Resource

import com.google.common.truth.Truth.assertThat

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import java.util.*
import javax.inject.Inject
import javax.inject.Named

import kotlin.coroutines.Continuation
import kotlinx.coroutines.runBlocking

@MediumTest
@HiltAndroidTest
class DefaultMainActivityRepositoryTest {


    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    @Named("test_db")
    lateinit var db: AppDatabase

    private lateinit var repo: DefaultMainActivityRepository

    private lateinit var api: FakeCurrencyApi

    private lateinit var dao: CurrencyDao

    @Before
    fun setUp() {
        hiltRule.inject()
        dao = db.currencyDao()
        api = FakeCurrencyApi()
        repo = DefaultMainActivityRepository(dao, api)
    }

    @After
    fun tearDown() {
        db.clearAllTables()
        db.close()
    }


    /**
     * Validates the timestamp.
     * Should return false; the timestamp given is not within the
     * timeframe.
     */
    @Test
    fun shouldCheckTimestampInValid() {
        val method = repo.javaClass.getDeclaredMethod("isTimestampValid", Int::class.java)
        method.isAccessible = true
        val param = (Calendar.getInstance().apply {
            clear()
            set(2021, 3, 20)
        }.timeInMillis / 1000).toInt()
        val isValid = method.invoke(repo, param) as Boolean

        assertThat(isValid).isFalse()
    }


    /**
     * Validates the timestamp.
     * Should return true; the timestamp given is within the
     * timeframe.
     */
    @Test
    fun shouldCheckTimestampValid() {
        val method = repo.javaClass.getDeclaredMethod("isTimestampValid", Int::class.java)
        method.isAccessible = true
        val param = (Calendar.getInstance().timeInMillis / 1000).toInt()
        val isValid = method.invoke(repo, param) as Boolean

        assertThat(isValid).isTrue()
    }

    /**
     * Gets the rates and check db for saving.
     */
    @Test
    fun getTheRatesAndSaveSuccessfully() {
        api.apiResult = ApiResult.SUCCESS
        runBlocking {
            val method =
                repo::class.java.getDeclaredMethod("getRatesAndSave", Continuation::class.java)
            method.isAccessible = true

            assertThat(dao.getAllRates().also(::logInstance)).isEmpty()

            method.invokeSuspend(repo)

            val saved = dao.getAllRates()

            assertThat(saved.also(::logInstance)).isNotEmpty()
        }
    }

    /**
     * Gets the converted amount.
     * Depending on the response sample, the exchange rate
     * should return amount between 119 and 122. (121.72860000000001)
     */
    @Test
    fun getTheConvertedAmount() {
        api.apiResult = ApiResult.SUCCESS
        runBlocking {
            // Checking for empty db
            assertThat(dao.getAllRates().also(::logInstance)).isEmpty()

            // Updating local db; getting the rates
            repo.javaClass.getDeclaredMethod("getRatesAndSave", Continuation::class.java).apply {
                isAccessible = true
                invokeSuspend(repo)
            }

            // Checking if the rates are successfully stored
            assertThat(dao.getAllRates()).isNotEmpty()

            // Getting the calculate method
            val method = repo.javaClass.getDeclaredMethod(
                "calculateAmount",
                Double::class.java,
                String::class.java,
                String::class.java,
                Continuation::class.java
            )
            method.isAccessible = true

            val result = method.invokeSuspend(repo, 100, "EUR", "USD") as Double

            // Assertions
            assertThat(result.also(::logInstance)).isNonZero()
            assertThat(result).isGreaterThan(119)
            assertThat(result).isLessThan(122)
            assertThat(result).isEqualTo(121.72860)
        }
    }

    /**
     * Gets the converted amount.
     * Should return the same amount since the FROM and TO currencies
     * are the same.
     */
    @Test
    fun getTheConvertedAmountWithSameCurr() {
        api.apiResult = ApiResult.SUCCESS
        runBlocking {
            // Checking for empty db
            assertThat(dao.getAllRates().also(::logInstance)).isEmpty()

            // Updating local db; getting the rates
            repo.javaClass.getDeclaredMethod("getRatesAndSave", Continuation::class.java).apply {
                isAccessible = true
                invokeSuspend(repo)
            }

            // Checking if the rates are successfully stored
            assertThat(dao.getAllRates()).isNotEmpty()

            // Getting the calculate method
            val method = repo.javaClass.getDeclaredMethod(
                "calculateAmount",
                Double::class.java,
                String::class.java,
                String::class.java,
                Continuation::class.java
            )
            method.isAccessible = true

            val result = method.invokeSuspend(repo, 100, "EUR", "EUR") as Double

            // Assertions
            assertThat(result.also(::logInstance)).isNonZero()
            assertThat(result).isEqualTo(100)
        }
    }

    /**
     * Fires the convert function with the following setup:
     * - Only two RoomRate instances; EUR and USD
     * - New valid timestamps within the timeframe
     * - Converts the amount and expects a Success Resource with data = 121
     */
    @Test
    fun convertWithLocalDataAndValidTimeStamp() {
        runBlocking {
            // Check db empty first
            assertThat(dao.getAllRates()).isEmpty()

            // Adding the test data to ensure a new timestamp
            dao.insertRoomRate(AndroidTestData.freshRate1) // EUR
            dao.insertRoomRate(AndroidTestData.freshRate2) // USD

            val timestampMethod =
                repo.javaClass.getDeclaredMethod("isTimestampValid", Int::class.java)
            timestampMethod.isAccessible = true

            // Checking timestamp validity
            val timestamp1 =
                timestampMethod.invoke(
                    repo,
                    dao.getRateByCurrencyName("EUR")!!.timestamp
                ) as Boolean
            assertThat(timestamp1.also(::logInstance)).isTrue()

            val timestamp2 =
                timestampMethod.invoke(
                    repo,
                    dao.getRateByCurrencyName("USD")!!.timestamp
                ) as Boolean
            assertThat(timestamp2.also(::logInstance)).isTrue()

            val convert = repo.convert(100.0, "EUR", "USD")

            assertThat(convert.also(::logInstance)).isInstanceOf(Resource.Success::class.java)
            assertThat(convert.data).isEqualTo(121)

        }
    }


    /**
     * Fires the convert function with the following setup:
     * - Only two RoomRate instances; EUR and USD
     * - Invalid timestamps -> gets the rates
     * - Converts the amount and expects a Success Resource with data = 121.72860000000001
     */
    @Test
    fun convertWithLocalDataAndInValidTimeStamp() {
        api.apiResult = ApiResult.SUCCESS
        runBlocking {
            // Check db empty first
            assertThat(dao.getAllRates()).isEmpty()

            // Adding the test data to ensure a new timestamp
            dao.insertRoomRate(AndroidTestData.staleRate1) // EUR
            dao.insertRoomRate(AndroidTestData.staleRate2) // USD

            val timestampMethod =
                repo.javaClass.getDeclaredMethod("isTimestampValid", Int::class.java)
            timestampMethod.isAccessible = true

            // Checking timestamp validity
            val timestamp1 =
                timestampMethod.invoke(
                    repo,
                    dao.getRateByCurrencyName("EUR")!!.timestamp
                ) as Boolean
            assertThat(timestamp1.also(::logInstance)).isFalse()

            val timestamp2 =
                timestampMethod.invoke(
                    repo,
                    dao.getRateByCurrencyName("USD")!!.timestamp
                ) as Boolean
            assertThat(timestamp2.also(::logInstance)).isFalse()

            val convert = repo.convert(100.0, "EUR", "USD")

            assertThat(convert.also(::logInstance)).isInstanceOf(Resource.Success::class.java)
            assertThat(convert.data).isEqualTo(121.72860)
        }
    }


    /**
     * Fires the convert function with the following setup:
     * - Only two RoomRate instances; EUR and USD
     * - Invalid timestamps
     * - Tries to get the rates and fails; returns a Fallback Resource
     */
    @Test
    fun convertWithLocalDataAndInValidTimeStampAndThrowException() {
        api.apiResult = ApiResult.EXCEPTION
        runBlocking {
            // Check db empty first
            assertThat(dao.getAllRates()).isEmpty()

            // Adding the test data to ensure a new timestamp
            dao.insertRoomRate(AndroidTestData.staleRate1) // EUR
            dao.insertRoomRate(AndroidTestData.staleRate2) // USD

            val timestampMethod =
                repo.javaClass.getDeclaredMethod("isTimestampValid", Int::class.java)
            timestampMethod.isAccessible = true

            // Checking timestamp validity
            val timestamp1 =
                timestampMethod.invoke(
                    repo,
                    dao.getRateByCurrencyName("EUR")!!.timestamp
                ) as Boolean
            assertThat(timestamp1.also(::logInstance)).isFalse()

            val timestamp2 =
                timestampMethod.invoke(
                    repo,
                    dao.getRateByCurrencyName("USD")!!.timestamp
                ) as Boolean
            assertThat(timestamp2.also(::logInstance)).isFalse()

            val convert = repo.convert(100.0, "EUR", "USD")

            assertThat(convert.also(::logInstance)).isInstanceOf(Resource.Fallback::class.java)
            assertThat(convert.data).isEqualTo(121)
            assertThat(convert.message.also(::logInstance)).isNotNull()
        }
    }

    /**
     * Fires the convert function with the following setup:
     * - Empty local DB
     * - Tries to get the rates and succeeds.
     * - Checks the DB for saving the new rates.
     * - Returns a Success Resource
     *   with data = 121.72860000000001
     */
    @Test
    fun convertWithLocalDataEmptyAndGetsRatesAndConvertsSuccessfully() {
        api.apiResult = ApiResult.SUCCESS
        runBlocking {
            // Make sure the db is clear
            assertThat(dao.getAllRates()).isEmpty()

            val result = repo.convert(100.0, "EUR", "USD")

            assertThat(dao.getAllRates()).isNotEmpty()
            assertThat(result.also(::logInstance)).isInstanceOf(Resource.Success::class.java)
            assertThat(result.data).isEqualTo(121.72860)
        }
    }

    /**
     * Fires the convert function with the following setup:
     * - Empty local DB
     * - Tries to get the rates and fails.
     * - Checks the DB for not saving anything.
     * - Returns an Error Success Resource.
     */
    @Test
    fun convertWithLocalDataEmptyAndGetsRatesAndThrowException() {
        api.apiResult = ApiResult.EXCEPTION
        runBlocking {
            // Make sure the db is clear
            assertThat(dao.getAllRates()).isEmpty()

            val result = repo.convert(100.0, "EUR", "USD")

            assertThat(dao.getAllRates()).isEmpty()
            assertThat(result.also(::logInstance)).isInstanceOf(Resource.Error::class.java)
            assertThat(result.data).isNull()
            assertThat(result.message).isNotNull()
        }
    }

    /**
     * Gets the rate for multiple currencies.
     */
    @Test
    fun getRateSuccessfully() {
        api.apiResult = ApiResult.SUCCESS
        runBlocking {
            // Make sure db empty
            assertThat(dao.getAllRates()).isEmpty()

            repo.javaClass.getDeclaredMethod("getRatesAndSave", Continuation::class.java).apply {
                isAccessible = true
                invokeSuspend(repo)
            }

            // Check DB
            assertThat(dao.getAllRates()).isNotEmpty()

            // Assertions
            val rate1 = repo.getRate("EUR", "USD")
            assertThat(rate1.also(::logInstance)).isEqualTo(1.217286)

            val rate2 = repo.getRate("USD", "EUR")
            assertThat(rate2.also(::logInstance)).isEqualTo(0.821499)

            val rate3 = repo.getRate("USD", "GBP")
            assertThat(rate3.also(::logInstance)).isEqualTo(0.706704)

        }
    }

    /**
     * Tries to get the rate and fails.
     * Should return 0.
     */
    @Test
    fun getRateUnsuccessfully() {
        api.apiResult = ApiResult.SUCCESS
        runBlocking {
            // Make sure db empty
            assertThat(dao.getAllRates()).isEmpty()

            // Assertions
            val rate1 = repo.getRate("EUR", "USD")
            assertThat(rate1.also(::logInstance)).isZero()

            val rate2 = repo.getRate("USD", "EUR")
            assertThat(rate2.also(::logInstance)).isZero()

            val rate3 = repo.getRate("USD", "GBP")
            assertThat(rate3.also(::logInstance)).isZero()

        }
    }

    /**
     * Tries to get the rate and fails due to
     * a saved rate of 0 on fromRate.
     * Should return 0.
     */
    @Test
    fun getRateZero() {
        api.apiResult = ApiResult.SUCCESS
        runBlocking {
            // Make sure db empty
            assertThat(dao.getAllRates()).isEmpty()

            dao.insertRoomRate(AndroidTestData.freshZeroRate1)
            dao.insertRoomRate(AndroidTestData.freshZeroRate2)

            val result = AndroidTestData.freshZeroRate1.rate / AndroidTestData.freshZeroRate2.rate
            assertThat(result).isNaN()

            // Assertions
            val rate1 = repo.getRate("EUR", "USD")
            assertThat(rate1.also(::logInstance)).isZero()

            val rate2 = repo.getRate("USD", "EUR")
            assertThat(rate2.also(::logInstance)).isZero()

            val rate3 = repo.getRate("USD", "GBP")
            assertThat(rate3.also(::logInstance)).isZero()
        }
    }


}