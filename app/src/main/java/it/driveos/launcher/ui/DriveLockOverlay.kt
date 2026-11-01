package it.driveos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.driveos.launcher.system.DriveLock
import kotlinx.coroutines.delay

/** Avviso a tutto schermo con conto alla rovescia mentre i tocchi sono bloccati. */
@Composable
fun DriveLockOverlay(lockedUntil: Long) {
    val left by produceState(0L, lockedUntil) {
        while (true) {
            value = (lockedUntil - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0)
            if (value == 0L) break
            delay(200)
        }
    }
    if (left <= 0) return
    val secs = ((left + 999) / 1000).toInt()
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(Icons.Filled.Warning, null, Modifier.size(72.dp), tint = Color(0xFFFFB020))
            Text("Concentrati sulla strada", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Troppe interazioni durante la guida.\nPotrai usare di nuovo lo schermo tra:", fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { left / (DriveLock.LOCK_SECONDS * 1000f) }, modifier = Modifier.size(110.dp),
                    strokeWidth = 8.dp, color = MaterialTheme.colorScheme.primary)
                Text("$secs", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text("Comandi vocali e tasti al volante restano attivi.", fontSize = 16.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}
