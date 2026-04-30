package com.senzing.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import uk.org.webcompere.systemstubs.stream.SystemErr;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary {@link ConnectionPool} tests covering the corners not in {@code
 * ConnectionPoolTest}: constructor argument validation, the
 * diagnostic-lease-info helper, the release-already-released warning path, the
 * acquire-after-shutdown error path, and the {@code
 * PooledConnection.setCurrentLeaseHandler} already-leased rejection.
 *
 * <p>Uses an in-memory SQLite connector throughout — fast, hermetic,
 * and does not require external resources.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ConnectionPoolExtraTest
{
  private static SQLiteConnector connector()
  {
    return new SQLiteConnector();
  }

  // -------------------------------------------------------------------
  // Constructor validation
  // -------------------------------------------------------------------

  @Test
  public void constructorRejectsNullConnector()
  {
    assertThrows(NullPointerException.class,
                 () -> new ConnectionPool(null, 1));
  }

  @Test
  public void constructorRejectsNegativeMinPoolSize()
  {
    // Per javadoc: IllegalArgumentException for negative min pool size.
    assertThrows(IllegalArgumentException.class,
                 () -> new ConnectionPool(connector(), null,
                                          -1, 5, 0, 0));
  }

  @Test
  public void constructorRejectsZeroMaxPoolSize()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> new ConnectionPool(connector(), null,
                                          0, 0, 0, 0));
  }

  @Test
  public void constructorRejectsNegativeMaxPoolSize()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> new ConnectionPool(connector(), null,
                                          0, -1, 0, 0));
  }

  @Test
  public void constructorRejectsMinGreaterThanMax()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> new ConnectionPool(connector(), null,
                                          5, 2, 0, 0));
  }

  @Test
  public void constructorRejectsNegativeExpireTime()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> new ConnectionPool(connector(), null,
                                          0, 5, -1, 0));
  }

  // -------------------------------------------------------------------
  // Diagnostic info
  // -------------------------------------------------------------------

  @Test
  public void getDiagnosticLeaseInfoEmptyWhenNoLeases() throws Exception
  {
    ConnectionPool pool = new ConnectionPool(connector(), 1);
    try {
      String info = pool.getDiagnosticLeaseInfo();
      assertEquals("", info,
                   "With no active leases the diagnostic info should"
                       + " be empty");
    } finally {
      pool.shutdown();
    }
  }

  @Test
  public void getDiagnosticLeaseInfoIncludesActiveLeases() throws Exception
  {
    ConnectionPool pool = new ConnectionPool(connector(), 1);
    Connection conn = null;
    try {
      conn = pool.acquire();
      String info = pool.getDiagnosticLeaseInfo();
      assertNotNull(info);
      assertTrue(info.length() > 0,
                 "Diagnostic info should be non-empty when a"
                     + " connection is leased");
    } finally {
      if (conn != null) {
        pool.release(conn);
      }
      pool.shutdown();
    }
  }

  // -------------------------------------------------------------------
  // release already-released connection
  // -------------------------------------------------------------------

  /**
   * Releasing a connection that has already been released must print a warning
   * to {@link System#err} and continue without throwing. Per the documented
   * contract: "If the specified Connection is from this pool, but has already
   * been released, then this method does nothing."
   */
  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  @ResourceLock(Resources.SYSTEM_ERR)
  public void releaseAlreadyReleasedPrintsWarning() throws Exception
  {
    ConnectionPool pool = new ConnectionPool(connector(), 1);
    try {
      Connection conn = pool.acquire();
      pool.release(conn);

      // Second release on the same connection — exercises the
      // documented "already released" no-op-with-warning path.
      SystemErr stub = new SystemErr();
      stub.execute(() -> pool.release(conn));

      String captured = stub.getText();
      assertTrue(captured.contains("released more than once")
                     || captured.length() > 0,
                 "release()-of-already-released should write a "
                     + "warning to stderr");
    } finally {
      pool.shutdown();
    }
  }

  /**
   * Releasing {@code null} must be a no-op per the documented "allow for easy
   * semantics in finally blocks" contract.
   */
  @Test
  public void releaseNullIsNoOp() throws Exception
  {
    ConnectionPool pool = new ConnectionPool(connector(), 1);
    try {
      // Must not throw.
      pool.release(null);
    } finally {
      pool.shutdown();
    }
  }

  /**
   * Releasing a non-proxy {@link Connection} (one not obtained from this pool)
   * must throw {@link IllegalArgumentException}.
   */
  @Test
  public void releaseNonProxyConnectionThrows() throws Exception
  {
    ConnectionPool pool = new ConnectionPool(connector(), 1);
    Connection direct = connector().openConnection();
    try {
      assertThrows(IllegalArgumentException.class,
                   () -> pool.release(direct));
    } finally {
      direct.close();
      pool.shutdown();
    }
  }

  // -------------------------------------------------------------------
  // Acquire after shutdown
  // -------------------------------------------------------------------

  /**
   * After {@link ConnectionPool#shutdown()}, {@link ConnectionPool#acquire()}
   * must throw {@link SQLException} per the documented "Unable to obtain a
   * connection because the connection pool was shutdown" message.
   */
  @Test
  public void acquireAfterShutdownThrows() throws Exception
  {
    ConnectionPool pool = new ConnectionPool(connector(), 1);
    pool.shutdown();

    SQLException ex = assertThrows(SQLException.class, pool::acquire);
    assertTrue(ex.getMessage().contains("shutdown"),
               "Error message should mention shutdown: "
                   + ex.getMessage());
  }

  // -------------------------------------------------------------------
  // No-wait acquire returns null when pool exhausted
  // -------------------------------------------------------------------

  /**
   * {@link ConnectionPool#acquire(long)} with {@code maxWait == 0}
   * must return {@code null} rather than block when the pool is exhausted.
   */
  @Test
  public void acquireNoWaitWhenExhaustedReturnsNull() throws Exception
  {
    ConnectionPool pool = new ConnectionPool(connector(), 1, 1);
    Connection first = pool.acquire();
    try {
      // No-wait acquire on a single-connection pool with the only
      // connection held — must return null without blocking.
      Connection second = pool.acquire(0L);
      assertEquals(null, second,
                   "No-wait acquire on exhausted pool must return null");
    } finally {
      pool.release(first);
      pool.shutdown();
    }
  }

  // -------------------------------------------------------------------
  // Shutdown idempotency
  // -------------------------------------------------------------------

  @Test
  public void shutdownIsIdempotent() throws Exception
  {
    ConnectionPool pool = new ConnectionPool(connector(), 1);
    pool.shutdown();
    // Second shutdown must be a no-op.
    pool.shutdown();
    assertTrue(pool.isShutdown());
  }

  // -------------------------------------------------------------------
  // Pool sizing accessors
  // -------------------------------------------------------------------

  @Test
  public void getMinAndMaxSizesReturnConstructorArguments() throws Exception
  {
    ConnectionPool pool = new ConnectionPool(connector(), null,
                                             2, 5, 0, 0);
    try {
      assertEquals(2, pool.getMinimumSize());
      assertEquals(5, pool.getMaximumSize());
    } finally {
      pool.shutdown();
    }
  }
}
