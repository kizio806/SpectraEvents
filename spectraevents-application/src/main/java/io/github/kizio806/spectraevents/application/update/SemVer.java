package io.github.kizio806.spectraevents.application.update;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Semantic Versioning (SemVer 2.0.0) parser and comparator. Handles major, minor, patch, and
 * prerelease identifiers (e.g. 0.1.0-beta.1 < 0.1.0-beta.2 < 0.1.0).
 */
public final class SemVer implements Comparable<SemVer> {
  private static final Pattern DOT = Pattern.compile("\\.");

  private final String raw;
  private final int major;
  private final int minor;
  private final int patch;
  private final String preRelease;

  public SemVer(int major, int minor, int patch, String preRelease, String raw) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.preRelease = preRelease;
    this.raw = Objects.requireNonNull(raw, "raw");
  }

  public static SemVer parse(String version) {
    if (version == null || version.isBlank()) {
      return new SemVer(0, 0, 0, null, "0.0.0");
    }
    String cleaned = version.trim();
    if (cleaned.startsWith("v") || cleaned.startsWith("V")) {
      cleaned = cleaned.substring(1);
    }

    String coreStr = cleaned;
    String preReleaseStr = null;
    int hyphenIndex = cleaned.indexOf('-');
    if (hyphenIndex != -1) {
      coreStr = cleaned.substring(0, hyphenIndex);
      preReleaseStr = cleaned.substring(hyphenIndex + 1);
    }

    String[] parts = DOT.split(coreStr);
    int maj = parts.length > 0 ? parseSafeInt(parts[0]) : 0;
    int min = parts.length > 1 ? parseSafeInt(parts[1]) : 0;
    int pat = parts.length > 2 ? parseSafeInt(parts[2]) : 0;

    return new SemVer(maj, min, pat, preReleaseStr, version);
  }

  public static boolean isNewer(String latestVersion, String currentVersion) {
    return parse(latestVersion).compareTo(parse(currentVersion)) > 0;
  }

  public int major() {
    return major;
  }

  public int minor() {
    return minor;
  }

  public int patch() {
    return patch;
  }

  public String preRelease() {
    return preRelease;
  }

  public String raw() {
    return raw;
  }

  @Override
  public int compareTo(SemVer other) {
    if (this.major != other.major) {
      return Integer.compare(this.major, other.major);
    }
    if (this.minor != other.minor) {
      return Integer.compare(this.minor, other.minor);
    }
    if (this.patch != other.patch) {
      return Integer.compare(this.patch, other.patch);
    }

    // Normal version vs Pre-release
    if (this.preRelease == null && other.preRelease == null) {
      return 0;
    }
    if (this.preRelease == null) {
      return 1; // 0.1.0 > 0.1.0-beta.1
    }
    if (other.preRelease == null) {
      return -1; // 0.1.0-beta.1 < 0.1.0
    }

    // Both have pre-release strings
    String[] thisTokens = DOT.split(this.preRelease);
    String[] otherTokens = DOT.split(other.preRelease);
    int minLen = Math.min(thisTokens.length, otherTokens.length);

    for (int i = 0; i < minLen; i++) {
      String t1 = thisTokens[i];
      String t2 = otherTokens[i];
      int comp = compareIdentifiers(t1, t2);
      if (comp != 0) {
        return comp;
      }
    }
    return Integer.compare(thisTokens.length, otherTokens.length);
  }

  private static int compareIdentifiers(String id1, String id2) {
    boolean isNum1 = isNumeric(id1);
    boolean isNum2 = isNumeric(id2);

    if (isNum1 && isNum2) {
      return Integer.compare(parseSafeInt(id1), parseSafeInt(id2));
    }
    if (isNum1) {
      return -1; // Numeric identifiers always have lower precedence than non-numeric
    }
    if (isNum2) {
      return 1;
    }
    return id1.compareTo(id2);
  }

  private static boolean isNumeric(String str) {
    if (str == null || str.isEmpty()) return false;
    for (int i = 0; i < str.length(); i++) {
      if (!Character.isDigit(str.charAt(i))) return false;
    }
    return true;
  }

  private static int parseSafeInt(String str) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SemVer semVer = (SemVer) o;
    return compareTo(semVer) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch, preRelease);
  }

  @Override
  public String toString() {
    return raw;
  }
}
