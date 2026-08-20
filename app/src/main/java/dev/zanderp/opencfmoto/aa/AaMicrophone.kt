// SPDX-License-Identifier: AGPL-3.0-or-later
// OpenCfMoto glue (uses AGPLv3 protocol from headunit-revived). Streams the phone's microphone to
// Android Auto over the AAP MIC channel, so "Hey Google" / the Assistant button works — the only
// hands-free way to set a destination on a bike. Ported from the ionutradu252/open-cfmoto fork.
package dev.zanderp.opencfmoto.aa

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import dev.zanderp.opencfmoto.aa.proto.Common
import dev.zanderp.opencfmoto.aa.proto.Media
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * The head unit's microphone, as far as Android Auto is concerned.
 *
 * AA asks for the mic with MICROPHONE_REQUEST(open=true) whenever the Assistant starts; we answer
 * MICROPHONE_RESPONSE and then push raw PCM up the MIC channel until it asks us to close.
 *
 * Audio source: a rider headset paired directly to the phone, or the phone microphone. The
 * `CFMOTO_BT` hands-free endpoint is deliberately excluded because selecting it opens the bike's
 * call UI and does not represent a directly usable rider microphone.
 *
 * Format must match what [ServiceDiscoveryResponse] advertises: 16 kHz, 16-bit, mono.
 */
