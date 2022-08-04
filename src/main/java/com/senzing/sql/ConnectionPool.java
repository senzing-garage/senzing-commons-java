package com.senzing.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static com.senzing.sql.ConnectionPool.Statistic.*;

/**
 * Provides a pool for JDBC {@link Connection} instances.  The pool can
 * be a fixed size or may have a maximum and minimum size that it may
 * grow and shrink between.  Additionally, each {@link Connection} can
 * have a maximum lifespan and a maximum number of uses before being
 * replaced with a new {@link Connection}.
 */
public class ConnectionPool {
  /**
   * Constant for converting between nanoseconds and milliseconds.
   */
  private static final long ONE_MILLION = 1000000L;

  /**
   * The amount of time to wait before forcing to wakeup and checking the
   * pool state.
   */
  private static final long WAIT_TIMEOUT = 2000L;

  /**
   * Encapsualtes and describes a pooled database connection.
   */
  protected static class PooledConnection {
    /**
     * The associated {@link Connection}.
     */
    private Connection connection = null;

    /**
     * The time that the {@link Connection} was opened.
     */
    private long createdTimeNanos;

    /**
     * The number of times the connection has been leased.
     */
    private int leaseCount = 0;

    /**
     * The {@link PooledConnectionHandler} for the current lease.  This
     * is <code>null</code> if not currently leased.
     */
    private PooledConnectionHandler currentLeaseHandler = null;

    /**
     * Constructs with the specified {@link Connection}.
     *
     * @param connection The {@link Connection} with which to construct.
     */
    public PooledConnection(Connection connection) {
      this.connection = connection;
      this.createdTimeNanos = System.nanoTime();
      this.leaseCount = 0;
      this.currentLeaseHandler = null;
    }

    /**
     * Gets the associated {@link Connection}.
     *
     * @return The associated {@link Connection}.
     */
    public Connection getConnection() {
      return this.connection;
    }

    /**
     * Gets the nanosecond creation timestamp.
     *
     * @return The nanosecond creation timestamp.
     */
    public long getCreatedTimeNanos() {
      return this.createdTimeNanos;
    }

    /**
     * Gets the lifespan (in milliseconds) of the associated {@link Connection}
     * thus far.
     *
     * @return The lifespan (in milliseconds) of the associated
     * {@link Connection} thus far.
     */
    public long getLifespan() {
      return (System.nanoTime() - this.createdTimeNanos) / ONE_MILLION;
    }

    /**
     * Gets the {@link PooledConnectionHandler} for the current lease.  This
     * returns <code>null</code> if not currently leased.
     *
     * @return The {@link PooledConnectionHandler} for the current lease, or
     * <code>null</code> if not currently leased.
     */
    public PooledConnectionHandler getCurrentLeaseHandler() {
      return this.currentLeaseHandler;
    }

    /**
     * Sets the {@link PooledConnectionHandler} for the current lease.
     *
     * @param handler The {@link PooledConnectionHandler} for the current lease,
     *                or <code>null</code> if not leased.
     */
    public void setCurrentLeaseHandler(PooledConnectionHandler handler) {
      if (handler != null && this.currentLeaseHandler != null) {
        throw new IllegalStateException(
            "Setting a connection handler on a pooled connection that already "
                + "has one set (i.e.: is already or still leased)");
      }

      // set the handler
      this.currentLeaseHandler = handler;

      // check if leasing
      if (handler != null) {
        this.leaseCount++;
      }
    }

    /**
     * Returns the number of times the {@link Connection} has been leased.
     *
     * @return The number of times the {@link Connection} has been leased.
     */
    public int getLeaseCount() {
      return this.leaseCount;
    }
  }

  /**
   * A background thread that will handle expiring available connections when
   * the {@link ConnectionPool} is idle.  This {@link Thread} does not do
   * anything when the {@link ConnectionPool} is actively providing new leases
   * on connections.
   */
  private class ConnectionExpireThread extends Thread {
    /**
     * Default constructor.
     */
    private ConnectionExpireThread() {
      // do nothing
    }

