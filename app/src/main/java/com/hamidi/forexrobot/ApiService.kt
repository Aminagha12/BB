package com.hamidi.forexrobot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Yahoo Finance symbol mapping (no API key needed)
    private val yahooMap = mapOf(
        "XAUUSD.m"  to "XAUUSD=X",
        "JP225.std" to "^N225",
        "BTCUSD.m"  to "BTC-USD"
    )

    // Decimal precision per symbol
    val decimalPlaces = mapOf(
        "XAUUSD.m"  to 2,
        "JP225.std" to 1,
        "BTCUSD.m"  to 2
    )

    suspend fun getPriceData(symbol: String): PriceData? {
        return withContext(Dispatchers.IO) {
            val yahooSym = yahooMap[symbol] ?: return@withContext null
            // Try Yahoo Finance v8 (primary)
            fetchYahoo(yahooSym) ?: fetchYahooV7(yahooSym)
        }
    }

    private fun fetchYahoo(yahooSym: String): PriceData? {
        return try {
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$yahooSym" +
                      "?interval=15m&range=2d&includePrePost=false"
            val req = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12)")
                .addHeader("Accept", "application/json")
                .build()
            val body = client.newCall(req).execute().body?.string() ?: return null
            parseYahooV8(body)
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchYahooV7(yahooSym: String): PriceData? {
        return try {
            val url = "https://query2.finance.yahoo.com/v8/finance/chart/$yahooSym" +
                      "?interval=15m&range=3d&includePrePost=false"
            val req = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                .build()
            val body = client.newCall(req).execute().body?.string() ?: return null
            parseYahooV8(body)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseYahooV8(json: String): PriceData? {
        return try {
            val root   = JSONObject(json)
            val chart  = root.getJSONObject("chart")
            val results = chart.optJSONArray("result") ?: return null
            if (results.length() == 0) return null

            val result     = results.getJSONObject(0)
            val indicators = result.getJSONObject("indicators")
            val quoteArr   = indicators.getJSONArray("quote")
            val quote      = quoteArr.getJSONObject(0)

            val closeArr = quote.getJSONArray("close")
            val highArr  = quote.getJSONArray("high")
            val lowArr   = quote.getJSONArray("low")
            val openArr  = quote.getJSONArray("open")

            val closes = mutableListOf<Double>()
            val highs  = mutableListOf<Double>()
            val lows   = mutableListOf<Double>()
            val opens  = mutableListOf<Double>()

            for (i in 0 until closeArr.length()) {
                if (closeArr.isNull(i) || highArr.isNull(i) ||
                    lowArr.isNull(i)  || openArr.isNull(i)) continue

                val c = closeArr.getDouble(i)
                val h = highArr.getDouble(i)
                val l = lowArr.getDouble(i)
                val o = openArr.getDouble(i)

                if (c > 0 && h > 0 && l > 0 && o > 0) {
                    closes.add(c); highs.add(h); lows.add(l); opens.add(o)
                }
            }

            if (closes.size < 40) return null

            PriceData(
                closes = closes.toDoubleArray(),
                highs  = highs.toDoubleArray(),
                lows   = lows.toDoubleArray(),
                opens  = opens.toDoubleArray()
            )
        } catch (e: Exception) {
            null
        }
    }
}
