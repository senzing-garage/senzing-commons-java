package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.*;
import static com.senzing.util.ErrorLogSuppressor.State.*;
import static com.senzing.util.ErrorLogSuppressor.*;

/**
 * Tests for {@link ErrorLogSuppressor}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@Execution(ExecutionMode.CONCURRENT)
public class ErrorLogSuppressorTest {
  @Test
  public void errorLogSuppressorTest() {
    try {
      Result result = null;
      ErrorLogSuppressor suppressor = new ErrorLogSuppressor(5,
                                                             1000,
                                                             1100);
      assertEquals(0, suppressor.getErrorCount(),
                   "Error count is not initially zero");
      assertEquals(1100, suppressor.getSuppressDuration(),
                   "Suppress duration is not as expected.");
      assertEquals(1000, suppressor.getTimeWindow(),
                   "Time window is not as expected");
      assertEquals(5, suppressor.getErrorLimit(),
                   "Error limit is not as expected");

      for (int index = 0; index < 10; index++) {
        if (index <= 5) {
          assertFalse(suppressor.isSuppressing(),
                      "Suppressor is suppressing prematurely: " + index);
        } else {
          assertTrue(suppressor.isSuppressing(),
                     "Suppressor is not suppressing: " + index);
        }
        assertEquals(index, suppressor.getErrorCount(),
                     "Unexpected error count before update: " + index);
        assertEquals(index, suppressor.getPeriodCount(),
                     "Unexpected period error count before update: "
                         + index);

        // update the suppressor on error
        result = suppressor.updateOnError();

        assertEquals(index + 1, suppressor.getErrorCount(),
                     "Unexpected error count after update: " + index);

        assertEquals(index + 1, suppressor.getPeriodCount(),
                     "Unexpected period error count after update: "
                         + index);
        if (index >= 5) {
          assertTrue(suppressor.isSuppressing(),
                      "Suppressor is not suppressing: " + index);
          assertEquals(SUPPRESSED, result.getState(),
                       "ErrorLogSuppressorResult has wrong state");
          assertEquals((index + 1) - suppressor.getErrorLimit(),
                       result.getSuppressedCount(),
                       "Wrong suppression count.");
        } else {
          assertFalse(suppressor.isSuppressing(),
                      "Suppressor is suppressing prematurely: " + index);
          assertEquals(ACTIVE, result.getState(),
                       "ErrorLogSuppressorResult has wrong state");
          assertEquals(0, result.getSuppressedCount(),
                       "Non-zero suppression count.");
        }
      }
      Thread.sleep(1200);
      result = suppressor.updateOnError();
      assertEquals(REACTIVATED, result.getState(),
                   "ErrorLogSuppressorResult has wrong state after "
                       + "reactivation.");
      assertEquals(5, result.getSuppressedCount(),
                   "Wrong suppressed count after reactivation");
      assertFalse(suppressor.isSuppressing(),
                  "Suppressor failed to stop suppressing");
      assertEquals(11, suppressor.getErrorCount(),
                   "Unexpected error count after sleep");
      assertEquals(1, suppressor.getPeriodCount(),
                   "Unexpected period error count after sleep");

      result = suppressor.updateOnError();
      assertEquals(ACTIVE, result.getState(),
                   "ErrorLogSuppressorResult has wrong state after "
                       + "reactivation.");
      assertEquals(0, result.getSuppressedCount(),
                   "Wrong suppressed count after reactivation");
      assertFalse(suppressor.isSuppressing(),
                  "Suppressor failed to stop suppressing");
      assertEquals(12, suppressor.getErrorCount(),
                   "Unexpected error count after sleep");
      assertEquals(2, suppressor.getPeriodCount(),
                   "Unexpected period error count after sleep");

    } catch (Exception e) {
      e.printStackTrace();
      fail("errorLogSuppressorTest() failed with exception: " + e);
    }
  }

  @Test
  public void badErrorLogLimitTest() {
    try {
      try {
        ErrorLogSuppressor s = new ErrorLogSuppressor(-10,
                                                      500,
                                                      600);
        fail("Successfully constructed a suppressor with negative error "
             + "limit.");
      } catch (IllegalArgumentException expected) {
        // do nothing
      }

      try {
        ErrorLogSuppressor s = new ErrorLogSuppressor(0,
                                                      500,
                                                      600);
        fail("Successfully constructed a suppressor with zero error limit.");
      } catch (IllegalArgumentException expected) {
        // do nothing
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("badErrorLogLimitTest() failed with exception: " + e);
    }
  }

  @Test
  public void badErrorTimeWindowTest() {
    try {
      try {
        ErrorLogSuppressor s = new ErrorLogSuppressor(10,
                                                      -500,
                                                      600);
        fail("Successfully constructed a suppressor with negative time "
                 + "window.");
      } catch (IllegalArgumentException expected) {
        // do nothing
      }

      try {
        ErrorLogSuppressor s = new ErrorLogSuppressor(10,
                                                      0,
                                                      600);
        fail("Successfully constructed a suppressor with zero time window.");
      } catch (IllegalArgumentException expected) {
        // do nothing
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("badErrorTimeWindowTest() failed with exception: " + e);
    }
  }

  @Test
  public void badSuppressDurationTest() {
    try {
      try {
        ErrorLogSuppressor s = new ErrorLogSuppressor(10,
                                                      500,
                                                      -600);
        fail("Successfully constructed a suppressor with negative suppress "
                 + "duration.");
      } catch (IllegalArgumentException expected) {
        // do nothing
      }

      try {
        ErrorLogSuppressor s = new ErrorLogSuppressor(10,
                                                      500,
                                                      0);
        fail("Successfully constructed a suppressor with zero suppress "
                 + "duration.");

      } catch (IllegalArgumentException expected) {
        // do nothing
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("badErrorTimeWindowTest() failed with exception: " + e);
    }
  }
}
