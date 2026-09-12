// Plain JUnit 4 test, no Android framework classes involved: exercises isValidIpv4(),
// isValidWfcSlotName() and WfcAccessPointSlot's serialize()/parse() round trip against the
// "enabled;name;dns;dns" format produced by WfcSettingsJNI.cpp.

package me.magnum.melonds.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WfcAccessPointSlotTest {

    @Test
    fun `valid IPv4 addresses are accepted`() {
        assertTrue(isValidIpv4("0.0.0.0"))
        assertTrue(isValidIpv4("255.255.255.255"))
        assertTrue(isValidIpv4("178.62.43.212"))
        assertTrue(isValidIpv4("167.235.229.36"))
    }

    @Test
    fun `malformed or out-of-range addresses are rejected`() {
        assertFalse(isValidIpv4(""))
        assertFalse(isValidIpv4("1.2.3"))
        assertFalse(isValidIpv4("1.2.3.4.5"))
        assertFalse(isValidIpv4("256.1.1.1"))
        assertFalse(isValidIpv4("1.2.3.-1"))
        assertFalse(isValidIpv4("abc.def.gh.i"))
        assertFalse(isValidIpv4("1.2.3.4 "))
        assertFalse(isValidIpv4("01.2.3.4"))
    }

    @Test
    fun `serialize matches the native enabled-name-dns-dns format`() {
        val slot = WfcAccessPointSlot(enabled = true, name = "melonAP", primaryDns = "178.62.43.212", secondaryDns = "0.0.0.0")

        assertEquals("1;melonAP;178.62.43.212;0.0.0.0", slot.serialize())
    }

    @Test
    fun `parse recovers the same slot serialize produced`() {
        val original = WfcAccessPointSlot(enabled = false, name = "", primaryDns = "167.235.229.36", secondaryDns = "172.104.88.237")

        assertEquals(original, WfcAccessPointSlot.parse(original.serialize()))
    }

    @Test
    fun `parse rejects strings that are not four semicolon-separated fields or have a bad enabled flag`() {
        assertNull(WfcAccessPointSlot.parse("not a valid slot"))
        assertNull(WfcAccessPointSlot.parse("1;name;1.2.3.4"))
        assertNull(WfcAccessPointSlot.parse("2;name;1.2.3.4;5.6.7.8"))
    }

    @Test
    fun `a slot name keeps whatever the user typed, separators aside`() {
        val slot = WfcAccessPointSlot(enabled = true, name = "Casa mia", primaryDns = "5.161.56.11", secondaryDns = "5.161.56.11")

        assertEquals("1;Casa mia;5.161.56.11;5.161.56.11", slot.serialize())
        assertEquals(slot, WfcAccessPointSlot.parse(slot.serialize()))
    }

    @Test
    fun `slot names are accepted up to the SSID field length`() {
        assertTrue(isValidWfcSlotName(""))
        assertTrue(isValidWfcSlotName("Casa mia"))
        assertTrue(isValidWfcSlotName("a".repeat(WfcAccessPointSlot.MAX_NAME_LENGTH)))
        // Multi-byte characters count as the bytes they take in the firmware field.
        assertTrue(isValidWfcSlotName("e".repeat(30) + "\u00e8"))
    }

    @Test
    fun `slot names longer than the field, or carrying separators, are rejected`() {
        assertFalse(isValidWfcSlotName("a".repeat(WfcAccessPointSlot.MAX_NAME_LENGTH + 1)))
        assertFalse(isValidWfcSlotName("e".repeat(31) + "\u00e8"))
        assertFalse(isValidWfcSlotName("Casa; mia"))
        assertFalse(isValidWfcSlotName("Casa\nmia"))
    }
}
