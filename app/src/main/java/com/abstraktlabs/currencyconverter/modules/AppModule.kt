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

package com.abstraktlabs.currencyconverter.modules

import android.content.Context
import androidx.room.Room

import com.abstraktlabs.currencyconverter.BuildConfig
import com.abstraktlabs.currencyconverter.api.CurrencyApi
import com.abstraktlabs.currencyconverter.repository.DefaultMainActivityRepository
import com.abstraktlabs.currencyconverter.repository.MainActivityRepository
import com.abstraktlabs.currencyconverter.room.AppDatabase
import com.abstraktlabs.currencyconverter.room.CurrencyDao
import com.abstraktlabs.currencyconverter.utils.DispatcherProvider

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providesAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "RoomRate").build()

    @Provides
    @Singleton
    fun providesRoomRateDao(db: AppDatabase): CurrencyDao = db.currencyDao()

    @Provides
    @Singleton
    fun providesCurrencyApi(): CurrencyApi =
        Retrofit.Builder().baseUrl(BuildConfig.API_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(CurrencyApi::class.java)

    @Provides
    @Singleton
    fun providesMainActivityRepository(dao: CurrencyDao, api: CurrencyApi): MainActivityRepository =
        DefaultMainActivityRepository(dao, api)

    @Provides
    @Singleton
    fun providesDispatcherProvider() = object : DispatcherProvider {
        override val main: CoroutineDispatcher
            get() = Dispatchers.Main
        override val io: CoroutineDispatcher
            get() = Dispatchers.IO
        override val default: CoroutineDispatcher
            get() = Dispatchers.Default
        override val unconfined: CoroutineDispatcher
            get() = Dispatchers.Unconfined
    }
}