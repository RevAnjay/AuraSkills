package dev.aurelium.auraskills.bukkit.util;

import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class VersionUtilsTest {

    @BeforeAll
    static void setUp() {
        MockBukkit.mock();
    }

    @AfterAll
    static void unload() {
        MockBukkit.unmock();
    }

    @Test
    void testGetMajorVersion() {
        assertEquals(20, VersionUtils.getMajorVersion("1.20"));
        assertEquals(21, VersionUtils.getMajorVersion("1.21.1"));
        assertEquals(21, VersionUtils.getMajorVersion("1.21.10"));
        assertEquals(26, VersionUtils.getMajorVersion("26.1"));
        assertEquals(27, VersionUtils.getMajorVersion("27.3.1"));
        assertEquals(26, VersionUtils.getMajorVersion("26.3.0-stable"));
        assertEquals(26, VersionUtils.getMajorVersion("26.3-stable"));
        assertEquals(20, VersionUtils.getMajorVersion(null));
    }

    @Test
    void testGetMinorVersion() {
        assertEquals(0, VersionUtils.getMinorVersion("1.20"));
        assertEquals(1, VersionUtils.getMinorVersion("1.21.1"));
        assertEquals(10, VersionUtils.getMinorVersion("1.21.10"));
        assertEquals(1, VersionUtils.getMinorVersion("26.1"));
        assertEquals(3, VersionUtils.getMinorVersion("27.3.1"));
        assertEquals(3, VersionUtils.getMinorVersion("26.3.0-stable"));
        assertEquals(3, VersionUtils.getMinorVersion("26.3-stable"));
        assertEquals(0, VersionUtils.getMinorVersion(null));
    }

    @Test
    void testGetPatchVersion() {
        assertEquals(0, VersionUtils.getPatchVersion("1.20"));
        assertEquals(0, VersionUtils.getPatchVersion("1.21.1"));
        assertEquals(0, VersionUtils.getPatchVersion("1.21.10"));
        assertEquals(0, VersionUtils.getPatchVersion("26.1"));
        assertEquals(1, VersionUtils.getPatchVersion("27.3.1"));
        assertEquals(12, VersionUtils.getPatchVersion("27.3.12"));
        assertEquals(0, VersionUtils.getPatchVersion("26.3.0-stable"));
        assertEquals(1, VersionUtils.getPatchVersion("26.3.1-stable"));
        assertEquals(0, VersionUtils.getPatchVersion("26.3-stable"));
        assertEquals(0, VersionUtils.getPatchVersion(null));
    }

    @Test
    void testGetVersionString() {
        assertEquals("26.3.0", VersionUtils.getVersionString("26.3.0-stable"));
        assertEquals("26.3", VersionUtils.getVersionString("26.3-stable"));
        assertEquals("26.1.2", VersionUtils.getVersionString("26.1.2.build.63-stable"));
        assertEquals("1.21.4", VersionUtils.getVersionString("1.21.4-R0.1-SNAPSHOT"));
        assertEquals("26.3", VersionUtils.getVersionString("git-Canvas-26.3 (MC: 26.3)"));
        assertNull(VersionUtils.getVersionString(null));
        assertNull(VersionUtils.getVersionString(""));
    }
}
