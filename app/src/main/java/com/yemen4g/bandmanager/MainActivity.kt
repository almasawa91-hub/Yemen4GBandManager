package com.yemen4g.bandmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

private val Navy = Color(0xFF10243A)
private val Teal = Color(0xFF00A896)
private val Pale = Color(0xFFF3F7FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 36, 58)
        window.navigationBarColor = android.graphics.Color.rgb(243, 247, 250)
        setContent { Yemen4GApp() }
    }
}

@Composable
private fun Yemen4GApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            secondary = Teal,
            background = Pale,
            surface = Color.White
        )
    ) {
        var selectedTab by remember { mutableIntStateOf(0) }
        Surface(modifier = Modifier.fillMaxSize(), color = Pale) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Scaffold(
                    topBar = { AppHeader() },
                    bottomBar = {
                        NavigationBar(containerColor = Color.White) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Router, contentDescription = null) },
                                label = { Text("المودم") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                label = { Text("الاستهلاك") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("الإعدادات") }
                            )
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedTab) {
                            0 -> RouterDashboard()
                            1 -> UsageDashboard()
                            else -> SettingsDashboard()
                        }
                        Text(
                            text = "فحص الاتصال قراءة فقط؛ لن يغيّر إعدادات المودم أو الباند تلقائياً.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppHeader() {
    TopAppBar(
        title = {
            Column {
                Text("مدير باند يمن فورجي", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text("تركيز خاص على مودمات ZTE و ZLT", fontSize = 12.sp, color = Color.Gray)
            }
        }
    )
}

@Composable
private fun RouterDashboard() {
    val context = LocalContext.current
    val profilePrefs = remember {
        context.getSharedPreferences("router_profile", android.content.Context.MODE_PRIVATE)
    }
    var address by remember { mutableStateOf(profilePrefs.getString("address", "192.168.0.1") ?: "192.168.0.1") }
    var model by remember { mutableStateOf(profilePrefs.getString("model", "") ?: "") }
    var firmware by remember { mutableStateOf(profilePrefs.getString("firmware", "") ?: "") }
    var profileSaved by remember { mutableStateOf(false) }
    var probing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RouterProbeResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Navy)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Router, contentDescription = null, tint = Color.White)
                Spacer(Modifier.padding(4.dp))
                Text("فحص مودم ZTE / ZLT", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Text(
                if (result?.reachable == true) "تم الوصول إلى صفحة الإدارة"
                else "جاهز لفحص الاتصال",
                color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "اتصل بشبكة Wi-Fi الخاصة بالمودم نفسه، ثم أدخل عنوان صفحة الإدارة.",
                color = Color(0xFFD5E1EA), fontSize = 13.sp
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("عنوان المودم", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            OutlinedTextField(
                value = address,
                onValueChange = { address = it; error = null; profileSaved = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("مثال: 192.168.0.1 أو 192.168.1.1") }
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it; profileSaved = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("موديل المودم (كما على الملصق)") },
                placeholder = { Text("مثال: ZTE MF286 أو ZLT S10") }
            )
            OutlinedTextField(
                value = firmware,
                onValueChange = { firmware = it; profileSaved = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("إصدار البرنامج الثابت (اختياري)") }
            )
            Button(
                onClick = {
                    profilePrefs.edit()
                        .putString("address", address.trim())
                        .putString("model", model.trim())
                        .putString("firmware", firmware.trim())
                        .apply()
                    profileSaved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ ملف المودم على الهاتف")
            }
            Button(
                onClick = {
                    try {
                        val entered = address.trim()
                        require(entered.isNotBlank() && !entered.contains(" ") && !entered.contains("@")) {
                            "أدخل عنوان إدارة صحيحاً أولاً."
                        }
                        val target = if (
                            entered.startsWith("http://", true) ||
                            entered.startsWith("https://", true)
                        ) entered else "http://$entered"
                        val parsed = java.net.URL(target)
                        require(parsed.protocol == "http" || parsed.protocol == "https") {
                            "نوع عنوان غير مدعوم."
                        }
                        require(parsed.host.isNotBlank() && parsed.userInfo == null) {
                            "عنوان إدارة المودم غير صالح."
                        }
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(target)
                            )
                        )
                    } catch (e: Exception) {
                        error = e.message ?: "تعذر فتح صفحة إدارة المودم."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("فتح صفحة إدارة المودم في المتصفح")
            }
            if (profileSaved) {
                Text("تم حفظ العنوان والموديل والإصدار محلياً على هذا الهاتف.", color = Color(0xFF047857), fontSize = 12.sp)
            }
            Button(
                onClick = {
                    probing = true
                    result = null
                    error = null
                    scope.launch {
                        try {
                            result = RouterProbe.inspect(address)
                        } catch (e: Exception) {
                            error = e.message ?: "تعذر فحص المودم."
                        } finally {
                            probing = false
                        }
                    }
                },
                enabled = !probing && address.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (probing) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Text(" جارٍ الفحص…")
                } else {
                    Text("فحص مودم ZTE / ZLT")
                }
            }
            if (error != null) {
                Text(error!!, color = Color(0xFFB91C1C), fontSize = 13.sp)
            }
            result?.let { probe ->
                SectionTitle("نتيجة الفحص")
                MetricCard("الشركة المحتملة", probe.vendor, probe.httpStatus, Modifier.fillMaxWidth())
                MetricCard("عنوان الإدارة", probe.baseUrl, probe.title, Modifier.fillMaxWidth())
                Text(
                    probe.details,
                    color = if (probe.reachable) Color(0xFF047857) else Color(0xFFB91C1C),
                    fontSize = 13.sp
                )
                Text(
                    "تنبيه: التعرف من صفحة الويب أولي وليس ضماناً للتوافق. لم تُرسل كلمة مرور أو أوامر تغيير.",
                    color = Color(0xFF64748B), fontSize = 12.sp
                )
            }
        }
    }

    SectionTitle("التحكم بباند 4G")
    if (model.isNotBlank()) {
        MetricCard(
            "ملف المودم المحفوظ",
            model,
            if (firmware.isBlank()) "لم يُسجل إصدار البرنامج الثابت" else "Firmware: $firmware",
            Modifier.fillMaxWidth()
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard("الباند الحالي", "غير مقروء", "يتطلب موصلًا خاصًا بالموديل", Modifier.weight(1f))
        MetricCard("قوة الإشارة", "—", "قراءة فعلية غير متاحة بعد", Modifier.weight(1f))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("تغيير الباند", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                "الخطوة التالية هي بناء موصل منفصل بعد تحديد موديل ZTE أو ZLT ورقم إصدار البرنامج الثابت. لن تظهر أزرار تغيير وهمية أو تُرسل أوامر غير مؤكدة.",
                color = Color(0xFF64748B), fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun UsageDashboard() {
    val context = LocalContext.current
    val ledger = remember { UsageLedger(context.applicationContext) }
    var downloadCounter by remember { mutableStateOf("") }
    var uploadCounter by remember { mutableStateOf("") }
    var monthlyLimit by remember { mutableStateOf(ledger.monthlyLimitGb().toString()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showResetBaselineDialog by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val summary = remember(refresh) { ledger.summary() }
    val recent = remember(refresh) { ledger.recentRecords(8) }

    SectionTitle("استهلاك الإنترنت")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Navy)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("إجمالي استهلاك هذا الشهر", color = Color(0xFFD5E1EA), fontSize = 14.sp)
            Text(
                String.format(Locale.US, "%.3f GB", (summary.monthDownloadMb + summary.monthUploadMb) / 1000.0),
                fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
            Text(
                "الحد الذي حددته: %.1f GB".format(Locale.US, summary.monthlyLimitGb),
                color = Color(0xFFD5E1EA), fontSize = 13.sp
            )
            val usedGb = (summary.monthDownloadMb + summary.monthUploadMb) / 1000.0
            val fraction = (usedGb / summary.monthlyLimitGb).toFloat().coerceIn(0f, 1f)
            androidx.compose.material3.LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = Teal,
                trackColor = Color(0xFF42576A)
            )
            if (usedGb >= summary.monthlyLimitGb) {
                Text("تنبيه: تجاوز الاستهلاك المسجل الحد الشهري المحدد.", color = Color(0xFFFFD7A8), fontSize = 12.sp)
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(
            "تنزيل اليوم",
            String.format(Locale.US, "%.2f MB", summary.todayDownloadMb),
            "من فروق العدادات المسجلة",
            Modifier.weight(1f)
        )
        MetricCard(
            "رفع اليوم",
            String.format(Locale.US, "%.2f MB", summary.todayUploadMb),
            "من فروق العدادات المسجلة",
            Modifier.weight(1f)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(
            "تنزيل الشهر",
            String.format(Locale.US, "%.2f GB", summary.monthDownloadMb / 1000.0),
            "حتى الآن",
            Modifier.weight(1f)
        )
        MetricCard(
            "رفع الشهر",
            String.format(Locale.US, "%.2f GB", summary.monthUploadMb / 1000.0),
            "حتى الآن",
            Modifier.weight(1f)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("تسجيل قراءة عدادات المودم", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                "انسخ القيم التراكمية للتنزيل والرفع من صفحة إحصاءات المودم. أول قراءة تحفظ خط أساس فقط، وبعدها يحسب التطبيق الفرق بين القراءات. هذه ليست قراءة تلقائية من المودم.",
                color = Color(0xFF64748B), fontSize = 12.sp
            )
            OutlinedTextField(
                value = downloadCounter,
                onValueChange = { downloadCounter = it; notice = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("إجمالي التنزيل منذ تشغيل العداد (MB)") }
            )
            OutlinedTextField(
                value = uploadCounter,
                onValueChange = { uploadCounter = it; notice = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("إجمالي الرفع منذ تشغيل العداد (MB)") }
            )
            androidx.compose.material3.OutlinedButton(
                onClick = { showResetBaselineDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إعادة ضبط خط الأساس للعدادات")
            }
            if (showResetBaselineDialog) {
                AlertDialog(
                    onDismissRequest = { showResetBaselineDialog = false },
                    title = { Text("إعادة ضبط خط الأساس؟") },
                    text = {
                        Text("سيُنسى آخر رقمي عداد فقط. لن يُحذف سجل الاستهلاك السابق. استخدم هذا الخيار إذا أُعيد تشغيل عداد المودم أو صُفّر.")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            ledger.resetBaseline()
                            showResetBaselineDialog = false
                            notice = "تمت إعادة ضبط خط الأساس. القراءة التالية ستُحفظ كخط أساس جديد."
                            isError = false
                            downloadCounter = ""
                            uploadCounter = ""
                        }) { Text("تأكيد إعادة الضبط") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetBaselineDialog = false }) { Text("إلغاء") }
                    }
                )
            }
            Button(
                onClick = {
                    val down = downloadCounter.trim().replace(',', '.').toDoubleOrNull()
                    val up = uploadCounter.trim().replace(',', '.').toDoubleOrNull()
                    if (down == null || up == null || !down.isFinite() || !up.isFinite() || down < 0 || up < 0) {
                        notice = "أدخل رقمي عداد صحيحين وغير سالبين."
                        isError = true
                    } else {
                        try {
                            val record = ledger.recordCumulativeCounters(down, up)
                            notice = if (record == null) {
                                "تم حفظ خط الأساس. ستُحسب الفروقات عند تسجيل القراءة التالية."
                            } else {
                                "تم حفظ القراءة: تنزيل %.2f MB، رفع %.2f MB.".format(Locale.US, record.downloadMb, record.uploadMb)
                            }
                            isError = false
                            refresh++
                        } catch (e: Exception) {
                            notice = e.message ?: "تعذر حفظ القراءة."
                            isError = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("حفظ القراءة وحساب الاستهلاك") }

            OutlinedTextField(
                value = monthlyLimit,
                onValueChange = { monthlyLimit = it; notice = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("حد الاستهلاك الشهري (GB)") }
            )
            Button(
                onClick = {
                    val limit = monthlyLimit.trim().replace(',', '.').toDoubleOrNull()
                    if (limit == null || !limit.isFinite() || limit !in 0.1..100000.0) {
                        notice = "أدخل حداً شهرياً بين 0.1 و100000 جيجابايت."
                        isError = true
                    } else {
                        ledger.setMonthlyLimitGb(limit)
                        notice = "تم حفظ حد الاستهلاك الشهري."
                        isError = false
                        refresh++
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("حفظ الحد الشهري") }
            if (notice != null) {
                Text(notice!!, color = if (isError) Color(0xFFB91C1C) else Color(0xFF047857), fontSize = 12.sp)
            }
        }
    }

    SectionTitle("آخر القراءات المحسوبة")
    if (recent.isEmpty()) {
        Text("لا توجد فروقات مسجلة بعد. أدخل قراءة أولى ثم قراءة جديدة لاحقاً.", color = Color(0xFF64748B), fontSize = 13.sp)
    } else {
        recent.forEach { item ->
            MetricCard(
                item.date,
                "↓ %.2f MB   ↑ %.2f MB".format(Locale.US, item.downloadMb, item.uploadMb),
                "تنزيل ورفع محسوبان من فرق قراءتين",
                Modifier.fillMaxWidth()
            )
        }
    }
    Button(
        onClick = {
            runCatching {
                UsageCsvExporter.share(context, ledger.exportCsv())
            }.onFailure {
                android.widget.Toast.makeText(
                    context,
                    "تعذر إنشاء ملف CSV: ${it.message ?: "خطأ غير معروف"}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("تصدير سجل الاستهلاك CSV")
    }
    Text(
        "مهم: دقة التقارير تعتمد على إدخال العدادات التراكمية الصحيحة. إذا أُعيد ضبط عداد المودم، فسيتجاهل التطبيق الفرق السالب بدلاً من احتساب استهلاك خاطئ.",
        color = Color(0xFF475569), fontSize = 12.sp
    )
}

@Composable
private fun SettingsDashboard() {
    SectionTitle("الإعدادات والتوافق")
    MetricCard("أولوية الدعم", "ZTE و ZLT", "كشف واجهة الإدارة محلياً أولاً", Modifier.fillMaxWidth())
    MetricCard("تغيير الباند", "حسب الموديل", "لا يُفعّل إلا بعد توثيق واجهة API للمودم", Modifier.fillMaxWidth())
    MetricCard("عداد الاستهلاك", "قراءات فعلية", "لا توجد قيم تجريبية أو تقديرات من سرعة الشبكة", Modifier.fillMaxWidth())
    MetricCard("إدارة Wi-Fi", "مرحلة لاحقة", "كلمة المرور والأجهزة المتصلة مؤجلة", Modifier.fillMaxWidth())
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, color = Color(0xFF64748B), fontSize = 13.sp)
            Text(value, color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF64748B), fontSize = 11.sp)
        }
    }
}

/** Convert an actual byte counter delta to decimal megabytes. */
internal fun bytesToMegabytes(bytes: Long): Double =
    bytes.coerceAtLeast(0L).toDouble() / 1_000_000.0

/** Difference between cumulative router byte counters, handling counter resets. */
internal fun counterDelta(previous: Long, current: Long): Long =
    if (previous < 0L || current < 0L || current < previous) 0L else current - previous

internal fun formatMegabytes(bytes: Long): String =
    String.format(Locale.US, "%.2f", bytesToMegabytes(bytes))
