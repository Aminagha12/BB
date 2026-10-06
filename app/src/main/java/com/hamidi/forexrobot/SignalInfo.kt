package com.hamidi.forexrobot

data class SignalInfo(
    val type: String,        // "BUY", "SELL", or "WAIT"
    val entry: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val macdValue: Double,
    val signalValue: Double,
    val histogram: Double,
    val symbol: String,
    val isCrossover: Boolean = false  // true = fresh crossover signal
)

data class PriceData(
    val closes: DoubleArray,
    val highs: DoubleArray,
    val lows: DoubleArray,
    val opens: DoubleArray
)
