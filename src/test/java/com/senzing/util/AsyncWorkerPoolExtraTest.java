package com.senzing.util;

import com.senzing.util.AsyncWorkerPool.AsyncResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary {@link AsyncWorkerPool.AsyncResult} tests covering the
 * failure-propagation branch in {@link AsyncResult#getValue()} and the
 * failure-formatting branch in
 * {@link AsyncResult#toString()} — both unreachable from the
 * happy-path tasks in {@code AsyncWorkerPoolTest}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class AsyncWorkerPoolExtraTest
{
  /**
   * If a task throws, {@link AsyncResult#getValue()} must rethrow that
   * exception verbatim per the documented contract.
   */
  @Test
  public void asyncResultGetValueRethrowsTaskFailure()
      throws Exception
  {
    AsyncWorkerPool<String> pool = new AsyncWorkerPool<>(1);
    try {
      Exception expected = new IllegalStateException("simulated");

      // First call: kick off the failing task. The previous
      // execution's result is null (no previous execution), so
      // execute returns null.
      AsyncResult<String> first = pool.execute(() -> {
        throw expected;
      });
      assertEquals(null, first,
                   "First execute() returns previous result, which"
                       + " is null on a fresh worker");

      // Second call: returns the previous (failed) AsyncResult.
      // Submit a no-op as the new task.
      AsyncResult<String> previous = pool.execute(() -> "ok");
      assertNotNull(previous,
                    "Second execute should return the previous"
                        + " AsyncResult");

      // getValue on the failed result must rethrow.
      Exception thrown = assertThrows(Exception.class,
                                      previous::getValue);
      assertSame(expected, thrown,
                 "AsyncResult.getValue() must rethrow the task's"
                     + " original exception verbatim");
    } finally {
      pool.close();
    }
  }

  /**
   * {@link AsyncResult#toString()} must include a
   * {@code failure=[...]} segment when the underlying task threw,
   * exercising the documented failure-formatting branch.
   */
  @Test
  public void asyncResultToStringIncludesFailureSegmentOnFailure()
      throws Exception
  {
    AsyncWorkerPool<String> pool = new AsyncWorkerPool<>(1);
    try {
      pool.execute(() -> {
        throw new IllegalArgumentException("boom");
      });
      AsyncResult<String> previous = pool.execute(() -> "next");
      assertNotNull(previous);

      String s = previous.toString();
      assertTrue(s.contains("failure=["),
                 "toString must include 'failure=[...]' for a failed"
                     + " AsyncResult; got: " + s);
    } finally {
      pool.close();
    }
  }

  /**
   * {@link AsyncResult#toString()} must NOT include a {@code
   * failure=} segment for a successful task — the failure-segment is appended
   * only when {@code failure != null}.
   */
  @Test
  public void asyncResultToStringOmitsFailureSegmentForSuccess()
      throws Exception
  {
    AsyncWorkerPool<String> pool = new AsyncWorkerPool<>(1);
    try {
      pool.execute(() -> "success-value");
      AsyncResult<String> previous = pool.execute(() -> "next");
      assertNotNull(previous);

      String s = previous.toString();
      assertTrue(!s.contains("failure="),
                 "toString must omit 'failure=' for a successful"
                     + " AsyncResult; got: " + s);
      assertTrue(s.contains("success-value"),
                 "toString must include the value: " + s);
    } finally {
      pool.close();
    }
  }
}
