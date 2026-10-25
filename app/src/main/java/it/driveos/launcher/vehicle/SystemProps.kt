package it.driveos.launcher.vehicle

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Lettura delle proprieta' di sistema (equivalente di `getprop`), senza permessi.
 * Prova prima il comando `getprop`; se non disponibile, nessun errore: ritorna vuoto.
 * Le proprieta' sono pubbliche (le stesse che mostrano le Opzioni sviluppatore).
 */
object SystemProps {
    fun all(): Map<String, String> = runCatching {
        val process = ProcessBuilder("getprop").redirectErrorStream(true).start()
        val text = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
        process.waitFor()
        GetpropParser.parse(text)
    }.getOrDefault(emptyMap())
}
