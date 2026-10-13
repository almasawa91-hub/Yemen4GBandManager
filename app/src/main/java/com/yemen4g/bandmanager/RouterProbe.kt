package com.yemen4g.bandmanager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Read-only first-pass detector for locally owned ZTE/ZLT routers.
 * It never submits credentials and never sends configuration-changing requests.
 */
internal data class RouterProbeResult(
    val reachable: Boolean,
    val baseUrl: String,
    val vendor: String,
    val title: String,
    val httpStatus: String,
    val details: String
)

internal object RouterProbe {
    private val candidatePaths = listOf(
        "/",
        "/index.html",
        "/goform/goform_get_cmd_process?cmd=wa_inner_version&multi_data=1",
        "/api/device/information"
    )

    suspend fun inspect(addressInput: String): RouterProbeResult = withContext(Dispatchers.IO) {
        val input = addressInput.trim().removeSuffix("/")
        require(input.isNotEmpty()) { "أدخل عنوان المودم أولاً." }
        require(!input.contains("@") && !input.contains(" ")) { "عنوان المودم غير صالح." }
        val base = when {
            input.startsWith("http://", true) || input.startsWith("https://", true) -> input
            else -> "http://$input"
        }
        val parsed = URL(base)
        require(parsed.host.isNotBlank()) { "عنوان المودم غير صالح." }

        var lastFailure = "تعذر الوصول إلى صفحة الإدارة."
        var detectedTitle = ""
        var detectedVendor = "مودم غير معروف"
        var lastStatus = "لا توجد استجابة HTTP"
        var reachable = false
        var fingerprint = ""

        for (path in candidatePaths) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Yemen4GBandManager/0.2")
                    setRequestProperty("Accept", "text/html,application/json,*/*")
                }
                val status = connection.responseCode
                val stream = if (status >= 400) connection.errorStream else connection.inputStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    val out = StringBuilder()
                    val buffer = CharArray(2048)
                    var total = 0
                    while (total < 96 * 1024) {
                        val count = reader.read(buffer, 0, minOf(buffer.size, 96 * 1024 - total))
                        if (count < 0) break
                        out.append(buffer, 0, count)
                        total += count
                    }
                    out.toString()
                }.orEmpty()
                val server = connection.getHeaderField("Server").orEmpty()
                val sample = (server + " " + body.take(24000)).lowercase(Locale.ROOT)
                reachable = true
                lastStatus = "HTTP $status"
                if (body.isNotBlank()) fingerprint += " " + body.take(24000)
                if (detectedTitle.isBlank()) {
                    detectedTitle = Regex("(?is)<title[^>]*>(.*?)</title>")
                        .find(body)?.groupValues?.getOrNull(1)?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
                }
                detectedVendor = when {
                    sample.contains("zte") || sample.contains("ztemt") || sample.contains("goform_get_cmd_process") -> "ZTE"
                    sample.contains("zlt") || sample.contains("zlt x") || sample.contains("zlt s") -> "ZLT"
                    detectedVendor != "مودم غير معروف" -> detectedVendor
                    else -> "مودم غير معروف"
                }
                if (body.isNotBlank() && path == "/") break
                if (status in 200..399 && body.isNotBlank()) break
                lastFailure = "تم الوصول إلى الجهاز، لكن واجهة الإدارة لم تعرض بيانات قابلة للتعرّف."
            } catch (e: Exception) {
                lastFailure = when (e) {
                    is java.net.SocketTimeoutException -> "انتهت مهلة الاتصال. تأكد أنك متصل بشبكة المودم."
                    is java.net.UnknownHostException -> "تعذر العثور على عنوان المودم."
                    else -> e.message?.take(140) ?: "تعذر الاتصال بواجهة الإدارة."
                }
            } finally {
                connection?.disconnect()
            }
        }

        val combined = fingerprint.lowercase(Locale.ROOT)
        if (detectedVendor == "مودم غير معروف") {
            detectedVendor = when {
                combined.contains("zte") || combined.contains("ztemt") -> "ZTE (احتمال)"
                combined.contains("zlt") -> "ZLT (احتمال)"
                else -> "مودم غير معروف"
            }
        }
        RouterProbeResult(
            reachable = reachable,
            baseUrl = base,
            vendor = detectedVendor,
            title = detectedTitle.ifBlank { "لم يتم العثور على عنوان الصفحة" },
            httpStatus = lastStatus,
            details = if (reachable) {
                when (detectedVendor) {
                    "ZTE" -> "تم الوصول إلى واجهة ويب يُحتمل أنها ZTE. يلزم تحديد رقم الموديل وإصدار البرنامج الثابت قبل تفعيل تغيير الباند."
                    "ZLT" -> "تم الوصول إلى واجهة ويب يُحتمل أنها ZLT. تختلف واجهات وأوامر الباند بين الموديلات؛ لم يتم إرسال أي تغيير."
                    else -> "واجهة الإدارة تستجيب، لكن لا يمكن تأكيد الشركة من الاستجابة. جرّب عنوان المودم الصحيح وتحقق من ملصق الموديل."
                }
            } else lastFailure
        )
    }
}
