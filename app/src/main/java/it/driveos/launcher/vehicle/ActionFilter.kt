package it.driveos.launcher.vehicle

/**
 * Decide quali stringhe estratte da un'app sembrano nomi di azioni broadcast
 * (es. "com.xxx.can.ACTION_DOOR", "com.xxx.radio.FREQ_CHANGED").
 * Meglio qualche falso positivo (un receiver in più non costa nulla) che perdere il dato.
 */
object ActionFilter {

    private val candidate = Regex("[a-z][a-z0-9_]*(\\.[A-Za-z0-9_]+){2,}")

    private val excludedPrefixes = listOf(
        "java.", "javax.", "kotlin.", "kotlinx.", "androidx.", "android.support.", "dalvik.",
        "com.google.", "org.json.", "org.apache.", "org.w3c.", "org.xml.", "sun.", "libcore.",
        "okhttp3.", "okio.", "retrofit2.", "io.reactivex.", "com.squareup.", "com.facebook.",
        "com.android.internal.", "android.",
    )

    /** Azioni di sistema utili da ascoltare sempre (stato schermo, volume, alimentazione). */
    val systemActions = listOf(
        "android.media.VOLUME_CHANGED_ACTION",
        "android.media.STREAM_MUTE_CHANGED_ACTION",
        "android.intent.action.SCREEN_ON",
        "android.intent.action.SCREEN_OFF",
        "android.intent.action.ACTION_POWER_CONNECTED",
        "android.intent.action.ACTION_POWER_DISCONNECTED",
        "android.intent.action.CONFIGURATION_CHANGED",
        "android.intent.action.HEADSET_PLUG",
        "android.bluetooth.adapter.action.STATE_CHANGED",
        "android.bluetooth.device.action.ACL_CONNECTED",
        "android.bluetooth.device.action.ACL_DISCONNECTED",
        "android.intent.action.MEDIA_MOUNTED",
    )

    private val classSuffixes = listOf(
        "Activity", "Service", "Receiver", "Provider", "Application", "Fragment", "View", "Exception",
        "Manager", "Helper", "Adapter", "Impl", "Utils", "Util", "Listener", "Callback", "Dialog",
        "Layout", "Widget", "Builder", "Factory", "Handler", "Thread", "Task", "Stub", "Proxy",
        "Interface", "Info", "Bean", "Model", "Config", "Holder", "Presenter", "Contract", "Kt",
        "java", "kt", "xml", "png", "jpg", "so", "html", "json", "txt", "dex", "apk",
    )

    /** Estrae da una stringa grezza i nomi che sembrano azioni. */
    fun extract(raw: String): List<String> =
        candidate.findAll(raw).map { it.value.trimEnd('.') }.filter { looksLikeAction(it) }.toList()

    fun looksLikeAction(s: String): Boolean {
        if (s.length !in 10..150) return false
        if (excludedPrefixes.any { s.startsWith(it) }) return false
        val parts = s.split('.')
        if (parts.size < 3 || parts.any { it.isEmpty() }) return false
        val last = parts.last()
        if (classSuffixes.any { last.endsWith(it) && last != it.uppercase() }) return false
        val lower = s.lowercase()
        // indizi forti: parole tipiche delle azioni
        if (".action." in lower || "action_" in lower || ".intent." in lower || "broadcast" in lower) return true
        // ULTIMO_SEGMENTO in maiuscolo o con trattino basso: tipico delle costanti broadcast
        val upper = last.any { it.isUpperCase() } && last == last.uppercase()
        val underscore = '_' in last && last.any { it.isLetter() }
        if (upper && last.length >= 3) return true
        if (underscore) return true
        // parole chiave dell'auto in minuscolo (es. "com.xxx.can.door")
        val carWords = listOf("can", "canbus", "mcu", "radio", "fm", "ill", "light", "lamp", "reverse",
            "door", "fuel", "tpms", "tire", "tyre", "warning", "carinfo", "vehicle", "acc", "brake",
            "toppal", "txz", "junsun", "forfan", "obd", "cluster", "rpm", "speed", "coolant", "oil", "temp")
        return parts.drop(1).any { it.lowercase() in carWords } && last.all { it.isLowerCase() || it.isDigit() || it == '_' }
    }
}
