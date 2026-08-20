package dev.zanderp.opencfmoto

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Pairing data read from the MotoPlay QR shown by the CFMOTO 800NK Advanced. */
data class QrData(
    val ssid: String,
    val pwd: String,
    val auth: String?,
    val mac: String?,
    val name: String?,
    val action: Int,
    val modelId: String?,
    val sn: String?,
    val channel: String?,
) {
    val supportsAp: Boolean get() = true

    companion object {
        const val MODEL_ID = "37426"

        fun parse(raw: String): QrData? {
            val query = queryParams(raw.trim())
            if (query.isEmpty()) return null

            val modelId = query["modelid"]?.trim()?.takeIf { it.isNotEmpty() }
            if (modelId != null && modelId != MODEL_ID) return null

            val ssid = query["ssid"].orEmpty().trim()
            val pwd = query["pwd"].orEmpty()
            if (ssid.isEmpty() || pwd.isEmpty()) return null

            return QrData(
                ssid = ssid,
                pwd = pwd,
                auth = query["auth"],
                mac = formatMac(query["mac"] ?: query["bm"]),
                name = "800NK Advanced",
                action = 1,
                modelId = modelId,
                sn = query["sn"],
                channel = query["channel"],
            )
        }

        fun parseFailureHint(raw: String): String? {
            val query = queryParams(raw.trim())
            val modelId = query["modelid"]?.trim()
            return when {
                !modelId.isNullOrEmpty() && modelId != MODEL_ID ->
                    "Este QR no corresponde a una CFMOTO 800NK Advanced"
                query["ssid"].isNullOrBlank() || query["pwd"].isNullOrEmpty() ->
                    "Abre MotoPlay en la 800NK Advanced y escanea el QR que incluye Wi-Fi y contrasena"
                else -> null
            }
        }

        private fun queryParams(raw: String): Map<String, String> {
            val query = raw.substringAfter('?', missingDelimiterValue = "").substringBefore('#')
            if (query.isEmpty()) return emptyMap()
            val out = LinkedHashMap<String, String>()
            for (part in query.split('&')) {
                if (part.isEmpty()) continue
                val eq = part.indexOf('=')
                val key = if (eq >= 0) part.substring(0, eq) else part
                val value = if (eq >= 0) part.substring(eq + 1) else ""
                if (key.isEmpty()) continue
                out[key.lowercase()] = runCatching {
                    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                }.getOrDefault(value)
            }
            return out
        }

        private fun formatMac(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val hex = raw.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
            if (hex.length != 12) return raw.takeIf { it.contains(':') && it.length >= 11 }
            return hex.chunked(2).joinToString(":") { it.lowercase() }
        }
    }
}
