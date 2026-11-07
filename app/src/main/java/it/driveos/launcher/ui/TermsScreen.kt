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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Condizioni d'uso: compaiono alla prima installazione e dopo OGNI aggiornamento (versione diversa da
 * quella accettata). Testo in italiano e in inglese, identico al README.
 */
object Terms {
    val it = listOf(
        "Natura del software" to "DriveOS è un software sperimentale sviluppato da un privato a titolo personale e amatoriale, senza scopo di lucro. Non è un prodotto commerciale e non è stato sottoposto a verifiche di sicurezza, omologazione o certificazione di alcun tipo.",
        "Nessuna affiliazione" to "DriveOS non è prodotto, approvato, sponsorizzato né in alcun modo collegato a Junsun, ForFan, MediaTek, Volkswagen, Google, Waze o ad altri titolari dei marchi citati. Tutti i marchi appartengono ai rispettivi proprietari e sono menzionati al solo scopo di indicare la compatibilità.",
        "Esclusione di garanzia" to "Il software è fornito «così com'è» («as is»), senza garanzie di alcun tipo, espresse o implicite, comprese, a titolo esemplificativo, quelle di funzionamento, di idoneità a uno scopo particolare e di assenza di errori.",
        "Limitazione di responsabilità" to "Nei limiti massimi consentiti dalla legge applicabile, l'autore non risponde di alcun danno, diretto o indiretto, a dispositivi, veicoli, persone o cose, né di perdite di dati o della garanzia del produttore, derivante dall'installazione, dall'aggiornamento o dall'uso del software.",
        "Uso alla guida" to "Il conducente resta l'unico responsabile della guida e del rispetto del Codice della Strada. Le informazioni mostrate (sensori, telecamera, stato del veicolo, indicazioni) sono puramente indicative e non sostituiscono specchietti, controlli diretti e attenzione alla strada.",
        "Accettazione" to "Installando, aggiornando o utilizzando DriveOS l'utente dichiara di aver letto e accettato integralmente le presenti condizioni e di utilizzare il software a proprio esclusivo rischio. Le condizioni sono riproposte a ogni installazione e a ogni aggiornamento.",
    )
    val en = listOf(
        "Nature of the software" to "DriveOS is experimental software developed by a private individual as a personal, non-commercial hobby project. It is not a commercial product and has not undergone any safety testing, homologation or certification.",
        "No affiliation" to "DriveOS is not made, endorsed, sponsored by or in any way affiliated with Junsun, ForFan, MediaTek, Volkswagen, Google, Waze or any other owner of the trademarks mentioned. All trademarks belong to their respective owners and are mentioned solely to indicate compatibility.",
        "Disclaimer of warranty" to "The software is provided \"as is\", without warranty of any kind, express or implied, including but not limited to warranties of operation, fitness for a particular purpose and freedom from errors.",
        "Limitation of liability" to "To the maximum extent permitted by applicable law, the author shall not be liable for any direct or indirect damage to devices, vehicles, persons or property, nor for loss of data or of the manufacturer's warranty, arising from installing, updating or using the software.",
        "Use while driving" to "The driver remains solely responsible for driving and for complying with traffic laws. Information shown (sensors, camera, vehicle status, directions) is indicative only and does not replace mirrors, direct checks and attention to the road.",
        "Acceptance" to "By installing, updating or using DriveOS the user declares to have read and fully accepted these terms and to use the software entirely at their own risk. These terms are shown again at every installation and every update.",
    )
}

@Composable
fun TermsScreen(version: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    val c = MaterialTheme.colorScheme
    var checked by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.background).padding(24.dp)) {
        Text("Condizioni d'uso · Terms of use", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.onBackground)
        Text("DriveOS $version", fontSize = 15.sp, color = c.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Terms.it.forEachIndexed { i, (t, b) ->
                Text("${i + 1}. $t", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = c.onBackground)
                Text(b, fontSize = 16.sp, color = c.onBackground)
            }
            Spacer(Modifier.height(8.dp))
            Text("English", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.onBackground)
            Terms.en.forEachIndexed { i, (t, b) ->
                Text("${i + 1}. $t", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.onSurfaceVariant)
                Text(b, fontSize = 14.sp, color = c.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().clickable { checked = !checked }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { checked = it })
            Text("Ho letto e accetto le condizioni · I have read and accept the terms", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = c.onBackground)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(onClick = onDecline, shape = RoundedCornerShape(16.dp), color = c.surfaceVariant, modifier = Modifier.weight(1f).height(60.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("Non accetto · Decline", fontSize = 18.sp, color = c.onSurface) }
            }
            Surface(onClick = { if (checked) onAccept() }, shape = RoundedCornerShape(16.dp),
                color = if (checked) c.primary else c.primary.copy(alpha = 0.3f), modifier = Modifier.weight(1f).height(60.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("Accetto · Accept", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = c.onPrimary) }
            }
        }
    }
}
