package com.senzing.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static com.senzing.sql.ConnectionPool.*;
import static com.senzing.sql.ConnectionPool.Stat.*;
import static com.senzing.util.Quantified.*;

/**
 * Tests for {@link ConnectionPool}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ConnectionPoolTest {
  private static final long ONE_MILLION = 1000000L;

  /**
   * An invocation handler for connection proxy that does nothing and always
   * returns <code>null</code>.
   */
  public static class DummyConnectionHandler implements InvocationHandler {
    private List<Exception> failures = null;
    private IdentityHashMap<Connection, Thread> connThreadMap = null;
    private IdentityHashMap<Thread, Connection> threadConnMap = null;
    private boolean closed = false;
    private boolean autoCommit = true;

    public DummyConnectionHandler(
        List<Exception>                     failures,
        IdentityHashMap<Connection, Thread> connThreadMap,
        IdentityHashMap<Thread, Connection> threadConnMap)
    {
      this.failures       = failures;
      this.connThreadMap  = connThreadMap;
      this.threadConnMap  = threadConnMap;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
    {
      switch (method.getName()) {
        case "close":
          this.closed = true;
          return null;
        case "isClosed":
          return this.closed;
        case "equals":
          return (proxy == args[0]);
        case "hashCode":
          return System.identityHashCode(proxy);
        case "toString":
          return "Connection(" + System.identityHashCode(proxy) + ")";
        case "getAutoCommit":
          return this.autoCommit;
        case "setAutoCommit":
          this.autoCommit = (Boolean) args[0];
          return null;
        case "rollback":
          // used by connection pool
          return null;
        default:
          try {
            this.record((Connection) proxy);
            Thread.sleep(20L);
          } catch (InterruptedException ignore) {
            // do nothing
          } catch (Exception e) {
            synchronized (this.failures) {
              this.failures.add(e);
              e.printStackTrace();
              try {
                Thread.sleep(5000L);
              } catch (InterruptedException ignore) {
                // do nothing
              }
              System.exit(1);
            }
          }
          return null;
      }
    }

    /**
     * Call this function to record / verify the state of the
     * thread and connection.
     */
    private void record(Connection connection) {
      synchronized (this.connThreadMap) {
        synchronized (this.threadConnMap) {
          Thread current = Thread.currentThread();
          Connection conn = this.threadConnMap.get(current);
          if (conn != null && conn != connection) {
            throw new IllegalStateException(
                "Thread connection does not match specified connection.");
          }
          if (this.connThreadMap.containsKey(connection)) {
            Thread thread = this.connThreadMap.get(connection);
            if (current != thread) {
              throw new IllegalStateException(
                  "Current thread (" + current.getName()
                      + ") does not match recorded thread (" + thread.getName()
                      + " for connection: " + connection);
            }
          }
          this.connThreadMap.put(connection, current);
          this.threadConnMap.put(current, connection);
        }
      }
    }
  }


  /**
   * Call this function after releasing the connection for a thread.
   */
  public static void clearTracking(
      @org.jetbrains.annotations.NotNull IdentityHashMap<Connection, Thread> connThreadMap,
      IdentityHashMap<Thread, Connection> threadConnMap)
  {
    synchronized (connThreadMap) {
      synchronized (threadConnMap) {
        Thread      current = Thread.currentThread();
        Connection  conn    = threadConnMap.remove(current);
        if (conn != null) {
          connThreadMap.remove(conn);
        }
      }
    }
  }

  /**
   * The array of interfaces to proxy when producing dummy connections.
   */
  private static final Class[] INTERFACES = { Connection.class };

  /**
   * A {@link Connector} implementation that produces dummy connections.
   */
  public static class DummyConnector implements Connector {
    private DummyConnectionHandler handler = null;
    public DummyConnector(List<Exception> failures,
                          IdentityHashMap<Connection, Thread> connThreadMap,
                          IdentityHashMap<Thread, Connection> threadConnMap)
    {
      this.handler = new DummyConnectionHandler(failures,
                                                connThreadMap,
                                                threadConnMap);
    }
    @Override
    public Connection openConnection() throws SQLException {
      return (Connection) Proxy.newProxyInstance(
          this.getClass().getClassLoader(), INTERFACES, this.handler);
    }
  }

  /**
   *
   */
  public List<Arguments> getPoolSizeParameters() {
    List<Arguments> result = new LinkedList<>();
    for (int min = 0; min < 5; min++) {
      for (int max = Math.max(1, min); max < 10; max++) {
        result.add(Arguments.arguments(min, max));
      }
    }
    return result;
  }

  /**
   *
   */
  public List<Arguments> getStatisticsParameters() {
    List<Arguments> result = new LinkedList<>();
    for (int min = 0; min < 5; min++) {
      for (int max = Math.max(1, min); max < 10; max++) {
        for (int expire = 0; expire < 2; expire++) {
          for (int retire = 0; retire < 20; retire += 10) {
            result.add(Arguments.arguments(min, max, expire, retire));
          }
        }
      }
    }
    return result;
  }

  /**
   *
   */
  public List<Arguments> getConcurrentParameters() {
    List<Arguments> result = new LinkedList<>();
    for (int min = 0; min < 5; min++) {
      for (int max = Math.max(1, min); max < 10; max++) {
        int[] threadCounts = { min, max, 2 * max, 5 * max };
        for (int threadCount : threadCounts) {
          result.add(Arguments.arguments(min, max, threadCount));
        }
      }
    }

    return result;
  }

  private static final SecureRandom PRNG = new SecureRandom();

  private static class DummyThread extends Thread {
    private ConnectionPool pool = null;
    private List<Exception> failures = null;
    private IdentityHashMap<Connection, Thread> connThreadMap = null;
    private IdentityHashMap<Thread, Connection> threadConnMap = null;
    private long maxWait = -1L;
    private int iterationCount = 1;
    private long minDuration  = 20L;
    private long maxDuration  = 50L;
    private int threadIndex = 0;
    private int threadCount = 0;

    public DummyThread(int                                  threadIndex,
                       int                                  threadCount,
                       ConnectionPool                       pool,
                       long                                 maxWaitMillis,
                       int                                  iterationCount,
                       long                                 minDurationMillis,
                       long                                 maxDurationMillis,
                       List<Exception>                      failures,
                       IdentityHashMap<Connection, Thread>  connThreadMap,
                       IdentityHashMap<Thread, Connection>  threadConnMap)
    {
      super("Dummy Thread " + threadIndex + " ("
                + System.identityHashCode(new Object()) + ")");
      this.threadIndex    = threadIndex;
      this.threadCount    = threadCount;
      this.pool           = pool;
      this.maxWait        = maxWaitMillis;
      this.iterationCount = iterationCount;
      this.minDuration    = minDurationMillis;
      this.maxDuration    = maxDurationMillis;
      this.failures       = failures;
      this.connThreadMap  = connThreadMap;
      this.threadConnMap  = threadConnMap;
    }
    @Override
    public void run() {
      String info = "minSize=[ " + this.pool.getMaximumSize() + " ], maxSize=[ "
          + this.pool.getMaximumSize() + " ], threadIndex=[ " + threadIndex
          + " ], threadCount =[ " + threadCount + " ], maxWait=[ "
          + this.maxWait + " ]";

      try {
        for (int index = 0; index < this.iterationCount; index++) {
          Connection conn = this.pool.acquire(this.maxWait);
          if (conn == null) {
            throw new IllegalStateException(
                "Failed to obtain connection in allotted time.  " + info);
          }
          try {
            // try at least two operations on the connection to record and
            // confirm usage within the same thread
            PreparedStatement ps = conn.prepareStatement("");
            Statement stmt = conn.createStatement();
            long waitTime = this.minDuration;
            if (this.maxDuration > this.minDuration) {
              double range    = this.maxDuration - this.minDuration;
              double percent  = PRNG.nextDouble();
              waitTime += (long) (range * percent);
            }
            try {
              Thread.sleep(waitTime);
            } catch (InterruptedException ignore) {
              // do nothing
            }
          } finally {
            // either close or release the connection back to the
            // pool, both should have the same effect
            try {
              clearTracking(this.connThreadMap, this.threadConnMap);
              if (PRNG.nextBoolean()) {
                SQLUtilities.close(conn);
              } else {
                this.pool.release(conn);
              }
            } catch (Exception ignore) {
              ignore.printStackTrace();
            }
            try {
              PreparedStatement ps = conn.prepareStatement("");
              throw new IllegalStateException(
                  "Used connection after closing without an error.  " + info);

            } catch (SQLException expected) {
              // do nothing
            }
          }
        }
      } catch (Exception e) {
        synchronized (this.failures) {
          this.failures.add(e);
        }
      } finally {
        clearTracking(this.connThreadMap, this.threadConnMap);
      }
    }
  }

  /**
   * Tests the basics of the connection pool.
   */
  @ParameterizedTest
  @MethodSource("getPoolSizeParameters")
  public void basicTest(int minPoolSize, int maxPoolSize)
  {
    ConnectionPool pool = null;
    String info = "min=[ " + minPoolSize + " ], max=[ " + maxPoolSize + " ]";
    try {
      IdentityHashMap<Connection, Thread> connThreadMap
          = new IdentityHashMap<>();
      IdentityHashMap<Thread, Connection> threadConnMap
          = new IdentityHashMap<>();
      List<Exception> failures = new LinkedList<>();

      DummyConnector connector = new DummyConnector(failures,
                                                    connThreadMap,
                                                    threadConnMap);

      if (minPoolSize == maxPoolSize) {
        pool = new ConnectionPool(connector, minPoolSize);
      } else {
        pool = new ConnectionPool(connector, minPoolSize, maxPoolSize);
      }

      assertEquals(minPoolSize, pool.getMinimumSize(),
                   "Minimum pool size is not as expected: " + info);
      assertEquals(maxPoolSize, pool.getMaximumSize(),
                   "Maximum pool size is not as expected: " + info);
      assertNull(pool.getRetireLimit(),
                 "Maximum connection leases not as expected: " + info);
      assertNull(pool.getExpireTime(),
                 "Maximum connection lifespan not as expected: "
                     + info);
      assertFalse(pool.isShutdown(),
          "Pool prematurely registering shutdown state: " + info);
      assertNull(pool.getGreatestLeasedCount(),
                   "Greatest lease count not initially null: " + info);
      assertNull(pool.getAverageLeasedCount(),
                 "Average lease count not initially null: " + info);
      assertEquals(minPoolSize, pool.getGreatestPoolSize(),
                   "Greatest pool size not as expected: " + info);
      assertNull(pool.getExpiredConnectionCount(),
                 "Expired connection count is not null: " + info);
      assertNull(pool.getRetiredConnectionCount(),
                 "Retired connection count is not null: " + info);
      assertNull(pool.getAverageAcquisitionTime(),
                 "Average acquisition time is not initially null: "
                     + info);
      assertNull(pool.getGreatestAcquisitionTime(),
                 "Greatest acquisition time is not initially null: "
                     + info);
      assertNull(pool.getGreatestLeaseTime(),
                 "Greatest lease time is not initially null: " + info);
      assertNull(pool.getAverageLeaseTime(),
                 "Average lease time is not initially null: " + info);
      assertEquals(minPoolSize, pool.getAvailableConnectionCount(),
                   "Available connection count is not as expected");
      assertEquals(minPoolSize, pool.getCurrentPoolSize(),
                   "Current pool size is not as expected");
      assertNull(pool.getAverageOutstandingLeaseTime(),
                 "Average outstanding lease time is not initially null");
      assertNull(pool.getGreatestOutstandingLeaseTime(),
                 "Greatest outstanding lease time is not initially null");
      assertEquals(0, pool.getOutstandingLeaseCount(),
                   "Outstanding lease count is not zero (0).");

      // attempt to acquire connections up to the min pool size
      List<Connection> leased = new LinkedList<>();
      try {
        for (int index = 0; index < minPoolSize; index++) {
          long startNanos = System.nanoTime();
          Connection conn = pool.acquire(PRNG.nextBoolean() ? 500L : 0L);
          long endNanos = System.nanoTime();
          long elapsed = (endNanos - startNanos) / ONE_MILLION;
          assertNotNull(conn, "Connection not acquired.  index=[ "
              + index + " ], " + info);
          leased.add(conn);
          assertTrue((elapsed < 200L),
                     "Blocked for more than 200ms (" + elapsed
                         + "ms) waiting for a connection.  index=[ "
                         + index + " ], " + info);
        }

      } finally {
        for (Connection connection: leased) {
          if (PRNG.nextBoolean()) {
            SQLUtilities.close(connection);
          } else {
            pool.release(connection);
          }
          try {
            PreparedStatement ps = connection.prepareStatement("");
            throw new IllegalStateException(
                "Used connection after closing without an error.");

          } catch (SQLException expected) {
            // do nothing
          }
        }
        leased.clear();
      }

      assertEquals(minPoolSize, pool.getCurrentPoolSize(),
                   "Current pool size expanded beyond minimum: "
                       + info);
      assertEquals(minPoolSize, pool.getAvailableConnectionCount(),
                   "Pool has leased connections when they should have "
                   + "been returned: " + info);
      assertEquals(0, pool.getOutstandingLeaseCount(),
                   "Outstanding leases exist when they should not: "
                       + info);
      if (minPoolSize > 0) {
        assertNotNull(pool.getGreatestAcquisitionTime(),
                      "Greatest acquisition time should not be null: "
                          + info);
        assertNotNull(pool.getGreatestLeasedCount(),
                      "Greatest lease count should not be null: " + info);
        assertNotNull(pool.getAverageLeaseTime(),
                      "Average lease time should not be null: " + info);
        assertNotNull(pool.getGreatestLeaseTime(),
                      "Greatest lease time should not be null: " + info);
      } else {
        assertNull(pool.getGreatestAcquisitionTime(),
                      "Greatest acquisition time should be null: "
                          + info);
        assertNull(pool.getGreatestLeasedCount(),
                      "Greatest lease count should be null: " + info);
        assertNull(pool.getAverageLeaseTime(),
                      "Average lease time should be null: " + info);
        assertNull(pool.getGreatestLeaseTime(),
                      "Greatest lease time should be null: " + info);

      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("Received a SQL exception", e);
    } finally {
      if (pool != null) pool.shutdown();
    }
  }

  /**
   * Tests the waiting on acquire() calls.
   */
  @Test
  public void waitTest()
  {
    final int minPoolSize = 5;
    final int maxPoolSize = 10;
    ConnectionPool pool = null;
    try {
      IdentityHashMap<Connection, Thread> connThreadMap
          = new IdentityHashMap<>();
      IdentityHashMap<Thread, Connection> threadConnMap
          = new IdentityHashMap<>();
      List<Exception> failures = new LinkedList<>();

      DummyConnector connector = new DummyConnector(failures,
                                                    connThreadMap,
                                                    threadConnMap);

      pool = new ConnectionPool(connector, 5, 10);

      // attempt to acquire connections up to the min pool size
      List<Connection> leased = new LinkedList<>();
      try {
        for (int index = 0; index < maxPoolSize; index++) {
          Connection conn = pool.acquire(0L);
          assertNotNull(conn, "Connection not immediately acquired.  "
              + "leased=[ " + leased.size() + " ], minPoolSize=[ "
              + minPoolSize + " ], maxPoolSize=[ " + maxPoolSize + " ]");

          leased.add(conn);
        }

        // now try to acquire when none are available and pool cannot grow
        Connection unavailable = pool.acquire(100L);
        assertNull(unavailable, "Connection acquired when none should "
            + "have been available.  leased=[ " + leased.size()
            + " ], minPoolSize=[ " + minPoolSize + " ], maxPoolSize=[ "
            + maxPoolSize + " ]");

      } finally {
        for (Connection connection: leased) {
          SQLUtilities.close(connection);
        }
        leased.clear();
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("Received a SQL exception", e);
    } finally {
      if (pool != null) pool.shutdown();
    }
  }

  /**
   * Tests the basics of the connection pool.
   */
  @ParameterizedTest
  @MethodSource("getPoolSizeParameters")
  public void expandTest(int minPoolSize, int maxPoolSize)
  {
    ConnectionPool pool = null;
    String info = "min=[ " + minPoolSize + " ], max=[ " + maxPoolSize + " ]";
    try {
      IdentityHashMap<Connection, Thread> connThreadMap
          = new IdentityHashMap<>();
      IdentityHashMap<Thread, Connection> threadConnMap
          = new IdentityHashMap<>();
      List<Exception> failures = new LinkedList<>();

      DummyConnector connector = new DummyConnector(failures,
                                                    connThreadMap,
                                                    threadConnMap);

      if (minPoolSize == maxPoolSize) {
        pool = new ConnectionPool(connector, minPoolSize);
      } else {
        pool = new ConnectionPool(connector, minPoolSize, maxPoolSize);
      }


      // attempt to acquire connections up to the min pool size
      List<Connection> leased = new LinkedList<>();

      // attempt to acquire connections up to the max pool size
      try {
        for (int index = 0; index < maxPoolSize; index++) {
          long startNanos = System.nanoTime();
          Connection conn = pool.acquire(PRNG.nextBoolean() ? 500L : 0L);
          long endNanos = System.nanoTime();
          long elapsed = (endNanos - startNanos) / ONE_MILLION;
          assertNotNull(conn, "Connection not acquired.  index=[ "
              + index + " ], " + info);
          leased.add(conn);
          assertTrue((elapsed < 200L),
                     "Blocked for more than 200ms (" + elapsed
                         + "ms) waiting for a connection.  index=[ "
                         + index + " ], " + info);
        }

      } finally {
        for (Connection connection: leased) {
          if (PRNG.nextBoolean()) {
            SQLUtilities.close(connection);
          } else {
            pool.release(connection);
          }
          try {
            PreparedStatement ps = connection.prepareStatement("");
            throw new IllegalStateException(
                "Used connection after closing without an error.");

          } catch (SQLException expected) {
            // do nothing
          }
        }
        leased.clear();
      }

      assertEquals(maxPoolSize,
                   pool.getAvailableConnectionCount(),
                   "Pool has leased connections when they should have "
                       + "been returned");
      assertEquals(0, pool.getOutstandingLeaseCount(),
                   "Outstanding leases exist when they should not");
      assertEquals(maxPoolSize, pool.getCurrentPoolSize(),
                   "The current pool size is less than the maximum");


    } catch (Exception e) {
      e.printStackTrace();
      fail("Received a SQL exception", e);
    } finally {
      if (pool != null) pool.shutdown();
    }
  }

  /**
   * Tests concurrent usage with varying thread counts and pool sizes.
   */
  @ParameterizedTest
  @MethodSource("getConcurrentParameters")
  public void concurrentTest(int minPoolSize, int maxPoolSize, int threadCount)
  {
    ConnectionPool pool = null;
    String info = "min=[ " + minPoolSize + " ], max=[ " + maxPoolSize
        + " ], threads=[ " + threadCount + " ]";
    try {
      IdentityHashMap<Connection, Thread> connThreadMap
          = new IdentityHashMap<>();
      IdentityHashMap<Thread, Connection> threadConnMap
          = new IdentityHashMap<>();
      List<Exception> failures = new LinkedList<>();

      DummyConnector connector = new DummyConnector(failures,
                                                    connThreadMap,
                                                    threadConnMap);

      if (minPoolSize == maxPoolSize) {
        pool = new ConnectionPool(connector, minPoolSize);
      } else {
        pool = new ConnectionPool(connector, minPoolSize, maxPoolSize);
      }

      // now create threads to handle test the minimum pool size
      List<Thread> threads = new ArrayList<>(minPoolSize);
      for (int index = 0; index < threadCount; index++) {
        threads.add(new DummyThread(index,
                                    threadCount,
                                    pool,
                                    (threadCount > maxPoolSize)
                                        ? -1L
                                        : (PRNG.nextBoolean() ? 500L : 0L),
                                    5,
                                    5L,
                                    10L,
                                    failures,
                                    connThreadMap,
                                    threadConnMap));
      }
      for (Thread thread : threads) {
        thread.start();
      }
      for (Thread thread : threads) {
        try {
          thread.join(2000L);

        } catch (InterruptedException ignore) {
          // do nothing
        }
      }
      for (Thread thread : threads) {
        assertFalse(thread.isAlive(),
                    "Worker thread is unexpectedly still alive: "
                        + info);
      }
      if (failures.size() > 0) {
        if (failures.size() == 1) {
          fail("Failure encountered", failures.get(0));
        } else {
          fail(failuresMessage(failures));
        }
      }

      if (threadCount <= minPoolSize) {
        assertEquals(minPoolSize, pool.getCurrentPoolSize(),
                     "Current pool size expanded beyond minimum");
        assertEquals(minPoolSize, pool.getAvailableConnectionCount(),
                     "Pool has leased connections when they should "
                         + "have been returned");
      }
      assertEquals(0, pool.getOutstandingLeaseCount(),
                   "Outstanding leases exist when they should not");

    } catch (Exception e) {
      e.printStackTrace();
      fail("Received a SQL exception", e);
    } finally {
      if (pool != null) pool.shutdown();
    }
  }

  /**
   * Utility method for obtaining a failure message for multiple exceptions.
   */
  private static String failuresMessage(List<Exception> failures) {
    StringWriter  sw = new StringWriter();
    PrintWriter   pw = new PrintWriter(sw);
    pw.println(failures.size() + " failures occurred");
    for (Exception failure: failures) {
      pw.println("------------------------------------------------");
      failure.printStackTrace(pw);
      pw.println();
    }
    pw.flush();
    return sw.toString();
  }

  @Test
  public void idleExpireTest() {
    ConnectionPool pool = null;
    try {
      IdentityHashMap<Connection, Thread> connThreadMap
          = new IdentityHashMap<>();
      IdentityHashMap<Thread, Connection> threadConnMap
          = new IdentityHashMap<>();
      List<Exception> failures = new LinkedList<>();

      DummyConnector connector = new DummyConnector(failures,
                                                    connThreadMap,
                                                    threadConnMap);

      final int maxPoolSize   = 5;
      final int expireSeconds = 2;
      final int noRetireLimit = 0;
      final int minPoolSize   = 0;
      pool = new ConnectionPool(
          connector, minPoolSize, maxPoolSize, expireSeconds, noRetireLimit);

      assertEquals(2000L, pool.getExpireTime(),
                   "Expire time is not as expected.");
      assertEquals(0, pool.getExpiredConnectionCount(),
                 "Expired connection count is not zero initially.");
      assertNull(pool.getRetiredConnectionCount(),
                 "Retired connection count is not null.");
      assertEquals(0, pool.getCurrentPoolSize(),
                   "Current pool size not as expected.");
      assertEquals(0, pool.getAvailableConnectionCount(),
                   "Available connection count not as expected");

      // expand the pool
      List<Connection> leases = new ArrayList<>(maxPoolSize);
      for (int index = 0; index < maxPoolSize; index++) {
        leases.add(pool.acquire());
      }
      for (Connection conn: leases) {
        conn.close();
      }
      assertEquals(maxPoolSize, pool.getCurrentPoolSize(),
                   "Current pool size not as expected.");
      assertEquals(maxPoolSize, pool.getAvailableConnectionCount(),
                   "Available connection count not as expected");

      // now sleep for 3 seconds
      Thread.sleep(3000L);

      // check the expired count
      assertEquals(maxPoolSize, pool.getExpiredConnectionCount(),
                   "Expired connection count not as expected");
      assertEquals(0, pool.getCurrentPoolSize(),
                   "Current pool size not as expected.");
      assertEquals(0, pool.getAvailableConnectionCount(),
                   "Available connection count not as expected");

    } catch (Exception e) {
      e.printStackTrace();
      fail("Received a SQL exception", e);
    } finally {
      if (pool != null) pool.shutdown();
    }
  }

  @Test
  public void activeExpireTest() {
    ConnectionPool pool = null;
    try {
      IdentityHashMap<Connection, Thread> connThreadMap
          = new IdentityHashMap<>();
      IdentityHashMap<Thread, Connection> threadConnMap
          = new IdentityHashMap<>();
      List<Exception> failures = new LinkedList<>();

      DummyConnector connector = new DummyConnector(failures,
                                                    connThreadMap,
                                                    threadConnMap);

      final int maxPoolSize   = 5;
      final int expireSeconds = 2;
      final int noRetireLimit = 0;
      final int minPoolSize   = 0;
      pool = new ConnectionPool(
          connector, minPoolSize, maxPoolSize, expireSeconds, noRetireLimit);

      assertEquals(2000L, pool.getExpireTime(),
                   "Expire time is not as expected.");
      assertEquals(0, pool.getExpiredConnectionCount(),
                   "Expired connection count is not zero initially.");
      assertNull(pool.getRetiredConnectionCount(),
                 "Retired connection count is not null.");
      assertEquals(0, pool.getCurrentPoolSize(),
                   "Current pool size not as expected.");
      assertEquals(0, pool.getAvailableConnectionCount(),
                   "Available connection count not as expected");

      // expand the pool
      for (int index1 = 0; index1 < 5; index1++) {
        List<Connection> leases = new ArrayList<>(maxPoolSize);
        for (int index2 = 0; index2 < maxPoolSize; index2++) {
          leases.add(pool.acquire());
        }
        for (Connection conn : leases) {
          conn.close();
        }
        Thread.sleep(550L);
      }
      assertEquals(maxPoolSize, pool.getCurrentPoolSize(),
                   "Current pool size not as expected.");
      assertEquals(maxPoolSize, pool.getAvailableConnectionCount(),
                   "Available connection count not as expected");

      // check the expired count
      assertEquals(maxPoolSize, pool.getExpiredConnectionCount(),
                   "Expired connection count not as expected");

    } catch (Exception e) {
      e.printStackTrace();
      fail("Received a SQL exception", e);
    } finally {
      if (pool != null) pool.shutdown();
    }
  }

  @Test
  public void retireTest() {
    ConnectionPool pool = null;
    try {
      IdentityHashMap<Connection, Thread> connThreadMap
          = new IdentityHashMap<>();
      IdentityHashMap<Thread, Connection> threadConnMap
          = new IdentityHashMap<>();
      List<Exception> failures = new LinkedList<>();

      DummyConnector connector = new DummyConnector(failures,
                                                    connThreadMap,
                                                    threadConnMap);

      final int maxPoolSize   = 5;
      final int noExpireTime  = 0;
      final int retireLimit   = 4;
      final int minPoolSize   = 0;
      pool = new ConnectionPool(
          connector, minPoolSize, maxPoolSize, noExpireTime, retireLimit);

      assertNull(pool.getExpireTime(),
                   "Expire time is not null.");
      assertNull(pool.getExpiredConnectionCount(),
                 "Expired connection count is not null.");
      assertEquals(0, pool.getRetiredConnectionCount(),
                 "Retired connection count is not initially zero.");
      assertEquals(0, pool.getCurrentPoolSize(),
                   "Current pool size not as expected.");
      assertEquals(0, pool.getAvailableConnectionCount(),
                   "Available connection count not as expected");

      // expand the pool
      for (int index1 = 0; index1 < 6; index1++) {
        List<Connection> leases = new ArrayList<>(maxPoolSize);
        for (int index2 = 0; index2 < maxPoolSize; index2++) {
          leases.add(pool.acquire());
        }
        for (Connection conn : leases) {
          conn.close();
        }
      }

      assertEquals(maxPoolSize, pool.getCurrentPoolSize(),
                   "Current pool size not as expected.");
      assertEquals(maxPoolSize, pool.getAvailableConnectionCount(),
                   "Available connection count not as expected");

      // check the retired count
      assertEquals(maxPoolSize, pool.getRetiredConnectionCount(),
                   "Expired connection count not as expected");

    } catch (Exception e) {
      e.printStackTrace();
      fail("Received a SQL exception", e);
    } finally {
      if (pool != null) pool.shutdown();
    }
  }


  /**
   * Tests the statistics of the connection pool.
   */
  @ParameterizedTest
  @MethodSource("getStatisticsParameters")
  public void statisticsTest(int minPoolSize,
                             int maxPoolSize,
                             int expireSeconds,
                             int retireLimit)
  {
    ConnectionPool pool = null;
    String info = "min=[ " + minPoolSize + " ], max=[ " + maxPoolSize
        + " ], expire=[ " + expireSeconds + " ], retire=[ " + retireLimit
        + " ]";

    try {
      IdentityHashMap<Connection, Thread> connThreadMap
          = new IdentityHashMap<>();
      IdentityHashMap<Thread, Connection> threadConnMap
          = new IdentityHashMap<>();
      List<Exception> failures = new LinkedList<>();

      long startTime = System.nanoTime();
      Thread.sleep(20L);
      DummyConnector connector = new DummyConnector(failures,
                                                    connThreadMap,
                                                    threadConnMap);

      pool = new ConnectionPool(
          connector, minPoolSize, maxPoolSize, expireSeconds, retireLimit);
      Thread.sleep(20L);
      long startTime2 = System.nanoTime();
      Thread.sleep(50L);
      long lowerLimit = (System.nanoTime() - startTime2) / ONE_MILLION;
      Map<Statistic, Number> statsMap = pool.getStatistics();

      assertEquals(minPoolSize, statsMap.get(minimumSize),
                   "Minimum pool size is not as expected: " + info);

      assertEquals(maxPoolSize, statsMap.get(maximumSize),
                   "Maximum pool size is not as expected: " + info);

      if (retireLimit == 0) {
        assertFalse(statsMap.containsKey(Stat.retireLimit),
                    "Retire limit unexpectedly present in map: "
                    + info);
      } else {
        assertEquals(retireLimit, statsMap.get(Stat.retireLimit),
                     "Maximum connection leases not as expected: " + info);
      }
      if (expireSeconds == 0) {
        assertFalse(statsMap.containsKey(expireTime),
                    "Expire time unexpectedly present in map: " + info);
      } else {
        assertEquals(expireSeconds * 1000L, statsMap.get(expireTime),
                     "Maximum connection lifespan not as expected: "
                         + info);
      }
      assertEquals(minPoolSize, statsMap.get(currentPoolSize),
                   "Current pool size not as expected: " + info);
      assertEquals(minPoolSize, statsMap.get(availableConnections),
                   "Available connections not as expected: " + info);
      assertEquals(0, statsMap.get(outstandingLeases),
                   "Outstanding leases not as expected: " + info);
      assertFalse(statsMap.containsKey(greatestOutstandingLeaseTime),
                  "Greatest outstanding lease time unexpectedly "
                  + "present: " + info);
      assertFalse(statsMap.containsKey(averageOutstandingLeaseTime),
                  "Average outstanding lease time unexpectedly "
                      + "present: " + info);
      assertFalse(statsMap.containsKey(greatestLeasedCount),
                  "Greatest leased count unexpectedly present: "
                      + info);
      assertFalse(statsMap.containsKey(averageLeasedCount),
                  "Average leased count unexpectedly present: "
                      + info);
      assertEquals(minPoolSize, statsMap.get(greatestPoolSize),
                  "Greatest pool size is not as expected: " + info);
      if (expireSeconds == 0) {
        assertFalse(statsMap.containsKey(expiredConnections),
                    "Expired connections is unexpectedly present: "
                        + info);
      } else {
        assertEquals(0, statsMap.get(expiredConnections),
                     "Expired connections is not as expected: " + info);
      }
      if (retireLimit == 0) {
        assertFalse(statsMap.containsKey(retiredConnections),
                    "Retired connections is unexpectedly present: "
                        + info);
      } else {
        assertEquals(0, statsMap.get(retiredConnections),
                     "Retired connections is not as expected: " + info);
      }

      assertFalse(statsMap.containsKey(averageAcquireTime),
                  "Average acquire time is unexpectedly present: "
                      + info);
      assertFalse(statsMap.containsKey(greatestAcquireTime),
                  "Greatest acquire time is unexpectedly present: "
                      + info);
      assertFalse(statsMap.containsKey(greatestLeaseTime),
                  "Greatest lease time is unexpectedly present: "
                      + info);
      assertFalse(statsMap.containsKey(averageLeaseTime),
                  "Average lease time is unexpectedly present: "
                      + info);
      long elapsed = (System.nanoTime() - startTime) / ONE_MILLION;
      assertTrue(statsMap.containsKey(idleTime),
                 "Idle time unexpectedly not present: " + info);
      assertTrue(statsMap.get(idleTime).longValue() < elapsed,
                 "Idle time is greater than expected.  idle=[ "
                     + statsMap.get(idleTime) + " ], elapsed=[ " + elapsed
                     + " ], "+ info);

      assertTrue(statsMap.get(idleTime).longValue() > lowerLimit,
                 "Idle time is less than expected.  idle=[ "
                     + statsMap.get(idleTime) + " ], lowerLimit=[ " + lowerLimit
                     + " ], "+ info);

      // attempt to acquire connections up to the min pool size
      List<Connection> leased = new LinkedList<>();
      long beforeAcquiredTime = System.nanoTime();
      long afterAcquiredTime = System.nanoTime();
      for (int index1 = 0; index1 < 20; index1++) {
        try {
          for (int index2 = 0; index2 < maxPoolSize; index2++) {
            beforeAcquiredTime = System.nanoTime();
            Thread.sleep(2L);
            Connection conn = pool.acquire();
            Thread.sleep(2L);
            afterAcquiredTime = System.nanoTime();
            assertNotNull(conn, "Connection not acquired.  index=[ "
                + index1 + ", " + index2 + " ], " + info);
            leased.add(conn);
          }
          Thread.sleep(100L);
          statsMap = pool.getStatistics();

          assertEquals(maxPoolSize, statsMap.get(outstandingLeases),
                       "Outstanding leases not as expected: " + info);
          assertTrue(statsMap.containsKey(greatestOutstandingLeaseTime),
                      "Greatest outstanding lease time unexpectedly "
                          + "missing: " + info);
          assertTrue(statsMap.containsKey(averageOutstandingLeaseTime),
                    "Average outstanding lease time unexpectedly "
                          + "missing: " + info);
        } finally {
          for (Connection connection: leased) {
            SQLUtilities.close(connection);
          }
          leased.clear();
        }
      }
      lowerLimit  = (System.nanoTime() - afterAcquiredTime) / ONE_MILLION;
      elapsed     = (System.nanoTime() - beforeAcquiredTime) / ONE_MILLION;

      statsMap = pool.getStatistics();

      assertEquals(minPoolSize, statsMap.get(minimumSize),
                   "Minimum pool size is not as expected: " + info);

      assertEquals(maxPoolSize, statsMap.get(maximumSize),
                   "Maximum pool size is not as expected: " + info);

      if (retireLimit == 0) {
        assertFalse(statsMap.containsKey(Stat.retireLimit),
                    "Retire limit unexpectedly present in map: "
                        + info);
      } else {
        assertEquals(retireLimit, statsMap.get(Stat.retireLimit),
                     "Maximum connection leases not as expected: " + info);
      }
      if (expireSeconds == 0) {
        assertFalse(statsMap.containsKey(expireTime),
                    "Expire time unexpectedly present in map: " + info);
      } else {
        assertEquals(expireSeconds * 1000L, statsMap.get(expireTime),
                     "Maximum connection lifespan not as expected: "
                         + info);
      }
      assertEquals(statsMap.get(currentPoolSize),
                   statsMap.get(availableConnections),
                   "All connections should be available: " + info);

      assertEquals(0, statsMap.get(outstandingLeases),
                   "Outstanding leases not as expected: " + info);

      assertFalse(statsMap.containsKey(greatestOutstandingLeaseTime),
                  "Greatest outstanding lease time unexpectedly "
                      + "present: " + info);
      assertFalse(statsMap.containsKey(averageOutstandingLeaseTime),
                  "Average outstanding lease time unexpectedly "
                      + "present: " + info);
      assertTrue(statsMap.containsKey(greatestLeasedCount),
                  "Greatest leased count unexpectedly missing: "
                      + info);
      assertTrue(statsMap.containsKey(averageLeasedCount),
                 "Average leased count unexpectedly missing: " + info);

      assertEquals(maxPoolSize, statsMap.get(greatestPoolSize),
                   "Greatest pool size is not as expected: " + info);

      if (expireSeconds == 0) {
        assertFalse(statsMap.containsKey(expiredConnections),
                    "Expired connections is unexpectedly present: "
                        + info);
      } else {
        assertTrue(statsMap.containsKey(expiredConnections),
                    "Expired connections is unexpectedly missing: "
                        + info);
      }
      if (retireLimit == 0) {
        assertFalse(statsMap.containsKey(retiredConnections),
                    "Retired connections is unexpectedly present: "
                        + info);
      } else {
        assertTrue(statsMap.containsKey(retiredConnections),
                    "Retired connections is unexpectedly missing: "
                        + info);
      }

      assertTrue(statsMap.containsKey(averageAcquireTime),
                  "Average acquire time is unexpectedly missing: "
                      + info);
      assertTrue(statsMap.containsKey(greatestAcquireTime),
                  "Greatest acquire time is unexpectedly missing: "
                      + info);
      assertTrue(statsMap.containsKey(greatestLeaseTime),
                  "Greatest lease time is unexpectedly missing: "
                      + info);
      assertTrue(statsMap.containsKey(averageLeaseTime),
                  "Average lease time is unexpectedly missing: "
                      + info);
      assertTrue(statsMap.containsKey(idleTime),
                 "Idle time unexpectedly not present: " + info);
      assertTrue(statsMap.get(idleTime).longValue() < elapsed,
                 "Idle time is greater than expected.  idle=[ "
                     + statsMap.get(idleTime) + " ], elapsed=[ " + elapsed
                     + " ], "+ info);

      assertTrue(statsMap.get(idleTime).longValue() > lowerLimit,
                 "Idle time is less than expected.  idle=[ "
                     + statsMap.get(idleTime) + " ], lowerLimit=[ " + lowerLimit
                     + " ], "+ info);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Received a SQL exception", e);
    } finally {
      if (pool != null) pool.shutdown();
    }
  }

  /**
   *
   */
  @Test
  public void sqliteTest() {
    ConnectionPool    pool  = null;
    Connection        conn  = null;
    PreparedStatement ps    = null;
    Statement         stmt  = null;
    ResultSet         rs    = null;
    try {
      final File tempFile = File.createTempFile("test-", ".db");
      tempFile.deleteOnExit();
      final String path = tempFile.getCanonicalPath();

      Connector connector
          = () -> DriverManager.getConnection("jdbc:sqlite:" + path);

      pool = new ConnectionPool(connector, 1);

      conn = pool.acquire();

      assertFalse(conn.getAutoCommit(), "Auto-commit is not false");

      List<String> setupList = List.of(
          "PRAGMA foreign_keys = ON;",
          "PRAGMA journal_mode = WAL;",
          "PRAGMA synchronous = 0;",
          "PRAGMA secure_delete = 0;",
          "PRAGMA automatic_index = 0;",
          "CREATE TABLE foo (foo_id INTEGER PRIMARY_KEY, description TEXT)");

      conn.setAutoCommit(true);
      stmt = conn.createStatement();
      for (String sql: setupList) {
        stmt.execute(sql);
      }
      stmt = SQLUtilities.close(stmt);

      Connection prev = conn;
      conn = SQLUtilities.close(conn);

      conn = pool.acquire();

      assertFalse(conn.getAutoCommit(), "Auto-commit is not false");
      assertFalse((conn == prev), "Got identical proxy connection");

      ps = conn.prepareStatement("INSERT INTO foo (description) VALUES (?)");
      String[] values = { "phoo", "bar", "bax", "foo" };
      for (String value : values) {
        ps.setString(1, value);
        ps.executeUpdate();
      }
      ps = SQLUtilities.close(ps);

      // now close the connection WITHOUT committing
      prev = conn;
      conn = SQLUtilities.close(conn);

      // get a new connection
      conn = pool.acquire();
      assertFalse(conn.getAutoCommit(), "Auto-commit is not false");
      assertFalse((conn == prev), "Got identical proxy connection");

      // query to ensure we rolled back
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT COUNT (*) FROM foo");
      int result = rs.getInt(1);
      assertEquals(0, result,
                   "Got more rows than expected: " + result);

      rs = SQLUtilities.close(rs);
      stmt = SQLUtilities.close(stmt);

      ps = conn.prepareStatement("INSERT INTO foo (description) VALUES (?)");
      for (String value : values) {
        ps.setString(1, value);
        ps.executeUpdate();
      }
      ps = SQLUtilities.close(ps);

      // this time, explicitly roll back
      conn.rollback();

      // now close the connection after rollback
      prev = conn;
      conn = SQLUtilities.close(conn);

      // get a new connection
      conn = pool.acquire();
      assertFalse(conn.getAutoCommit(), "Auto-commit is not false");
      assertFalse((conn == prev), "Got identical proxy connection");

      // query to ensure we rolled back
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT COUNT (*) FROM foo");
      result = rs.getInt(1);
      assertEquals(0, result,
                   "Got more rows than expected: " + result);

      rs = SQLUtilities.close(rs);
      stmt = SQLUtilities.close(stmt);

      ps = conn.prepareStatement("INSERT INTO foo (description) VALUES (?)");
      for (String value : values) {
        ps.setString(1, value);
        ps.executeUpdate();
      }
      ps = SQLUtilities.close(ps);

      // this time we commit the transaction
      conn.commit();

      // now close the connection after commit
      prev = conn;
      conn = SQLUtilities.close(conn);

      // get a new connection
      conn = pool.acquire();
      assertFalse(conn.getAutoCommit(), "Auto-commit is not false");
      assertFalse((conn == prev), "Got identical proxy connection");

      // query to ensure we rolled back
      stmt = conn.createStatement();
      rs = stmt.executeQuery("SELECT COUNT (*) FROM foo");
      result = rs.getInt(1);
      assertEquals(values.length, result,
                   "Got unexpected number of rows: " + result);

      rs = SQLUtilities.close(rs);
      stmt = SQLUtilities.close(stmt);
      conn = SQLUtilities.close(conn);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Received a SQL exception", e);
    } finally {
      rs    = SQLUtilities.close(rs);
      ps    = SQLUtilities.close(ps);
      stmt  = SQLUtilities.close(stmt);
      conn  = SQLUtilities.close(conn);
      if (pool != null) pool.shutdown();

    }
  }
}
