package it.driveos.launcher.voice

/** Comando capito da una frase italiana. */
sealed class VoiceCommand {
    data class Navigate(val destination: String) : VoiceCommand()
    object GoHome : VoiceCommand()
    object GoWork : VoiceCommand()
    object StopNav : VoiceCommand()
    object Radio : VoiceCommand()
    data class Station(val name: String) : VoiceCommand()
    data class Frequency(val khz: Int) : VoiceCommand()
    object Music : VoiceCommand()
    object Next : VoiceCommand()
    object Previous : VoiceCommand()
    object Pause : VoiceCommand()
    object Play : VoiceCommand()
    object VolumeUp : VoiceCommand()
    object VolumeDown : VoiceCommand()
    object Mute : VoiceCommand()
    object Climate : VoiceCommand()
    object Vehicle : VoiceCommand()
    object Home : VoiceCommand()
    object Phone : VoiceCommand()
    data class Unknown(val text: String) : VoiceCommand()
}

/**
 * Interpreta la frase riconosciuta (minuscole, senza punteggiatura). Regole semplici e prevedibili:
 * "portami / vai / naviga / andiamo a …" = navigazione, "metti radio X" = stazione, ecc.
 */
object VoiceCommands {
    private val navPrefix = Regex("^(?:portami|vai|naviga|andiamo|guidami|portaci|percorso|navigazione|naviga verso|vai verso)\\s+(?:verso\\s+|a\\s+|al\\s+|alla\\s+|allo\\s+|ai\\s+|alle\\s+|in\\s+|da\\s+|dal\\s+|dalla\\s+)?(.+)$")
    private val numbers = mapOf(
        "zero" to 0, "uno" to 1, "due" to 2, "tre" to 3, "quattro" to 4, "cinque" to 5, "sei" to 6, "sette" to 7,
        "otto" to 8, "nove" to 9, "dieci" to 10, "ottantotto" to 88, "ottantanove" to 89, "novanta" to 90,
        "novantuno" to 91, "novantadue" to 92, "novantatre" to 93, "novantaquattro" to 94, "novantacinque" to 95,
        "novantasei" to 96, "novantasette" to 97, "novantotto" to 98, "novantanove" to 99, "cento" to 100,
        "centouno" to 101, "centodue" to 102, "centotre" to 103, "centoquattro" to 104, "centocinque" to 105,
        "centosei" to 106, "centosette" to 107, "centootto" to 108, "centotto" to 108,
    )

    fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9àèéìòù ]"), " ").replace(Regex("\\s+"), " ").trim()

    fun parse(raw: String): VoiceCommand {
        val t = normalize(raw)
        if (t.isEmpty()) return VoiceCommand.Unknown(raw)
        fun has(vararg w: String) = w.any { Regex("\\b$it\\b").containsMatchIn(t) }

        if (has("termina", "ferma", "interrompi", "annulla", "chiudi") && has("navigazione", "percorso", "navigatore", "guida"))
            return VoiceCommand.StopNav
        if (Regex("\\b(a|verso) casa\\b").containsMatchIn(t) && has("portami", "vai", "andiamo", "naviga", "torna", "torniamo", "portaci", "guidami"))
            return VoiceCommand.GoHome
        if (Regex("\\b(al|a) lavoro\\b|\\bin ufficio\\b").containsMatchIn(t) && has("portami", "vai", "andiamo", "naviga", "portaci", "guidami"))
            return VoiceCommand.GoWork
        navPrefix.find(t)?.let { m ->
            val d = m.groupValues[1].trim()
            if (d == "casa") return VoiceCommand.GoHome
            if (d == "lavoro") return VoiceCommand.GoWork
            if (d.isNotEmpty()) return VoiceCommand.Navigate(d)
        }
        frequency(t)?.let { return VoiceCommand.Frequency(it) }
        Regex("^(?:metti|ascolta|accendi|sintonizza|vai su)?\\s*(?:la\\s+)?radio\\s+(.+)$").find(t)?.let { m ->
            return VoiceCommand.Station(m.groupValues[1].trim())
        }
        Regex("^(?:metti|ascolta|sintonizza)\\s+(.+)$").find(t)?.let { m ->
            val n = m.groupValues[1].trim()
            if (n == "musica" || n == "la musica") return VoiceCommand.Music
            if (n == "la radio" || n == "radio") return VoiceCommand.Radio
            return VoiceCommand.Station(n)
        }
        return when {
            has("alza", "aumenta") || t.contains("volume su") || t.contains("più forte") -> VoiceCommand.VolumeUp
            has("abbassa", "diminuisci") || t.contains("volume giù") || t.contains("più piano") -> VoiceCommand.VolumeDown
            has("muto", "silenzio", "zitto") -> VoiceCommand.Mute
            has("prossima", "prossimo", "successiva", "successivo", "avanti", "salta") -> VoiceCommand.Next
            has("precedente", "indietro") -> VoiceCommand.Previous
            has("pausa", "stop", "ferma") -> VoiceCommand.Pause
            has("riprendi", "play", "continua", "riproduci") -> VoiceCommand.Play
            has("radio") -> VoiceCommand.Radio
            has("musica", "canzone", "canzoni", "brani") -> VoiceCommand.Music
            has("clima", "climatizzatore", "aria") -> VoiceCommand.Climate
            has("veicolo", "auto", "macchina", "carburante", "benzina", "gasolio") -> VoiceCommand.Vehicle
            has("telefono", "chiama", "telefona") -> VoiceCommand.Phone
            has("home", "inizio", "principale") -> VoiceCommand.Home
            else -> VoiceCommand.Unknown(raw)
        }
    }

    /** "frequenza 101 punto 5", "novantanove virgola due", "101.5" → kHz. */
    fun frequency(t: String): Int? {
        Regex("(\\d{2,3})[ .,](\\d)\\b").find(t)?.let { m ->
            val khz = m.groupValues[1].toInt() * 1000 + m.groupValues[2].toInt() * 100
            if (khz in 87500..108000 && (t.contains("frequenza") || t.contains("sintonizza") || t.contains("mhz") || t.contains("radio"))) return khz
        }
        Regex("(?:frequenza|sintonizza)\\s+(\\S+)\\s+(?:punto|virgola)\\s+(\\S+)").find(t)?.let { m ->
            val a = m.groupValues[1].toIntOrNull() ?: numbers[m.groupValues[1]] ?: return null
            val b = m.groupValues[2].toIntOrNull() ?: numbers[m.groupValues[2]] ?: return null
            val khz = a * 1000 + b * 100
            if (khz in 87500..108000) return khz
        }
        return null
    }
}
