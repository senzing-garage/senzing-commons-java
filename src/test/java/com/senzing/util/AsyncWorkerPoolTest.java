package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AsyncWorkerPool}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class AsyncWorkerPoolTest {
  @Test
  public void asyncWorkerPoolTest() {
    try {
      final int                   THREAD_COUNT    = 100;
      final int                   POOL_SIZE       = 10;
      final Set<Long>             workerThreadIds = new LinkedHashSet<>();
      final Set<Long>             appThreadIds    = new LinkedHashSet<>();
      final Set<Long>             failedThreads   = new LinkedHashSet<>();
      final Map<Long, Exception>  exceptions      = new LinkedHashMap<>();

      final AsyncWorkerPool<Long> pool = new AsyncWorkerPool<>(POOL_SIZE);

      assertEquals(POOL_SIZE, pool.size(),
                   "AsyncWorkerPool is not the expected size.");
      assertEquals(false, pool.isClosed(),
                   "AsyncWorkerPool unexpectedly closed.");

      Runnable runnable = () -> {
        Long appThreadId = Thread.currentThread().getId();
        synchronized (appThreadIds) {
          appThreadIds.add(appThreadId);
        }
        AsyncWorkerPool.AsyncResult<Long> result = pool.execute(() -> {
          Long workerThreadId = Thread.currentThread().getId();
          synchronized (workerThreadIds) {
            workerThreadIds.add(workerThreadId);
          }
          Thread.sleep(100);
          return workerThreadId;
        });
        if (result != null) {
          try {
            Long resultValue = result.getValue();
            synchronized (workerThreadIds) {
              if (!workerThreadIds.contains(resultValue)) {
                failedThreads.add(resultValue);
              }
            }
          } catch (Exception e) {
            synchronized (exceptions) {
              exceptions.put(appThreadId, e);
            }
          }
        }
      };

      // create the app threads
      List<Thread> appThreads = new ArrayList<>(THREAD_COUNT);
      for (int index = 0; index < THREAD_COUNT; index++) {
        appThreads.add(new Thread(runnable));
      }

      // start the app threads
      for (Thread thread : appThreads) {
        thread.start();
      }

      // join the app threads
      for (Thread thread : appThreads) {
        thread.join();
      }

      Thread.sleep(1000);

      // close the pool
      List<AsyncWorkerPool.AsyncResult<Long>> results = pool.close();

      assertEquals(THREAD_COUNT, appThreadIds.size(),
                   "Unexpected number of application threads");
      assertEquals(POOL_SIZE, workerThreadIds.size(),
                   "Unexpected number of worker threads");

      Set<Long> intersection = new LinkedHashSet<>(appThreadIds);
      intersection.retainAll(workerThreadIds);

      assertEquals(0, intersection.size(),
                   "Found overlap between application and worker threads");

      assertEquals(0, failedThreads.size(),
                   "At least one worker thread failed a result.");
      assertEquals(POOL_SIZE, results.size(),
                   "Unexpectedly found unfinished results on close");
      assertEquals(true, pool.isClosed(),
                   "Pool unexpectedly not marked as closed.");

      for (AsyncWorkerPool.AsyncResult<Long> result : results) {
        Long resultValue = result.getValue();
        if (!workerThreadIds.contains(resultValue)) {
          fail("Unexpected result (" + resultValue + ") from worker thread: "
                   + workerThreadIds);
        }
      }

      // try to use the pool after closed
      try {
        pool.execute(() -> {
          return 10L;
        });
        fail("AsyncWorkerPool execution succeeded after being closed.");

      } catch (IllegalStateException expected) {
        // ignore the exception, it is expected
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("asyncWorkerPoolTest() failed with exception: " + e);
    }
  }
}
