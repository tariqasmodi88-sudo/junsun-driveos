package it.driveos.launcher.voice

/** Lingue dei comandi vocali: codice DriveOS, nome, codice Vosk e modello "small" di riserva. */
data class VoiceLanguage(val code: String, val label: String, val vosk: String, val fallbackModel: String)

object VoiceLanguages {
    val all = listOf(
        VoiceLanguage("it", "Italiano", "it", "vosk-model-small-it-0.22"),
        VoiceLanguage("en", "English", "en-us", "vosk-model-small-en-us-0.15"),
        VoiceLanguage("fr", "Français", "fr", "vosk-model-small-fr-0.22"),
        VoiceLanguage("de", "Deutsch", "de", "vosk-model-small-de-0.15"),
        VoiceLanguage("es", "Español", "es", "vosk-model-small-es-0.42"),
        VoiceLanguage("pt", "Português", "pt", "vosk-model-small-pt-0.3"),
    )
    fun of(code: String?) = all.firstOrNull { it.code == code } ?: all.first()
}

/** Parole chiave per le lingue diverse dall'italiano (l'italiano ha regole piu' ricche in VoiceCommands). */
internal class Lexicon(
    val navPrefix: Regex,
    val home: List<String>, val work: List<String>,
    val stopNav: List<String>, val navWords: List<String>,
    val radio: List<String>, val music: List<String>,
    val next: List<String>, val prev: List<String>, val pause: List<String>, val play: List<String>,
    val volUp: List<String>, val volDown: List<String>, val mute: List<String>,
    val climate: List<String>, val vehicle: List<String>, val homeScreen: List<String>, val phone: List<String>,
    val putOn: Regex,
)

internal object Lexicons {
    val map: Map<String, Lexicon> = mapOf(
        "en" to Lexicon(
            Regex("^(?:take me|navigate|drive|go|directions|route)\\s+(?:to\\s+|towards\\s+)?(.+)$"),
            listOf("home"), listOf("work", "office"), listOf("stop", "cancel", "end", "exit"), listOf("navigation", "route", "directions"),
            listOf("radio"), listOf("music", "song", "songs"), listOf("next", "skip"), listOf("previous", "back"),
            listOf("pause", "stop"), listOf("play", "resume", "continue"), listOf("louder", "volume up", "turn up"),
            listOf("quieter", "volume down", "turn down"), listOf("mute", "silence"), listOf("climate", "air", "temperature"),
            listOf("vehicle", "car", "fuel"), listOf("home screen", "main screen"), listOf("phone", "call"),
            Regex("^(?:play|put on|tune to|listen to)\\s+(?:the\\s+)?(?:radio\\s+)?(.+)$"),
        ),
        "fr" to Lexicon(
            Regex("^(?:emmene moi|emmène moi|va|aller|navigue|naviguer|itinéraire|itineraire|conduis moi)\\s+(?:à\\s+|a\\s+|au\\s+|vers\\s+|chez\\s+)?(.+)$"),
            listOf("maison", "chez moi"), listOf("travail", "bureau"), listOf("arrête", "arrete", "annule", "termine"), listOf("navigation", "itinéraire", "guidage"),
            listOf("radio"), listOf("musique", "chanson"), listOf("suivant", "suivante"), listOf("précédent", "precedent", "précédente"),
            listOf("pause"), listOf("lecture", "reprendre", "joue"), listOf("monte le son", "plus fort", "augmente"),
            listOf("baisse", "moins fort"), listOf("muet", "silence", "coupe le son"), listOf("climatisation", "clim", "climat"),
            listOf("véhicule", "vehicule", "voiture", "carburant"), listOf("accueil"), listOf("téléphone", "telephone", "appelle"),
            Regex("^(?:mets|écoute|ecoute)\\s+(?:la\\s+)?(?:radio\\s+)?(.+)$"),
        ),
        "de" to Lexicon(
            Regex("^(?:bring mich|fahre|fahr|navigiere|navigation|route)\\s+(?:nach\\s+|zu\\s+|zur\\s+|zum\\s+|in\\s+die\\s+)?(.+)$"),
            listOf("nach hause", "heim"), listOf("arbeit", "büro", "buero"), listOf("beende", "stopp", "abbrechen"), listOf("navigation", "route", "zielführung"),
            listOf("radio"), listOf("musik", "lied"), listOf("nächster", "naechster", "weiter", "nächste"), listOf("zurück", "zurueck", "vorheriger"),
            listOf("pause"), listOf("abspielen", "fortsetzen", "spiel"), listOf("lauter"), listOf("leiser"), listOf("stumm", "ruhe"),
            listOf("klima", "klimaanlage"), listOf("fahrzeug", "auto", "tank"), listOf("startseite"), listOf("telefon", "anrufen"),
            Regex("^(?:spiele|spiel|höre|hoere)\\s+(?:radio\\s+)?(.+)$"),
        ),
        "es" to Lexicon(
            Regex("^(?:llévame|llevame|ve|ir|navega|navegar|ruta|vamos)\\s+(?:a\\s+|al\\s+|hacia\\s+)?(.+)$"),
            listOf("casa"), listOf("trabajo", "oficina"), listOf("termina", "detén", "deten", "cancela", "para"), listOf("navegación", "navegacion", "ruta"),
            listOf("radio"), listOf("música", "musica", "canción", "cancion"), listOf("siguiente"), listOf("anterior"),
            listOf("pausa"), listOf("reproduce", "continúa", "continua"), listOf("sube el volumen", "más alto", "mas alto"),
            listOf("baja el volumen", "más bajo", "mas bajo"), listOf("silencio", "mudo"), listOf("clima", "aire"),
            listOf("vehículo", "vehiculo", "coche", "combustible"), listOf("inicio"), listOf("teléfono", "telefono", "llama"),
            Regex("^(?:pon|escucha|sintoniza)\\s+(?:la\\s+)?(?:radio\\s+)?(.+)$"),
        ),
        "pt" to Lexicon(
            Regex("^(?:leva me|leve me|vai|ir|navega|navegar|rota|vamos)\\s+(?:para\\s+|a\\s+|ao\\s+|até\\s+)?(.+)$"),
            listOf("casa"), listOf("trabalho", "escritório", "escritorio"), listOf("termina", "pára", "para", "cancela"), listOf("navegação", "navegacao", "rota"),
            listOf("rádio", "radio"), listOf("música", "musica", "canção"), listOf("próxima", "proxima", "seguinte"), listOf("anterior"),
            listOf("pausa"), listOf("toca", "continua", "retoma"), listOf("aumenta", "mais alto"), listOf("diminui", "baixa", "mais baixo"),
            listOf("silêncio", "silencio", "mudo"), listOf("clima", "ar condicionado"), listOf("veículo", "veiculo", "carro", "combustível"),
            listOf("início", "inicio"), listOf("telefone", "liga"),
            Regex("^(?:põe|poe|ouve|sintoniza)\\s+(?:a\\s+)?(?:rádio\\s+|radio\\s+)?(.+)$"),
        ),
    )
}
