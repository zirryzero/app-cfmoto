package dev.zanderp.opencfmoto

import android.os.Build
import org.json.JSONObject
import java.io.OutputStream

/** Android Auto resolutions retained for display tuning and backwards-compatible preferences. */
enum class AaResolution(val w: Int, val h: Int) {
    PORTRAIT_720x1280(720, 1280),
    PORTRAIT_1080x1920(1080, 1920),
}

data class AaVideoSpec(val resolution: AaResolution, val dpi: Int) {
    val width: Int get() = resolution.w
    val height: Int get() = resolution.h
}

/** Unused pixels in the coded Android Auto frame. */
data class AaMargins(val marginW: Int, val marginH: Int) {
    val any: Boolean get() = marginW > 0 || marginH > 0

    companion object {
        val NONE = AaMargins(0, 0)

        fun forAspect(coded: AaVideoSpec, targetW: Int, targetH: Int): AaMargins {
            if (targetW <= 0 || targetH <= 0) return NONE
            val cw = coded.width
            val ch = coded.height
            val codedAspect = cw.toDouble() / ch
            val targetAspect = targetW.toDouble() / targetH
            return if (codedAspect < targetAspect) {
                val usableH = Math.round(cw * targetH.toDouble() / targetW).toInt().coerceIn(16, ch)
                AaMargins(0, (ch - usableH).coerceAtLeast(0))
            } else {
                val usableW = Math.round(ch * targetW.toDouble() / targetH).toInt().coerceIn(16, cw)
                AaMargins((cw - usableW).coerceAtLeast(0), 0)
            }
        }
    }
}

/** Protocol behavior required by the CFMOTO 800NK Advanced dashboard. */
interface BikeProfile {
    val name: String
    fun score(info: JSONObject): Int
    fun matchesModelId(modelId: String): Boolean
    val aaVideo: AaVideoSpec
    val requiresSockServerAuth: Boolean
    val supportsScreenTouch: Boolean
    val advertisedSupportFunction: Int
    fun buildClientInfoReply(info: JSONObject, huid: String?, phoneUuid: String): JSONObject
    fun handleUnknownControl(
        tag: String,
        frame: PxcFrame,
        out: OutputStream,
        log: (String) -> Unit,
    ): Boolean

    fun roundCaptureDimensions(w: Int, h: Int): Pair<Int, Int> =
        (w and 0xFFF0) to (h and 0xFFF0)

    val forceBaseline: Boolean get() = true
    val videoBitrate: Int get() = 2_500_000
    val videoFrameRate: Int get() = 30
    val videoIFrameIntervalSec: Int get() = 1
    fun versionReply(): Pair<Int, Int> = 3 to 1
    val panelSize: Pair<Int, Int> get() = 720 to 712
    val defaultMargins: IntArray get() = intArrayOf(22, 0, 0, 0)
}

/** The app intentionally supports one motorcycle and never selects an alternate profile. */
object BikeProfiles {
    val only: BikeProfile = Cfdl26NkTouchProfile

    fun select(info: JSONObject, log: (String) -> Unit): BikeProfile {
        val score = only.score(info)
        if (score <= 0) {
            log("[profile] unexpected CLIENT_INFO; keeping fixed 800NK Advanced profile")
        } else {
            log("[profile] 800NK Advanced match score=$score")
        }
        return only
    }

    fun selectByQr(qr: QrData?, context: android.content.Context? = null): BikeProfile = only
    fun selectByModelId(modelId: String?): BikeProfile = only
}

object BikeProfileHolder {
    @Volatile var active: BikeProfile = BikeProfiles.only
    @Volatile var aaVideoOverride: AaVideoSpec? = null
    @Volatile var aaContentMargins: AaMargins = AaMargins.NONE

    val aaVideo: AaVideoSpec get() = aaVideoOverride ?: active.aaVideo
    val aaUsableWidth: Int
        get() = (aaVideo.width - aaContentMargins.marginW).coerceIn(1, aaVideo.width)
    val aaUsableHeight: Int
        get() = (aaVideo.height - aaContentMargins.marginH).coerceIn(1, aaVideo.height)
    val advertisesScreenTouch: Boolean get() = true
}

private fun basePhoneClientInfo(huid: String?, phoneUuid: String): JSONObject =
    JSONObject().apply {
        put("pxcVersion", "1.0.2")
        put("phoneUUID", phoneUuid)
        put("phoneBrand", Build.BRAND)
        put("phoneModel", Build.MODEL)
        put("phoneOsVersion", Build.VERSION.SDK_INT.toString())
        put("phoneOs", "Android")
        put("package", EasyConnProber.SPOOFED_PACKAGE)
        put("versionCode", 126)
        put("token", 0)
        put("pubkey", RsaKeys.publicKeyBase64)
        put("encryptedHUID", huid?.let { RsaKeys.signHuid(it) } ?: "")
        put("bluetoothName", "800NK ADV Link")
        put("supportH264IFrame", true)
        put("supportFunction", 128)
        put("supportSyncCorrectTime", false)
        put("appVersionFingerPrint", "800nk-adv-link")
    }

