package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the {@link Quantified} interface and its inner
 * {@link Quantified.Statistic} interface.
 *
 * <p>The {@link Quantified.Statistic#getName()} default method is
 * documented to return a name composed of the enclosing class'
 * simple name (when present) and {@code this.toString()}. This test
 * exercises both the with-enclosing branch (via a nested test
 * fixture) and the abstract method dispatch via a lambda.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class QuantifiedTest
{
  /**
   * Test fixture: an enum nested inside this test class implementing
   * {@link Quantified.Statistic}. Its enclosing class is
   * {@code QuantifiedTest}, so the default {@code getName()} must
   * return {@code "QuantifiedTest:&lt;name&gt;"}.
   */
  protected enum FixtureStat implements Quantified.Statistic
  {
    HITS,
    MISSES;

    @Override
    public String getUnits()
    {
      return "count";
    }
  }

  /**
   * The default {@link Quantified.Statistic#getName()} must produce
   * {@code "<EnclosingClass>:<toString()>"} when the implementing
   * class has an enclosing class — verified for the nested
   * {@link FixtureStat} enum.
   */
  @Test
  public void defaultGetNameUsesEnclosingClassPrefix()
  {
    assertEquals("QuantifiedTest:HITS", FixtureStat.HITS.getName());
    assertEquals("QuantifiedTest:MISSES",
                 FixtureStat.MISSES.getName());
  }

  /**
   * The abstract {@link Quantified.Statistic#getUnits()} method must
   * dispatch to the implementation's value.
   */
  @Test
  public void getUnitsReturnsImplementationValue()
  {
    assertEquals("count", FixtureStat.HITS.getUnits());
  }

  /**
   * The abstract {@link Quantified#getStatistics()} method must be
   * dispatched correctly to a lambda implementation. Exercises the
   * outer-interface contract to give it line coverage and to confirm
   * the abstract-method shape works with a method-reference style
   * impl.
   */
  @Test
  public void getStatisticsDispatchesToImplementation()
  {
    Map<Quantified.Statistic, Number> sample = Collections.emptyMap();
    Quantified q = () -> sample;
    assertSame(sample, q.getStatistics());
  }
}
