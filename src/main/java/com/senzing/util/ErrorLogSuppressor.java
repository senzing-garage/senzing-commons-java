package com.senzing.util;

import java.util.Objects;

/**
 * Provides state-tracking functionality for suppressing errors for a period
 * when there are too many.
 */
public class ErrorLogSuppressor {
  /**
   * The default time window for checking for reaching the error limit.
   */
  private static final long DEFAULT_TIME_WINDOW = 5000L;

  /**
   * The default time to suppress the errors.
   */
  private static final long DEFAULT_SUPPRESS_DURATION = 60000L;

  /**
   * The default error limit for the time window.
   */
  private static final int DEFAULT_ERROR_LIMIT = 10;

  /**
   * The state of the suppressor.
   */
  public enum State {
    /**
     * Logging of these errors is currently being suppresed.
     */
    SUPPRESSED,

    /**
     * Logging of these errors was being snoozed, but is now awoken.
     */
    REACTIVATED,

    /**
     * Logging of
     */
    ACTIVE
  }

  /**
   * The result of evaluating the current error for whether or not it
   * should be logged.
   */
  public static class Result {
    /**
     * The state of suppressor after evaluating the current error.
     */
    private State state;

    /**
     * The suppressed count if currently suppressing or just reactived.
     */
    private int suppressedCount;

    /**
     * Constructs with the specified {@link State} and suppressed count.
     * @param state The {@link State} of the error suppressor.
     * @param suppressedCount The number of errors that have been suppressed
     *                        so far if suppressing.
     */
    public Result(State state, int suppressedCount) {
      this.state            = state;
      this.suppressedCount  = suppressedCount;
    }

    /**
     * Gets the {@link State} of the error suppressor after evaluating the
     * last error.
     *
     * @return The {@link State} of the error suppressor after evaluating the
     *         last error.
     */
    public State getState() {
      return this.state;
    }

    /**
     * Gets the number of errors that have been suppressed if not in an
     * {@link State#ACTIVE} state.
     *
     * @return The number of errors that have been suppressed if not in an
     *         {@link State#ACTIVE} state.
     */
    public int getSuppressedCount() {
      return this.suppressedCount;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Result result = (Result) o;
      return (this.getSuppressedCount() == result.getSuppressedCount()
              && this.getState() == result.getState());
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.getState(), this.getSuppressedCount());
    }

