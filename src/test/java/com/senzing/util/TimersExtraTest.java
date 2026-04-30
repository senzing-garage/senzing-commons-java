package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary {@link Timers} tests covering the corners not in
 * {@code TimersTest}: behavior when a timer name is unknown,
 * idempotent pause/resume on already-paused/running timers, multi-name
 * start/pause/resume return counts, the all-timer variants, the {@link
 * Timers#mergeWith} merge logic, and the default {@link Timers#Timers()}
 * constructor.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class TimersExtraTest
{
  // -------------------------------------------------------------------
  // Unknown-timer behavior
  // -------------------------------------------------------------------

  @Test
  public void hasTimerReturnsFalseForUnknownName()
  {
    Timers timers = new Timers();
    assertFalse(timers.hasTimer("nope"));
  }

  @Test
  public void isPausedReturnsFalseForUnknownName()
  {
    Timers timers = new Timers();
    // Per javadoc: "true if the timer exists and is paused" — must
    // return false when timer does not exist.
    assertFalse(timers.isPaused("nope"),
                "isPaused must return false for unknown timer");
  }

  @Test
  public void isRunningReturnsFalseForUnknownName()
  {
    Timers timers = new Timers();
    assertFalse(timers.isRunning("nope"),
                "isRunning must return false for unknown timer");
  }

  @Test
  public void getElapsedTimeReturnsMinusOneForUnknownName()
  {
    // Per javadoc: returns negative-one if timer name is unrecognized.
    Timers timers = new Timers();
    assertEquals(-1L, timers.getElapsedTime("nope"));
  }

  // -------------------------------------------------------------------
  // Idempotent pause / resume — repeating an action does not increment
  // the return count.
  // -------------------------------------------------------------------

  @Test
  public void pauseRunningTimerSucceedsThenSecondPauseFails()
  {
    Timers timers = new Timers("t1");
    assertEquals(1, timers.pause("t1"),
                 "Pausing a running timer should return 1");
    assertEquals(0, timers.pause("t1"),
                 "Pausing an already-paused timer should return 0");
  }

  @Test
  public void resumePausedTimerSucceedsThenSecondResumeFails()
  {
    Timers timers = new Timers("t1");
    timers.pause("t1");
    assertEquals(1, timers.resume("t1"),
                 "Resuming a paused timer should return 1");
    assertEquals(0, timers.resume("t1"),
                 "Resuming an already-running timer should return 0");
  }

  @Test
  public void pauseUnknownTimerReturnsZero()
  {
    Timers timers = new Timers();
    assertEquals(0, timers.pause("nope"));
  }

  @Test
  public void resumeUnknownTimerReturnsZero()
  {
    Timers timers = new Timers();
    assertEquals(0, timers.resume("nope"));
  }

  // -------------------------------------------------------------------
  // Multi-name start / pause / resume
  // -------------------------------------------------------------------

  @Test
  public void startMultipleNamesCreatesEach()
  {
    Timers timers = new Timers();
    int created = timers.start("a", "b", "c");
    assertEquals(3, created,
                 "start should return one per newly-created timer");
    assertTrue(timers.isRunning("a"));
    assertTrue(timers.isRunning("b"));
    assertTrue(timers.isRunning("c"));
  }

  @Test
  public void startResumesAlreadyPausedTimers()
  {
    Timers timers = new Timers("a", "b");
    timers.pause("a", "b");
    int resumed = timers.start("a", "b");
    assertEquals(2, resumed,
                 "start on paused timers should resume each");
    assertTrue(timers.isRunning("a"));
    assertTrue(timers.isRunning("b"));
  }

  @Test
  public void startSkipsAlreadyRunningTimers()
  {
    Timers timers = new Timers("a", "b");
    int created = timers.start("a", "b", "c");
    assertEquals(1, created,
                 "start should return 1 for the only newly-created timer");
  }

  @Test
  public void pauseMultipleNamesReturnsCount()
  {
    Timers timers = new Timers("a", "b", "c");
    int paused = timers.pause("a", "b", "c");
    assertEquals(3, paused);
  }

  @Test
  public void resumeMultipleNamesReturnsCount()
  {
    Timers timers = new Timers("a", "b", "c");
    timers.pauseAll();
    int resumed = timers.resume("a", "b", "c");
    assertEquals(3, resumed);
  }

  // -------------------------------------------------------------------
  // pauseAll / resumeAll
  // -------------------------------------------------------------------

  @Test
  public void pauseAllReturnsRunningCount()
  {
    Timers timers = new Timers("a", "b", "c");
    assertEquals(3, timers.pauseAll(),
                 "pauseAll should return number of timers paused");
    // Already paused → second pauseAll returns 0.
    assertEquals(0, timers.pauseAll());
  }

  @Test
  public void resumeAllReturnsPausedCount()
  {
    Timers timers = new Timers("a", "b", "c");
    timers.pauseAll();
    assertEquals(3, timers.resumeAll());
    assertEquals(0, timers.resumeAll(),
                 "resumeAll on already-running timers should return 0");
  }

  // -------------------------------------------------------------------
  // mergeWith
  // -------------------------------------------------------------------

  @Test
  public void mergeWithNullIsNoOp()
  {
    Timers timers = new Timers("a");
    timers.mergeWith(null);
    assertNotNull(timers.getTimings().get("a"),
                  "Existing timers should be untouched");
  }

  @Test
  public void mergeWithAddsNewTimers() throws InterruptedException
  {
    Timers source = new Timers("x");
    Thread.sleep(5);
    source.pauseAll();

    Timers target = new Timers();
    target.mergeWith(source);

    assertTrue(target.hasTimer("x"),
               "Target should now contain the merged timer");
    assertTrue(target.getElapsedTime("x") >= 0L,
               "Merged timer should have non-negative elapsed time");
  }

  @Test
  public void mergeWithAccumulatesIntoExistingTimer()
      throws InterruptedException
  {
    Timers t1 = new Timers("x");
    Thread.sleep(5);
    t1.pauseAll();
    long before = t1.getElapsedTime("x");

    Timers t2 = new Timers("x");
    Thread.sleep(5);
    t2.pauseAll();

    t1.mergeWith(t2);
    long after = t1.getElapsedTime("x");

    assertTrue(after >= before,
               "Merged timer's elapsed time should be at least the "
                   + "pre-merge value");
  }

  // -------------------------------------------------------------------
  // getTimings
  // -------------------------------------------------------------------

  @Test
  public void getTimingsReturnsMapWithAllTimers()
      throws InterruptedException
  {
    Timers timers = new Timers("a", "b");
    Thread.sleep(5);
    Map<String, Long> snapshot = timers.getTimings();

    assertEquals(2, snapshot.size());
    assertTrue(snapshot.containsKey("a"));
    assertTrue(snapshot.containsKey("b"));
    assertTrue(snapshot.get("a") >= 0L);
  }

  @Test
  public void getTimingsAfterPauseReturnsAccumulated()
      throws InterruptedException
  {
    Timers timers = new Timers("a");
    Thread.sleep(5);
    timers.pauseAll();
    long paused = timers.getElapsedTime("a");
    Thread.sleep(5);
    long second = timers.getElapsedTime("a");

    assertEquals(paused, second,
                 "Paused timer's elapsed time should not advance");
  }

  // -------------------------------------------------------------------
  // Default constructor — no initial timers
  // -------------------------------------------------------------------

  @Test
  public void defaultConstructorYieldsEmptyTimers()
  {
    Timers timers = new Timers();
    assertEquals(0, timers.getTimings().size());
  }

  @Test
  public void varargsNullArrayTreatedAsEmpty()
  {
    // Per the constructor's javadoc: "A null array is treated like
    // an empty array."
    Timers timers = new Timers((String[]) null);
    assertEquals(0, timers.getTimings().size());
  }
}
