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
    var address by remember { mutableStateOf("192.168.0.1") }
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
                onValueChange = { address = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("مثال: 192.168.0.1 أو 192.168.1.1") }
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
    SectionTitle("استهلاك الإنترنت")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("استهلاك اليوم", color = Color(0xFF64748B))
            Text("— MB", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text("بانتظار ربط عدادات المودم الفعلية", fontSize = 13.sp, color = Color(0xFF64748B))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard("التنزيل", "—", "MB / GB", Modifier.weight(1f))
        MetricCard("الرفع", "—", "MB / GB", Modifier.weight(1f))
    }
    MetricCard("استهلاك الشهر", "— GB", "يُحسب لاحقاً من فروق العدادات المحفوظة", Modifier.fillMaxWidth())
    Text(
        "الدقة: تُحوّل فروق عدادات البايت الحقيقية إلى MB (1 MB = 1,000,000 بايت). لن تُستخدم سرعة الإنترنت لتقدير الاستهلاك.",
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
