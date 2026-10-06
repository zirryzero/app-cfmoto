// SPDX-License-Identifier: AGPL-3.0-or-later
package dev.zanderp.opencfmoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpstreamCompatibilityTest {
    @Test
    fun androidAutoHandshakeDropRetriesOnlyOnce() {
        BikeLink.beginHandoff()

        assertTrue(BikeLink.takeAaDropRetry())
        assertFalse(BikeLink.takeAaDropRetry())
    }

    @Test
    fun dpiOverrideKeepsTheOptimized800NkResolution() {
        val previousVideo = BikeProfileHolder.aaVideoOverride
        val previousDpi = BikeProfileHolder.aaDpiOverride
        try {
            BikeProfileHolder.aaVideoOverride = AaVideoSpec(AaResolution.LANDSCAPE_1280x720, 160)
            BikeProfileHolder.aaDpiOverride = 240

            assertEquals(AaResolution.LANDSCAPE_1280x720, BikeProfileHolder.aaVideo.resolution)
            assertEquals(240, BikeProfileHolder.aaVideo.dpi)
        } finally {
            BikeProfileHolder.aaVideoOverride = previousVideo
            BikeProfileHolder.aaDpiOverride = previousDpi
        }
    }
}
