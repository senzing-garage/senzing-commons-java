package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import uk.org.webcompere.systemstubs.stream.SystemErr;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary {@link WorkerThreadPool} tests covering the corners not in
 * {@code WorkerThreadPoolTest}: constructor argument validation, base-name
 * suffix trimming, close-then-execute error path, the pause/resume {@link
 * AccessToken} flow, task-failure propagation, and the {@link
 * WorkerThreadPool#main} smoke driver.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class WorkerThreadPoolExtraTest
{
  // -------------------------------------------------------------------
  // Constructor validation
  // -------------------------------------------------------------------

  @Test
  public void constructorRejectsZeroSize()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> new WorkerThreadPool("test", 0));
  }

  @Test
  public void constructorRejectsNegativeSize()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> new WorkerThreadPool("test", -1));
  }

  @Test
  public void constructorTrimsTrailingDashFromBaseName()
  {
    // Per the constructor: a trailing "-" on baseName is stripped
    // before being prepended to thread names with a "-" separator.
    WorkerThreadPool pool = new WorkerThreadPool("foo-", 2);
    try {
      assertEquals(2, pool.size());
      // Internal thread names are not exposed; just verify the
      // constructor accepts a baseName ending in "-" without crashing
      // and produces the documented thread count.
    } finally {
      pool.close(true);
    }
  }

  // -------------------------------------------------------------------
  // size / isClosed
  // -------------------------------------------------------------------

  @Test
  public void sizeReturnsConstructorArgument()
  {
    WorkerThreadPool pool = new WorkerThreadPool(3);
    try {
      assertEquals(3, pool.size());
    } finally {
      pool.close(true);
    }
  }

  @Test
  public void isClosedReturnsFalseBeforeCloseAndTrueAfter()
  {
    WorkerThreadPool pool = new WorkerThreadPool(1);
    assertFalse(pool.isClosed());
    pool.close(true);
    assertTrue(pool.isClosed());
  }

  @Test
  public void closeIsIdempotent()
  {
    WorkerThreadPool pool = new WorkerThreadPool(1);
    pool.close(true);
    // Second close should be a no-op (the impl returns early when
    // already closed).
    pool.close(true);
    assertTrue(pool.isClosed());
  }

  @Test
  public void closeWithoutJoinReturnsImmediately()
  {
    WorkerThreadPool pool = new WorkerThreadPool(2);
    pool.close(false);
    assertTrue(pool.isClosed(),
               "close(false) should still mark the pool closed");
  }

  // -------------------------------------------------------------------
  // execute on closed pool
  // -------------------------------------------------------------------

  @Test
  public void executeOnClosedPoolThrows()
  {
    WorkerThreadPool pool = new WorkerThreadPool(1);
    pool.close(true);
    assertThrows(IllegalStateException.class,
                 () -> pool.execute(() -> "anything"));
  }

  // -------------------------------------------------------------------
  // Task failure propagation
  // -------------------------------------------------------------------

  @Test
  public void taskExceptionIsPropagatedToCaller()
  {
    WorkerThreadPool pool = new WorkerThreadPool(1);
    try {
      RuntimeException expected = new RuntimeException("boom");
      RuntimeException thrown = assertThrows(RuntimeException.class,
          () -> pool.execute(() -> { throw expected; }));
      assertEquals("boom", thrown.getMessage(),
                   "Task's exception should propagate verbatim");
    } finally {
      pool.close(true);
    }
  }

  // -------------------------------------------------------------------
  // pause / resume / AccessToken
  // -------------------------------------------------------------------

  @Test
  public void pauseReturnsTokenAndIsPausedReportsTrue()
  {
    WorkerThreadPool pool = new WorkerThreadPool(2);
    try {
      AccessToken token = pool.pause();
      assertNotNull(token, "pause should return a non-null token");
      assertTrue(pool.isPaused());

      // Subsequent pause() returns null.
      assertNull(pool.pause(),
                 "Second pause() should return null when already paused");
    } finally {
      pool.close(true);
    }
  }

  @Test
  public void resumeWithCorrectTokenSucceeds()
  {
    WorkerThreadPool pool = new WorkerThreadPool(2);
    try {
      AccessToken token = pool.pause();
      assertTrue(pool.resume(token));
      assertFalse(pool.isPaused());
    } finally {
      pool.close(true);
    }
  }

  @Test
  public void resumeWithNullTokenReturnsFalse()
  {
    WorkerThreadPool pool = new WorkerThreadPool(1);
    try {
      // Per javadoc: a null token is a no-op that returns false.
      assertFalse(pool.resume(null));
    } finally {
      pool.close(true);
    }
  }

  @Test
  public void resumeWhenNotPausedThrows()
  {
    WorkerThreadPool pool = new WorkerThreadPool(1);
    try {
      AccessToken token = new AccessToken();
      assertThrows(IllegalStateException.class,
                   () -> pool.resume(token));
    } finally {
      pool.close(true);
    }
  }

  @Test
  public void resumeWithWrongTokenThrows()
  {
    WorkerThreadPool pool = new WorkerThreadPool(1);
    try {
      pool.pause();
      AccessToken wrong = new AccessToken();
      assertThrows(IllegalArgumentException.class,
                   () -> pool.resume(wrong));
    } finally {
      pool.close(true);
    }
  }

  // -------------------------------------------------------------------
  // main() smoke driver
  // -------------------------------------------------------------------

  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  @ResourceLock(Resources.SYSTEM_OUT)
  @ResourceLock(Resources.SYSTEM_ERR)
  public void mainExecutesArgumentsAndPrintsOutput() throws Exception
  {
    SystemOut out = new SystemOut();
    SystemErr err = new SystemErr();
    err.execute(() -> {
      out.execute(() -> {
        WorkerThreadPool.main(
            new String[] {"hello", "world", "ERROR-fail", "again"});
      });
    });

    String output = out.getText();
    // Each non-ERROR arg produces a line; the JOINING / JOINED
    // diagnostics also appear.
    assertTrue(output.contains("hello"),
               "Output should contain 'hello' result: " + output);
    assertTrue(output.contains("JOINING AGAINST THE POOL"),
               "Output should contain joining diagnostic: " + output);
    assertTrue(output.contains("JOINED AGAINST THE POOL"),
               "Output should contain joined diagnostic: " + output);
  }
}
