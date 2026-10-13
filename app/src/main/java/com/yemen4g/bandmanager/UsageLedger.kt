package com.yemen4g.bandmanager

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class UsageRecord(
    val date: String,
    val downloadMb: Double,
    val uploadMb: Double
)

internal data class UsageSummary(
    val todayDownloadMb: Double,
    val todayUploadMb: Double,
    val monthDownloadMb: Double,
    val monthUploadMb: Double,
    val latestRecordDate: String?,
    val monthlyLimitGb: Double
)

/** Computes a cumulative MB counter delta; resets are treated as a new baseline. */
internal fun cumulativeCounterDeltaMb(previous: Double, current: Double): Double =
    if (!previous.isFinite() || !current.isFinite() || previous < 0.0 || current < previous) 0.0
    else current - previous

/**
 * Stores snapshots copied from the router's own usage counters.
 * The app never estimates usage from connection speed. The first snapshot is a baseline.
 */
internal class UsageLedger(context: Context) {
    private val prefs = context.getSharedPreferences("usage_ledger_v1", Context.MODE_PRIVATE)

    fun setMonthlyLimitGb(value: Double) {
        require(value.isFinite() && value in 0.1..100_000.0) { "أدخل حداً شهرياً بين 0.1 و100000 جيجابايت." }
        prefs.edit().putFloat(KEY_LIMIT, value.toFloat()).apply()
    }

    fun monthlyLimitGb(): Double = prefs.getFloat(KEY_LIMIT, 30f).toDouble()

    fun recordCumulativeCounters(downloadMb: Double, uploadMb: Double, date: String = today()): UsageRecord? {
        require(downloadMb.isFinite() && uploadMb.isFinite() && downloadMb >= 0.0 && uploadMb >= 0.0) {
            "يجب إدخال عدادات تراكمية صحيحة وغير سالبة من صفحة المودم."
        }
        require(isValidDate(date)) { "تاريخ القراءة غير صالح." }
        val previousDown = prefs.getString(KEY_DOWN, null)?.toDoubleOrNull()
        val previousUp = prefs.getString(KEY_UP, null)?.toDoubleOrNull()
        prefs.edit().putString(KEY_DOWN, downloadMb.toString()).putString(KEY_UP, uploadMb.toString()).apply()
        if (previousDown == null || previousUp == null) return null

        val record = UsageRecord(
            date,
            cumulativeCounterDeltaMb(previousDown, downloadMb),
            cumulativeCounterDeltaMb(previousUp, uploadMb)
        )
        val records = readRecords().toMutableList()
        records.add(record)
        while (records.size > 2000) records.removeAt(0)
        val json = JSONArray()
        records.forEach {
            json.put(JSONObject().put("date", it.date).put("down", it.downloadMb).put("up", it.uploadMb))
        }
        prefs.edit().putString(KEY_RECORDS, json.toString()).apply()
        return record
    }

    fun resetBaseline() {
        prefs.edit().remove(KEY_DOWN).remove(KEY_UP).apply()
    }

    fun summary(date: String = today()): UsageSummary {
        val monthPrefix = date.take(7)
        val records = readRecords()
        val todayRecords = records.filter { it.date == date }
        val monthRecords = records.filter { it.date.startsWith(monthPrefix) }
        return UsageSummary(
            todayRecords.sumOf { it.downloadMb },
            todayRecords.sumOf { it.uploadMb },
            monthRecords.sumOf { it.downloadMb },
            monthRecords.sumOf { it.uploadMb },
            records.lastOrNull()?.date,
            monthlyLimitGb()
        )
    }

    fun recentRecords(limit: Int = 10): List<UsageRecord> = readRecords().takeLast(limit.coerceIn(1, 100)).reversed()

    private fun readRecords(): List<UsageRecord> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { i ->
                val item = json.optJSONObject(i) ?: return@mapNotNull null
                val date = item.optString("date")
                val down = item.optDouble("down", Double.NaN)
                val up = item.optDouble("up", Double.NaN)
                if (!isValidDate(date) || !down.isFinite() || !up.isFinite() || down < 0 || up < 0) null
                else UsageRecord(date, down, up)
            }
        }.getOrDefault(emptyList())
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun isValidDate(value: String): Boolean = runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        formatter.parse(value) != null
    }.getOrDefault(false)

    private companion object {
        const val KEY_DOWN = "last_download_mb"
        const val KEY_UP = "last_upload_mb"
        const val KEY_RECORDS = "records"
        const val KEY_LIMIT = "monthly_limit_gb"
    }
}