class AaMicrophone(
    private val context: Context,
    private val transport: AapTransport,
    private val log: (String) -> Unit,
) {
    companion object {
        const val SAMPLE_RATE = 16000
        /** ~20 ms of audio per message. */
        private const val CHUNK_SAMPLES = SAMPLE_RATE / 50

        /** The bike's HFP endpoint opens its call UI but does not provide a rider microphone. */
        internal fun isBikeHandsFreeRoute(productName: CharSequence?): Boolean =
            productName?.toString()?.contains("CFMOTO", ignoreCase = true) == true

        /** Headunit Revived's Android microphone path timestamps media in elapsed milliseconds. */
        internal fun timestampMillisFromElapsedNanos(elapsedNanos: Long): Long =
            elapsedNanos / 1_000_000L

        /** Build `[header][timestamp ms, BE][PCM16, LE]`, the AAP microphone media layout. */
        internal fun buildMicData(samples: ShortArray, count: Int, timestampMillis: Long): AapMessage {
            require(count in 0..samples.size)
            val total = AapMessage.HEADER_SIZE + Long.SIZE_BYTES + count * 2
            val data = ByteArray(total)
            data[0] = Channel.ID_MIC.toByte()
            data[1] = 0x0b
            val bb = ByteBuffer.wrap(data, AapMessage.HEADER_SIZE, total - AapMessage.HEADER_SIZE)
                .order(ByteOrder.BIG_ENDIAN)
            bb.putLong(timestampMillis)
            bb.order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until count) bb.putShort(samples[i])
            return AapMessage(Channel.ID_MIC, 0x0b, -1, 2, total, data)
        }

        internal fun signalPeak(samples: ShortArray, count: Int): Int {
            var peak = 0
            for (i in 0 until count.coerceAtMost(samples.size)) {
                val level = if (samples[i] == Short.MIN_VALUE) 32768
                    else kotlin.math.abs(samples[i].toInt())
                if (level > peak) peak = level
            }
            return peak
        }
    }

    @Volatile private var recording = false
    private var recorder: AudioRecord? = null
    private var thread: Thread? = null
    @Volatile private var sessionId = 0
    private var audioManager: AudioManager? = null
    private var previousAudioMode = AudioManager.MODE_NORMAL

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Handle AA's MICROPHONE_REQUEST: open or close the mic, and answer it. */
    fun onRequest(open: Boolean, channel: Int) {
        if (open) {
            dev.zanderp.opencfmoto.MediaButtonBridge.instance?.setVoiceActive(true)
            start()
            if (!recording) dev.zanderp.opencfmoto.MediaButtonBridge.instance?.setVoiceActive(false)
        } else {
            stop("AA closed the mic")
            dev.zanderp.opencfmoto.MediaButtonBridge.instance?.setVoiceActive(false)
        }
        // Answer either way, so AA isn't left waiting. If the open failed (no permission, mic busy)
        // say so rather than claiming success — otherwise AA sits listening to a stream we never send.
        val status = if (!open || recording) Common.MessageStatus.STATUS_SUCCESS_VALUE
                     else Common.MessageStatus.STATUS_INTERNAL_ERROR_VALUE
        transport.send(
            AapMessage(
                channel, Media.MsgType.MEDIA_MESSAGE_MICROPHONE_RESPONSE_VALUE,
                Media.MicrophoneResponse.newBuilder()
                    .setStatus(status).setSessionId(sessionId).build()
            )
        )
        log("[MIC] request open=$open → ${if (recording) "recording" else "closed"}")
    }

    fun setSessionId(id: Int) { sessionId = id }

    @SuppressLint("MissingPermission")
    private fun start() {
        if (recording) return
        if (!hasPermission()) {
            log("[MIC] no RECORD_AUDIO permission — voice won't work. Grant it in the app.")
            return
        }
        try {
            dev.zanderp.opencfmoto.AndroidAutoService.updateForegroundType()
        } catch (e: Exception) {
            log("[MIC] failed to update service foreground type: $e")
        }
        try {
            val audioSource = configureAudioRoute()
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(CHUNK_SAMPLES * 2 * 4)
            val r = AudioRecord(
                audioSource,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf
            )
            if (r.state != AudioRecord.STATE_INITIALIZED) {
                log("[MIC] AudioRecord init failed")
                r.release()
                releaseBluetoothMic()
                return
            }
            recorder = r
            recording = true
            r.startRecording()
            thread = thread(name = "aa-mic", isDaemon = true) { pump(r) }
            log("[MIC] recording started (${SAMPLE_RATE}Hz mono) → Android Auto")
        } catch (e: Exception) {
            log("[MIC] start failed: $e")
            recording = false
            releaseBluetoothMic()
        }
    }

    /** Prefer a directly paired rider headset, but never the bike's call-profile endpoint. */
    private fun configureAudioRoute(): Int {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager = am
            previousAudioMode = am.mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val bt = am.availableCommunicationDevices.firstOrNull {
                    (it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET) &&
                        !isBikeHandsFreeRoute(it.productName)
                }
                if (bt != null) {
                    am.mode = AudioManager.MODE_IN_COMMUNICATION
                    if (am.setCommunicationDevice(bt)) {
                        log("[MIC] using rider headset mic (${bt.productName})")
                        return MediaRecorder.AudioSource.VOICE_COMMUNICATION
                    }
                }
                val bikeRoute = am.availableCommunicationDevices.firstOrNull {
                    (it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET) &&
                        isBikeHandsFreeRoute(it.productName)
                }
                if (bikeRoute != null) {
                    log("[MIC] ignoring bike hands-free route (${bikeRoute.productName}); using phone mic")
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.clearCommunicationDevice()
            am.mode = previousAudioMode
            log("[MIC] using phone microphone")
        } catch (e: Exception) {
            log("[MIC] audio routing failed ($e); using phone microphone")
        }
        return MediaRecorder.AudioSource.VOICE_RECOGNITION
    }

    private fun releaseBluetoothMic() {
        try {
            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.clearCommunicationDevice()
                am.mode = previousAudioMode
            }
        } catch (_: Exception) {}
        audioManager = null
    }

    /** Read PCM and push it up the MIC channel as AAP media-data messages. */
    private fun pump(r: AudioRecord) {
        val buf = ShortArray(CHUNK_SAMPLES)
        var sent = 0L
        var maxPeak = 0
        while (recording) {
            val n = try { r.read(buf, 0, buf.size) } catch (e: Exception) { break }
            if (n <= 0) continue
            try {
                transport.send(
                    buildMicData(buf, n, timestampMillisFromElapsedNanos(SystemClock.elapsedRealtimeNanos()))
                )
                maxPeak = maxOf(maxPeak, signalPeak(buf, n))
                sent++
                if (sent == 50L || sent % 250L == 0L) {
                    log("[MIC] chunks sent=$sent peak=$maxPeak/32768")
                    maxPeak = 0
                }
            } catch (e: Exception) {
                log("[MIC] send failed: $e"); break
            }
        }
    }

    fun stop(reason: String) {
        if (!recording && recorder == null) return
        recording = false
        try { thread?.interrupt() } catch (_: Exception) {}
        thread = null
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        releaseBluetoothMic()
        log("[MIC] stopped ($reason)")
    }
}
