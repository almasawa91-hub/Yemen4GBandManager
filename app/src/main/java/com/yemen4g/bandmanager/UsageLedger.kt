package com.yemen4g.bandmanager

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

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

    fun recordCumulativeCounters(downloadMb: Double, uploadMb: Double, date: LocalDate = LocalDate.now()): UsageRecord? {
        require(downloadMb.isFinite() && uploadMb.isFinite() && downloadMb >= 0.0 && uploadMb >= 0.0) {
            "يجب إدخال عدادات تراكمية صحيحة وغير سالبة من صفحة المودم."
        }
        val previousDown = prefs.getString(KEY_DOWN, null)?.toDoubleOrNull()
        val previousUp = prefs.getString(KEY_UP, null)?.toDoubleOrNull()
        prefs.edit().putString(KEY_DOWN, downloadMb.toString()).putString(KEY_UP, uploadMb.toString()).apply()
        if (previousDown == null || previousUp == null) return null

        val downDelta = if (downloadMb >= previousDown) downloadMb - previousDown else 0.0
        val upDelta = if (uploadMb >= previousUp) uploadMb - previousUp else 0.0
        val record = UsageRecord(date.toString(), downDelta, upDelta)
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

    fun summary(today: LocalDate = LocalDate.now()): UsageSummary {
        val month = YearMonth.from(today)
        val records = readRecords()
        val todayRecords = records.filter { it.date == today.toString() }
        val monthRecords = records.filter {
            runCatching { YearMonth.from(LocalDate.parse(it.date)) == month }.getOrDefault(false)
        }
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
                if (runCatching { LocalDate.parse(date) }.isFailure || !down.isFinite() || !up.isFinite() || down < 0 || up < 0) null
                else UsageRecord(date, down, up)
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val KEY_DOWN = "last_download_mb"
        const val KEY_UP = "last_upload_mb"
        const val KEY_RECORDS = "records"
        const val KEY_LIMIT = "monthly_limit_gb"
    }
}
