package com.senzing.util;

import java.util.Map;

/**
 * Interface used to describe a statistic.
 */
public interface Quantified {
  /**
   * Describes a statistic that is
   */
  interface Statistic {
    /**
     * Gets the descriptive name of the statistic.
     *
     * @return The descriptive name of the statistic.
     */
    default String getName() {
      Class c = this.getClass();
      Class enclosing = c.getEnclosingClass();
      if (enclosing != null) {
        return enclosing.getSimpleName() + ":" + this.toString();
      } else {
        return c.getSimpleName() + ":" + this.toString();
      }
    }

    /**
     * Gets the unit of measure for this statistic.  This is the unit that
     * the {@link Number} value that has been measured.
     *
     * @return The unit of measure for this statistic.
     */
    String getUnits();
  }

  /**
   * Gets the {@link Map} of {@link Statistic} keys to their {@link Number}
   * values in an atomic thread-safe manner.
   *
   * @return The {@link Map} of {@link Statistic} keys to their {@link Number}
   *         values.
   */
  Map<Statistic, Number> getStatistics();
}
