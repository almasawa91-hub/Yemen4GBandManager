package com.yemen4g.bandmanager

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes the CSV to app-private cache and shares it with a temporary read-only URI grant. */
internal object UsageCsvExporter {
    fun share(context: Context, csv: String) {
        val directory = File(context.cacheDir, "exports").apply {
            if (!exists() && !mkdirs()) error("تعذر إنشاء مجلد التصدير.")
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(directory, "yemen4g_usage_$stamp.csv")
        file.writeText(csv, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "سجل استهلاك يمن فورجي")
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "سجل استهلاك يمن فورجي", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "مشاركة ملف سجل الاستهلاك"))
    }
}
