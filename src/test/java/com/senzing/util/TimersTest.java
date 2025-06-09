package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link Timers}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class TimersTest {
  @Test
  public void testConstructEmpty() {
    Timers timers = new Timers();
  }

  @Test
  public void testConstructWithNullArray() {
    Timers timers = new Timers(((String[]) null));
  }

  @Test
  public void testConstructWithNull() {
    try {
      Timers timers = new Timers("Foo", null, "Bar");
      fail("Expected NullPointerException when constructing with null, but "
          + "got success instead");

    } catch (NullPointerException e) {
      // success
    } catch (Exception e) {
      e.printStackTrace();
      fail("Expected NullPointerException when constructing with null: " + e);
    }
  }

  @Test
  public void testConstructWithDuplicates() {
    try {
      Timers timers = new Timers("Foo", "Bar", "Foo", "Phoo");
      fail("Expected an IllegalArgumentException when constructing with "
          + "duplicates, but got success instead.");

    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      e.printStackTrace();
      fail("Expected IllegalArgumentException when constructing with "
          + "duplicates, but got a different exception: " + e);

    }
  }

  public List<Arguments> provideHasTimerParams() {
    List<Arguments> result = new LinkedList<>();

    result.add(arguments(
        new Timers("foo", "bar"), "phoo", false, null));
    result.add(arguments(
        new Timers("foo", "bar"), "foo", true, null));
    result.add(arguments(
        new Timers("foo", "bar"), "bar", true, null));
    result.add(arguments(new Timers(), "bar", false, null));

    return result;
  }

  @ParameterizedTest
  @MethodSource("provideHasTimerParams")
  @SuppressWarnings("unchecked")
  public void hasTimerTest(Timers timer,
      String timerName,
      Boolean expectedResult,
      Class expectedFailure) {
    String info = "  timer=[ " + timer
        + " ], timerName=[ " + timerName + " ], expectedResult=[ "
        + expectedResult + " ], expectedFailure=[ "
        + ((expectedFailure == null) ? null : expectedFailure.getName()) + " ]";

    try {
      boolean result = timer.hasTimer(timerName);

      if (expectedFailure != null) {
        fail("Expected a failure, but got success." + info);
      }

      assertEquals(expectedResult, result, "Unexpected result." + info);

    } catch (Exception e) {
      if (expectedFailure == null) {
        e.printStackTrace();
        fail("Unexpected failure." + info + ", failure=[ " + e + " ]");

      } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
        e.printStackTrace();
        fail("Expected a different failure." + info
            + ", failure=[ " + e + " ]");
      }
    }
  }

  public List<Arguments> providePauseParams() {
    List<Arguments> result = new LinkedList<>();

    return result;
  }

  @Test
  public void elapsedTimeTest() {
    long start = System.nanoTime();
    Timers timers = new Timers("foo");
    try {
      Thread.sleep(10L);
    } catch (InterruptedException ignore) {
      // ignore the exception
    }
    timers.pause("foo");
    long end = System.nanoTime();
    long duration = (end - start) / 1000000L;
    long elapsed = timers.getElapsedTime("foo");
    assertTrue((elapsed <= duration),
        "The elapsed time (" + elapsed + ") exceeds the bookend "
            + "duration (" + duration + ").");

    Map<String, Long> timings = timers.getTimings();
    assertEquals(timings.get("foo"), elapsed,
        "The elapsed time does not match the timings.");
  }

  @Test
  public void usageTest() {
    try {
      String[] timerNames = { "foo", "bar", "phoo", "foox" };
      long start = milliTime();
      Timers timers = new Timers(timerNames);

      Set<String> timerNameSet = new LinkedHashSet<>(Arrays.asList(timerNames));

      for (String timerName : timerNameSet) {
        assertTrue(timers.isRunning(timerName),
            "Timer (" + timerName + ") expected to be running: "
                + timers);
        assertFalse(timers.isPaused(timerName),
            "Timer (" + timerName + ") unexpectedly paused: "
                + timers);
      }

      Set<String> pauseSet = Set.of("bar", "foox");

      long prePause = milliTime() - start;

      Thread.sleep(10L);

      // pause two of the timers
      timers.pause("bar", "foox");

      for (String timerName : timerNameSet) {
        if (pauseSet.contains(timerName)) {
          assertTrue(timers.isPaused(timerName),
              "Paused timer (" + timerName + ") not registering "
                  + "as paused: " + timers);
          assertFalse(timers.isRunning(timerName),
              "Paused timer (" + timerName + ") still "
                  + "registering as running: " + timers);

        } else {
          assertTrue(timers.isRunning(timerName),
              "Running timer (" + timerName + ") not "
                  + "registering as running: " + timers);
          assertFalse(timers.isPaused(timerName),
              "Running timer (" + timerName + ") unexpectedly "
                  + "paused: " + timers);

        }
      }

      Thread.sleep(10L);

      long postPause = milliTime() - start;

      Thread.sleep(10L);

      // get the timings
      Map<String, Long> timings = timers.getTimings();

      Thread.sleep(10L);

      long postTimings = milliTime() - start;

      assertEquals(timerNames.length, timings.size(),
          "Timings map is not the expected size: " + timings);

      assertEquals(timerNameSet, timings.keySet(),
          "Timings does not have expected timer names: "
              + timings);

      timings.forEach((key, value) -> {
        if (pauseSet.contains(key)) {
          assertTrue((value >= prePause),
              "Paused timer (" + key + ") value (" + value
                  + ") should not be less than pre-paused value: "
                  + prePause);
          assertTrue((value <= postPause),
              "Paused timer (" + key + ") value (" + value
                  + ") should not be greater than post-paused value: "
                  + postPause);
        } else {
          assertTrue((value >= postPause),
              "Running timer (" + key + ") value (" + value
                  + ") should not be less than post-paused value: "
                  + postPause);
          assertTrue((value <= postTimings),
              "Running timer (" + key + ") value (" + value + ") "
                  + "should not be greater than post-timings duration: "
                  + postTimings);
        }
      });

      long preResume = milliTime() - start;

      Thread.sleep(10L);

      // now resume the paused timers
      timers.resume("bar", "foox");

      long postResume = milliTime() - start;

      for (String timerName : timerNameSet) {
        if (pauseSet.contains(timerName)) {
          assertTrue(timers.isRunning(timerName),
              "Resumed timer (" + timerName
                  + ") not registering as running: " + timers);
          assertFalse(timers.isPaused(timerName),
              "Resumed timer (" + timerName + ") still "
                  + "registering as paused: " + timers);

        } else {
          assertTrue(timers.isRunning(timerName),
              "Running timer (" + timerName + ") not "
                  + "registering as running post-resume: " + timers);
          assertFalse(timers.isPaused(timerName),
              "Running timer (" + timerName + ") unexpectedly "
                  + "paused post-resume: " + timers);

        }
      }

      Thread.sleep(10L);

      timings = timers.getTimings();

      long postTimings2 = milliTime() - start;

      assertEquals(timerNames.length, timings.size(),
          "Timings map is not the expected size: " + timings);

      assertEquals(timerNameSet, timings.keySet(),
          "Timings does not have expected timer names: "
              + timings);

      timings.forEach((key, value) -> {
        if (pauseSet.contains(key)) {
          assertTrue((value >= (prePause + (postTimings2 - postResume))),
              "Resumed timer (" + key + ") value (" + value
                  + ") should not be less than pre-paused + resumed "
                  + "value: "
                  + (prePause + (postTimings2 - postResume)));
          assertTrue((value <= (postPause + (postTimings2 - preResume))),
              "Resumed timer (" + key + ") value (" + value
                  + ") should not be greater than post-paused + resumed "
                  + "value: "
                  + (postPause + (postTimings2 - preResume)));
        } else {
          assertTrue((value >= (postPause + (postTimings2 - postResume))),
              "Running timer (" + key + ") value (" + value
                  + ") should not be less than post-paused + resumed "
                  + "value: "
                  + (postPause + (postTimings2 - postResume)));
          assertTrue((value <= postTimings2),
              "Running timer (" + key + ") value (" + value + ") "
                  + "should not be greater than post-timings + "
                  + "resumed duration: " + postTimings2);
        }
      });

    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed unexpectedly with an exception: " + e);
    }
  }

  @Test
  public void usageTestAll() {
    try {
      String[] timerNames = { "foo", "bar", "phoo", "foox" };
      long start = milliTime();
      Timers timers = new Timers(timerNames);

      Set<String> timerNameSet = new LinkedHashSet<>(Arrays.asList(timerNames));

      for (String timerName : timerNameSet) {
        assertTrue(timers.isRunning(timerName),
            "Timer (" + timerName + ") expected to be running: "
                + timers);
        assertFalse(timers.isPaused(timerName),
            "Timer (" + timerName + ") unexpectedly paused: "
                + timers);
      }

      long prePause = milliTime() - start;

      Thread.sleep(10L);

      // pause two of the timers
      timers.pauseAll();

      for (String timerName : timerNameSet) {
        assertTrue(timers.isPaused(timerName),
            "Paused timer (" + timerName + ") not registering "
                + "as paused: " + timers);
        assertFalse(timers.isRunning(timerName),
            "Paused timer (" + timerName + ") still "
                + "registering as running: " + timers);
      }

      Thread.sleep(10L);

      long postPause = milliTime() - start;

      Thread.sleep(10L);

      // get the timings
      Map<String, Long> timings = timers.getTimings();

      Thread.sleep(10L);

      long postTimings = milliTime() - start;

      assertEquals(timerNames.length, timings.size(),
          "Timings map is not the expected size: " + timings);

      assertEquals(timerNameSet, timings.keySet(),
          "Timings does not have expected timer names: "
              + timings);

      timings.forEach((key, value) -> {
        assertTrue((value >= prePause),
            "Paused timer (" + key + ") value (" + value
                + ") should not be less than pre-paused value: "
                + prePause);
        assertTrue((value <= postPause),
            "Paused timer (" + key + ") value (" + value
                + ") should not be greater than post-paused value: "
                + postPause);
      });

      long preResume = milliTime() - start;

      Thread.sleep(10L);

      // now resume the paused timers
      timers.resumeAll();

      long postResume = milliTime() - start;

      Thread.sleep(10L);

      for (String timerName : timerNameSet) {
        assertTrue(timers.isRunning(timerName),
            "Resumed timer (" + timerName + ") not registering "
                + "as running: " + timers);
        assertFalse(timers.isPaused(timerName),
            "Resumed timer (" + timerName + ") still "
                + "registering as paused: " + timers);
      }

      timings = timers.getTimings();

      long postTimings2 = milliTime() - start;

      assertEquals(timerNames.length, timings.size(),
          "Timings map is not the expected size: " + timings);

      assertEquals(timerNameSet, timings.keySet(),
          "Timings does not have expected timer names: "
              + timings);

      timings.forEach((key, value) -> {
        assertTrue((value >= (prePause + (postTimings2 - postResume))),
            "Resumed timer (" + key + ") value (" + value
                + ") should not be less than pre-paused + resumed "
                + "value: "
                + (prePause + (postTimings2 - postResume)));
        assertTrue((value <= (postPause + (postTimings2 - preResume))),
            "Resumed timer (" + key + ") value (" + value
                + ") should not be greater than post-paused + resumed "
                + "value: "
                + (postPause + (postTimings2 - preResume)));
      });

    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed unexpectedly with an exception: " + e);
    }
  }

  @Test
  public void testDelayedStart() {
    try {
      Set<String> timerSet = Set.of("foo", "bar", "phoo", "foox");
      Set<String> delayedSet = Set.of("phoo", "foox");

      long start1a = milliTime();
      Timers timers = new Timers("foo", "bar");
      long start1b = milliTime();

      Thread.sleep(10L);

      long start2a = milliTime();
      timers.start("phoo", "foox");
      long start2b = milliTime();

      Thread.sleep(10L);

      long endB = milliTime();
      Thread.sleep(2L);
      Map<String, Long> timings = timers.getTimings();
      long endA = milliTime();

      long duration1a = endA - start1a;
      long duration1b = endB - start1b;

      long duration2a = endA - start2a;
      long duration2b = endB - start2b;

      timings.forEach((key, value) -> {
        if (delayedSet.contains(key)) {
          assertTrue((value >= duration2b),
              "Delayed timer (" + key + ") value (" + value
                  + ") should not be less than " + duration2b);
          assertTrue((value <= duration2a),
              "Delayed timer (" + key + ") value (" + value
                  + ") should not be greater than " + duration2a);
        } else {
          assertTrue((value >= duration1b),
              "Initial timer (" + key + ") value (" + value
                  + ") should not be less than " + duration1b);
          assertTrue((value <= duration1a),
              "Initial timer (" + key + ") value (" + value
                  + ") should not be greater than " + duration1a);
        }
      });

    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed unexpectedly with an exception: " + e);
    }
  }

  @Test
  public void testPausedTimings() {
    try {
      String[] timerNames = { "foo", "bar", "phoo", "foox" };
      Set<String> pausedTimers = Set.of("foo", "bar");

      Timers timers = new Timers(timerNames);
      Thread.sleep(10L);
      timers.pause("foo", "bar");
      Thread.sleep(10L);

      Map<String, Long> timings1 = timers.getTimings();

      Thread.sleep(10L);

      Map<String, Long> timings2 = timers.getTimings();

      for (String timerName : timerNames) {
        if (pausedTimers.contains(timerName)) {
          assertEquals(timings1.get(timerName), timings2.get(timerName),
              "Paused timer values do not match.  timerName=[ "
                  + timerName + " ]");
        } else {
          assertTrue(timings2.get(timerName) > timings1.get(timerName),
              "Running timer value dod not increase.  "
                  + "timerName=[ " + timerName + " ], value1=[ "
                  + timings1.get(timerName) + " ], value2=[ "
                  + timings2.get(timerName) + " ]");
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed unexpectedly with an exception: " + e);
    }
  }

  private long milliTime() {
    return System.nanoTime() / 1000000L;
  }
}
