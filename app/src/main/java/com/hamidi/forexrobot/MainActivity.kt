package com.hamidi.forexrobot

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val SYMBOLS = listOf("XAUUSD.m", "JP225.std", "BTCUSD.m")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { loadAllSignals() }
        loadAllSignals()
    }

    private fun loadAllSignals() {
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        val tvStatus   = findViewById<TextView>(R.id.tvStatus)

        btnRefresh.isEnabled = false
        btnRefresh.text = "⏳  Loading..."
        tvStatus.text = "🔄  Fetching live data..."
        tvStatus.visibility = View.VISIBLE

        lifecycleScope.launch {
            var ok = 0
            for (sym in SYMBOLS) {
                try {
                    val data = ApiService.getPriceData(sym)
                    if (data != null) {
                        val signal = MACDCalculator.generateSignal(
                            data.closes, data.highs, data.lows, sym
                        )
                        updateCard(sym, signal)
                        ok++
                    } else {
                        setCardError(sym, "⚠  No Data")
                    }
                } catch (e: Exception) {
                    setCardError(sym, "⚠  Error")
                }
            }

            btnRefresh.isEnabled = true
            btnRefresh.text = "🔄  Refresh Signals"
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            tvStatus.text = if (ok > 0) "✅  Updated at $time" else "❌  Check internet connection"
        }
    }

    private fun updateCard(symbol: String, sig: SignalInfo) {
        val ids = getIds(symbol) ?: return
        val dp  = ApiService.decimalPlaces[symbol] ?: 2
        val fmt = DecimalFormat("#,##0.${"0".repeat(dp)}")

        val tvSignal = findViewById<TextView>(ids[0])
        val tvEntry  = findViewById<TextView>(ids[1])
        val tvSL     = findViewById<TextView>(ids[2])
        val tvTP     = findViewById<TextView>(ids[3])
        val tvMacd   = findViewById<TextView>(ids[4])
        val tvCross  = findViewById<TextView>(ids[5])

        when (sig.type) {
            "BUY" -> {
                tvSignal.text = "▲   B U Y"
                tvSignal.setBackgroundResource(R.drawable.bg_buy_signal)
                tvSignal.setTextColor(ContextCompat.getColor(this, R.color.buyGreen))
            }
            "SELL" -> {
                tvSignal.text = "▼   S E L L"
                tvSignal.setBackgroundResource(R.drawable.bg_sell_signal)
                tvSignal.setTextColor(ContextCompat.getColor(this, R.color.sellRed))
            }
            else -> {
                tvSignal.text = "—   W A I T"
                tvSignal.setBackgroundResource(R.drawable.bg_wait_signal)
                tvSignal.setTextColor(ContextCompat.getColor(this, R.color.textGray))
            }
        }

        tvEntry.text = "📍  Entry:                    ${fmt.format(sig.entry)}"
        tvSL.text    = "🔴  Stop Loss:            ${fmt.format(sig.stopLoss)}"
        tvTP.text    = "🟢  Take Profit (1:3): ${fmt.format(sig.takeProfit)}"

        tvMacd.text = "MACD: ${String.format("%.4f", sig.macdValue)}  |  Signal: ${String.format("%.4f", sig.signalValue)}"
        tvCross.visibility = if (sig.isCrossover) View.VISIBLE else View.GONE
    }

    private fun setCardError(symbol: String, msg: String) {
        val ids = getIds(symbol) ?: return
        val tvSignal = findViewById<TextView>(ids[0])
        val tvEntry  = findViewById<TextView>(ids[1])
        val tvSL     = findViewById<TextView>(ids[2])
        val tvTP     = findViewById<TextView>(ids[3])
        val tvMacd   = findViewById<TextView>(ids[4])
        val tvCross  = findViewById<TextView>(ids[5])

        tvSignal.text = msg
        tvSignal.setBackgroundResource(R.drawable.bg_wait_signal)
        tvSignal.setTextColor(ContextCompat.getColor(this, R.color.textGray))
        tvEntry.text = "📍  Entry: —"
        tvSL.text    = "🔴  Stop Loss: —"
        tvTP.text    = "🟢  Take Profit: —"
        tvMacd.text  = ""
        tvCross.visibility = View.GONE
    }

    // Returns [signalId, entryId, slId, tpId, macdId, crossId]
    private fun getIds(symbol: String): List<Int>? = when (symbol) {
        "XAUUSD.m" -> listOf(
            R.id.tvSignalGold, R.id.tvEntryGold, R.id.tvSLGold,
            R.id.tvTPGold, R.id.tvMacdGold, R.id.tvCrossGold
        )
        "JP225.std" -> listOf(
            R.id.tvSignalJP, R.id.tvEntryJP, R.id.tvSLJP,
            R.id.tvTPJP, R.id.tvMacdJP, R.id.tvCrossJP
        )
        "BTCUSD.m" -> listOf(
            R.id.tvSignalBTC, R.id.tvEntryBTC, R.id.tvSLBTC,
            R.id.tvTPBTC, R.id.tvMacdBTC, R.id.tvCrossBTC
        )
        else -> null
    }
}
