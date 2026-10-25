package it.driveos.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.device.DeviceInfoCollector
import it.driveos.launcher.system.CrashLog
import it.driveos.launcher.system.Exporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Riepilogo hardware + esportazione. L'export NON usa più il selettore di file di sistema
 * (che sul firmware Junsun mancava e causava il crash): salva direttamente in
 * Download/DriveOS e nella cartella dell'app su ogni memoria/chiavetta.
 */
@Composable
fun DeviceInfoScreen(
    extraSections: suspend () -> JSONObject = { JSONObject() },
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<JSONObject?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        info = withContext(Dispatchers.Default) {
            runCatching {
                DeviceInfoCollector.collect(context.applicationContext).apply {
                    put("veicolo", runCatching { extraSections() }.getOrElse { JSONObject().put("errore", it.toString()) })
                    CrashLog.read(context)?.let { put("ultimoCrash", it) }
                }
            }.getOrElse { JSONObject().put("errore", it.toString()) }
        }
    }

    fun export() {
        val data = info ?: return
        scope.launch {
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    Exporter.save(context, "driveos_info_$stamp.json", "application/json",
                        data.toString(2).toByteArray())
                }.getOrElse { Exporter.Result(emptyList(), listOf(it.message ?: "errore")) }
            }
            status = if (result.ok) "Salvato in:\n• " + result.saved.joinToString("\n• ")
            else "Esportazione non riuscita. " + result.errors.firstOrNull().orEmpty()
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Info dispositivo", onBack) {
            ActionButton("Esporta JSON", Icons.Filled.Share, onClick = { export() },
                modifier = Modifier.widthIn(max = 260.dp), highlighted = true)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            status?.let {
                Text(it, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
            }
            val data = info
            if (data == null) {
                Text("Raccolta informazioni in corso…", fontSize = 18.sp)
            } else {
                SelectionContainer {
                    Column {
                        DeviceInfoCollector.summary(data).forEach { (label, value) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(220.dp))
                                Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Il file esportato contiene app, launcher, servizi vocali, sensori, dispositivi di input, " +
                        "i segnali del veicolo letti e l'eventuale ultimo errore. Viene salvato in Download/DriveOS " +
                        "e sulla chiavetta USB (cartella Android/data/it.driveos.launcher/files).",
                    fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                data.optString("ultimoCrash").takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(16.dp))
                    Text("Ultimo errore registrato", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error)
                    Text(it.take(600), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
