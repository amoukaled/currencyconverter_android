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

import com.abstraktlabs.currencyconverter.room.RoomRate
import java.util.Calendar

object AndroidTestData {

    val freshRate1 = RoomRate(
        "EUR",
        1.0,
        "EUR",
        (Calendar.getInstance().timeInMillis / 1000).toInt()
    )

    val freshRate2 = RoomRate(
        "USD",
        1.21,
        "EUR",
        (Calendar.getInstance().timeInMillis / 1000).toInt()
    )

    val freshZeroRate1 = RoomRate(
        "EUR",
        0.0,
        "EUR",
        (Calendar.getInstance().timeInMillis / 1000).toInt()
    )

    val freshZeroRate2 = RoomRate(
        "USD",
        0.0,
        "EUR",
        (Calendar.getInstance().timeInMillis / 1000).toInt()
    )

    val staleRate1 = RoomRate(
        "EUR",
        1.0,
        "EUR",
        (Calendar.getInstance().apply {
            clear()
            set(2021, 2, 14)
        }.timeInMillis / 1000).toInt()
    )

    val staleRate2 = RoomRate(
        "USD",
        1.21,
        "EUR",
        (Calendar.getInstance().apply {
            clear()
            set(2021, 2, 14)
        }.timeInMillis / 1000).toInt()
    )
}