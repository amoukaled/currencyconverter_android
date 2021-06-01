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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.filters.MediumTest

import com.abstraktlabs.currencyconverter.api.ApiResult
import com.abstraktlabs.currencyconverter.api.FakeCurrencyApi
import com.abstraktlabs.currencyconverter.repository.DefaultMainActivityRepository
import com.abstraktlabs.currencyconverter.room.AppDatabase
import com.abstraktlabs.currencyconverter.room.CurrencyDao
import com.abstraktlabs.currencyconverter.testResources.invokeSuspend
import com.abstraktlabs.currencyconverter.testResources.logInstance
import com.abstraktlabs.currencyconverter.utils.CurrencyEvent
import com.abstraktlabs.currencyconverter.utils.DispatcherProvider

import com.google.common.truth.Truth.assertThat

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlin.coroutines.Continuation

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import javax.inject.Inject
import javax.inject.Named


@ExperimentalCoroutinesApi
@MediumTest
@HiltAndroidTest
class MainActivityViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    @Named("test_dispatcher")
    lateinit var dispatcher: DispatcherProvider

    private lateinit var scope: TestCoroutineScope

    @Inject
    @Named("test_db")
    lateinit var db: AppDatabase

    private lateinit var repo: DefaultMainActivityRepository
    private lateinit var api: FakeCurrencyApi
    private lateinit var dao: CurrencyDao
    private lateinit var viewModel: MainActivityViewModel

    @Before
    fun setUp() {
        hiltRule.inject()

        // Doesn't matter which dispatcher
        // All props refer to the same TestDispatcher
        scope = TestCoroutineScope(dispatcher.main)

        dao = db.currencyDao()
        api = FakeCurrencyApi()
        repo = DefaultMainActivityRepository(dao, api)
        viewModel = MainActivityViewModel(repo, dispatcher)
    }

    @After
    fun tearDown() {
        db.clearAllTables()
        db.close()
    }

    /**
     * Tests the currency formatter.
     * It should round on the 7th decimal point.
     */
    @Test
    fun formatCurr() {
        api.apiResult = ApiResult.SUCCESS
        val method = viewModel.javaClass.getDeclaredMethod(
            "formatCurr",
            String::class.java,
            Double::class.java
        )
        method.isAccessible = true

        val result1 = method.invoke(viewModel, "USD", 100)
        assertThat(result1.also(::logInstance)).isEqualTo("$100.00")

        val result2 = method.invoke(viewModel, "USD", 100.99)
        assertThat(result2.also(::logInstance)).isEqualTo("$100.99")

        val result3 = method.invoke(viewModel, "USD", 100.99999)
        assertThat(result3.also(::logInstance)).isEqualTo("$100.99999")

        val result4 = method.invoke(viewModel, "USD", 100.999999)
        assertThat(result4.also(::logInstance)).isEqualTo("$100.999999")

        val result5 = method.invoke(viewModel, "USD", 100.9999999)
        assertThat(result5.also(::logInstance)).isEqualTo("$101.00")
    }

    /**
     * Converts an amount to the same currency.
     */
    @Test
    fun convertWithSuccessReturnValueOfIdenticalCurr() {
        api.apiResult = ApiResult.SUCCESS

        var event: CurrencyEvent? = null
        val job = scope.launch {
            viewModel.currencyEvent.collect { flow ->
                event = flow
            }
        }

        assertThat(event).isNotNull()
        assertThat(event.also(::logInstance)).isInstanceOf(CurrencyEvent.Empty::class.java)

        viewModel.convert("100", "EUR", "EUR", scope)

        assertThat(event.also(::logInstance)).isInstanceOf(CurrencyEvent.Success::class.java)
        job.cancel()
    }


    /**
     * Converts 100 EUR to USD.
     */
    @Test
    fun convertWithSuccessReturnValue() {
        api.apiResult = ApiResult.SUCCESS

        var event: CurrencyEvent? = null
        val job = scope.launch {
            viewModel.currencyEvent.collect { flow ->
                event = flow
            }
        }

        assertThat(event).isNotNull()
        assertThat(event.also(::logInstance)).isInstanceOf(CurrencyEvent.Empty::class.java)


        viewModel.convert("100", "EUR", "USD", scope)
        assertThat(event.also(::logInstance)).isInstanceOf(CurrencyEvent.Success::class.java)
        assertThat(event?.rate).isEqualTo("1.217286")
        assertThat(event?.amountFromStr).isEqualTo("100")
        assertThat(event?.amountFromFormatted).isEqualTo("€100.00")
        assertThat(event?.amountToStr).isEqualTo("121.7286")
        assertThat(event?.amountToFormatted).isEqualTo("$121.7286")
        assertThat(event?.error).isNull()

        job.cancel()
    }


    /**
     * Converts 100 EUR to USD.
     * Must return Fallback
     */
    @Test
    fun convertWithFallbackReturnValue() {

        api.apiResult = ApiResult.SUCCESS

        scope.runBlockingTest {
            repo.javaClass.getDeclaredMethod("getRatesAndSave", Continuation::class.java).apply {
                isAccessible = true
                invokeSuspend(repo)
            }
            assertThat(dao.getAllRates().also(::logInstance)).isNotEmpty()
        }


        api.apiResult = ApiResult.FAILURE
        var event: CurrencyEvent? = null
        val job = scope.launch {
            viewModel.currencyEvent.collect { flow ->
                event = flow
            }
        }

        assertThat(event).isNotNull()
        assertThat(event.also(::logInstance)).isInstanceOf(CurrencyEvent.Empty::class.java)

        scope.runBlockingTest {
            viewModel.convert("100", "EUR", "USD", scope)
            assertThat(event.also(::logInstance)).isInstanceOf(CurrencyEvent.Fallback::class.java)
            assertThat(event?.rate).isEqualTo("1.217286")
            assertThat(event?.amountFromStr).isEqualTo("100")
            assertThat(event?.amountFromFormatted).isEqualTo("€100.00")
            assertThat(event?.amountToStr).isEqualTo("121.7286")
            assertThat(event?.amountToFormatted).isEqualTo("$121.7286")
            assertThat(event?.error).isNotNull()
        }
        job.cancel()
    }

    /**
     * Converts an 100 EUR to USD.
     * Must return Error.
     */
    @Test
    fun convertWithErrorReturnValue() {

        scope.runBlockingTest {
            assertThat(dao.getAllRates().also(::logInstance)).isEmpty()
        }

        api.apiResult = ApiResult.FAILURE
        var event: CurrencyEvent? = null
        val job = scope.launch {
            viewModel.currencyEvent.collect { flow ->
                event = flow
            }
        }

        assertThat(event).isNotNull()
        assertThat(event.also(::logInstance)).isInstanceOf(CurrencyEvent.Empty::class.java)

        scope.runBlockingTest {
            viewModel.convert("100", "EUR", "USD", scope)
            assertThat(event.also(::logInstance)).isInstanceOf(CurrencyEvent.Failure::class.java)
            assertThat(event?.rate).isNull()
            assertThat(event?.amountFromStr).isNull()
            assertThat(event?.amountFromFormatted).isNull()
            assertThat(event?.amountToStr).isNull()
            assertThat(event?.amountToFormatted).isNull()
            assertThat(event?.error).isNotNull()
        }
        job.cancel()
    }

    /**
     * Tests the number formatter.
     * It should round on the 7th decimal point.
     */
    @Test
    fun formatNumber() {
        api.apiResult = ApiResult.SUCCESS
        val method = viewModel.javaClass.getDeclaredMethod(
            "formatNumber",
            Double::class.java
        )
        method.isAccessible = true

        val result1 = method.invoke(viewModel, 100)
        assertThat(result1.also(::logInstance)).isEqualTo("100")

        val result2 = method.invoke(viewModel, 100.99)
        assertThat(result2.also(::logInstance)).isEqualTo("100.99")

        val result3 = method.invoke(viewModel, 100.99999)
        assertThat(result3.also(::logInstance)).isEqualTo("100.99999")

        val result4 = method.invoke(viewModel, 100.999999)
        assertThat(result4.also(::logInstance)).isEqualTo("100.999999")

        val result5 = method.invoke(viewModel, 100.9999999)
        assertThat(result5.also(::logInstance)).isEqualTo("101")

        val result6 = method.invoke(viewModel, (1.217286 / 1835.545148)) // 0.000663
        assertThat(result6.also(::logInstance)).isEqualTo("0.000663")
    }


}