/** Clock acknowledgement used by the CFDL26 control channel. */
internal object HuTimeSync {
    private const val PAYLOAD_LEN = 45
    private const val TIME_OFF = 16
    private const val TIME_LEN = 29
    private val stampRe = Regex("""^(\d{4})-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}""")
    private val timeFmt = java.text.SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        java.util.Locale.US,
    ).apply { timeZone = java.util.TimeZone.getDefault() }

    data class Ack(val payload: ByteArray, val mode: String, val stamp: String)

    fun ackPayload(request: ByteArray): ByteArray = ack(request).payload

    fun ack(request: ByteArray): Ack {
        val len = maxOf(PAYLOAD_LEN, request.size)
        val out = ByteArray(len)
        if (request.isNotEmpty()) {
            System.arraycopy(request, 0, out, 0, minOf(request.size, len))
        }
        if (request.size < TIME_OFF) {
            val bb = java.nio.ByteBuffer.wrap(out).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            bb.putInt(-2)
            bb.putInt(0)
            bb.putInt(1)
            bb.putInt(0)
        }
        val bikeStamp = extractStamp(request)
        val stamp: String
        val mode: String
        if (shouldEcho(bikeStamp, request)) {
            stamp = bikeStamp.ifBlank { extractStamp(out) }
            mode = "echo"
        } else {
            stamp = phoneStamp()
            mode = "phone"
            val ascii = stamp.toByteArray(Charsets.US_ASCII)
            System.arraycopy(ascii, 0, out, TIME_OFF, minOf(TIME_LEN, ascii.size))
        }
        return Ack(out, mode, stamp)
    }

    internal fun extractStamp(buf: ByteArray): String {
        if (buf.size < TIME_OFF) return ""
        val n = minOf(TIME_LEN, buf.size - TIME_OFF)
        return String(buf, TIME_OFF, n, Charsets.US_ASCII).trimEnd('\u0000', ' ')
    }

    internal fun shouldEcho(stamp: String, request: ByteArray): Boolean {
        if (request.size < TIME_OFF) return false
        if (stamp.isBlank() || stamp.all { it == '\u0000' || it == ' ' }) return false
        val year = stampRe.find(stamp)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return year == null || year !in 1969..1971
    }

    internal fun isSaneStamp(stamp: String): Boolean {
        val year = stampRe.find(stamp)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return false
        return year in 2020..2099
    }

    private fun phoneStamp(): String {
        val ms = System.currentTimeMillis()
        return synchronized(timeFmt) {
            timeFmt.timeZone = java.util.TimeZone.getDefault()
            String.format(
                java.util.Locale.US,
                "%s.%03d000000",
                timeFmt.format(java.util.Date(ms)),
                (ms % 1000L).toInt(),
            )
        }
    }
}

/** Fixed CFDL26 profile for the CFMOTO 800NK Advanced touch panel. */
object Cfdl26NkTouchProfile : BikeProfile {
    override val name = "CFMOTO 800NK Advanced"
    override val requiresSockServerAuth = true
    override val supportsScreenTouch = true
    override val advertisedSupportFunction = 128
    override val panelSize = 720 to 712
    override val aaVideo = AaVideoSpec(AaResolution.PORTRAIT_720x1280, dpi = 160)
    override val defaultMargins = intArrayOf(22, 0, 0, 0)

    override fun matchesModelId(modelId: String): Boolean = modelId.trim() == "37426"

    override fun score(info: JSONObject): Int {
        var score = 0
        if (info.optString("version_name").startsWith("CFDL26")) score += 4
        if (info.optString("package_name") == "com.cfmoto.easyconnect") score += 3
        if (info.optBoolean("enableSockServerAuth", false)) score += 2
        val sdk = info.optString("sdkVersion")
        if (sdk.isNotEmpty() && !sdk.startsWith("0.")) score += 2
        if (info.optString("HUID").startsWith("6KWV")) score += 4
        if (info.optBoolean("supportMirrorOverlayTouch", false)) score += 1
        if (info.optBoolean("supportScreenTouch", false)) score += 1
        return score
    }

    override fun buildClientInfoReply(
        info: JSONObject,
        huid: String?,
        phoneUuid: String,
    ): JSONObject = basePhoneClientInfo(huid, phoneUuid).apply {
        put("supportScreenTouch", true)
    }

    override fun handleUnknownControl(
        tag: String,
        frame: PxcFrame,
        out: OutputStream,
        log: (String) -> Unit,
    ): Boolean {
        if (frame.cmd == PxcFrame.CMD_HU_TIME_SYNC) {
            val ack = HuTimeSync.ack(frame.payload)
            log("[$tag] HU_TIME_SYNC len=${frame.payload.size} -> ack 0x10601 mode=${ack.mode}")
            PxcFrame(PxcFrame.CMD_HU_TIME_SYNC_ACK, ack.payload).write(out)
            return true
        }

        val payload = if (frame.payload.isEmpty()) "" else String(frame.payload, Charsets.UTF_8)
        val hex = if (frame.payload.isEmpty()) "" else
            " hex=" + BleProtocol.bytesToHex(frame.payload.copyOf(minOf(48, frame.payload.size)))
        val ack = frame.cmd + 1
        log(
            "[$tag] 800NK ctrl ${frame.cmdHex()} (${PxcFrame.nameOf(frame.cmd)}) " +
                "len=${frame.payload.size} $payload$hex -> ack 0x${ack.toUInt().toString(16)}",
        )
        PxcFrame(ack, ByteArray(0)).write(out)
        return true
    }
}
