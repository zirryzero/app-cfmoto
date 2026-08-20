// SPDX-License-Identifier: AGPL-3.0-or-later
package dev.zanderp.opencfmoto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsBackupTest {
    @Test
    fun androidAutoHandlebarControlRequiresExplicitOptIn() {
        assertFalse(ButtonMode.DEFAULT_CONTROL_AA)
    }

    @Test
    fun assistantTemporarilyDisablesHandlebarCapture() {
        assertFalse(ButtonMode.shouldCapture(controlAa = true, voiceActive = true))
    }

    @Test
    fun formatAndVersionAreStable() {
        assertEquals("800nk-adv-link.settings", SettingsBackup.FORMAT)
        assertEquals(1, SettingsBackup.VERSION)
    }

    @Test
    fun buttonActionIdsRoundTrip() {
        for (action in ButtonAction.entries) {
            assertEquals(action, ButtonAction.byId(action.id))
        }
        for (gesture in ButtonGesture.entries) {
            assertEquals(gesture.default, ButtonAction.byId(gesture.default.id))
        }
    }
}
