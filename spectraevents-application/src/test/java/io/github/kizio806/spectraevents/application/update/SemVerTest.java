package io.github.kizio806.spectraevents.application.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SemVerTest {

  @Test
  void testPrereleasePrecedence() {
    assertTrue(SemVer.isNewer("0.1.0-beta.2", "0.1.0-beta.1"));
    assertFalse(SemVer.isNewer("0.1.0-beta.1", "0.1.0-beta.2"));

    assertTrue(SemVer.isNewer("0.1.0", "0.1.0-beta.2"));
    assertFalse(SemVer.isNewer("0.1.0-beta.2", "0.1.0"));
  }

  @Test
  void testCoreVersionPrecedence() {
    assertTrue(SemVer.isNewer("0.1.1", "0.1.0"));
    assertTrue(SemVer.isNewer("0.2.0", "0.1.9"));
    assertTrue(SemVer.isNewer("1.0.0", "0.9.9"));
    assertFalse(SemVer.isNewer("0.1.0", "0.1.0"));
  }

  @Test
  void testPrefixedVersions() {
    assertTrue(SemVer.isNewer("v0.1.0-beta.2", "0.1.0-beta.1"));
    assertTrue(SemVer.isNewer("v0.1.0", "v0.1.0-beta.2"));
  }

  @Test
  void testParseDetails() {
    SemVer v = SemVer.parse("0.1.0-beta.1");
    assertEquals(0, v.major());
    assertEquals(1, v.minor());
    assertEquals(0, v.patch());
    assertEquals("beta.1", v.preRelease());
  }
}
