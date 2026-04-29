package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static com.senzing.util.OperatingSystemFamily.MAC_OS;
import static com.senzing.util.OperatingSystemFamily.RUNTIME_OS_FAMILY;
import static com.senzing.util.OperatingSystemFamily.UNIX;
import static com.senzing.util.OperatingSystemFamily.WINDOWS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link OperatingSystemFamily}.
 *
 * <p>Each test asserts the documented contract from the enum's javadoc:
 * the three is-predicate methods reflect the enum identity, the three
 * predicates are mutually exclusive, and {@link
 * OperatingSystemFamily#RUNTIME_OS_FAMILY} is initialized non-null and
 * matches the JVM's {@code os.name} system property.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class OperatingSystemFamilyTest
{
  // -------------------------------------------------------------------
  // Enum value count and identity
  // -------------------------------------------------------------------

  /**
   * The enum must declare exactly three constants per its public
   * javadoc: {@code WINDOWS}, {@code MAC_OS}, {@code UNIX}.
   */
  @Test
  public void enumDeclaresThreeConstants()
  {
    assertEquals(3, OperatingSystemFamily.values().length,
                 "OperatingSystemFamily must declare exactly 3 constants");
  }

  // -------------------------------------------------------------------
  // isWindows() / isMacOS() / isUnix() contract
  // -------------------------------------------------------------------

  /**
   * {@code isWindows()} must return {@code true} only for {@code
   * WINDOWS}; the other two predicates must return {@code false}.
   */
  @Test
  public void windowsPredicatesAreMutuallyExclusive()
  {
    assertTrue(WINDOWS.isWindows(), "WINDOWS.isWindows() must be true");
    assertFalse(WINDOWS.isMacOS(), "WINDOWS.isMacOS() must be false");
    assertFalse(WINDOWS.isUnix(), "WINDOWS.isUnix() must be false");
  }

  /**
   * {@code isMacOS()} must return {@code true} only for {@code MAC_OS};
   * the other two predicates must return {@code false}.
   */
  @Test
  public void macOsPredicatesAreMutuallyExclusive()
  {
    assertFalse(MAC_OS.isWindows(), "MAC_OS.isWindows() must be false");
    assertTrue(MAC_OS.isMacOS(), "MAC_OS.isMacOS() must be true");
    assertFalse(MAC_OS.isUnix(), "MAC_OS.isUnix() must be false");
  }

  /**
   * {@code isUnix()} must return {@code true} only for {@code UNIX};
   * the other two predicates must return {@code false}.
   */
  @Test
  public void unixPredicatesAreMutuallyExclusive()
  {
    assertFalse(UNIX.isWindows(), "UNIX.isWindows() must be false");
    assertFalse(UNIX.isMacOS(), "UNIX.isMacOS() must be false");
    assertTrue(UNIX.isUnix(), "UNIX.isUnix() must be true");
  }

  // -------------------------------------------------------------------
  // RUNTIME_OS_FAMILY contract
  // -------------------------------------------------------------------

  /**
   * {@link OperatingSystemFamily#RUNTIME_OS_FAMILY} must be initialized
   * non-null on class-load.
   */
  @Test
  public void runtimeOsFamilyIsNonNull()
  {
    assertNotNull(RUNTIME_OS_FAMILY,
                  "RUNTIME_OS_FAMILY must be initialized non-null");
  }

  /**
   * {@link OperatingSystemFamily#RUNTIME_OS_FAMILY} must match the
   * classification rules documented in the static initializer:
   *
   * <ul>
   *   <li>{@code os.name} starts with "windows" (case-insensitive) →
   *       {@code WINDOWS}.</li>
   *   <li>{@code os.name} starts with "mac" (case-insensitive) or
   *       contains "darwin" → {@code MAC_OS}.</li>
   *   <li>otherwise → {@code UNIX}.</li>
   * </ul>
   */
  @Test
  public void runtimeOsFamilyMatchesSystemProperty()
  {
    String osName = System.getProperty("os.name");
    assertNotNull(osName, "Test JVM must define os.name");
    String lower = osName.toLowerCase().trim();

    OperatingSystemFamily expected;
    if (lower.startsWith("windows")) {
      expected = WINDOWS;
    } else if (lower.startsWith("mac") || lower.contains("darwin")) {
      expected = MAC_OS;
    } else {
      expected = UNIX;
    }

    assertSame(expected, RUNTIME_OS_FAMILY,
               "RUNTIME_OS_FAMILY (" + RUNTIME_OS_FAMILY
                   + ") does not match classification of os.name=["
                   + osName + "]");
  }
}
