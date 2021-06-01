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

package com.abstraktlabs.currencyconverter.testResources

import android.content.Context
import androidx.room.Room

import com.abstraktlabs.currencyconverter.room.AppDatabase
import com.abstraktlabs.currencyconverter.utils.DispatcherProvider

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher

import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object TestAppModule {

    @Provides
    @Named("test_db")
    fun providesTestDB(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, AppDatabase::class.java, "RoomDB").build()

    @Provides
    @Named("test_dao")
    fun providesTestDao(@Named("test_db") db: AppDatabase) = db.currencyDao()

    @ExperimentalCoroutinesApi
    @Provides
    @Named("test_coroutineDispatcher")
    fun provideTestCoroutineDispatcher() = TestCoroutineDispatcher()

    @ExperimentalCoroutinesApi
    @Provides
    @Named("test_dispatcher")
    fun providesDispatcherProvider(@Named("test_coroutineDispatcher") dispatcher: TestCoroutineDispatcher) =
        object : DispatcherProvider {
            override val main: CoroutineDispatcher
                get() = dispatcher
            override val io: CoroutineDispatcher
                get() = dispatcher
            override val default: CoroutineDispatcher
                get() = dispatcher
            override val unconfined: CoroutineDispatcher
                get() = dispatcher
        }

}