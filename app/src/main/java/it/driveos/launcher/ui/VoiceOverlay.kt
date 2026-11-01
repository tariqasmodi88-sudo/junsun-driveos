package it.driveos.launcher.ui

import it.driveos.launcher.i18n.tr
import it.driveos.launcher.i18n.trf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import it.driveos.launcher.voice.OfflineVoice

/** Finestra dei comandi vocali: ascolto, testo in tempo reale, download del modello la prima volta. */
@Composable
fun VoiceOverlay(state: OfflineVoice.State, lastHeard: String?, onInstall: () -> Unit, onRetry: () -> Unit, onClose: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(28.dp), color = c.surface, modifier = Modifier.widthIn(min = 420.dp, max = 620.dp)) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val listening = state is OfflineVoice.State.Listening
                Surface(shape = CircleShape, color = if (listening) c.primary else c.surfaceVariant, modifier = Modifier.size(96.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Mic, null, Modifier.size(52.dp), tint = if (listening) c.onPrimary else c.onSurfaceVariant)
                    }
                }
                when (state) {
                    is OfflineVoice.State.NotInstalled -> {
                        Text(tr("Comandi vocali offline"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        Text(tr("Serve scaricare una volta il riconoscimento vocale (circa 50 MB). Poi funziona anche senza internet."),
                            fontSize = 17.sp, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Btn(tr("Scarica ora"), true, onInstall); Btn(tr("Chiudi"), false, onClose)
                        }
                    }
                    is OfflineVoice.State.Downloading -> {
                        Text(trf("Scarico la voce… {0}%", state.percent), fontSize = 20.sp)
                        LinearProgressIndicator(progress = { state.percent / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                        Text(tr("Puoi chiudere: continua in sottofondo."), fontSize = 14.sp, color = c.onSurfaceVariant)
                    }
                    is OfflineVoice.State.Loading -> Text(tr("Preparo il riconoscimento…"), fontSize = 20.sp)
                    is OfflineVoice.State.Listening -> {
                        Text(tr("Ti ascolto…"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.primary)
                        Text(state.partial.ifBlank { tr("Es. \"portami a casa\", \"metti radio deejay\", \"prossima canzone\", \"alza il volume\"") },
                            fontSize = if (state.partial.isBlank()) 15.sp else 24.sp, color = if (state.partial.isBlank()) c.onSurfaceVariant else c.onSurface,
                            textAlign = TextAlign.Center)
                        Btn(tr("Annulla"), false, onClose)
                    }
                    is OfflineVoice.State.Ready -> {
                        Text(lastHeard?.let { trf("Non ho capito: \"{0}\"", it) } ?: tr("Non ho sentito nulla."), fontSize = 19.sp, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Btn(tr("Riprova"), true, onRetry); Btn(tr("Chiudi"), false, onClose) }
                    }
                    is OfflineVoice.State.Error -> {
                        Text(state.message, fontSize = 18.sp, color = c.error, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Btn(tr("Riprova"), true, onRetry); Btn(tr("Chiudi"), false, onClose) }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun Btn(text: String, primary: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = if (primary) c.primary else c.surfaceVariant,
        contentColor = if (primary) c.onPrimary else c.onSurface, modifier = Modifier.height(56.dp).widthIn(min = 150.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
    }
}
