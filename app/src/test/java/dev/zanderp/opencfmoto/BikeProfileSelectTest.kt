package dev.zanderp.opencfmoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BikeProfileSelectTest {
    @Test
    fun fixedProfileMatches800nkAdvancedContract() {
        assertEquals("CFMOTO 800NK Advanced", Cfdl26NkTouchProfile.name)
        assertTrue(Cfdl26NkTouchProfile.matchesModelId(QrData.MODEL_ID))
        assertFalse(Cfdl26NkTouchProfile.matchesModelId("other"))
        assertTrue(Cfdl26NkTouchProfile.supportsScreenTouch)
        assertTrue(Cfdl26NkTouchProfile.requiresSockServerAuth)
        assertEquals(720 to 712, Cfdl26NkTouchProfile.panelSize)
        assertEquals(AaResolution.PORTRAIT_720x1280, Cfdl26NkTouchProfile.aaVideo.resolution)
    }

    @Test
    fun registryNeverSelectsAnAlternateMotorcycle() {
        assertSame(Cfdl26NkTouchProfile, BikeProfiles.selectByModelId(QrData.MODEL_ID))
        assertSame(Cfdl26NkTouchProfile, BikeProfiles.selectByModelId("unexpected"))
        assertSame(Cfdl26NkTouchProfile, BikeProfiles.only)
    }

    @Test
    fun buttonDefaultsRemainAvailableFor800nkControls() {
        assertEquals(ButtonAction.KNOB_BACK, ButtonGesture.NAV_BACK.default)
        assertEquals(ButtonAction.KNOB_FORWARD, ButtonGesture.NAV_FWD.default)
        assertEquals(ButtonAction.SELECT, ButtonGesture.SELECT_PRESS.default)
        assertEquals(ButtonAction.HOME, ButtonGesture.SELECT_LONG.default)
        assertEquals(ButtonAction.DPAD_LEFT, ButtonGesture.NAV_BACK_DOUBLE.default)
        assertEquals(ButtonAction.DPAD_RIGHT, ButtonGesture.NAV_FWD_DOUBLE.default)
        assertEquals(ButtonAction.BACK, ButtonGesture.SELECT_DOUBLE.default)
    }
}
