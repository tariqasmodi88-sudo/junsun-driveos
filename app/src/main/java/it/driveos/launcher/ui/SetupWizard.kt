package it.driveos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.i18n.tr
import it.driveos.launcher.voice.VoiceLanguages

/** Preferenze scelte nella procedura iniziale. */
data class SetupChoices(val lang: String, val rearCamera: Boolean, val frontSensors: Boolean, val touchLimit: Boolean, val canSerial: Boolean)

/**
 * Procedura al primo avvio: lingua → avvertenze (da accettare) → preferenze del veicolo → fine.
 * [onLanguage] applica subito la lingua (l'app si ridisegna tradotta).
 */
@Composable
fun SetupWizard(current: SetupChoices, onLanguage: (String) -> Unit, onPickNav: () -> Unit, navLabel: String?, onFinish: (SetupChoices) -> Unit) {
    val c = MaterialTheme.colorScheme
    var step by rememberSaveable { mutableStateOf(0) }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var cam by rememberSaveable { mutableStateOf(current.rearCamera) }
    var front by rememberSaveable { mutableStateOf(current.frontSensors) }
    var touch by rememberSaveable { mutableStateOf(current.touchLimit) }
    var serial by rememberSaveable { mutableStateOf(current.canSerial) }
    Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(28.dp), color = c.surface, modifier = Modifier.widthIn(max = 900.dp).fillMaxWidth().padding(24.dp)) {
            Column(Modifier.padding(28.dp)) {
                Text("DriveOS · " + tr("Configurazione") + "  ${step + 1}/4", fontSize = 15.sp, color = c.primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (step) {
                        0 -> {
                            Text(tr("Scegli la lingua"), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            VoiceLanguages.all.forEach { l ->
                                Surface(onClick = { onLanguage(l.code) }, shape = RoundedCornerShape(16.dp),
                                    color = if (l.code == current.lang) c.primary.copy(alpha = 0.22f) else c.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                                    Text(l.label, fontSize = 22.sp, modifier = Modifier.padding(16.dp),
                                        fontWeight = if (l.code == current.lang) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        1 -> {
                            Text(tr("Avvertenze"), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            listOf(
                                tr("DriveOS è un progetto personale e sperimentale, non ufficiale e non collegato a Junsun, Volkswagen, Google o Waze."),
                                tr("È stato provato solo su una radio Junsun FF_866X (Android 10) montata su una Volkswagen Tiguan 2016. Su altre radio, versioni di Android o auto potrebbe non funzionare."),
                                tr("Comunica con la radio e con la centralina dell'auto: in casi rari può causare blocchi, riavvii o malfunzionamenti della radio. Le funzioni dell'auto restano comunque gestite dall'auto."),
                                tr("Non distrarti alla guida: usa lo schermo solo quando è sicuro e rispetta il codice della strada. Le indicazioni dei sensori e della telecamera sono un aiuto, non sostituiscono gli specchietti e lo sguardo."),
                                tr("Lo usi a tuo rischio: l'autore non risponde di danni alla radio, all'auto, a persone o cose."),
                            ).forEach { Text("• $it", fontSize = 17.sp, color = c.onSurface) }
                            Row(Modifier.fillMaxWidth().clickable { accepted = !accepted }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                                Text(tr("Ho letto e accetto"), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        2 -> {
                            Text(tr("La tua auto"), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Toggle(tr("Seriale auto"), tr("Legge porte, cinture, sensori, clima e dati motore dalla centralina."), serial) { serial = it }
                            Toggle(tr("Telecamera posteriore"), tr("In retromarcia mostra immagine, linee guida e sensori."), cam) { cam = it }
                            Toggle(tr("Sensori di parcheggio anteriori"), tr("Accendi solo se sono montati."), front) { front = it }
                            Toggle(tr("Limite tocchi in marcia"), tr("Come Android Auto: troppi tocchi in marcia bloccano lo schermo per 10 secondi."), touch) { touch = it }
                            Surface(onClick = onPickNav, shape = RoundedCornerShape(16.dp), color = c.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(tr("App di navigazione"), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                                    Text(navLabel ?: tr("Tocca per impostare"), fontSize = 15.sp, color = c.onSurfaceVariant)
                                }
                            }
                        }
                        else -> {
                            Text(tr("Tutto pronto"), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Text(tr("Puoi cambiare tutte queste scelte in qualsiasi momento da Impostazioni. Alcune funzioni (musica, guida, finestre Junsun) chiederanno i permessi la prima volta che le usi."),
                                fontSize = 18.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (step > 0) WizBtn(tr("Indietro"), false, true) { step-- }
                    Spacer(Modifier.weight(1f))
                    val canNext = step != 1 || accepted
                    WizBtn(if (step == 3) tr("Inizia") else tr("Avanti"), true, canNext) {
                        if (step < 3) step++ else onFinish(SetupChoices(current.lang, cam, front, touch, serial))
                    }
                }
            }
        }
    }
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = if (on) c.primary.copy(alpha = 0.14f) else c.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, fontSize = 14.sp, color = c.onSurfaceVariant)
            }
            Switch(checked = on, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun WizBtn(text: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(onClick = { if (enabled) onClick() }, shape = RoundedCornerShape(16.dp),
        color = when { !enabled -> c.surfaceVariant.copy(alpha = 0.5f); primary -> c.primary; else -> c.surfaceVariant },
        contentColor = if (primary && enabled) c.onPrimary else c.onSurface.copy(alpha = if (enabled) 1f else 0.4f),
        modifier = Modifier.height(60.dp).widthIn(min = 170.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) { Text(text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold) }
    }
}
