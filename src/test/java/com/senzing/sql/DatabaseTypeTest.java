package com.senzing.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static com.senzing.sql.DatabaseType.DB2;
import static com.senzing.sql.DatabaseType.GENERIC;
import static com.senzing.sql.DatabaseType.MYSQL;
import static com.senzing.sql.DatabaseType.ORACLE;
import static com.senzing.sql.DatabaseType.POSTGRESQL;
import static com.senzing.sql.DatabaseType.SQLITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DatabaseType}.
 *
 * <p>Each test asserts the documented contract from
 * {@link DatabaseType}'s javadoc: every {@code @throws} clause is
 * exercised, every {@code @return} branch is verified, and the
 * SQLite-specific behaviors of {@link DatabaseType#getTimestampBindingSQL()},
 * {@link DatabaseType#setTimestamp(PreparedStatement, int, Timestamp)},
 * {@link DatabaseType#setTimestamp(CallableStatement, int, Timestamp)},
 * {@link DatabaseType#sqlLeast(String, String, String...)}, and
 * {@link DatabaseType#sqlGreatest(String, String, String...)} are
 * exercised against both the SQLite path and the default path.
 *
 * <p>For statement-level binding the tests use dynamic-proxy
 * {@link PreparedStatement} / {@link CallableStatement} instances that
 * record their invocations, so the assertions describe the documented
 * behavior precisely (which method is called, with which arguments)
 * rather than relying on a real database round-trip.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class DatabaseTypeTest
{
  // -------------------------------------------------------------------
  // Enum value count
  // -------------------------------------------------------------------

  /**
   * The enum must declare exactly six constants per its javadoc:
   * {@code GENERIC}, {@code SQLITE}, {@code POSTGRESQL}, {@code MYSQL},
   * {@code ORACLE}, {@code DB2}.
   */
  @Test
  public void enumDeclaresSixConstants()
  {
    assertEquals(6, DatabaseType.values().length,
                 "DatabaseType must declare exactly 6 constants");
  }

  // -------------------------------------------------------------------
  // detect(Connection) contract
  // -------------------------------------------------------------------

  /**
   * {@code detect(null)} must return {@code null} per the javadoc.
   */
  @Test
  public void detectReturnsNullForNullConnection() throws SQLException
  {
    assertNull(DatabaseType.detect(null));
  }

  /**
   * {@code detect} on a real SQLite connection must return
   * {@link DatabaseType#SQLITE}.
   */
  @Test
  public void detectReturnsSqliteForSqliteConnection() throws SQLException
  {
    try (Connection conn = openSqliteConnection()) {
      assertSame(SQLITE, DatabaseType.detect(conn));
    }
  }

  /**
   * {@code detect} on a connection whose product name is
   * "PostgreSQL" must return {@link DatabaseType#POSTGRESQL}. The
   * match is case-insensitive per the {@code toUpperCase()} call in
   * the implementation.
   */
  @Test
  public void detectReturnsPostgresForPostgresProductName()
      throws SQLException
  {
    Connection conn = connectionWithProductName("PostgreSQL");
    assertSame(POSTGRESQL, DatabaseType.detect(conn));
  }

  /**
   * {@code detect} on a connection whose product name is "MySQL"
   * must return {@link DatabaseType#MYSQL}.
   */
  @Test
  public void detectReturnsMysqlForMysqlProductName()
      throws SQLException
  {
    Connection conn = connectionWithProductName("MySQL");
    assertSame(MYSQL, DatabaseType.detect(conn));
  }

  /**
   * {@code detect} on a connection whose product name is "Oracle"
   * must return {@link DatabaseType#ORACLE}.
   */
  @Test
  public void detectReturnsOracleForOracleProductName()
      throws SQLException
  {
    Connection conn = connectionWithProductName("Oracle");
    assertSame(ORACLE, DatabaseType.detect(conn));
  }

  /**
   * {@code detect} on a connection whose product name is "DB2"
   * must return {@link DatabaseType#DB2}.
   */
  @Test
  public void detectReturnsDb2ForDb2ProductName() throws SQLException
  {
    Connection conn = connectionWithProductName("DB2");
    assertSame(DB2, DatabaseType.detect(conn));
  }

  /**
   * {@code detect} on a connection whose product name is not
   * recognized must return {@link DatabaseType#GENERIC} per the
   * javadoc.
   */
  @Test
  public void detectReturnsGenericForUnknownProductName()
      throws SQLException
  {
    Connection conn = connectionWithProductName("WeirdNewDatabase");
    assertSame(GENERIC, DatabaseType.detect(conn));
  }

  /**
   * {@code detect} must propagate {@link SQLException} from the
   * underlying driver, per the javadoc.
   */
  @Test
  public void detectPropagatesSqlException()
  {
    Connection conn = (Connection) Proxy.newProxyInstance(
        DatabaseType.class.getClassLoader(),
        new Class<?>[] { Connection.class },
        (proxy, method, args) -> {
          if ("getMetaData".equals(method.getName())) {
            throw new SQLException("simulated");
          }
          return null;
        });
    assertThrows(SQLException.class, () -> DatabaseType.detect(conn));
  }

  // -------------------------------------------------------------------
  // getTimestampBindingSQL() contract
  // -------------------------------------------------------------------

  /**
   * For {@link DatabaseType#SQLITE} the binding SQL must be the
   * STRFTIME-based format documented in the implementation.
   */
  @Test
  public void getTimestampBindingSqlForSqliteUsesStrftime()
  {
    assertEquals("(STRFTIME('%Y-%m-%d %H:%M:%f', ?))",
                 SQLITE.getTimestampBindingSQL());
  }

  /**
   * For every non-SQLite database type the binding SQL must be the
   * default {@code "?"} placeholder.
   */
  @Test
  public void getTimestampBindingSqlForNonSqliteIsDefaultPlaceholder()
  {
    for (DatabaseType type : DatabaseType.values()) {
      if (type == SQLITE) continue;
      assertEquals("?", type.getTimestampBindingSQL(),
                   type + " must use default '?' binding SQL");
    }
  }

  // -------------------------------------------------------------------
  // setTimestamp(PreparedStatement, ...) contract
  // -------------------------------------------------------------------

  /**
   * {@code SQLITE.setTimestamp(ps, ...)} must call
   * {@link PreparedStatement#setString(int, String)} with a UTC
   * formatted date-time string, and must NOT call
   * {@link PreparedStatement#setTimestamp}.
   */
  @Test
  public void setTimestampForSqlitePreparedStatementCallsSetString()
      throws SQLException
  {
    Recorder recorder = new Recorder();
    PreparedStatement ps = preparedStatementProxy(recorder);
    Timestamp ts = Timestamp.valueOf("2025-04-15 12:34:56.789");

    SQLITE.setTimestamp(ps, 1, ts);

    assertEquals(1, recorder.calls.size(),
                 "Exactly one binding call expected for SQLITE");
    Call c = recorder.calls.get(0);
    assertEquals("setString", c.method);
    assertEquals(1, c.args[0], "Index argument must be 1");
    assertTrue(((String) c.args[1]).startsWith("2025-04-15 "),
               "Bound string must start with the UTC date prefix");
  }

  /**
   * {@code POSTGRESQL.setTimestamp(ps, ...)} (and every other
   * non-SQLite type) must call
   * {@link PreparedStatement#setTimestamp(int, Timestamp, java.util.Calendar)}
   * directly with the UTC calendar.
   */
  @Test
  public void setTimestampForNonSqlitePreparedStatementCallsSetTimestamp()
      throws SQLException
  {
    Recorder recorder = new Recorder();
    PreparedStatement ps = preparedStatementProxy(recorder);
    Timestamp ts = Timestamp.valueOf("2025-04-15 12:34:56.789");

    POSTGRESQL.setTimestamp(ps, 2, ts);

    assertEquals(1, recorder.calls.size(),
                 "Exactly one binding call expected for POSTGRESQL");
    Call c = recorder.calls.get(0);
    assertEquals("setTimestamp", c.method);
    assertEquals(2, c.args[0]);
    assertSame(ts, c.args[1]);
  }

  // -------------------------------------------------------------------
  // setTimestamp(CallableStatement, ...) contract
  // -------------------------------------------------------------------

  /**
   * {@code SQLITE.setTimestamp(cs, ...)} on a {@link CallableStatement}
   * must call {@link CallableStatement#setString(int, String)} with a
   * UTC formatted date-time string, and must NOT also call
   * {@link CallableStatement#setTimestamp} (mirroring the
   * {@link PreparedStatement} overload's behavior).
   */
  @Test
  public void setTimestampForSqliteCallableStatementCallsSetString()
      throws SQLException
  {
    Recorder recorder = new Recorder();
    CallableStatement cs = callableStatementProxy(recorder);
    Timestamp ts = Timestamp.valueOf("2025-04-15 12:34:56.789");

    SQLITE.setTimestamp(cs, 1, ts);

    assertEquals(1, recorder.calls.size(),
                 "Exactly one binding call expected for SQLITE on"
                     + " CallableStatement");
    assertEquals("setString", recorder.calls.get(0).method);
  }

  /**
   * {@code POSTGRESQL.setTimestamp(cs, ...)} on a
   * {@link CallableStatement} must call
   * {@link CallableStatement#setTimestamp(int, Timestamp,
   * java.util.Calendar)} directly.
   */
  @Test
  public void setTimestampForNonSqliteCallableStatementCallsSetTimestamp()
      throws SQLException
  {
    Recorder recorder = new Recorder();
    CallableStatement cs = callableStatementProxy(recorder);
    Timestamp ts = Timestamp.valueOf("2025-04-15 12:34:56.789");

    POSTGRESQL.setTimestamp(cs, 3, ts);

    assertEquals(1, recorder.calls.size());
    Call c = recorder.calls.get(0);
    assertEquals("setTimestamp", c.method);
    assertEquals(3, c.args[0]);
    assertSame(ts, c.args[1]);
  }

  // -------------------------------------------------------------------
  // sqlLeast() contract
  // -------------------------------------------------------------------

  /**
   * {@code sqlLeast(null, second)} must throw
   * {@link NullPointerException} per the javadoc.
   */
  @Test
  public void sqlLeastThrowsNpeForNullFirst()
  {
    assertThrows(NullPointerException.class,
                 () -> POSTGRESQL.sqlLeast(null, "b"));
  }

  /**
   * {@code sqlLeast(first, null)} must throw
   * {@link NullPointerException} per the javadoc
   * ({@code @throws NullPointerException If either the first or
   * second parameters is null.}).
   */
  @Test
  public void sqlLeastThrowsNpeForNullSecond()
  {
    assertThrows(NullPointerException.class,
                 () -> POSTGRESQL.sqlLeast("a", null));
  }

  /**
   * For {@link DatabaseType#SQLITE}, {@code sqlLeast} must produce
   * {@code MIN(...)} since SQLite lacks a {@code LEAST} function.
   */
  @Test
  public void sqlLeastForSqliteUsesMin()
  {
    assertEquals("MIN(a, b)", SQLITE.sqlLeast("a", "b"));
  }

  /**
   * For non-SQLite database types, {@code sqlLeast} must produce
   * {@code LEAST(...)}.
   */
  @Test
  public void sqlLeastForOthersUsesLeast()
  {
    assertEquals("LEAST(a, b)", POSTGRESQL.sqlLeast("a", "b"));
    assertEquals("LEAST(a, b)", MYSQL.sqlLeast("a", "b"));
    assertEquals("LEAST(a, b)", GENERIC.sqlLeast("a", "b"));
  }

  /**
   * {@code sqlLeast} must include the variadic {@code other}
   * parameters in the formatted SQL.
   */
  @Test
  public void sqlLeastIncludesVarargsOtherParameters()
  {
    assertEquals("MIN(a, b, c, d)", SQLITE.sqlLeast("a", "b", "c", "d"));
    assertEquals("LEAST(a, b, c)", POSTGRESQL.sqlLeast("a", "b", "c"));
  }

  /**
   * A {@code null} {@code other} array must be tolerated (no NPE)
   * since the implementation explicitly null-checks it.
   */
  @Test
  public void sqlLeastTolerantsNullOtherArray()
  {
    assertEquals("MIN(a, b)",
                 SQLITE.sqlLeast("a", "b", (String[]) null));
  }

  // -------------------------------------------------------------------
  // sqlGreatest() contract
  // -------------------------------------------------------------------

  /**
   * {@code sqlGreatest(null, second)} must throw
   * {@link NullPointerException} per the javadoc.
   */
  @Test
  public void sqlGreatestThrowsNpeForNullFirst()
  {
    assertThrows(NullPointerException.class,
                 () -> POSTGRESQL.sqlGreatest(null, "b"));
  }

  /**
   * {@code sqlGreatest(first, null)} must throw
   * {@link NullPointerException} per the javadoc.
   */
  @Test
  public void sqlGreatestThrowsNpeForNullSecond()
  {
    assertThrows(NullPointerException.class,
                 () -> POSTGRESQL.sqlGreatest("a", null));
  }

  /**
   * For {@link DatabaseType#SQLITE}, {@code sqlGreatest} must produce
   * {@code MAX(...)} since SQLite lacks a {@code GREATEST} function.
   */
  @Test
  public void sqlGreatestForSqliteUsesMax()
  {
    assertEquals("MAX(a, b)", SQLITE.sqlGreatest("a", "b"));
  }

  /**
   * For non-SQLite database types, {@code sqlGreatest} must produce
   * {@code GREATEST(...)}.
   */
  @Test
  public void sqlGreatestForOthersUsesGreatest()
  {
    assertEquals("GREATEST(a, b)", POSTGRESQL.sqlGreatest("a", "b"));
    assertEquals("GREATEST(a, b)", MYSQL.sqlGreatest("a", "b"));
    assertEquals("GREATEST(a, b)", GENERIC.sqlGreatest("a", "b"));
  }

  /**
   * {@code sqlGreatest} must include the variadic {@code other}
   * parameters in the formatted SQL.
   */
  @Test
  public void sqlGreatestIncludesVarargsOtherParameters()
  {
    assertEquals("MAX(a, b, c, d)",
                 SQLITE.sqlGreatest("a", "b", "c", "d"));
    assertEquals("GREATEST(a, b, c)",
                 POSTGRESQL.sqlGreatest("a", "b", "c"));
  }

  // -------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------

  private static Connection openSqliteConnection() throws SQLException
  {
    return DriverManager.getConnection("jdbc:sqlite::memory:");
  }

  /**
   * Returns a dynamic-proxy {@link Connection} whose
   * {@link DatabaseMetaData#getDatabaseProductName()} returns the
   * specified product name. All other methods return null.
   */
  private static Connection connectionWithProductName(String productName)
  {
    DatabaseMetaData md = (DatabaseMetaData) Proxy.newProxyInstance(
        DatabaseTypeTest.class.getClassLoader(),
        new Class<?>[] { DatabaseMetaData.class },
        (proxy, method, args) -> {
          if ("getDatabaseProductName".equals(method.getName())) {
            return productName;
          }
          return null;
        });
    return (Connection) Proxy.newProxyInstance(
        DatabaseTypeTest.class.getClassLoader(),
        new Class<?>[] { Connection.class },
        (proxy, method, args) -> {
          if ("getMetaData".equals(method.getName())) {
            return md;
          }
          return null;
        });
  }

  /** A single recorded invocation. */
  private static final class Call
  {
    final String   method;
    final Object[] args;

    Call(String method, Object[] args)
    {
      this.method = method;
      this.args   = (args == null) ? new Object[0] : args;
    }
  }

  /** Records every invoked method/args pair on the proxy. */
  private static final class Recorder implements InvocationHandler
  {
    final List<Call> calls = new ArrayList<>();

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
    {
      this.calls.add(new Call(method.getName(), args));
      return null;
    }
  }

  private static PreparedStatement preparedStatementProxy(Recorder r)
  {
    return (PreparedStatement) Proxy.newProxyInstance(
        DatabaseTypeTest.class.getClassLoader(),
        new Class<?>[] { PreparedStatement.class },
        r);
  }

  private static CallableStatement callableStatementProxy(Recorder r)
  {
    return (CallableStatement) Proxy.newProxyInstance(
        DatabaseTypeTest.class.getClassLoader(),
        new Class<?>[] { CallableStatement.class },
        r);
  }
}
