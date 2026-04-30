package com.senzing.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PoolConnectionProvider}.
 *
 * <p>Each test asserts the documented contract: both constructors
 * reject a {@code null} pool with {@link NullPointerException}; the single-arg
 * constructor configures the indefinite wait ({@code -1L}); {@link
 * PoolConnectionProvider#getConnection()} returns a usable {@link Connection}
 * from the backing pool when available; and it throws {@link SQLException} when
 * the pool cannot satisfy the request within the configured wait time.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class PoolConnectionProviderTest
{
  // -------------------------------------------------------------------
  // Constructor contract
  // -------------------------------------------------------------------

  /**
   * The single-arg {@code PoolConnectionProvider(ConnectionPool)} constructor
   * must configure the maximum wait time as {@code -1L} per its javadoc ("wait
   * indefinitely for a Connection").
   */
  @Test
  public void singleArgConstructorSetsIndefiniteWait() throws SQLException
  {
    ConnectionPool pool = newPool(1);
    try {
      PoolConnectionProvider provider = new PoolConnectionProvider(pool);
      assertEquals(-1L, provider.getMaximumWaitTime(),
                   "Single-arg constructor must default to indefinite "
                       + "wait (-1L)");
    } finally {
      pool.shutdown();
    }
  }

  /**
   * The two-arg constructor must store the configured maximum wait time so
   * {@code getMaximumWaitTime()} returns the same value.
   */
  @Test
  public void twoArgConstructorPropagatesMaxWait() throws SQLException
  {
    ConnectionPool pool = newPool(1);
    try {
      assertEquals(0L,
                   new PoolConnectionProvider(pool, 0L)
                       .getMaximumWaitTime());
      assertEquals(5000L,
                   new PoolConnectionProvider(pool, 5000L)
                       .getMaximumWaitTime());
      assertEquals(-1L,
                   new PoolConnectionProvider(pool, -1L)
                       .getMaximumWaitTime());
    } finally {
      pool.shutdown();
    }
  }

  /**
   * The single-arg constructor must throw {@link NullPointerException} when
   * given a null pool, per its
   * {@code @throws NullPointerException} clause.
   */
  @Test
  public void singleArgConstructorThrowsNpeForNullPool()
  {
    assertThrows(NullPointerException.class,
                 () -> new PoolConnectionProvider(null));
  }

  /**
   * The two-arg constructor must throw {@link NullPointerException} when given
   * a null pool, per its javadoc.
   */
  @Test
  public void twoArgConstructorThrowsNpeForNullPool()
  {
    assertThrows(NullPointerException.class,
                 () -> new PoolConnectionProvider(null, 1000L));
  }

  // -------------------------------------------------------------------
  // getConnection() contract
  // -------------------------------------------------------------------

  /**
   * {@code getConnection()} must return a usable {@link Connection}
   * acquired from the backing pool when one is available.
   */
  @Test
  public void getConnectionReturnsUsableConnectionFromPool()
      throws SQLException
  {
    ConnectionPool pool = newPool(1);
    try {
      PoolConnectionProvider provider = new PoolConnectionProvider(pool);
      try (Connection conn = provider.getConnection();
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT 1"))
      {
        assertNotNull(conn);
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
      }
    } finally {
      pool.shutdown();
    }
  }

  /**
   * {@code getConnection()} must throw {@link SQLException} when the
   * pool cannot provide a connection within the configured wait time. Driving
   * this with a single-connection pool whose only connection is currently
   * leased and a no-wait provider.
   */
  @Test
  public void getConnectionThrowsSqlExceptionWhenPoolExhausted()
      throws SQLException
  {
    ConnectionPool pool = newPool(1);
    Connection leased = null;
    try {
      // exhaust the pool
      leased = pool.acquire();
      assertNotNull(leased,
                    "Pool must have provided its single connection");

      // a no-wait provider must fail because the only connection is
      // already leased
      PoolConnectionProvider provider
          = new PoolConnectionProvider(pool, 0L);
      SQLException sqe = assertThrows(SQLException.class,
                                      provider::getConnection);
      assertTrue(sqe.getMessage().contains("could not be obtained"),
                 "SQLException message must mention the failure: "
                     + sqe.getMessage());
    } finally {
      if (leased != null) {
        leased.close();
      }
      pool.shutdown();
    }
  }

  // -------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------

  /**
   * Builds a {@link ConnectionPool} of the specified size backed by an
   * in-memory SQLite database. Each call returns a fresh pool; the caller must
   * invoke {@link ConnectionPool#shutdown()} when done.
   */
  private static ConnectionPool newPool(int size) throws SQLException
  {
    SQLiteConnector connector = new SQLiteConnector();
    return new ConnectionPool(connector, size);
  }
}
