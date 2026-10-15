package de.fleximetronom.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Ink = Color(0xFF11131A)
private val Panel = Color(0xFF1D202B)
private val Accent = Color(0xFFB9F36A)
private val Muted = Color(0xFF9BA3B7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FlexiMetronomeApp() }
    }
}

@Composable
fun FlexiMetronomeApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("flexi_prefs", 0) }
    var numerator by remember { mutableIntStateOf(prefs.getInt("num", 13)) }
    var denominator by remember { mutableIntStateOf(prefs.getInt("den", 32)) }
    var bpm by remember { mutableIntStateOf(prefs.getInt("bpm", 100)) }
    var beatUnit by remember { mutableIntStateOf(prefs.getInt("unit", 32)) }
    var sound by remember { mutableStateOf(prefs.getString("sound", "Click") ?: "Click") }
    var groupsText by remember { mutableStateOf(prefs.getString("groups", "3+3+3+4") ?: "3+3+3+4") }
    var customAccents by remember { mutableStateOf(prefs.getString("accents", "1") ?: "1") }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPulse by remember { mutableIntStateOf(-1) }

    fun persist() {
        prefs.edit().putInt("num", numerator).putInt("den", denominator).putInt("bpm", bpm)
            .putInt("unit", beatUnit).putString("sound", sound).putString("groups", groupsText)
            .putString("accents", customAccents).apply()
    }

    fun stop() {
        val intent = Intent(context, MetronomeService::class.java).setAction(MetronomeService.ACTION_STOP)
        context.startService(intent)
        isPlaying = false
        currentPulse = -1
    }

    fun start() {
        persist()
        val intent = Intent(context, MetronomeService::class.java).apply {
            action = MetronomeService.ACTION_START
            putExtra(MetronomeService.EXTRA_NUM, numerator)
            putExtra(MetronomeService.EXTRA_DEN, denominator)
            putExtra(MetronomeService.EXTRA_BPM, bpm)
            putExtra(MetronomeService.EXTRA_UNIT, beatUnit)
            putExtra(MetronomeService.EXTRA_SOUND, sound)
            putExtra(MetronomeService.EXTRA_GROUPS, groupsText)
            putExtra(MetronomeService.EXTRA_ACCENTS, customAccents)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
        isPlaying = true
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, onPrimary = Ink, background = Ink, surface = Panel,
        onBackground = Color.White, onSurface = Color.White, secondary = Accent
    )) {
        Column(
            Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FLEXI METRONOM", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("MUSIKER-EDITION", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Text("STUDIO", color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TAKTART", color = Muted, fontSize = 12.sp, letterSpacing = 2.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        NumberStepper(numerator.toString(), { numerator = (numerator - 1).coerceAtLeast(1); persist() },
                            { numerator = (numerator + 1).coerceAtMost(64); persist() })
                        Text("/", color = Accent, fontSize = 40.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(horizontal = 12.dp))
                        NumberStepper(denominator.toString(), {
                            denominator = nextDenominator(denominator, -1); persist()
                        }, {
                            denominator = nextDenominator(denominator, 1); persist()
                        })
                    }
                    Text("Frei definierbar · 1–64 / 1–32", color = Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TEMPO", color = Muted, fontSize = 12.sp, letterSpacing = 2.sp)
                    Text("$bpm", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold, lineHeight = 70.sp)
                    Text("BPM · pro ${beatUnit}tel Note", color = Accent, fontSize = 13.sp)
                    Slider(value = bpm.toFloat(), onValueChange = { bpm = it.toInt(); persist() },
                        valueRange = 20f..300f, colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { bpm = (bpm - 1).coerceAtLeast(20); persist() }) { Text("− 1") }
                        OutlinedButton(onClick = { bpm = (bpm + 1).coerceAtMost(300); persist() }) { Text("+ 1") }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("SCHLAG-EINHEIT", color = Muted, fontSize = 11.sp, letterSpacing = 1.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(listOf(4, 8, 16, 32)) { unit ->
                            FilterChip(selected = beatUnit == unit, onClick = { beatUnit = unit; persist() },
                                label = { Text("1/$unit") })
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val visiblePulses = (numerator.toDouble() * beatUnit / denominator).toInt().coerceAtLeast(1).coerceAtMost(32)
                repeat(visiblePulses) { i ->
                    Box(Modifier.size(if (i == currentPulse) 15.dp else 9.dp)
                        .background(if (i == currentPulse) Accent else Color(0xFF454A59), CircleShape))
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = { if (isPlaying) stop() else start() },
                modifier = Modifier.fillMaxWidth().height(62.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                shape = RoundedCornerShape(18.dp)) {
                Text(if (isPlaying) "■  STOPPEN" else "▶  STARTEN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle("AKZENTE")
            OutlinedTextField(value = customAccents, onValueChange = { customAccents = it; persist() },
                label = { Text("Schläge mit Akzent (z. B. 1, 4, 7)") },
                supportingText = { Text("Schlagnummern, durch Kommas getrennt", color = Muted) },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(14.dp))
            SectionTitle("RHYTHMUSGRUPPEN")
            OutlinedTextField(value = groupsText, onValueChange = { groupsText = it; persist() },
                label = { Text("Gruppierung (z. B. 3+3+3+4)") },
                supportingText = { Text("Akzent am Anfang jeder Gruppe", color = Muted) },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(14.dp))
            SectionTitle("KLANG")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Click", "Woodblock", "Bell").forEach { option ->
                    FilterChip(selected = sound == option, onClick = { sound = option; persist() }, label = { Text(option) })
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Einstellungen werden automatisch gespeichert.", color = Muted, fontSize = 12.sp)
            Text("Hinweis: Dies ist ein erster Prototyp. Für Bühneneinsatz braucht das Timing weitere Tests.", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }
}

@Composable
private fun NumberStepper(value: String, minus: () -> Unit, plus: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = plus, contentPadding = PaddingValues(4.dp)) { Text("＋", color = Accent, fontSize = 22.sp) }
        Text(value, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = minus, contentPadding = PaddingValues(4.dp)) { Text("−", color = Accent, fontSize = 22.sp) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
}

private fun nextDenominator(current: Int, direction: Int): Int {
    val values = listOf(1, 2, 4, 8, 16, 32)
    val index = values.indexOf(current).coerceAtLeast(0)
    return values[(index + direction).coerceIn(0, values.lastIndex)]
}

private fun parseAccentIndices(text: String): Set<Int> =
    text.split(",", " ", ";").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.toSet()

private fun parseGroups(text: String): List<Int> =
    text.split("+", "-", ",", " ").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }

private fun groupStarts(groups: List<Int>): Set<Int> {
    val starts = mutableSetOf<Int>()
    var total = 0
    groups.dropLast(1).forEach { group -> total += group; starts.add(total) }
    return starts
}
