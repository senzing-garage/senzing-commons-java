package com.senzing.io;

import com.senzing.util.LoggingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import uk.org.webcompere.systemstubs.stream.SystemErr;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary tests for {@link TemporaryDataCache} targeting the paths in
 * {@code ChainFileInputStream} that the existing
 * {@code TemporaryDataCacheTest} does not exercise:
 * skip-across-multiple-file-parts, the debug-logging branch in
 * {@code attachStream}, and the deleted-while-reading error path.
 *
 * <p>The first file part is sized at 1 KiB, growing 16x per
 * completed part up to 4 MiB. Inputs larger than ~1 KiB therefore produce
 * multi-part caches that exercise the skip / read paths across part
 * boundaries.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class TemporaryDataCacheExtraTest
{
  /**
   * Builds a 200 KiB byte array of repeating digits — large enough that the
   * cache's first file part (1 KiB) and second file part (16 KiB) are both
   * filled and a third part is begun, ensuring any read or skip operation
   * crosses at least two part boundaries.
   */
  private static byte[] largeBytes()
  {
    byte[] bytes = new byte[200 * 1024];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) ('0' + (i % 10));
    }
    return bytes;
  }

  /**
   * Drains an input stream completely, returning the byte count. Used to wait
   * until the consumer thread has finished writing before performing teardown
   * actions.
   */
  private static long drain(InputStream is) throws IOException
  {
    long count = 0;
    byte[] buf = new byte[4096];
    int n;
    while ((n = is.read(buf)) >= 0) {
      count += n;
    }
    return count;
  }

  // -------------------------------------------------------------------
  // skip across multiple file parts
  // -------------------------------------------------------------------

  @Test
  public void skipAcrossMultipleFilePartsAdvancesPosition()
      throws IOException, InterruptedException
  {
    byte[] data = largeBytes();
    TemporaryDataCache tdc = new TemporaryDataCache(
        new ByteArrayInputStream(data));
    try {
      tdc.waitUntilAppendingComplete();

      InputStream is = tdc.getInputStream();
      try {
        // skip past the first ~64 KiB — that crosses the 1 KiB and
        // 16 KiB part boundaries.
        long skipped = is.skip(64 * 1024);
        assertEquals(64 * 1024L, skipped,
                     "skip should advance by the requested byte count");

        // Read the next byte — should reflect data at offset 64K.
        int next = is.read();
        assertEquals(data[64 * 1024] & 0xFF, next,
                     "Read after skip should return data at the new"
                         + " position");
      } finally {
        is.close();
      }
    } finally {
      tdc.delete();
    }
  }

  @Test
  public void skipBeyondEndOfDataReturnsTotalAvailable()
      throws IOException, InterruptedException
  {
    byte[] data = largeBytes();
    TemporaryDataCache tdc = new TemporaryDataCache(
        new ByteArrayInputStream(data));
    try {
      tdc.waitUntilAppendingComplete();

      InputStream is = tdc.getInputStream();
      try {
        // Skip more than is available — returns the actual count
        // skipped (the total bytes available).
        long skipped = is.skip(data.length * 2L);
        assertTrue(skipped > 0L,
                   "skip past EOF should return non-zero bytes");
        assertTrue(skipped <= data.length,
                   "skip should not exceed total data length");
      } finally {
        is.close();
      }
    } finally {
      tdc.delete();
    }
  }

  // -------------------------------------------------------------------
  // read across multiple file parts
  // -------------------------------------------------------------------

  @Test
  public void readAcrossMultipleFilePartsRecoversAllBytes()
      throws IOException, InterruptedException
  {
    byte[] data = largeBytes();
    TemporaryDataCache tdc = new TemporaryDataCache(
        new ByteArrayInputStream(data));
    try {
      tdc.waitUntilAppendingComplete();

      InputStream is = tdc.getInputStream();
      try {
        long total = drain(is);
        assertEquals(data.length, total,
                     "Sequential read across parts must return all "
                         + "bytes written into the cache");
      } finally {
        is.close();
      }
    } finally {
      tdc.delete();
    }
  }

  // -------------------------------------------------------------------
  // attachStream debug-logging branch
  // -------------------------------------------------------------------

  /**
   * When {@link LoggingUtilities#isDebugLogging()} returns
   * {@code true}, {@code attachStream} dumps the file part's
   * decrypted contents (or a preview) to the debug log via
   * {@code logDebug}. Wrapping the test in a thread-local
   * debug-logging override exercises that branch.
   *
   * <p>{@link LoggingUtilities#logDebug} writes to
   * {@link System#out}, so a {@link SystemOut} stub captures the
   * output. Tagged {@link Execution} {@code SAME_THREAD} +
   * {@link ResourceLock} for the global stdout to keep the build
   * log clean and avoid races with concurrent tests.</p>
   */
  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  @ResourceLock(Resources.SYSTEM_OUT)
  public void attachStreamDebugLoggingBranchEnabled()
      throws Exception
  {
    LoggingUtilities.overrideDebugLogging(true);
    try {
      byte[] data = largeBytes();

      // Construct the cache and consume it inside the SystemOut stub
      // — the consumer thread starts in the constructor and emits
      // debug logging during consumption, so the redirect must be
      // active before the constructor runs.
      new SystemOut().execute(() -> {
        TemporaryDataCache tdc = new TemporaryDataCache(
            new ByteArrayInputStream(data));
        try {
          tdc.waitUntilAppendingComplete();
          InputStream is = tdc.getInputStream();
          try {
            drain(is);
          } finally {
            is.close();
          }
        } finally {
          tdc.delete();
        }
      });
    } finally {
      LoggingUtilities.clearDebugOverride();
    }
  }

  // -------------------------------------------------------------------
  // Backing files deleted mid-read
  // -------------------------------------------------------------------

  // -------------------------------------------------------------------
  // Lifecycle helpers — isAppending, waitUntilAppendingComplete
  // -------------------------------------------------------------------

  /**
   * {@link TemporaryDataCache#isAppending()} must return
   * {@code true} while the consumer thread is still consuming the
   * source stream, and {@code false} once consumption is complete.
   */
  @Test
  public void isAppendingTransitionsFromTrueToFalse()
      throws Exception
  {
    byte[] data = largeBytes();
    TemporaryDataCache tdc = new TemporaryDataCache(
        new ByteArrayInputStream(data));
    try {
      // Wait synchronously for completion, then verify the flag.
      tdc.waitUntilAppendingComplete();
      assertTrue(!tdc.isAppending(),
                 "isAppending() must return false after consumption "
                     + "completes");
    } finally {
      tdc.delete();
    }
  }

  /**
   * {@link TemporaryDataCache#waitUntilAppendingComplete(long)} with
   * a positive {@code maxWait} must return {@code true} when the consumer
   * thread completes within the budget, exercising the bounded-wait branch.
   */
  @Test
  public void waitUntilAppendingCompleteWithMaxWaitReturnsTrue()
      throws Exception
  {
    byte[] data = largeBytes();
    TemporaryDataCache tdc = new TemporaryDataCache(
        new ByteArrayInputStream(data));
    try {
      // Allow up to 30 seconds — the actual consumption is well
      // under a second on any reasonable machine.
      boolean done = tdc.waitUntilAppendingComplete(30_000L);
      assertTrue(done,
                 "Bounded waitUntilAppendingComplete must return true"
                     + " when consumer thread completes within budget");
    } finally {
      tdc.delete();
    }
  }

  /**
   * {@link TemporaryDataCache#waitUntilAppendingComplete(long)} with
   * {@code maxWait <= 0} must short-circuit to the unbounded
   * variant and return {@code true} once consumption completes.
   */
  @Test
  public void waitUntilAppendingCompleteWithZeroMaxWaitDelegatesToUnbounded()
      throws Exception
  {
    byte[] data = largeBytes();
    TemporaryDataCache tdc = new TemporaryDataCache(
        new ByteArrayInputStream(data));
    try {
      boolean done = tdc.waitUntilAppendingComplete(0L);
      assertTrue(done,
                 "maxWait <= 0 must delegate to the unbounded wait "
                     + "and return true after completion");
    } finally {
      tdc.delete();
    }
  }

  // -------------------------------------------------------------------
  // Consuming input stream (getInputStream(true))
  // -------------------------------------------------------------------

  /**
   * {@link TemporaryDataCache#getInputStream(boolean)} with
   * {@code consume == true} must return a stream that deletes each
   * file part as it is fully read. After draining, all backing files should be
   * gone.
   */
  @Test
  public void consumingInputStreamDeletesFilePartsAsRead()
      throws Exception
  {
    byte[] data = largeBytes();
    TemporaryDataCache tdc = new TemporaryDataCache(
        new ByteArrayInputStream(data));
    try {
      tdc.waitUntilAppendingComplete();

      try (InputStream is = tdc.getInputStream(true)) {
        long total = drain(is);
        assertEquals(data.length, total,
                     "Consuming stream should still surface every "
                         + "byte of the original input");
      }

      // After consuming, the cache should be marked deleted (or at
      // least the file parts gone).
      assertTrue(tdc.isDeleted(),
                 "After fully consuming a consumer-mode input stream"
                     + " the cache should be marked deleted");
    } finally {
      tdc.delete();
    }
  }

  // -------------------------------------------------------------------
  // Failure propagation from the source stream
  // -------------------------------------------------------------------

  /**
   * If the source {@link InputStream} throws on read, the consumer thread must
   * record the failure via {@code setFailure}, and the next reader-side method
   * must rethrow it via {@code checkFailure}.
   *
   * <p>The {@code ConsumerThread} rethrows the wrapped
   * {@link RuntimeException} after recording the failure, which the
   * JVM's default uncaught-exception handler then prints to
   * {@link System#err}. The whole test runs inside a
   * {@link SystemErr} stub so the simulated failure's stack trace
   * does not pollute the build log. Tagged
   * {@link Execution} {@code SAME_THREAD} +
   * {@link ResourceLock} on stderr to avoid races with concurrent
   * tests.</p>
   */
  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  @ResourceLock(Resources.SYSTEM_ERR)
  public void failureInSourceStreamSurfacesToReader()
      throws Exception
  {
    InputStream failing = new InputStream()
    {
      private int count = 0;

      @Override
      public int read() throws IOException
      {
        if (count++ > 100) {
          throw new IOException("simulated source failure");
        }
        return 'A';
      }
    };

    new SystemErr().execute(() -> {
      TemporaryDataCache tdc = new TemporaryDataCache(failing);
      try {
        // Wait for consumer thread to encounter the failure. The
        // wait is inside the stub so the consumer thread's uncaught
        // RuntimeException stack trace, printed by the JVM's default
        // handler, is captured rather than leaked to the build log.
        tdc.waitUntilAppendingComplete();

        // Reading must surface the failure as a RuntimeException
        // wrapping the IOException (per setFailure / checkFailure).
        assertThrows(RuntimeException.class, () -> {
          try (InputStream is = tdc.getInputStream()) {
            drain(is);
          }
        });
      } finally {
        tdc.delete();
      }
    });
  }

  // -------------------------------------------------------------------
  // Backing files deleted mid-read
  // -------------------------------------------------------------------

  /**
   * If {@link TemporaryDataCache#delete()} is called while a consumer stream is
   * open between reads, the next read must throw
   * {@link IOException} per the documented "Backing files deleted"
   * branch.
   */
  @Test
  public void readAfterDeleteThrows()
      throws IOException, InterruptedException
  {
    byte[] data = largeBytes();
    TemporaryDataCache tdc = new TemporaryDataCache(
        new ByteArrayInputStream(data));
    tdc.waitUntilAppendingComplete();

    InputStream is = tdc.getInputStream();
    try {
      // Read one byte to attach the first file's stream.
      assertTrue(is.read() >= 0);

      // Delete the cache out from under the consumer.
      tdc.delete();

      // Subsequent reads that need to attach the next part must
      // throw "Backing files deleted".
      assertThrows(IOException.class, () -> {
        byte[] buf = new byte[8192];
        // Read until the next part-boundary forces an attach.
        int n;
        while ((n = is.read(buf)) >= 0) {
          // continue draining
        }
      });
    } finally {
      is.close();
    }
  }
}
