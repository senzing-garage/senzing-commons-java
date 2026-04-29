package com.senzing.util;

import com.senzing.util.ErrorLogSuppressor.Result;
import com.senzing.util.ErrorLogSuppressor.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary tests for the {@link ErrorLogSuppressor.Result}
 * inner class — covering equals (identity / null / different-class
 * / value comparison), hashCode consistency, and the {@code toString}
 * format. The default no-arg {@link ErrorLogSuppressor} constructor
 * is exercised here too; the existing
 * {@code ErrorLogSuppressorTest} covers only the configurable
 * three-arg form.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ErrorLogSuppressorExtraTest
{
  // -------------------------------------------------------------------
  // Result.equals / hashCode / toString
  // -------------------------------------------------------------------

  @Test
  public void resultEqualsReturnsTrueForSelf()
  {
    Result r = new Result(State.ACTIVE, 0);
    assertEquals(r, r,
                 "A Result instance must be equal to itself");
  }

  @Test
  public void resultEqualsReturnsFalseForNull()
  {
    Result r = new Result(State.ACTIVE, 0);
    assertNotEquals(null, r);
  }

  @Test
  public void resultEqualsReturnsFalseForDifferentClass()
  {
    Result r = new Result(State.ACTIVE, 0);
    assertNotEquals("ACTIVE(0)", r,
                    "Result must not be equal to a String even with"
                        + " matching toString");
  }

  @Test
  public void resultEqualsReturnsTrueForEqualComponents()
  {
    Result a = new Result(State.SUPPRESSED, 5);
    Result b = new Result(State.SUPPRESSED, 5);
    assertEquals(a, b,
                 "Two Result instances with identical state and "
                     + "suppressedCount must be equal");
  }

  @Test
  public void resultEqualsReturnsFalseForDifferentState()
  {
    Result a = new Result(State.ACTIVE, 5);
    Result b = new Result(State.SUPPRESSED, 5);
    assertNotEquals(a, b,
                    "Different state must yield non-equal Results");
  }

  @Test
  public void resultEqualsReturnsFalseForDifferentSuppressedCount()
  {
    Result a = new Result(State.SUPPRESSED, 5);
    Result b = new Result(State.SUPPRESSED, 6);
    assertNotEquals(a, b);
  }

  @Test
  public void resultHashCodeMatchesForEqualResults()
  {
    Result a = new Result(State.SUPPRESSED, 5);
    Result b = new Result(State.SUPPRESSED, 5);
    assertEquals(a.hashCode(), b.hashCode(),
                 "Equal Results must have equal hashCodes");
  }

  @Test
  public void resultToStringFormatIsStateThenCountInParens()
  {
    Result r = new Result(State.SUPPRESSED, 7);
    assertEquals("SUPPRESSED(7)", r.toString(),
                 "toString format is '<state>(<count>)'");
  }

  // -------------------------------------------------------------------
  // Default no-arg constructor
  // -------------------------------------------------------------------

  /**
   * The no-arg {@link ErrorLogSuppressor} constructor delegates to
   * the three-arg form with the documented defaults. Verify it
   * constructs successfully and produces a usable instance.
   */
  @Test
  public void defaultConstructorYieldsUsableInstance()
  {
    ErrorLogSuppressor suppressor = new ErrorLogSuppressor();
    assertNotNull(suppressor);
    assertEquals(0, suppressor.getErrorCount());
    assertEquals(0, suppressor.getSuppressedCount());
    assertEquals(0, suppressor.getPeriodCount());
    assertTrue(!suppressor.isSuppressing());
  }

  /**
   * {@link ErrorLogSuppressor#getSuppressedCount()} starts at zero
   * for a fresh instance.
   */
  @Test
  public void getSuppressedCountStartsAtZero()
  {
    ErrorLogSuppressor suppressor
        = new ErrorLogSuppressor(5, 1000L, 5000L);
    assertEquals(0, suppressor.getSuppressedCount());
  }
}
