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

package com.abstraktlabs.currencyconverter.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM roomrate")
    suspend fun getAllRates(): List<RoomRate>

    @Query("SELECT * FROM roomrate WHERE currency IN (:currency)")
    suspend fun getRateByCurrencyName(currency: String): RoomRate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomRates(vararg roomRate: RoomRate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomRate(roomRate: RoomRate)
}