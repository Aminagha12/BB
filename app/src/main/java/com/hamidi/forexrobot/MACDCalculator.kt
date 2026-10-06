package com.hamidi.forexrobot

object MACDCalculator {

    /**
     * Calculate Exponential Moving Average (EMA)
     */
    private fun ema(prices: DoubleArray, period: Int): DoubleArray {
        val result = DoubleArray(prices.size) { 0.0 }
        if (prices.size < period) return result

        val k = 2.0 / (period + 1.0)

        // First value = SMA of first `period` bars
        var sum = 0.0
        for (i in 0 until period) sum += prices[i]
        result[period - 1] = sum / period

        for (i in period until prices.size) {
            result[i] = prices[i] * k + result[i - 1] * (1.0 - k)
        }
        return result
    }

    /**
     * Full MACD calculation (12, 26, 9) — standard settings
     * Returns Triple<macdLine, signalLine, histogram>
     */
    fun calculate(
        closes: DoubleArray,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Triple<DoubleArray, DoubleArray, DoubleArray> {

        val fastEMA = ema(closes, fastPeriod)
        val slowEMA = ema(closes, slowPeriod)

        // MACD Line = Fast EMA - Slow EMA (valid only after slowPeriod - 1)
        val macdLine = DoubleArray(closes.size) { i ->
            if (slowEMA[i] == 0.0) 0.0 else fastEMA[i] - slowEMA[i]
        }

        // Signal Line = EMA(macdLine, 9) — only over valid portion
        val validStart = slowPeriod - 1
        val macdValid = macdLine.copyOfRange(validStart, macdLine.size)
        val signalValid = ema(macdValid, signalPeriod)

        val signalLine = DoubleArray(closes.size) { i ->
            val idx = i - validStart
            if (idx < 0 || signalValid[idx] == 0.0) 0.0 else signalValid[idx]
        }

        val histogram = DoubleArray(closes.size) { i ->
            if (macdLine[i] == 0.0 || signalLine[i] == 0.0) 0.0
            else macdLine[i] - signalLine[i]
        }

        return Triple(macdLine, signalLine, histogram)
    }

    /**
     * Generate trading signal based on MACD crossover.
     * BUY  → MACD line crosses ABOVE Signal line (bullish crossover)
     * SELL → MACD line crosses BELOW Signal line (bearish crossover)
     * If no fresh crossover, show current trend direction.
     */
    fun generateSignal(
        closes: DoubleArray,
        highs: DoubleArray,
        lows: DoubleArray,
        symbol: String
    ): SignalInfo {

        val minRequired = 26 + 9 + 2   // 37 bars minimum
        if (closes.size < minRequired) {
            return SignalInfo("WAIT", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, symbol)
        }

        val (macdLine, signalLine, histogram) = calculate(closes)
        val size = closes.size
        val last = size - 1
        val prev = size - 2

        val currMACD = macdLine[last]
        val prevMACD = macdLine[prev]
        val currSig  = signalLine[last]
        val prevSig  = signalLine[prev]

        // Not enough valid data yet
        if (currMACD == 0.0 || currSig == 0.0 || prevMACD == 0.0 || prevSig == 0.0) {
            return SignalInfo("WAIT", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, symbol)
        }

        val entry   = closes[last]
        val lookback = minOf(10, size)

        // ── BULLISH CROSSOVER ── MACD crosses above Signal
        val isBullishCross = prevMACD <= prevSig && currMACD > currSig
        // ── BEARISH CROSSOVER ── MACD crosses below Signal
        val isBearishCross = prevMACD >= prevSig && currMACD < currSig

        return when {
            isBullishCross || currMACD > currSig -> {
                val sl  = lows.takeLast(lookback).minOrNull() ?: (entry * 0.995)
                val dist = (entry - sl).coerceAtLeast(entry * 0.001)
                val tp  = entry + dist * 3.0
                SignalInfo(
                    type        = "BUY",
                    entry       = entry,
                    stopLoss    = sl,
                    takeProfit  = tp,
                    macdValue   = currMACD,
                    signalValue = currSig,
                    histogram   = histogram[last],
                    symbol      = symbol,
                    isCrossover = isBullishCross
                )
            }
            isBearishCross || currMACD < currSig -> {
                val sl  = highs.takeLast(lookback).maxOrNull() ?: (entry * 1.005)
                val dist = (sl - entry).coerceAtLeast(entry * 0.001)
                val tp  = entry - dist * 3.0
                SignalInfo(
                    type        = "SELL",
                    entry       = entry,
                    stopLoss    = sl,
                    takeProfit  = tp,
                    macdValue   = currMACD,
                    signalValue = currSig,
                    histogram   = histogram[last],
                    symbol      = symbol,
                    isCrossover = isBearishCross
                )
            }
            else -> SignalInfo("WAIT", entry, 0.0, 0.0, currMACD, currSig, histogram[last], symbol)
        }
    }
}
