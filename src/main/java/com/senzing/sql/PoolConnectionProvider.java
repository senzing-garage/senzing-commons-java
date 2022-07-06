package com.senzing.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * A {@link ConnectionProvider} implementation that is backed by a {@link
 * ConnectionPool} with an optional maximum wait time.
 */
public class PoolConnectionProvider {
  /**
   * The {@link ConnectionPool} to back this instance.
   */
  private ConnectionPool pool;

  /**
   * The configured maximum wait time, or zero if no-wait and negative if
   * waiting indefinitely.
   */
  private long maxWait = 0L;

  /**
   * Constructs with the specified {@link ConnectionPool} to back this instance.
   * The constructed instance will wait indefinitely for a {@link Connection}
   * to become available.
   *
   * @param pool The {@link ConnectionPool} to use.
   *
   * @throws NullPointerException If the specified {@link ConnectionPool} is
   *                              <code>null</code>.
   */
  public PoolConnectionProvider(ConnectionPool pool)
      throws NullPointerException
  {
    this(pool, -1L);
  }

  /**
   * Constructs with the specified {@link ConnectionPool} and the maximum wait
   * time for waiting for a {@link Connection} from the pool.  To specify
   * that no wait should be done, specify zero (0) as the wait time.  To
   * specify that the provider should wait indefinitely then specify a negative
   * number.
   *
   * @param pool The {@link ConnectionPool} to use.
   * @param maxWait The maximum number of milliseconds to wait for a
   *                {@link Connection}, or zero (0) if no wait should be done,
   *                or a negative number to indicate an indefinite wait time.
   * @throws NullPointerException If the specified {@link ConnectionPool} is
   *                              <code>null</code>.
   */
  public PoolConnectionProvider(ConnectionPool pool, long maxWait) {
    Objects.requireNonNull(pool, "The specified pool cannot be null");
    this.pool     = pool;
    this.maxWait  = maxWait;
  }

  /**
   * Implemented to get the {@link Connection} within the optionally configured
   * maximum wait time.  If a {@link Connection} cannot be obtained then an
   * exception is thrown.
   *
   * @return The {@link Connection} that was obtained.
   * @throws SQLException If a failure occurs.
   */
  public Connection getConnection() throws SQLException {
    Connection conn =this.pool.acquire(this.getMaximumWaitTime());
    if (conn == null) {
      throw new SQLException(
          "The connection could not be obtained within the allotted time.  "
          + "maximumWait=[ " + this.getMaximumWaitTime() + " ]");
    }
    return conn;
  }

  /**
   * Gets the maximum number of milliseconds to wait for a {@link Connection}
   * before throwing an exception.
   *
   * @return The maximum number of milliseconds to wait for a {@link Connection}
   *         before throwing an exception.
   */
  public long getMaximumWaitTime() {
    return this.maxWait;
  }
}
