package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import static com.senzing.util.ThreadJoinerPool.PostJoinCallback;

/**
 * Tests for {@link AsyncWorkerPool}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ThreadJoinerPoolTest {
  @Test
  public void testThreadJoinerPool() {
    final int   THREAD_COUNT    = 50;
    final int   POOL_SIZE       = 10;
    final long  WORKER_TIMEOUT  = 2000L;

    final ThreadJoinerPool pool = new ThreadJoinerPool(POOL_SIZE);

    List<Thread> workers    = new ArrayList<>(THREAD_COUNT);
    List<Thread> running    = new LinkedList<>();
    List<Thread> completed  = new LinkedList<>();
    for (int index = 0; index < THREAD_COUNT; index++) {
      workers.add(new Thread(() -> {
        try {
          synchronized (running) {
            running.add(Thread.currentThread());
          }
          Thread.sleep(WORKER_TIMEOUT);
          synchronized (running) {
            running.remove(Thread.currentThread());
          }

        } catch (Exception ignore) {
          // do nothing
        }
      }));
    }

    // start the workers
    for (Thread worker : workers) {
      worker.start();
    }

    // check for instant return time on the first N (pool size)
    for (int index = 0; index < THREAD_COUNT; index++) {
      // check if closed
      assertFalse(pool.isClosed(),
                  "Pool prematurely indicating it is closed");

      Thread worker = workers.get(index);
      long start = System.nanoTime();
      pool.join(worker, (thread) -> {
        synchronized (completed) {
          completed.add(Thread.currentThread());
        }
      });
      long duration = (System.nanoTime() - start) / 1000000L;
      if (index < POOL_SIZE) {
        assertTrue((duration < 100L),
                   "Join call " + index + " was not instant: "
                       + duration + " ms");
      } else if (index == POOL_SIZE) {
        assertTrue((duration > 500L),
            "Join call " + index + " returned too quickly for pool: "
                + duration + " ms");

        synchronized (completed) {
          assertTrue((completed.size() > (index - POOL_SIZE)),
                     "Joined more than the pool size, but the count of "
                         + "completed threads is not as expected: "
                         + completed.size());

        }
      }
    }

    // check if closed
    assertFalse(pool.isClosed(),
                "Pool prematurely indicating it is closed");

    // do a join-and-close
    pool.joinAndClose();

    // check if closed
    assertTrue(pool.isClosed(),
               "Pool prematurely indicating it is closed");

    // ensure none are running
    assertEquals(0, running.size(),
                 "The number of running threads is unexpected: "
                     + running.size());

    // ensure all are completed
    assertEquals(THREAD_COUNT, completed.size(),
                 "The number of completed threads is unexpected: "
                 + completed.size());

  }
}
