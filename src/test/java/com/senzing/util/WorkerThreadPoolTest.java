package com.senzing.util;

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
import static com.senzing.util.WorkerThreadPool.*;

/**
 * Tests for {@link WorkerThreadPool}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class WorkerThreadPoolTest {
  public List<Arguments> providePoolParams() {
    List<Arguments> result = new LinkedList<>();

    result.add(arguments(((String) null)));
    result.add(arguments("TestThread"));

    return result;
  }

  @ParameterizedTest
  @MethodSource("providePoolParams")
  public void testPool(String baseName) {
    WorkerThreadPool pool = null;
    final int POOL_SIZE = 10;
    try {
      pool = (baseName == null) ? new WorkerThreadPool(POOL_SIZE)
          : new WorkerThreadPool(baseName, POOL_SIZE);

      assertEquals(POOL_SIZE, pool.size(),
                   "Pool size not as expected.");
      assertFalse(pool.isClosed(),
          "Pool unexpectedly registers as closed.");
      assertFalse(pool.isPaused(),
                  "Pool unexpectedly registers as paused.");

      String threadName = pool.execute(() -> {
        return Thread.currentThread().getName();
      });

      if (baseName == null) {
        assertTrue(threadName.startsWith(DEFAULT_BASE_NAME),
                   "Thread base name (" + threadName
                       + ") does not have default prefix: "
                       + DEFAULT_BASE_NAME);
      } else {
        assertTrue(threadName.startsWith(baseName),
                   "Thread base name (" + threadName
                       + ") does not have specified prefix: "
                       + baseName);
      }

      // pause the pool
      AccessToken token = pool.pause();

      // check if paused
      assertTrue(pool.isPaused(),
                 "Pool is not registering as paused.");

      // create an alternate thread to attempt to use the paused pool
      final boolean[] completed = { false };
      final WorkerThreadPool poolRef = pool;
      Thread thread = new Thread(()-> {
        boolean complete = poolRef.execute(() -> true);
        synchronized (completed) {
          completed[0] = complete;
        }
      });
      thread.start();

      Thread.sleep(100L);
      synchronized (completed) {
        assertFalse(completed[0],
                    "Paused pool unexpectedly executed a task.");
      }

      // attempt to re-pause
      assertNull(pool.pause(),
                 "Attempt to re-pause pool did not return null");

      Thread.sleep(100L);

      // check if paused
      assertTrue(pool.isPaused(),
                 "Pool is not registering as paused after re-pause.");

      synchronized (completed) {
        assertFalse(completed[0],
                    "Paused pool unexpectedly executed a task after "
                    + "re-pause.");
      }

      assertFalse(pool.resume(null),
                  "Attempt to resume without an access token did "
                      + "not return false");

      Thread.sleep(100L);

      // check if paused
      assertTrue(pool.isPaused(),
                 "Pool is not registering as paused after resume "
                     + "with null token");

      synchronized (completed) {
        assertFalse(completed[0],
                    "Paused pool unexpectedly executed a task after "
                        + "resume with null token");
      }

      AccessToken fakeToken = new AccessToken();
      try {
        pool.resume(fakeToken);
        fail("Resumed pool with invalid access token");

      } catch (IllegalArgumentException expected) {
        // success
      }

      Thread.sleep(100L);
      // check if paused
      assertTrue(pool.isPaused(),
                 "Pool is not registering as paused after resume "
                     + "with invalid token.");

      synchronized (completed) {
        assertFalse(completed[0],
                    "Paused pool unexpectedly executed a task after "
                        + "resume with invalid token");
      }

      // resume the pool for real
      assertTrue(pool.resume(token), "Pool resume did not return true");

      // check if paused
      assertFalse(pool.isPaused(),
                 "Pool is still registering as paused after resume.");

      Thread.sleep(100L);

      synchronized (completed) {
        assertTrue(completed[0],
                  "Pool has not executed a task after resume");
      }

      // try to resume when not paused
      try {
        pool.resume(fakeToken);
        fail("Resumed pool allowed a second resume call.");
      } catch (IllegalStateException expected) {
        // success
      }

      // close the pool
      pool.close(true);

      // check if closed
      assertTrue(pool.isClosed(),
                  "Closed pool does not register as closed.");
      pool = null;

    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed unexpectedly with an exception: " + e);

    } finally {
      if (pool != null) {
        pool.close(true);
      }
    }

  }
}