    /**
     * Provides a loop to handle cleaning up expired connections when the
     * {@link ConnectionPool} becomes idle.
     */
    public void run() {
      // get the pool
      ConnectionPool pool = ConnectionPool.this;

      // determine the wait time
      long waitTime = pool.getExpireTime() / 2L;

      // loop until the pool is shutdown
      while (!pool.isShutdown()) {

        // wait for the wait time
        synchronized (this) {
          try {
            this.wait(waitTime);
          } catch (InterruptedException ignore) {
            // do nothing
          }
        }

        // synchronize
        synchronized (pool) {
          // check if shutdown
          if (pool.isShutdown()) continue;

          // check to see if pool is idle, if not let the pool handle it
          if (pool.getIdleTime() < waitTime) continue;

          // expire any old connections
          try {
            pool.expireConnections();
          } catch (SQLException e) {
            System.err.println(
                "*** WARNING: Exception while expiring connections");
            e.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * The {@link Connector} used for establishing new connections.
   */
  private Connector connector = null;

  /**
   * The {@link TransactionIsolation} level to set on the {@link Connection}
   * before returning it, or <code>null</code> if none is to be set.
   */
  private TransactionIsolation isolationLevel = null;

  /**
   * The {@link List} of all {@link PooledConnection} instances managed by
   * the pool.
   */
  private IdentityHashMap<PooledConnection, Long> allConnections = null;

  /**
   * The {@link List} of {@link PooledConnection} instances that are available
   * for lease from the pool.
   */
  private List<PooledConnection> availableConnections = null;

  /**
   * The {@link IdentityHashMap} of currently leased {@link Connection}
   * instances to the {@link PooledConnection} containing additional
   * data associated with the connection.
   */
  private IdentityHashMap<Connection, PooledConnection> leasedMap;

  /**
   * The minimum number of connections in the pool.
   */
  private int minPoolSize = 0;

  /**
   * The maximum number of connections in the pool.
   */
  private int maxPoolSize = 1;

  /**
   * The maximum number of milliseconds a {@link Connection} will be
   * used before it is closed and replaced with a new {@link Connection}.
   */
  private long expireTime = 0L;

  /**
   * The maximum number of times a {@link Connection} can be leased
   * before it is replaced.
   */
  private int retireLimit = 0;

  /**
   * The flag indicating if this pool has been shutdown.
   */
  private boolean shutdown = false;

  /**
   * The greatest size the connection pool has grown to.
   */
  private int greatestPoolSize = 0;

  /**
   * The total number of leased connections counted on each acquisition and
   * release for measuring an average number of used connections from the pool.
   */
  private long cumulativeLeaseCount = 0L;

  /**
   * The number of times that the leased count is checked.
   */
  private long cumulativeLeaseChecks = 0L;

  /**
   * The greatest number of connections that have been acquired at any time.
   */
  private int greatestLeasedCount = -1;

  /**
   * The greatest amount of time in milliseconds that a connection has been
   * leased.
   */
  private long greatestLeaseTime = -1L;

  /**
   * The total amount of time in milliseconds for all leases that have
   * concluded.
   */
  private long totalLeaseTime = 0L;

  /**
   * The number of {@link Connection} instances that have been expired due to
   * the time they have been open exceeding a limit.
   */
  private int expiredCount = 0;

  /**
   * The number of {@link Connection} instances that have been retired due
   * to the number of times they have been leased exceeding a limit.
   */
  private int retiredCount = 0;

  /**
   * The total number of connections that have been leased including those
   * that are currently leased.
   */
  private long totalLeaseCount = 0L;

  /**
   * The total number of connection leases that have completed.
   */
  private long completedLeaseCount = 0;

  /**
   * The total number of milliseconds spent acquiring connections.
   */
  private long totalAcquisitionTime = 0L;

  /**
   * The greatest number of milliseconds spent acquiring a connection.
   */
  private long greatestAcquisitionTime = -1L;

  /**
   * The nanosecond timestamp when the last lease was obtained.
   */
  private long idleStartTimeNanos = -1L;

  /**
   * The background expiration thread if one is needed.
   */
  private ConnectionExpireThread expireThread = null;

  /**
   * The object to synchronize on when computing statistics.
   */
  private final Object statsMonitor = new Object();

  /**
   * The constant for statistic units for connection units.
   */
  private static final String CONNECTION_UNITS = "connections";

  /**
   * The constant for statistic units for millisecond units.
   */
  private static final String MILLISECOND_UNITS = "ms";

  /**
   * The constant for statistic units that are measured in leases.
   */
  private static final String LEASE_UNITS = "leases";

  /**
   * Enumerates the statistics associated with a {@link ConnectionPool}.
   */
  public enum Statistic {
    /**
     * The minimum number of {@link Connection} instances maintained
     * in the {@link ConnectionPool}.
     */
    minimumSize(CONNECTION_UNITS),

    /**
     * The maximum number of {@link Connection} instances to which the
     * {@link ConnectionPool} may grow.
     */
    maximumSize(CONNECTION_UNITS),

    /**
     * The current total number of {@link Connection} instances in the
     * {@link ConnectionPool}.
     */
    currentPoolSize(CONNECTION_UNITS),

    /**
     * The current number of available {@link Connection} instances in the
     * {@link ConnectionPool}.
     */
    availableConnections(CONNECTION_UNITS),

    /**
     * The current number of outstanding {@link Connection} leases that the
     * {@link ConnectionPool} is waiting on.
     */
    outstandingLeases(LEASE_UNITS),

    /**
     * The longest outstanding lease time in milliseconds.  This statistic is
     * absent if no currently outstanding leases.
     */
    greatestOutstandingLeaseTime(MILLISECOND_UNITS),

    /**
     * The average outstanding lease time in milliseconds.  This statistic is
     * absernt if no currently outstanding leases.
     */
    averageOutstandingLeaseTime(MILLISECOND_UNITS),

    /**
     * The maximum number of a milliseconds that a {@link Connection}
     * will be used by the pool before it is closed and replaced.  If
     * no maximum exists then the value for this statistic is
     * <code>null</code>.
     */
    expireTime(MILLISECOND_UNITS),

    /**
     * The maximum number of times that a {@link Connection} will be
     * leased by the pool before it is closed and replaced.  If no
     * maximum exists then the value for this statistic is
     * <code>null</code>.
     */
    retireLimit(LEASE_UNITS),

    /**
     * The greatest number of {@link Connection} instances that have
     * been simultaneously leased.  This is the greatest utilization
     * of the {@link ConnectionPool}.
     */
    greatestLeasedCount(CONNECTION_UNITS),

    /**
     * The average number of {@link Connection} instances that have been
     * leased simultaneously.  This is measured each time a {@link
     * Connection} is acquired or released and the average number of those
     * samples is returned.
     */
    averageLeasedCount(CONNECTION_UNITS),

    /**
     * The greatest number of {@link Connection} instances to which the
     * {@link ConnectionPool} has grown.
     */
    greatestPoolSize(CONNECTION_UNITS),

    /**
     * The total number of {@link Connection} instances that have been expired
     * due to exceeding the maximum allowed lifespan.
     */
    expiredConnections(CONNECTION_UNITS),

    /**
     * The total number of {@link Connection} instances that have been retired
     * due to be leased the maximum number of times.
     */
    retiredConnections(CONNECTION_UNITS),

    /**
     * The average number of milliseconds taken to acquire a {@link
     * Connection} from the {@link ConnectionPool}.
     */
    averageAcquireTime(MILLISECOND_UNITS),

    /**
     * The greatest number of milliseconds taken to acquire a {@link
     * Connection} from the {@link ConnectionPool}.
     */
    greatestAcquireTime(MILLISECOND_UNITS),

    /**
     * The greatest number of milliseconds that a {@link Connection}
     * has been leased from the {@link ConnectionPool}.  This includes
     * currently leased connections.
     */
    greatestLeaseTime(MILLISECOND_UNITS),

    /**
     * The average number of milliseconds for all {@link Connection}
     * leases including those currently leased.
     */
    averageLeaseTime(MILLISECOND_UNITS),

    /**
     * The total number of {@link Connection} leases that have been
     * granted by this pool.
     */
    lifetimeLeaseCount(LEASE_UNITS),

    /**
     * The number of milliseconds that have passed since the last time a
     * {@link Connection} lease was requested.  If a {@link Connection} lease
     * has never been requested then this returns the number of milliseconds
     * since the construction of the {@link ConnectionPool}.
     */
    idleTime(MILLISECOND_UNITS);

    /**
     * Constructs with the specified units.
     *
     * @param units The units for the statistics.
     */
    Statistic(String units) {
      this.units = units;
    }

    /**
     * Gets the description of the units for the statistic.
     *
     * @return The description of the units for the statistic.
     */
    public String getUnits() {
      return this.units;
    }

    /**
     * The units for the statistic.
     */
    private String units = null;
  }

  /**
   * Constructs with the {@link Connector} and the size of the connection pool.
   *
   * @param connector The {@link Connector} for establishing new database
   *                  connections.
   * @param poolSize  The number of connections to initialize the connection
   *                  pool with.
   * @throws SQLException             If a failure occurs in establishing a connection.
   * @throws IllegalArgumentException If the specified pool size is less-than
   *                                  or equal-to zero (0).
   * @throws NullPointerException     If the specified {@link Connector} is
   *                                  <code>null</code>.
   */
  public ConnectionPool(Connector connector, int poolSize)
      throws SQLException, IllegalArgumentException, NullPointerException {
    this(connector, null, poolSize);
  }

  /**
   * Constructs with the {@link Connector} and the size of the connection pool.
   *
   * @param connector      The {@link Connector} for establishing new database
   *                       connections.
   * @param isolationLevel The {@link TransactionIsolation} level to ensure is
   *                       set on each {@link Connection} before providing it,
   *                       or <code>null</code> if the isolation level need not
   *                       be verified and set.
   * @param poolSize       The number of connections to initialize the connection
   *                       pool with.
   * @throws SQLException             If a failure occurs in establishing a connection.
   * @throws IllegalArgumentException If the specified pool size is less-than
   *                                  or equal-to zero (0).
   * @throws NullPointerException     If the specified {@link Connector} is
   *                                  <code>null</code>.
   */
  public ConnectionPool(Connector connector,
                        TransactionIsolation isolationLevel,
                        int poolSize)
      throws SQLException, IllegalArgumentException, NullPointerException {
    this(connector, isolationLevel, poolSize, poolSize);
  }

  /**
   * Constructs with the specified {@link Connector} for establishing new
   * JDBC connections, a minimum connection pool size and maximum connection
   * pool size.  The pool will start at the minimum size and grow to the
   * maximum size if demand for connections requires it.
   *
   * @param connector   The {@link Connector} for establishing new database
   *                    connections.
   * @param minPoolSize The minimum number of connections to initialize the
   *                    connection pool with.
   * @param maxPoolSize The maximum number of connections that the pool can
   *                    grow to have.
   * @throws SQLException             If a failure occurs in establishing a connection.
   * @throws IllegalArgumentException If the specified maximum pool size is
   *                                  less-than or equal-to zero (0), the
   *                                  minimum pool size is less-than zero (0),
   *                                  or the minimum pool size is greater-than
   *                                  the maximum pool size.
   * @throws NullPointerException     If the specified {@link Connector} is
   *                                  <code>null</code>.
   */
  public ConnectionPool(Connector connector,
                        int minPoolSize,
                        int maxPoolSize)
      throws SQLException, IllegalArgumentException, NullPointerException {
    this(connector, null, minPoolSize, maxPoolSize);
  }

  /**
   * Constructs with the specified {@link Connector} for establishing new
   * JDBC connections, a minimum connection pool size and maximum connection
   * pool size.  The pool will start at the minimum size and grow to the
   * maximum size if demand for connections requires it.
   *
   * @param connector      The {@link Connector} for establishing new database
   *                       connections.
   * @param isolationLevel The {@link TransactionIsolation} level to ensure is
   *                       set on each {@link Connection} before providing it,
   *                       or <code>null</code> if the isolation level need not
   *                       be verified and set.
   * @param minPoolSize    The minimum number of connections to initialize the
   *                       connection pool with.
   * @param maxPoolSize    The maximum number of connections that the pool can
   *                       grow to have.
   * @throws SQLException             If a failure occurs in establishing a connection.
   * @throws IllegalArgumentException If the specified maximum pool size is
   *                                  less-than or equal-to zero (0), the
   *                                  minimum pool size is less-than zero (0),
   *                                  or the minimum pool size is greater-than
   *                                  the maximum pool size.
   * @throws NullPointerException     If the specified {@link Connector} is
   *                                  <code>null</code>.
   */
  public ConnectionPool(Connector             connector,
                        TransactionIsolation  isolationLevel,
                        int                   minPoolSize,
                        int                   maxPoolSize)
      throws SQLException, IllegalArgumentException, NullPointerException {
    this(connector, isolationLevel, minPoolSize, maxPoolSize, 0, 0);
  }

  /**
   * Constructs with the specified {@link Connector} for establishing new
   * JDBC connections, a minimum connection pool size and maximum connection
   * pool size.  The pool will start at the minimum size and grow to the
   * maximum size if demand for connections requires it.  Additionally, optional
   * limits on the lifespan of a {@link Connection} (an expire time) and on the
   * number of times a {@link Connection} will be leased (a retire count) can
   * be specified as non-zero values to impose limits.
   *
   * @param connector The {@link Connector} for establishing new database
   *                  connections.
   * @param minPoolSize The minimum number of connections to initialize the
   *                    connection pool with.
   * @param maxPoolSize The maximum number of connections that the pool can
   *                    grow to have.
   * @param expireTime The maximum number of <b>seconds</b> that a
   *                   {@link Connection} will be used before it is closed and
   *                   replaced, or zero (0) then {@link Connection} instances
   *                   will not be expired based on time.
   * @param retireLimit The maximum number of times a {@link Connection}
   *                    will be leased before it is closed and replaced, or
   *                    zero (0) if {@link Connection} instances should not be
   *                    retired after exceeding a number of leases.
   * @throws SQLException If a failure occurs in establishing a connection.
   * @throws IllegalArgumentException If the specified maximum pool size is
   *                                  less-than or equal-to zero (0), the
   *                                  minimum pool size is less-than zero (0),
   *                                  or the minimum pool size is greater-than
   *                                  the maximum pool size or if the maximum
   *                                  lifespan or number of leases are less-than
   *                                  or equal-to zero (0).
   * @throws NullPointerException If the specified {@link Connector} is
   *                              <code>null</code>.
   */
  public ConnectionPool(Connector connector,
                        int       minPoolSize,
                        int       maxPoolSize,
                        int       expireTime,
                        int       retireLimit)
      throws SQLException, IllegalArgumentException, NullPointerException
  {
    this(connector, null, minPoolSize, maxPoolSize, expireTime, retireLimit);
  }

  /**
   * Constructs with the specified {@link Connector} for establishing new
   * JDBC connections, a minimum connection pool size and maximum connection
   * pool size.  The pool will start at the minimum size and grow to the
   * maximum size if demand for connections requires it.  Additionally, optional
   * limits on the lifespan of a {@link Connection} (an expire time) and on the
   * number of times a {@link Connection} will be leased (a retire count) can
   * be specified as non-zero values to impose limits.
   *
   * @param connector The {@link Connector} for establishing new database
   *                  connections.
   * @param isolationLevel The {@link TransactionIsolation} level to ensure is
   *                       set on each {@link Connection} before providing it,
   *                       or <code>null</code> if the isolation level need not
   *                       be verified and set.
   * @param minPoolSize The minimum number of connections to initialize the
   *                    connection pool with.
   * @param maxPoolSize The maximum number of connections that the pool can
   *                    grow to have.
   * @param expireTime The maximum number of <b>seconds</b> that a
   *                   {@link Connection} will be used before it is closed and
   *                   replaced, or zero (0) then {@link Connection} instances
   *                   will not be expired based on time.
   * @param retireLimit The maximum number of times a {@link Connection}
   *                    will be leased before it is closed and replaced, or
   *                    zero (0) if {@link Connection} instances should not be
   *                    retired after exceeding a number of leases.
   * @throws SQLException If a failure occurs in establishing a connection.
   * @throws IllegalArgumentException If the specified maximum pool size is
   *                                  less-than or equal-to zero (0), the
   *                                  minimum pool size is less-than zero (0),
   *                                  or the minimum pool size is greater-than
   *                                  the maximum pool size or if the maximum
   *                                  lifespan or number of leases are less-than
   *                                  or equal-to zero (0).
   * @throws NullPointerException If the specified {@link Connector} is
   *                              <code>null</code>.
   */
  public ConnectionPool(Connector             connector,
                        TransactionIsolation  isolationLevel,
                        int                   minPoolSize,
                        int                   maxPoolSize,
                        int                   expireTime,
                        int                   retireLimit)
      throws SQLException, IllegalArgumentException, NullPointerException
  {
    // check if the connector is null
    Objects.requireNonNull(connector,
                           "The specified connector cannot be null");

    // check the min pool size is non-negative
    if (minPoolSize < 0) {
      throw new IllegalArgumentException(
          "The minimum pool size cannot be negative: " + minPoolSize);
    }
    if (maxPoolSize <= 0) {
      throw new IllegalArgumentException(
          "The maximum pool size must be a positive number: " + maxPoolSize);
    }

    // check the maximum and minimum pool sizes
    if (minPoolSize > maxPoolSize) {
      throw new IllegalArgumentException(
          "Minimum pool size (" + minPoolSize
              + ") cannot exceeed maximum poll size (" + maxPoolSize + ").");
    }

    // check the max connection lifespan
    if (expireTime < 0) {
      throw new IllegalArgumentException(
          "The maximum connection lifespan (expire time) cannot be negative: "
              + expireTime);
    }

    // check the max connection leases
    if (expireTime < 0) {
      throw new IllegalArgumentException(
          "The maximum connection leases (retire count) cannot be negative: "
          + retireLimit);
    }

    // set the fields
    this.connector      = connector;
    this.isolationLevel = isolationLevel;
    this.minPoolSize    = minPoolSize;
    this.maxPoolSize    = maxPoolSize;
    this.expireTime     = (expireTime * 1000);
    this.retireLimit    = retireLimit;

    // loop through and populate the initial connections
    this.allConnections = new IdentityHashMap<>();
    for (int index = 0; index < this.minPoolSize; index++) {
      PooledConnection pooledConnection = new PooledConnection(
          this.connector.openConnection());

      this.allConnections.put(pooledConnection,
                              pooledConnection.getCreatedTimeNanos());
    }

    // initialize the greatest pool size
    this.greatestPoolSize = this.allConnections.size();

    // create the list of available connections
    this.availableConnections = new LinkedList<>();
    for (PooledConnection conn : this.allConnections.keySet()) {
      this.availableConnections.add(conn);
    }

    // create the identity hash map for leases
    this.leasedMap = new IdentityHashMap<>();

    // check the expire time
    if (this.expireTime > 0L) {
      this.expireThread = new ConnectionExpireThread();
      this.expireThread.start();
    } else {
      this.expireThread = null;
    }

    // initialize the idle start time
    this.idleStartTimeNanos = System.nanoTime();
  }

  /**
   * Gets the {@link TransactionIsolation} level that is ensured on the
   * {@link Connection} instances when they are acquired.  This returns
   * <code>null</code> if no isolation level is being enforced.
   *
   * @return The {@link TransactionIsolation} level that is ensured on the
   *         {@link Connection} instances when they are acquired, or
   *         <code>null</code> if none is enforced.
   */
  public TransactionIsolation getIsolationLevel() {
    return this.isolationLevel;
  }

  /**
   * Gets the statistics for this instance as a {@link Map} of {@link Statistic}
   * keys to {@link Number} values.
   *
   * @return The statistics for this instance as a {@link Map} of {@link
   * Statistic} keys to {@link Number} values.
   */
  public Map<Statistic, Number> getStatistics() {
    Map<Statistic, Number> result = new LinkedHashMap<>();
    synchronized (this) {
      putStat(result, minimumSize, this.getMinimumSize());
      putStat(result, maximumSize, getMaximumSize());
      putStat(result, Statistic.expireTime, this.getExpireTime());
      putStat(result, Statistic.retireLimit, this.getRetireLimit());
      putStat(result, Statistic.greatestLeasedCount,
              this.getGreatestLeasedCount());
      putStat(result, averageLeasedCount, this.getAverageLeasedCount());
      putStat(result, Statistic.greatestPoolSize, this.getGreatestPoolSize());
      putStat(result, expiredConnections, this.getExpiredConnectionCount());
      putStat(result, retiredConnections, this.getRetiredConnectionCount());
      putStat(result, averageAcquireTime, this.getAverageAcquisitionTime());
      putStat(result, greatestAcquireTime, this.getGreatestAcquisitionTime());
      putStat(result, Statistic.greatestLeaseTime, this.getGreatestLeaseTime());
      putStat(result, averageLeaseTime, this.getAverageLeaseTime());
      putStat(result, lifetimeLeaseCount, this.getLifetimeLeaseCount());
      putStat(result, currentPoolSize, this.getCurrentPoolSize());
      putStat(result, Statistic.availableConnections,
              this.getAvailableConnectionCount());
      putStat(result, outstandingLeases, this.getOutstandingLeaseCount());
      putStat(result, greatestOutstandingLeaseTime,
              this.getGreatestOutstandingLeaseTime());
      putStat(result, averageOutstandingLeaseTime,
              this.getAverageOutstandingLeaseTime());
      putStat(result, idleTime, this.getIdleTime());
    }
    return result;
  }

  /**
   * Puts a {@link Statistic} key and {@link Number} value in the specified
   * {@link Map} if the specified value is not <code>null</code>.  It does
   * nothing if the value is <code>null</code>.
   *
   * @param map The {@link Map} to populate.
   * @param key The {@link Statistic} key for the map.
   * @param value The value to associate with the key.
   */
  private static void putStat(Map<Statistic, Number>  map,
                              Statistic               key,
                              Number                  value)
  {
    if (value == null) return;
    map.put(key, value);
  }

  /**
   * Computes the number of milliseconds since the specified starting
   * nanosecond system time.
   *
   * @param startNanos The starting nanosecond time.
   * @return The elapsed number of milliseconds.
   */
  private static long elapsed(long startNanos) {
    return (System.nanoTime() - startNanos) / ONE_MILLION;
  }

  /**
   * Acquires a {@link Connection} from the {@link ConnectionPool}, blocking
   * indefinitely if necessary.  If no {@link Connection} instances are
   * available and the pool has not reached its {@linkplain #getMaximumSize()
   * maximum size} then a new {@link Connection} is opened.
   *
   * @return The {@link Connection} acquired from the pool.
   *
   * @throws SQLException If a failure occurs.
   */
  public Connection acquire() throws SQLException {
    return this.acquire(-1L);
  }

  /**
   * Acquires a {@link Connection} from the {@link ConnectionPool}, blocking
   * for the specified maximum wait time if necessary.  If no {@link Connection}
   * instances are available and the pool has not reached its {@linkplain
   * #getMaximumSize() maximum size} then a new {@link Connection} is opened.
   * If the specified wait time is zero (0) then this method will not wait for
   * a {@link Connection} to become available, but will immediately return a
   * {@link Connection} if one is available or if the {@link ConnectionPool} can
   * grow/expand to make one available, otherwise it returns <code>null</code>.
   * Finally, a negative wait time can be specified to indicate no maximum
   * wait time and therefore indefinite waiting for a {@link Connection} to
   * become available.
   *
   * @param maxWait The maximum about af time (in milliseconds) to wait for a
   *                {@link Connection} to become available, or zero (0) if not
   *                waiting at all (only returning a {@link Connection} if one
   *                is immediately available, or a negative number if no
   *                maximum wait time and willing to wait indefinitely.
   *
   * @return The {@link Connection} acquired from the pool.
   *
   * @throws SQLException If a failure occurs.
   */
  public Connection acquire(long maxWait)
    throws SQLException, IllegalArgumentException
  {
    final long startTime = System.nanoTime();
    PooledConnection  acquired      = null;
    Integer           newPoolSize   = null;
    Integer           leasedCount   = null;
    synchronized (this) {
      // loop until we have an acquired connection
      while (acquired == null && !this.isShutdown()
             && (maxWait <= 0L || elapsed(startTime) < maxWait))
      {
        // wait for a connection to become available
        while (!this.isShutdown() && (this.availableConnections.size() == 0)
               && (this.allConnections.size() == this.getMaximumSize())
               && (maxWait < 0L || elapsed(startTime) < maxWait))
        {
          // unless no-wait was specified, then wait for a connection
          if (maxWait != 0L) {
            try {
              long timeout = (maxWait < 0L) ? WAIT_TIMEOUT
                  : Math.min(WAIT_TIMEOUT, maxWait - elapsed(startTime));
              if (timeout < 0L) break;
              this.wait(timeout);

            } catch (InterruptedException ignore) {
              // do nothing
            }
          }
        }

        // now check if shutdown
        if (this.isShutdown()) {
          throw new SQLException(
              "Unable to obtain a connection because the connection pool was "
                  + "shutdown");
        }

        // check if we have any connections that have exceeded their maximum
        // lifespan (we handle maximum leases on release)
        Long maxLifespan = this.getExpireTime();
        if (maxLifespan != null) {
          this.expireConnections();

          // record the pool size
          newPoolSize = this.allConnections.size();
        }

        // check if we should grow the pool
        if (this.availableConnections.size() == 0
            && this.allConnections.size() < this.getMaximumSize())
        {
          // create a new pooled connection
          acquired = new PooledConnection(this.connector.openConnection());

          this.allConnections.put(acquired, acquired.getCreatedTimeNanos());

          // record the new pool size
          newPoolSize = this.allConnections.size();

        } else if (this.availableConnections.size() > 0) {
          // acquire the first connection
          acquired = this.availableConnections.remove(0);
        }

        // check if no wait and skip looping
        if (maxWait == 0L) break;
      }

      // check if shutdown
      if (acquired == null && this.isShutdown()) {
        throw new SQLException(
            "Unable to obtain a connection because the connection pool was "
                + "shutdown");
      }

      // we must have an acquired connection
      if (acquired == null && maxWait < 0L) {
        // we must have an acquired connection
        throw new IllegalStateException(
            "Exited wait loop, but did not acquire a pooled connection.");
      }

      // check if we acquired a connection
      if (acquired != null) {
        // we must have an acquired connection
        // create a handler
        PooledConnectionHandler handler
            = new PooledConnectionHandler(this, acquired.getConnection());

        // set the handler
        acquired.setCurrentLeaseHandler(handler);

        // record the lease
        this.leasedMap.put(handler.getProxiedConnection(), acquired);
      }

      // set the acquired count
      leasedCount = this.leasedMap.size();

      // notify all
      this.notifyAll();

      // compute the statistics
      if (acquired != null) {
        // compute acquisition time stats
        long acquisitionTime = (System.nanoTime() - startTime) / ONE_MILLION;
        this.totalAcquisitionTime += acquisitionTime;
        if (acquisitionTime > this.greatestAcquisitionTime) {
          this.greatestAcquisitionTime = acquisitionTime;
        }
      }

      // check if the greatest pool size increased
      if (newPoolSize != null && newPoolSize > this.greatestPoolSize) {
        this.greatestPoolSize = newPoolSize;
      }

      // compute based on the number of concurrent leases
      if (leasedCount != null) {
        this.cumulativeLeaseCount += leasedCount;
        this.cumulativeLeaseChecks++;
        if (leasedCount > this.greatestLeasedCount) {
          this.greatestLeasedCount = leasedCount;
        }
      }

      // increment the lease count if we acquired a connection
      if (acquired != null) {
        this.totalLeaseCount++;
      }

      // set the timestamp for the last lease
      if (acquired != null) {
        this.idleStartTimeNanos = System.nanoTime();
      }
    }

    // if no connection acquired, return null
    if (acquired == null) {
      return null;
    }

    // get the connection
    Connection conn = acquired.getConnection();

    // ensure auto-commit is turned off
    if (conn.getAutoCommit()) conn.setAutoCommit(false);

    // check isolation level
    if (this.getIsolationLevel() != null) {
      this.getIsolationLevel().applyTo(conn);
    }

    // return the proxied connection
    return acquired.getCurrentLeaseHandler().getProxiedConnection();
  }

  /**
   * Releases the specified {@link Connection} back to the {@link
   * ConnectionPool}.  If the specified {@link Connection} did not come from
   * this {@link ConnectionPool} instance then an {@link
   * IllegalArgumentException} is thrown.  If the specified {@link Connection}
   * is from this pool, but has already been released, then this method does
   * nothing.
   *
   * @param connection The {@link Connection} to release, or <code>null</code>
   *                   if none was acquired and therefore none need be released.
   *
   * @throws SQLException If a JDBC failure occurs.
   */
  public void release(Connection connection) throws SQLException {
    // check if null -- allow for easy semantics in finally blocks
    // when acquisition times out and null is returned
    if (connection == null) return;

    // init the stats tracking variables
    Long    leasedTime  = null;
    Integer leasedCount = null;
    int     retired     = 0;

    synchronized (this) {
      // make sure we notify after relinquishing lock
      this.notifyAll();

      // check if this is one of our connections
      InvocationHandler handler = null;
      try {
        handler = Proxy.getInvocationHandler(connection);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "The specified Connection is not from this pool instance ("
                + "not a proxy).");
      }
      if (!(handler instanceof PooledConnectionHandler)) {
        throw new IllegalArgumentException(
            "The specified Connection is not from this pool instance ("
                + "wrong handler type): " + handler.getClass().getName());
      }
      PooledConnectionHandler pch = (PooledConnectionHandler) handler;
      if (pch.getPool() != this) {
        throw new IllegalArgumentException(
            "The specified Connection is not from this pool instance ("
                + "wrong pool instance).");
      }

      // get the associated pooled connection
      PooledConnection pooledConn = this.leasedMap.get(connection);

      // check if null (i.e.: already released)
      if (pooledConn == null) {
        Exception exception
            = new Exception("WARNING: Connection released more than once");
        System.err.println();
        System.err.println("-------------------------------------------------");
        exception.printStackTrace();
        return;
      }

      // if not null then do some cleanup and release
      this.leasedMap.remove(connection);

      // rollback anything not committed in the transaction
      Connection backingConn = pooledConn.getConnection();
      if (!backingConn.getAutoCommit()) {
        backingConn.rollback();
      }
      backingConn.setAutoCommit(false);

      // get the new leased count
      leasedCount = this.leasedMap.size();

      // ensure the handler is marked as closed
      pch.markClosed();

      // check the lease time
      leasedTime = pch.getLeaseTime();

      // clear the handler on the pooled connection
      pooledConn.setCurrentLeaseHandler(null);

      // conditionally make the connection available again
      if (this.getRetireLimit() != null
          && pooledConn.getLeaseCount() > this.getRetireLimit())
      {
        // remove from the list of all connections
        this.allConnections.remove(pooledConn);

        // close the connection
        SQLUtilities.close(pooledConn.getConnection());

        // increment the retired count
        retired++;

        // check if we have dropped below the minimum
        if (this.allConnections.size() < this.getMinimumSize()) {
          try {
            PooledConnection refill
                = new PooledConnection(this.connector.openConnection());

            this.allConnections.put(refill, refill.getCreatedTimeNanos());
            this.availableConnections.add(refill);

          } catch (SQLException ignore) {
            ignore.printStackTrace();
          }
        }
      } else {
        this.availableConnections.add(pooledConn);
      }

      // update the statistics
      if (leasedTime != null) {
        if (leasedTime > this.greatestLeaseTime) {
          this.greatestLeaseTime = leasedTime;
        }
        this.totalLeaseTime += leasedTime;
      }
      if (leasedCount != null) {
        this.cumulativeLeaseCount += leasedCount;
        this.cumulativeLeaseChecks++;
        if (leasedCount > this.greatestLeasedCount) {
          this.greatestLeasedCount = leasedCount;
        }
      }
      this.retiredCount += retired;
      this.completedLeaseCount++;
    }
  }

  /**
   * Checks if this {@link ConnectionPool} has been shutdown.
   *
   * @return <code>true</code> if this {@link ConnectionPool} has been
   *         shutdown, otherwise <code>false</code>.
   */
  public synchronized boolean isShutdown() {
    return this.shutdown;
  }

  /**
   * Sets the shutdown flag and notifies all that the shutdown process has
   * begun.
   *
   */
  public void shutdown() {
    // set the shutdown flag and notify all
    synchronized (this) {
      if (this.shutdown) return;
      this.shutdown = true;
      this.notifyAll();
    }

    if (this.expireThread != null) {
      synchronized (this.expireThread) {
        this.expireThread.notifyAll();
      }
    }

    // now wait for all connections to become available
    synchronized (this) {
      while (this.availableConnections.size() < this.allConnections.size()) {
        try {
          this.wait(WAIT_TIMEOUT);
        } catch (InterruptedException ignore) {
          // do nothing
        }
      }

      // once we get here, it is time to close all connections
      for (PooledConnection pooledConn : this.allConnections.keySet()) {
        SQLUtilities.close(pooledConn.getConnection());
      }

      // clear the lists
      this.availableConnections.clear();
      this.allConnections.clear();
      this.leasedMap.clear();
    }

    // join with the expire thread
    if (this.expireThread != null) {
      try {
        this.expireThread.join();
      } catch (InterruptedException ignore) {
        // do nothing
      }
    }
  }

  /**
   * Gets the minimum number of {@link Connection} instances to be maintained
   * by this {@link ConnectionPool}.
   *
   * @return The minimum number of {@link Connection} instances to be
   *         maintained by this {@link ConnectionPool}.
   */
  public int getMinimumSize() {
    return this.minPoolSize;
  }

  /**
   * Gets the maximum number of {@link Connection} instances to which this
   * {@link ConnectionPool} can grow.
   *
   * @return The maximum number of {@link Connection} instances to which this
   *         {@link ConnectionPool} can grow.
   */
  public int getMaximumSize() {
    return this.maxPoolSize;
  }

  /**
   * Gets the maximum number of milliseconds a {@link Connection} in this
   * pool will be used before it is closed and replaced.  This returns
   * <code>null</code> if no maximum lifespan is configured for this pool.
   *
   * @return The maximum number of milliseconds a {@link Connection} in this
   *         pool will be used before it is closed and replaced, or
   *         <code>null</code> if no maximum is configured.
   */
  public Long getExpireTime() {
    if (this.expireTime <= 0) {
      return null;
    } else {
      return this.expireTime;
    }
  }

  /**
   * Gets the maximum number of times a {@link Connection} will be leased from
   * this pool before it is closed and replaced.  This returns <code>null</code>
   * if no maximum number of leases is configured for this pool.
   *
   * @return The maximum number of times a {@link Connection} will be leased
   *         from this pool before it is closed and replaced, or
   *         <code>null</code> if no maximum number of leases is configured.
   */
  public Integer getRetireLimit() {
    if (this.retireLimit <= 0) {
      return null;
    } else {
      return this.retireLimit;
    }
  }

  /**
   * Gets the greatest number of {@link Connection} instances that have been
   * concurrently leased from the pool at any given time.  This returns
   * <code>null</code> if no connections have been leased.
   *
   * @return The greatest number of {@link Connection} instances that have been
   *         concurrently leased from the pool at any given time, or
   *         <code>null</code> if none have ever been leased.
   */
  public Integer getGreatestLeasedCount() {
    synchronized (this) {
      if (this.greatestLeasedCount < 0) {
        return null;
      } else {
        return this.greatestLeasedCount;
      }
    }
  }

  /**
   * Returns the average number of {@link Connection} instances that have been
   * acquired from the pool at each time when a {@link Connection} is acquired
   * or released. That is the count is taken upon each call to {@link
   * #acquire()} and {@link #release(Connection)} that acquires or releases a
   * {@link Connection} and the total of the samples is divided by the number
   * of times a sample was taken.  If no connections have been acquired then
   * this returns <code>null</code>.
   *
   * @return The average number of {@link Connection} instances that have been
   *         acquired from the pool at each time when a {@link Connection} is
   *         acquired or released, or <code>null</code> if none have been
   *         acquired.
   */
  public Double getAverageLeasedCount() {
    synchronized (this) {
      if (this.totalLeaseCount <= 0 || this.cumulativeLeaseChecks <= 0) {
        return null;
      } else {
        double acquireCount = (double) this.cumulativeLeaseCount;
        double checkCount   = (double) this.cumulativeLeaseChecks;
        return acquireCount / checkCount;
      }
    }
  }

  /**
   * Returns the greatest number of connections that the pool has grown to
   * over its lifespan.  If no connections have ever been created then this
   * returns zero (0).
   *
   * @return The greatest number of connections that the pool has grown to
   *         over its lifespan.
   */
  public Integer getGreatestPoolSize() {
    synchronized (this) {
      return this.greatestPoolSize;
    }
  }

  /**
   * Gets the number of {@link Connection} instances that have been expired
   * due to exceeding the {@linkplain #getExpireTime() maximum lifespan limit}.
   * This returns <code>null</code> if no such maximum limits is set.
   *
   * @return The number of {@link Connection} instances that have been expired,
   *         or <code>null</code> if connections are not being expired.
   */
  public Integer getExpiredConnectionCount() {
    synchronized (this) {
      if (this.getExpireTime() == null) {
        return null;
      } else {
        return this.expiredCount;
      }
    }
  }

  /**
   * Gets the number of {@link Connection} instances that have been retired
   * due to reaching the {@linkplain #getRetireLimit() maximum lease limit}.
   * This returns <code>null</code> if no such maximum limit is set.
   *
   * @return The number of {@link Connection} instances that have been retired,
   *         or <code>null</code> if connections are not being retired.
   */
  public Integer getRetiredConnectionCount() {
    synchronized (this) {
      if (this.getRetireLimit() == null) {
        return null;
      } else {
        return this.retiredCount;
      }
    }
  }

  /**
   * Gets the average amount of time in milliseconds that it takes to
   * acquire a connection from the pool.  This does not include attempts to
   * acquire a {@link Connection} that fail within the allotted time limit.
   * This returns <code>null</code> if no connections have been acquired.
   *
   * @return The average amount of time in milliseconds that it takes to
   *         acquire a connection from the pool, or <code>null</code> if no
   *         connections have been acquired.
   */
  public Double getAverageAcquisitionTime() {
    synchronized (this) {
      if (this.totalAcquisitionTime < 0 || this.totalLeaseCount <= 0) {
        return null;
      } else {
        double totalTime  = (double) this.totalAcquisitionTime;
        double totalCount = (double) this.totalLeaseCount;
        return (totalTime / totalCount);
      }
    }
  }

  /**
   * Gets the greatest amount of time in milliseconds that it has taken to
   * acquire a connection from the pool.  This returns <code>null</code> if
   * no connections have been acquired.
   *
   * @return The greatest amount of time in milliseconds that it has taken to
   *         acquire a connection from the pool, or <code>null</code> if no
   *         connections have been acquired.
   */
  public Long getGreatestAcquisitionTime() {
    synchronized (this) {
      if (this.totalLeaseCount == 0 || this.greatestAcquisitionTime < 0L) {
        return null;
      } else {
        return this.greatestAcquisitionTime;
      }
    }
  }

  /**
   * Gets the greatest amount of time that a connection has been leased.
   * This includes connections that are currently leased and have not yet
   * been released back to the pool.  If no connections have been ever
   * been leased from this pool instance, then <code>null</code> is returned.
   *
   * @return The greatest amount of time that a connection has been leased,
   *         or <code>null</code> if no connections have been leased from this
   *         pool.
   */
  public Long getGreatestLeaseTime() {
    long greatestTime = -1L;
    synchronized (this) {
      for (PooledConnection pooledConn: this.leasedMap.values()) {
        PooledConnectionHandler handler   = pooledConn.getCurrentLeaseHandler();
        Long                    leaseTime = handler.getLeaseTime();
        if (leaseTime != null && leaseTime > greatestTime) {
          greatestTime = leaseTime;
        }
      }

      if (this.totalLeaseCount == 0) {
        return null;
      } else {
        return Math.max(greatestTime, this.greatestLeaseTime);
      }
    }
  }

  /**
   * Gets the average amount of time connections are leased.  This only
   * includes times for leases that have completed (i.e.: connections that have
   * been released back to the pool).  If no connections have completed their
   * leases for this pool instance, then <code>null</code> is returned.
   *
   * @return The average amount of time it has taken to complete a connection
   *         lease, or <code>null</code> if no connection leases have completed.
   */
  public Double getAverageLeaseTime() {
    synchronized (this) {
      if (this.completedLeaseCount == 0) {
        return null;
      } else {
        double totalTime = (double) this.totalLeaseTime;
        double totalCount = (double) this.totalLeaseCount;
        return (totalTime / totalCount);
      }
    }
  }

  /**
   * Gets the current total number of {@link Connection} instances in this
   * {@link ConnectionPool} instance.
   *
   * @return The current total number of {@link Connection} instances in this
   *         {@link ConnectionPool} instance.
   */
  public int getCurrentPoolSize() {
    synchronized (this) {
      return this.allConnections.size();
    }
  }

  /**
   * Gets the current number of available {@link Connection} instances in this
   * {@link ConnectionPool} instance.
   *
   * @return The current number of available {@link Connection} instances in this
   *         {@link ConnectionPool} instance.
   */
  public int getAvailableConnectionCount() {
    synchronized (this) {
      return this.availableConnections.size();
    }
  }

  /**
   * Gets the current number of outstanding {@link Connection} leases on which
   * this {@link ConnectionPool} is waiting.
   *
   * @return The current number of outstanding {@link Connection} leases on
   *         which this {@link ConnectionPool} is waiting.
   */
  public int getOutstandingLeaseCount() {
    synchronized (this) {
      return this.leasedMap.size();
    }
  }

  /**
   * Gets the longest outstanding lease time in milliseconds.  This returns
   * <code>null</code> if there are currently no outstanding leases.
   *
   * @return The longest outstanding lease time in milliseconds, or
   *         <code>null</code> if there are currently no outstanding leases.
   */
  public Long getGreatestOutstandingLeaseTime() {
    synchronized (this) {
      if (this.leasedMap.size() == 0) return null;
      long greatestTime = -1L;
      for (PooledConnection pooledConn : this.leasedMap.values()) {
        long leaseTime = pooledConn.getCurrentLeaseHandler().getLeaseTime();
        if (leaseTime > greatestTime) {
          greatestTime = leaseTime;
        }
      }
      return greatestTime;
    }
  }

  /**
   * Gets the average outstanding lease time in milliseconds.  This returns
   * <code>null</code> if there are currently no outstanding leases.
   *
   * @return The average outstanding lease time in milliseconds, or
   *         <code>null</code> if there are currently no outstanding leases.
   */
  public Double getAverageOutstandingLeaseTime() {
    synchronized (this) {
      if (this.leasedMap.size() == 0) return null;
      long totalTime = 0L;
      for (PooledConnection pooledConn : this.leasedMap.values()) {
        totalTime += pooledConn.getCurrentLeaseHandler().getLeaseTime();
      }
      double leaseCount = (double) this.leasedMap.size();
      return ((double) totalTime) / leaseCount;
    }
  }

  /**
   * Gets the number of milliseconds that have past since the last {@link
   * Connection} lease was requested.  This returns the number of milliseconds
   * since construction if a {@link Connection} lease has never been requested.
   *
   * @return The number of milliseconds that have past since the last {@link
   *         Connection} lease was requested, or the number of milliseconds
   *         since construction if a {@link Connection} lease has never been
   *         requested.
   */
  public long getIdleTime() {
    synchronized (this) {
      return (System.nanoTime() - this.idleStartTimeNanos) / ONE_MILLION;
    }
  }

  /**
   * Gets the total number of leases that have been granted over the lifetime
   * of this {@link ConnectionPool}.
   *
   * @return The total number of leases that have been granted over the lifetime
   *         of this {@link ConnectionPool}.
   */
  public long getLifetimeLeaseCount() {
    synchronized (this) {
      return this.totalLeaseCount;
    }
  }

  /**
   * Handles expiring connections that have exceeded their maximum lifespan
   * and refilling the pool to the minimum size as needed.
   *
   * @throws SQLException If a failure occurs.
   */
  protected synchronized void expireConnections() throws SQLException {
    // check if no expiration defined
    if (this.getExpireTime() == null) return;

    int   expired     = 0;
    long  maxLifespan = this.getExpireTime();

    // get an iterator
    Iterator<PooledConnection> iter = this.availableConnections.iterator();

    while (iter.hasNext()) {
      // get the next pooled connection
      PooledConnection pooledConn = iter.next();

      // check if it has exceeded the maximum lifespan
      if (pooledConn.getLifespan() > maxLifespan) {
        // close the connection
        SQLUtilities.close(pooledConn.getConnection());

        // increment the expired count
        expired++;

        // remove from the available connections
        iter.remove();

        // remove from all connections
        this.allConnections.remove(pooledConn);
      }
    }

    // now check if we need to refill to maintain the minimum pool size
    while (this.allConnections.size() < this.getMinimumSize()) {
      PooledConnection refill
          = new PooledConnection(this.connector.openConnection());
      this.allConnections.put(refill, refill.getCreatedTimeNanos());
      this.availableConnections.add(refill);
    }

    // update the number of expired connections
    this.expiredCount += expired;
  }

}
