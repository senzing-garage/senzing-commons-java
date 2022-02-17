package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link SemanticVersion}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class SemanticVersionTest {

  @Test
  public void testNullEquals() {
    SemanticVersion version = new SemanticVersion("1.0.0");
    boolean result = version.equals(null);
    assertEquals(false, result,
                 "Equality versus null is unexpectedly true.");
  }

  @Test
  public void testCrossTypeEquals() {
    SemanticVersion version = new SemanticVersion("1.0.0");
    boolean result = version.equals("1.0.0");
    assertEquals(false, result,
                 "Equality versus an object of a different type is "
                 + "unexpectedly true.");
  }

  public List<Arguments> provideEqualParams() {
    List<Arguments> result = new LinkedList<>();

    Class nullPointer = NullPointerException.class;
    Class illegalArg = IllegalArgumentException.class;

    result.add(arguments("1", "1", true, null));
    result.add(arguments("1.0", "1.0", true, null));
    result.add(arguments("2.0.0", "2.0.0", true, null));
    result.add(arguments("1.0", "1", true, null));
    result.add(arguments("1.0.0", "1", true, null));
    result.add(arguments("1.0.0", "1.0", true, null));
    result.add(arguments("2.1.2", "2.1.2", true, null));
    result.add(arguments("2.1.0", "2.1", true, null));
    result.add(arguments("2.1.0", "3.1", false, null));
    result.add(arguments("1.1", "1", false, null));
    result.add(arguments("ABC", "1", null, illegalArg));
    result.add(arguments(null, "1.0", null, nullPointer));
    result.add(arguments("1.0", null, null, nullPointer));
    result.add(arguments("1.-1.0", "2.1.0", null, illegalArg));

    return result;
  }
  @ParameterizedTest
  @MethodSource("provideEqualParams")
  public void testEquals(String   version1,
                         String   version2,
                         Boolean  expectedResult,
                         Class    expectedFailure)
  {
    try {
      SemanticVersion v1 = new SemanticVersion(version1);
      SemanticVersion v2 = new SemanticVersion(version2);

      if (expectedFailure != null)  {
        fail("Expected " + expectedFailure.getName() + " when constructing "
              + "SemanticVersion objects for " + version1 + " or " + version2);
      }

      boolean result = v1.equals(v2);

      assertEquals(expectedResult, result,
                   "Unexpected result comparing " + version1 + " to "
                   + version2 + " for equality.");

      result = v2.equals(v1);

      assertEquals(expectedResult, result,
                   "Inconsistent result comparing " + version2 + " to "
                       + version1 + " for equality.");

      if (result) {
        int hash1 = v1.hashCode();
        int hash2 = v2.hashCode();

        assertEquals(hash1, hash2,
                     "Inconsistent hash codes for " + version1 + " and "
                      + version2);

        int compare = v1.compareTo(v2);

        assertEquals(0, compare,
                     "Comparison of " + version1 + " to " + version2
                     + " is non-zero even though they equal.");

        compare = v2.compareTo(v1);

        assertEquals(0, compare,
                     "Comparison of " + version2 + " to " + version1
                         + " is non-zero even though they equal.");
      } else {
        int compare = v1.compareTo(v2);

        assertNotEquals(0, compare,
                     "Comparison of " + version1 + " to " + version2
                         + " is zero even though they are NOT equal.");

        compare = v2.compareTo(v1);

        assertNotEquals(0, compare,
                     "Comparison of " + version2 + " to " + version1
                         + " is zero even though they are NOT equal.");
      }

    } catch (Exception e) {
      if (expectedFailure == null) {
        e.printStackTrace();
        fail("Expected success for equality test with of " + version1
                 + " versus " + version2);

      } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
        e.printStackTrace();
        fail("Expected " + expectedFailure.getName() + " when constructing "
                 + "SemanticVersion objects for " + version1 + " or "
                 + version2 + " instead of: " + e);
      }
    }
  }

  public List<Arguments> provideToStringParams() {
    List<Arguments> result = new LinkedList<>();

    Class nullPointer = NullPointerException.class;
    Class illegalArg = IllegalArgumentException.class;

    result.add(arguments("1.0", "1.0", null));
    result.add(arguments("1", "1", null));
    result.add(arguments("1.0.0", "1.0.0", null));
    result.add(arguments("1.1.2", "1.1.2", null));

    result.add(arguments("ABC", null, illegalArg));
    result.add(arguments(null,  null, nullPointer));
    result.add(arguments("1.-1.0", null, illegalArg));

    return result;
  }

  @ParameterizedTest
  @MethodSource("provideToStringParams")
  public void testToString(String version,
                           String expectedResult,
                           Class  expectedFailure)
  {
    try {
      SemanticVersion ver = new SemanticVersion(version);

      if (expectedFailure != null)  {
        fail("Expected " + expectedFailure.getName() + " when constructing "
                 + "SemanticVersion object for " + version);
      }

      String result = ver.toString();

      assertEquals(expectedResult, result,
                   "Unexpected result converting version to string: "
                       + version);

    } catch (Exception e) {
      if (expectedFailure == null) {
        e.printStackTrace();
        fail("Expected success for toString() test with " + version);

      } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
        e.printStackTrace();
        fail("Expected " + expectedFailure.getName() + " with toString() "
             + "test of " + version);
      }
    }

  }

  public List<Arguments> provideCompareParams() {
    List<Arguments> result = new LinkedList<>();

    Class nullPointer = NullPointerException.class;
    Class illegalArg = IllegalArgumentException.class;

    result.add(arguments("1.0.0", "1.0.1", -1, null));
    result.add(arguments("1", "1", 0, null));
    result.add(arguments("1", "2", -1, null));
    result.add(arguments("2", "1", 1, null));
    result.add(arguments("1.0", "1.0", 0, null));
    result.add(arguments("1.0", "1.0.1", -1, null));
    result.add(arguments("1.0.2", "1.0.1", 1, null));
    result.add(arguments("2.0.0", "2.0.0", 0, null));
    result.add(arguments("1.0", "1", 0, null));
    result.add(arguments("3.1.2", "1.0.0", 1, null));
    result.add(arguments("1.0.0", "1", 0, null));
    result.add(arguments("1.0.0", "1.0", 0, null));
    result.add(arguments("2.1.2", "2.1.2", 0, null));
    result.add(arguments("2.1.0", "2.1", 0, null));
    result.add(arguments("2.1.0", "3.1", -1, null));
    result.add(arguments("1.1", "1", 1, null));
    result.add(arguments("ABC", "1", null, illegalArg));
    result.add(arguments(null, "1.0", null, nullPointer));
    result.add(arguments("1.0", null, null, nullPointer));
    result.add(arguments("1.-1.0", "2.1.0", null, illegalArg));

    return result;
  }

  @ParameterizedTest
  @MethodSource("provideCompareParams")
  public void testCompare(String  version1,
                          String  version2,
                          Integer expectedResult,
                          Class   expectedFailure)
  {
    try {
      SemanticVersion v1 = new SemanticVersion(version1);
      SemanticVersion v2 = new SemanticVersion(version2);

      if (expectedFailure != null)  {
        fail("Expected " + expectedFailure.getName() + " when constructing "
                 + "SemanticVersion objects for " + version1 + " or " + version2);
      }

      int result = v1.compareTo(v2);

      if (expectedResult == 0) {
        assertEquals(expectedResult, result,
                     "Unexpected result comparing " + version1 + " to "
                         + version2 + " for equality.");

        result = v2.compareTo(v1);

        assertEquals(expectedResult, result,
                     "Inconsistent result comparing " + version2 + " to "
                         + version1 + " for equality.");

        int hash1 = v1.hashCode();
        int hash2 = v2.hashCode();

        assertEquals(hash1, hash2,
                     "Inconsistent hash codes for " + version1
                         + " and " + version2
                         + " despite compareTo() returning zero (0)");

        assertEquals(v1, v2,
                     "Version " + version1 + " is not equal to "
                     + version2 + " despite compareTo() returning zero (0)");

        assertEquals(v2, v1,
                     "Version " + version2 + " is not equal to "
                         + version1 + " despite compareTo() returning zero (0)");

      } else if (expectedResult < 0) {
        assertTrue( (result < 0),
                    "Unexpected result comparing " + version1 + " to "
                        + version2 + " for less-than.");

        result = v2.compareTo(v1);

        assertTrue( (result > 0),
                    "Unexpected result reverse comparing " + version2
                        + " to " + version1 + " for greater-than.");

        assertNotEquals(true, v1.equals(v2),
                        "Version " + version1 + " is equal to "
                            + version2 + " despite compareTo() being negative");

        assertNotEquals(true, v2.equals(v1),
                        "Version " + version2 + " is equal to "
                            + version1 + " despite compareTo() being positive");

      } else {
        assertTrue( (result > 0),
                    "Unexpected result comparing " + version1 + " to "
                        + version2 + " for greater-than.");

        result = v2.compareTo(v1);

        assertTrue( (result < 0),
                    "Unexpected result reverse comparing " + version2
                        + " to " + version1 + " for less-than.");

        assertNotEquals(true, v1.equals(v2),
                        "Version " + version1 + " is equal to "
                            + version2 + " despite compareTo() being positive");

        assertNotEquals(true, v2.equals(v1),
                        "Version " + version2 + " is equal to "
                            + version1 + " despite compareTo() being negative");
      }

    } catch (Exception e) {
      if (expectedFailure == null) {
        e.printStackTrace();
        fail("Expected success for equality test with of " + version1
                 + " versus " + version2);

      } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
        e.printStackTrace();
        fail("Expected " + expectedFailure.getName() + " when constructing "
                 + "SemanticVersion objects for " + version1 + " or "
                 + version2 + " instead of: " + e);
      }
    }
  }
}