    /**
     * Returns a {@link String} representation of this result.
     *
     * @return A {@link String} representation of this result.
     */
    public String toString() {
      return "" + this.getState() + "(" + this.getSuppressedCount() + ")";
    }
  }

  /**
   * The total number of errors.
   */
  private int errorCount = 0;

  /**
   * The number of errors in the current period.
   */
  private int periodCount = 0;

  /**
   * The number of errors snoozed.
   */
  private int suppressedCount = 0;

  /**
   * The last time an error was reported.
   */
  private long lastReportTime = 0L;

  /**
   * Flag indicating if errors are being snoozed.
   */
  private boolean suppressing = false;

  /**
   * The time window for checking if the error limit has been reached.
   */
  private long timeWindow = DEFAULT_TIME_WINDOW;

  /**
   * The suppression duration in nanoseconds.
   */
  private long nanoWindow = DEFAULT_TIME_WINDOW * 1000000L;

  /**
   * The number of milliseconds to suppress the reporting.
   */
  private long suppressDuration = DEFAULT_SUPPRESS_DURATION;

  /**
   * The suppression duration in nanoseconds.
   */
  private long nanoDuration = DEFAULT_SUPPRESS_DURATION * 1000000L;

  /**
   * The threshold for the number of errors that can occur within the specified
   * window before triggering suppression.
   */
  private int errorLimit = DEFAULT_ERROR_LIMIT;

  /**
   * Default constructor.
   */
  public ErrorLogSuppressor() {
    this(DEFAULT_ERROR_LIMIT,
         DEFAULT_TIME_WINDOW,
         DEFAULT_SUPPRESS_DURATION);
  }

  /**
   * Constructs with the specified parameters.
   *
   * @param errorLimit The maximum number of errors that can occur within the
   *                   time window before triggering supression.
   * @param timeWindow The length of the time window in milliseconds.
   * @param suppressDuration How long to suppress errors if the number of
   *                         errors exceeds the limit within the specified
   *                         time window.
   */
  public ErrorLogSuppressor(int  errorLimit,
                            long timeWindow,
                            long suppressDuration)
  {
    if (errorLimit <= 0) {
      throw new IllegalArgumentException(
          "Error limit must be a positive number: " + errorLimit);
    }
    if (timeWindow <= 0L) {
      throw new IllegalArgumentException(
          "Time window must be a positive number: " + timeWindow);
    }
    if (suppressDuration <= 0L) {
      throw new IllegalArgumentException(
          "Suppress duration must be a positive number: " + suppressDuration);
    }
    this.errorLimit       = errorLimit;
    this.timeWindow       = timeWindow;
    this.suppressDuration = suppressDuration;
    this.nanoWindow       = this.timeWindow * 1000000L;
    this.nanoDuration     = this.suppressDuration * 1000000L;
  }

  /**
   * Returns the total error count as of the last update on error.
   *
   * @return The total error count as of the last update on error.
   */
  public synchronized int getErrorCount() {
    return this.errorCount;
  }

  /**
   * Checks if the errors are currently being suppressed as of the last
   * update on error.
   *
   * @return <code>true</code> if suppressing as of the last update on error,
   *         and <code>false</code> if not.
   */
  public synchronized boolean isSuppressing() {
    return this.suppressing;
  }

  /**
   * Gets the number of suppressed error messages as of the last update on
   * error.
   *
   * @return The number of suppressed error messages as of the last update on
   *         error.
   */
  public synchronized int getSuppressedCount() {
    return this.suppressedCount;
  }

  /**
   * Gets the number of errors within the period as of the last update on
   * error.
   *
   * @return The number of errors within the period as of the last update on
   *         error.
   */
  public synchronized int getPeriodCount() {
    return this.periodCount;
  }

  /**
   * Gets the error limit for the time window.
   *
   * @return The error limit for the time window.
   */
  public int getErrorLimit() {
    return this.errorLimit;
  }

  /**
   * The length of the time window in milliseconds.
   *
   * @return The length of the time window in milliseconds.
   */
  public long getTimeWindow() {
    return this.timeWindow;
  }

  /**
   * Gets the duration of the suppression period in milliseconds.
   *
   * @return The duration of the suppression period in milliseconds.
   */
  public long getSuppressDuration() {
    return this.suppressDuration;
  }

  /**
   * Function to handle when an error occurred.
   *
   * @return The result of handling the error.
   */
  public synchronized Result updateOnError() {
    long  now = System.nanoTime();
    State state = null;
    int   count = 0;

    // increment the total error count
    this.errorCount++;

    if (this.suppressing) {
      // check if suppressing is over
      if ((now - this.lastReportTime) > this.nanoDuration) {
        // suppression is over, set state for reactivation
        count = this.suppressedCount;
        state = State.REACTIVATED;
        this.lastReportTime   = now;
        this.suppressedCount  = 0;
        this.suppressing      = false;
        this.periodCount      = 1;

      } else {
        // suppression is continuing, increment the suppressed count
        this.suppressedCount++;
        this.periodCount++;
        this.suppressing = true;
        count = this.suppressedCount;
        state = State.SUPPRESSED;
      }

    } else if ((now - this.lastReportTime) > this.nanoWindow) {
      // last error was before the time window, reset the period
      this.periodCount      = 1;
      this.suppressedCount  = 0;
      this.lastReportTime   = now;
      this.suppressing      = false;
      state = State.ACTIVE;
      count = 0;

    } else {
      // increment the period count
      this.periodCount++;

      // check if period count exceeds the limit
      if (this.periodCount > this.errorLimit) {
        // exceeded the limit so go into the suppression mode
        this.suppressedCount  = 1;
        this.lastReportTime   = now;
        this.suppressing      = true;
        state = State.SUPPRESSED;
        count = 1;

      } else {
        // not suppressed, just increment
        this.suppressedCount = 0;
        this.lastReportTime  = now;
        this.suppressing     = false;
        state = State.ACTIVE;
        count = 0;
      }
    }

    // return the result
    return new Result(state, count);
  }

}
