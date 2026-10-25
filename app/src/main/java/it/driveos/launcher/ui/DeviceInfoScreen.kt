package it.driveos.launcher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.device.DeviceInfoCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DeviceInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var info by remember { mutableStateOf<JSONObject?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        info = withContext(Dispatchers.Default) {
            runCatching { DeviceInfoCollector.collect(context.applicationContext) }
                .getOrElse { JSONObject().put("errore", it.toString()) }
        }
    }

    // Esportazione tramite il selettore di file di sistema: nessun permesso di archiviazione.
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val data = info
        if (uri == null || data == null) return@rememberLauncherForActivityResult
        status = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(data.toString(2).toByteArray()) }
                ?: error("impossibile aprire il file")
            "File esportato correttamente."
        }.getOrElse { "Esportazione non riuscita: ${it.message}" }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Info dispositivo", onBack) {
            ActionButton(
                "Esporta JSON",
                Icons.Filled.Share,
                onClick = {
                    val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                    exporter.launch("driveos_info_$stamp.json")
                },
                modifier = Modifier.widthIn(max = 260.dp),
                highlighted = true,
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            status?.let {
                Text(it, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
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
                                Text(
                                    label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(220.dp),
                                )
                                Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Il file esportato contiene l'elenco completo di app, launcher, servizi vocali, " +
                        "sensori e dispositivi di input. Salvalo su chiavetta USB e invialo per l'analisi.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
