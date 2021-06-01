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

package com.abstraktlabs.currencyconverter

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope

import com.abstraktlabs.currencyconverter.databinding.ActivityMainBinding
import com.abstraktlabs.currencyconverter.utils.CurrencyEvent
import com.abstraktlabs.currencyconverter.viewModels.MainActivityViewModel
import com.google.android.gms.ads.AdRequest

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val req = AdRequest.Builder().build()
        binding.bannerAd.loadAd(req)

        val model: MainActivityViewModel by viewModels()

        lifecycleScope.launchWhenStarted {
            model.currencyEvent.collect {
                setDataToViews(it)
            }
        }

        binding.convertButton.setOnClickListener {
            if (model.currencyEvent.value !is CurrencyEvent.Loading) {
                binding.amountFromET.text?.toString().also {
                    if (it != null) {
                        val currFrom: String = binding.currencyFromSP.selectedItem.toString()
                        val currTo: String = binding.currencyToSP.selectedItem.toString()
                        model.convert(it, currFrom, currTo)
                    }
                }
            }
        }

    }

    override fun onDestroy() {
        binding.bannerAd.destroy()
        super.onDestroy()
    }

    private fun setDataToViews(event: CurrencyEvent) {
        val symbol =
            binding.currencyFromSP.selectedItem?.toString() + binding.currencyToSP.selectedItem?.toString()
        binding.apply {
            exchangeRateTV.text = event.rate ?: "-"
            formattedFromTV.text = event.amountFromFormatted ?: "-"
            formattedToTV.text = event.amountToFormatted ?: "-"
            errorTV.text = event.error ?: ""
        }

        when (event) {
            is CurrencyEvent.Empty -> {
                binding.apply {
                    amountFromET.text?.clear()
                    currencyFromSP.setSelection(0)
                    currencyToSP.setSelection(0)
                    rateSymbol.text = getString(R.string.rate)
                }
                enableInteractions()
            }
            is CurrencyEvent.Failure -> {
                binding.rateSymbol.text = getString(R.string.rate)
                enableInteractions()
            }
            is CurrencyEvent.Fallback -> {
                binding.rateSymbol.text = symbol
                enableInteractions()
            }
            is CurrencyEvent.Loading -> {
                binding.rateSymbol.text = getString(R.string.rate)
                disableInteractions()
            }
            is CurrencyEvent.Success -> {
                binding.rateSymbol.text = symbol
                enableInteractions()
            }
        }
    }

    private fun enableInteractions() {
        binding.apply {
            convertButton.isEnabled = true
            convertButton.background = ContextCompat.getDrawable(
                applicationContext,
                R.drawable.circular_button
            )
            convertButtonPB.isGone = true
            convertButtonTV.isVisible = true
            amountFromET.isEnabled = true
        }
    }

    private fun disableInteractions() {
        binding.apply {
            convertButton.isEnabled = false
            convertButton.background = ContextCompat.getDrawable(
                applicationContext,
                R.drawable.disabled_button
            )
            convertButtonPB.isVisible = true
            convertButtonTV.isGone = true
            amountFromET.isEnabled = false
        }

    }

}
