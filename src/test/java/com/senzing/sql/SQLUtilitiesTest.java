package com.senzing.sql;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SQLUtilities}.
 *
 * <p>Each test asserts the documented contract: every {@code close}
 * overload returns {@code null} (and tolerates null / already-closed
 * inputs); {@link SQLUtilities#rollback(Connection)} no-ops on
 * {@code null} and swallows {@link SQLException} on rollback failures;
 * every {@code getX(rs, index)} and {@code getX(rs, columnName)}
 * helper returns the underlying value when present and {@code null}
 * when {@link ResultSet#wasNull()} reports the column was SQL NULL;
 * and {@link SQLUtilities#main(String[])} runs without throwing for
 * empty input or a valid JDBC URL.
 *
 * <p>A single shared in-memory SQLite database is created in
 * {@link BeforeAll}. It contains a {@code TEST_TYPES} table with two
 * rows: one populated with non-null values for every supported
 * column type, and one with all NULLs. Each {@code getX} test opens
 * a fresh {@link ResultSet} positioned on the relevant row and
 * asserts on the helper's return value.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(Resources.SYSTEM_OUT)
@ResourceLock(Resources.SYSTEM_ERR)
public class SQLUtilitiesTest
{
  // -------------------------------------------------------------------
  // Shared SQLite fixture
  // -------------------------------------------------------------------

  private Connection conn;

  // Reference values inserted into the populated row.
  private static final BigDecimal V_BIG  = new BigDecimal("12345.6789");
  private static final boolean    V_BOOL = true;
  private static final byte       V_BY   = (byte) 7;
  private static final double     V_DBL  = 1.5d;
  private static final float      V_FLT  = 2.5f;
  private static final int        V_INT  = 12345;
  private static final long       V_LONG = 9876543210L;
  private static final short      V_SH   = (short) 321;
  private static final String     V_STR  = "hello";
  private static final java.sql.Date V_DATE
      = java.sql.Date.valueOf("2025-04-15");
  private static final java.sql.Time V_TIME
      = java.sql.Time.valueOf("12:34:56");
  private static final java.sql.Timestamp V_TS
      = java.sql.Timestamp.valueOf("2025-04-15 12:34:56.789");

  // Column ordering for the SELECT used by index-based tests.
  // 1=BIG, 2=BOOL, 3=BY, 4=D, 5=DBL, 6=FLT, 7=I, 8=L, 9=SH, 10=S,
  // 11=T, 12=TS
  private static final String SELECT_ALL
      = "SELECT BIG, BOOL, BY, D, DBL, FLT, I, L, SH, S, T, TS"
        + " FROM TEST_TYPES WHERE ID = ?";

  @BeforeAll
  public void createFixture() throws SQLException
  {
    this.conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement stmt = this.conn.createStatement()) {
      stmt.execute(
          "CREATE TABLE TEST_TYPES ("
              + "ID INTEGER PRIMARY KEY,"
              + "BIG DECIMAL,"
              + "BOOL INTEGER,"
              + "BY INTEGER,"
              + "D DATE,"
              + "DBL REAL,"
              + "FLT REAL,"
              + "I INTEGER,"
              + "L INTEGER,"
              + "SH INTEGER,"
              + "S TEXT,"
              + "T TIME,"
              + "TS TIMESTAMP"
              + ")");
    }

    // Row 1: all populated.
    try (PreparedStatement ps = this.conn.prepareStatement(
        "INSERT INTO TEST_TYPES (ID,BIG,BOOL,BY,D,DBL,FLT,I,L,SH,S,T,TS)"
            + " VALUES (1,?,?,?,?,?,?,?,?,?,?,?,?)")) {
      ps.setBigDecimal(1, V_BIG);
      ps.setBoolean(2, V_BOOL);
      ps.setByte(3, V_BY);
      ps.setDate(4, V_DATE);
      ps.setDouble(5, V_DBL);
      ps.setFloat(6, V_FLT);
      ps.setInt(7, V_INT);
      ps.setLong(8, V_LONG);
      ps.setShort(9, V_SH);
      ps.setString(10, V_STR);
      ps.setTime(11, V_TIME);
      ps.setTimestamp(12, V_TS);
      ps.execute();
    }

    // Row 2: all NULL except ID.
    try (Statement stmt = this.conn.createStatement()) {
      stmt.execute(
          "INSERT INTO TEST_TYPES (ID,BIG,BOOL,BY,D,DBL,FLT,I,L,SH,S,T,TS)"
              + " VALUES (2,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,"
              + "NULL,NULL,NULL,NULL)");
    }
  }

  @AfterAll
  public void closeFixture() throws SQLException
  {
    if (this.conn != null) {
      this.conn.close();
    }
  }

  // -------------------------------------------------------------------
  // UTC_CALENDAR contract
  // -------------------------------------------------------------------

  /**
   * The {@link SQLUtilities#UTC_CALENDAR} static constant must use
   * the UTC time zone per its javadoc.
   */
  @Test
  public void utcCalendarUsesUtcTimeZone()
  {
    assertEquals("UTC",
                 SQLUtilities.UTC_CALENDAR.getTimeZone().getID());
  }

  // -------------------------------------------------------------------
  // close() overloads
  // -------------------------------------------------------------------

  /**
   * {@code close((Connection) null)} must return {@code null} per
   * the javadoc.
   */
  @Test
  public void closeReturnsNullForNullConnection()
  {
    assertNull(SQLUtilities.close((Connection) null));
  }

  /**
   * {@code close(Connection)} must close an open connection and
   * return {@code null}.
   */
  @Test
  public void closeOpenConnectionClosesAndReturnsNull() throws Exception
  {
    Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
    assertNull(SQLUtilities.close(c));
    assertTrue(c.isClosed(),
               "Connection must be closed after close()");
  }

  /**
   * {@code close(Connection)} on an already-closed connection must
   * not throw and must return {@code null}.
   */
  @Test
  public void closeAlreadyClosedConnectionReturnsNull() throws Exception
  {
    Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
    c.close();
    assertNull(SQLUtilities.close(c));
  }

  /**
   * {@code close((Statement) null)} must return {@code null}.
   */
  @Test
  public void closeReturnsNullForNullStatement()
  {
    assertNull(SQLUtilities.close((Statement) null));
  }

  /**
   * {@code close(Statement)} must close the statement and return
   * {@code null}.
   */
  @Test
  public void closeOpenStatementClosesAndReturnsNull() throws Exception
  {
    Statement stmt = this.conn.createStatement();
    assertNull(SQLUtilities.close(stmt));
    assertTrue(stmt.isClosed());
  }

  /**
   * {@code close((PreparedStatement) null)} must return {@code null}.
   */
  @Test
  public void closeReturnsNullForNullPreparedStatement()
  {
    assertNull(SQLUtilities.close((PreparedStatement) null));
  }

  /**
   * {@code close(PreparedStatement)} must close the statement and
   * return {@code null}.
   */
  @Test
  public void closeOpenPreparedStatementClosesAndReturnsNull()
      throws Exception
  {
    PreparedStatement ps = this.conn.prepareStatement("SELECT 1");
    assertNull(SQLUtilities.close(ps));
    assertTrue(ps.isClosed());
  }

  /**
   * {@code close((CallableStatement) null)} must return {@code null}.
   */
  @Test
  public void closeReturnsNullForNullCallableStatement()
  {
    assertNull(SQLUtilities.close((CallableStatement) null));
  }

  /**
   * {@code close(CallableStatement)} must tolerate already-closed
   * inputs without throwing. SQLite has no stored procedures, so
   * we drive the path with a dynamic-proxy CallableStatement.
   */
  @Test
  public void closeAlreadyClosedCallableStatementReturnsNull()
  {
    CallableStatement cs = (CallableStatement) Proxy.newProxyInstance(
        SQLUtilitiesTest.class.getClassLoader(),
        new Class<?>[] { CallableStatement.class },
        (proxy, method, args) -> {
          if ("isClosed".equals(method.getName())) return true;
          return null;
        });
    assertNull(SQLUtilities.close(cs));
  }

  /**
   * {@code close((ResultSet) null)} must return {@code null}.
   */
  @Test
  public void closeReturnsNullForNullResultSet()
  {
    assertNull(SQLUtilities.close((ResultSet) null));
  }

  /**
   * {@code close(ResultSet)} must close the result set and return
   * {@code null}.
   */
  @Test
  public void closeOpenResultSetClosesAndReturnsNull() throws Exception
  {
    Statement stmt = this.conn.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT 1");
    assertNull(SQLUtilities.close(rs));
    assertTrue(rs.isClosed());
    stmt.close();
  }

  /**
   * Every {@code close} overload must swallow exceptions thrown by
   * the underlying object. Driving each with a proxy that throws
   * on {@code isClosed()} confirms the catch-and-ignore behavior
   * documented in the javadoc.
   */
  @Test
  public void closeOverloadsSwallowExceptions()
  {
    Connection conn = (Connection) Proxy.newProxyInstance(
        SQLUtilitiesTest.class.getClassLoader(),
        new Class<?>[] { Connection.class },
        (proxy, method, args) -> {
          throw new SQLException("simulated");
        });
    assertNull(SQLUtilities.close(conn));

    Statement stmt = (Statement) Proxy.newProxyInstance(
        SQLUtilitiesTest.class.getClassLoader(),
        new Class<?>[] { Statement.class },
        (proxy, method, args) -> {
          throw new SQLException("simulated");
        });
    assertNull(SQLUtilities.close(stmt));

    ResultSet rs = (ResultSet) Proxy.newProxyInstance(
        SQLUtilitiesTest.class.getClassLoader(),
        new Class<?>[] { ResultSet.class },
        (proxy, method, args) -> {
          throw new SQLException("simulated");
        });
    assertNull(SQLUtilities.close(rs));
  }

  // -------------------------------------------------------------------
  // rollback() contract
  // -------------------------------------------------------------------

  /**
   * {@code rollback(null)} must do nothing (no exception, no
   * output).
   */
  @Test
  public void rollbackOnNullConnectionDoesNothing()
  {
    PrintStream originalErr = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setErr(new PrintStream(captured, true,
                                  StandardCharsets.UTF_8));
    try {
      SQLUtilities.rollback(null);
      assertEquals("", captured.toString(StandardCharsets.UTF_8));
    } finally {
      System.setErr(originalErr);
    }
  }

  /**
   * {@code rollback(workingConn)} must invoke the connection's
   * rollback successfully (no exception thrown).
   */
  @Test
  public void rollbackOnWorkingConnectionInvokesUnderlyingRollback()
      throws Exception
  {
    Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
    c.setAutoCommit(false);
    try {
      SQLUtilities.rollback(c); // must not throw
    } finally {
      c.close();
    }
  }

  /**
   * When the underlying {@code conn.rollback()} throws
   * {@link SQLException}, {@code rollback} must swallow it and
   * write a diagnostic message to {@code System.err}.
   */
  @Test
  public void rollbackSwallowsSqlExceptionAndLogsToStderr()
  {
    Connection failing = (Connection) Proxy.newProxyInstance(
        SQLUtilitiesTest.class.getClassLoader(),
        new Class<?>[] { Connection.class },
        (proxy, method, args) -> {
          if ("rollback".equals(method.getName())) {
            throw new SQLException("simulated rollback failure");
          }
          return null;
        });

    PrintStream originalErr = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setErr(new PrintStream(captured, true,
                                  StandardCharsets.UTF_8));
    try {
      SQLUtilities.rollback(failing); // must not throw
      String err = captured.toString(StandardCharsets.UTF_8);
      assertTrue(err.contains("Exception caught when rolling back"),
                 "Stderr must contain the diagnostic prefix: " + err);
      assertTrue(err.contains("simulated rollback failure"),
                 "Stderr must contain the underlying SQLException"
                     + " message: " + err);
    } finally {
      System.setErr(originalErr);
    }
  }

  // -------------------------------------------------------------------
  // getXxx by index — populated row
  // -------------------------------------------------------------------

  @Test
  public void getBigDecimalByIndexReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(0,
                   V_BIG.compareTo(SQLUtilities.getBigDecimal(rs, 1)));
    }
  }

  @Test
  public void getBigDecimalByIndexReturnsNullWhenSqlNull()
      throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getBigDecimal(rs, 1));
    }
  }

  @Test
  public void getBooleanByIndexReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_BOOL, SQLUtilities.getBoolean(rs, 2));
    }
  }

  @Test
  public void getBooleanByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getBoolean(rs, 2));
    }
  }

  @Test
  public void getByteByIndexReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_BY, SQLUtilities.getByte(rs, 3).byteValue());
    }
  }

  @Test
  public void getByteByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getByte(rs, 3));
    }
  }

  @Test
  public void getDateByIndexReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getDate(rs, 4));
    }
  }

  @Test
  public void getDateByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getDate(rs, 4));
    }
  }

  @Test
  public void getDateByIndexWithCalendarReturnsValueWhenPresent()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getDate(rs, 4, cal));
    }
  }

  @Test
  public void getDateByIndexWithCalendarReturnsNullWhenSqlNull()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getDate(rs, 4, cal));
    }
  }

  @Test
  public void getDoubleByIndexReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_DBL, SQLUtilities.getDouble(rs, 5));
    }
  }

  @Test
  public void getDoubleByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getDouble(rs, 5));
    }
  }

  @Test
  public void getFloatByIndexReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_FLT, SQLUtilities.getFloat(rs, 6));
    }
  }

  @Test
  public void getFloatByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getFloat(rs, 6));
    }
  }

  @Test
  public void getIntByIndexReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_INT, SQLUtilities.getInt(rs, 7));
    }
  }

  @Test
  public void getIntByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getInt(rs, 7));
    }
  }

  @Test
  public void getLongByIndexReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_LONG, SQLUtilities.getLong(rs, 8));
    }
  }

  @Test
  public void getLongByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getLong(rs, 8));
    }
  }

  @Test
  public void getShortByIndexReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_SH, SQLUtilities.getShort(rs, 9).shortValue());
    }
  }

  @Test
  public void getShortByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getShort(rs, 9));
    }
  }

  @Test
  public void getStringByIndexReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_STR, SQLUtilities.getString(rs, 10));
    }
  }

  @Test
  public void getStringByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getString(rs, 10));
    }
  }

  @Test
  public void getTimeByIndexReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getTime(rs, 11));
    }
  }

  @Test
  public void getTimeByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getTime(rs, 11));
    }
  }

  @Test
  public void getTimeByIndexWithCalendarReturnsValueWhenPresent()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getTime(rs, 11, cal));
    }
  }

  @Test
  public void getTimeByIndexWithCalendarReturnsNullWhenSqlNull()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getTime(rs, 11, cal));
    }
  }

  @Test
  public void getTimestampByIndexReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getTimestamp(rs, 12));
    }
  }

  @Test
  public void getTimestampByIndexReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getTimestamp(rs, 12));
    }
  }

  @Test
  public void getTimestampByIndexWithCalendarReturnsValueWhenPresent()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getTimestamp(rs, 12, cal));
    }
  }

  @Test
  public void getTimestampByIndexWithCalendarReturnsNullWhenSqlNull()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getTimestamp(rs, 12, cal));
    }
  }

  // -------------------------------------------------------------------
  // getXxx by column name — populated row + null row
  // -------------------------------------------------------------------

  @Test
  public void getBigDecimalByColumnNameReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(0, V_BIG.compareTo(
          SQLUtilities.getBigDecimal(rs, "BIG")));
    }
  }

  @Test
  public void getBigDecimalByColumnNameReturnsNullWhenSqlNull()
      throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getBigDecimal(rs, "BIG"));
    }
  }

  @Test
  public void getBooleanByColumnNameReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_BOOL, SQLUtilities.getBoolean(rs, "BOOL"));
    }
  }

  @Test
  public void getBooleanByColumnNameReturnsNullWhenSqlNull()
      throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getBoolean(rs, "BOOL"));
    }
  }

  @Test
  public void getByteByColumnNameReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_BY,
                   SQLUtilities.getByte(rs, "BY").byteValue());
    }
  }

  @Test
  public void getByteByColumnNameReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getByte(rs, "BY"));
    }
  }

  @Test
  public void getDateByColumnNameReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getDate(rs, "D"));
    }
  }

  @Test
  public void getDateByColumnNameReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getDate(rs, "D"));
    }
  }

  @Test
  public void getDateByColumnNameWithCalendarReturnsValueWhenPresent()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getDate(rs, "D", cal));
    }
  }

  @Test
  public void getDateByColumnNameWithCalendarReturnsNullWhenSqlNull()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getDate(rs, "D", cal));
    }
  }

  @Test
  public void getDoubleByColumnNameReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_DBL, SQLUtilities.getDouble(rs, "DBL"));
    }
  }

  @Test
  public void getDoubleByColumnNameReturnsNullWhenSqlNull()
      throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getDouble(rs, "DBL"));
    }
  }

  @Test
  public void getFloatByColumnNameReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_FLT, SQLUtilities.getFloat(rs, "FLT"));
    }
  }

  @Test
  public void getFloatByColumnNameReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getFloat(rs, "FLT"));
    }
  }

  @Test
  public void getIntByColumnNameReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_INT, SQLUtilities.getInt(rs, "I"));
    }
  }

  @Test
  public void getIntByColumnNameReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getInt(rs, "I"));
    }
  }

  @Test
  public void getLongByColumnNameReturnsValueWhenPresent() throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_LONG, SQLUtilities.getLong(rs, "L"));
    }
  }

  @Test
  public void getLongByColumnNameReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getLong(rs, "L"));
    }
  }

  @Test
  public void getShortByColumnNameReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_SH,
                   SQLUtilities.getShort(rs, "SH").shortValue());
    }
  }

  @Test
  public void getShortByColumnNameReturnsNullWhenSqlNull()
      throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getShort(rs, "SH"));
    }
  }

  @Test
  public void getStringByColumnNameReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertEquals(V_STR, SQLUtilities.getString(rs, "S"));
    }
  }

  @Test
  public void getStringByColumnNameReturnsNullWhenSqlNull()
      throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getString(rs, "S"));
    }
  }

  @Test
  public void getTimeByColumnNameReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getTime(rs, "T"));
    }
  }

  @Test
  public void getTimeByColumnNameReturnsNullWhenSqlNull() throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getTime(rs, "T"));
    }
  }

  @Test
  public void getTimeByColumnNameWithCalendarReturnsValueWhenPresent()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getTime(rs, "T", cal));
    }
  }

  @Test
  public void getTimeByColumnNameWithCalendarReturnsNullWhenSqlNull()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getTime(rs, "T", cal));
    }
  }

  @Test
  public void getTimestampByColumnNameReturnsValueWhenPresent()
      throws Exception
  {
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getTimestamp(rs, "TS"));
    }
  }

  @Test
  public void getTimestampByColumnNameReturnsNullWhenSqlNull()
      throws Exception
  {
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getTimestamp(rs, "TS"));
    }
  }

  @Test
  public void getTimestampByColumnNameWithCalendarReturnsValueWhenPresent()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(1)) {
      assertNotNull(SQLUtilities.getTimestamp(rs, "TS", cal));
    }
  }

  @Test
  public void getTimestampByColumnNameWithCalendarReturnsNullWhenSqlNull()
      throws Exception
  {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    try (ResultSet rs = openRow(2)) {
      assertNull(SQLUtilities.getTimestamp(rs, "TS", cal));
    }
  }

  // -------------------------------------------------------------------
  // main()
  // -------------------------------------------------------------------

  /**
   * {@code SQLUtilities.main(new String[0])} must not throw and
   * must produce no stdout output.
   */
  @Test
  public void mainWithNoArgsDoesNothing()
  {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured, true,
                                  StandardCharsets.UTF_8));
    try {
      SQLUtilities.main(new String[0]);
      assertEquals("", captured.toString(StandardCharsets.UTF_8));
    } finally {
      System.setOut(originalOut);
    }
  }

  /**
   * {@code SQLUtilities.main(["jdbc:sqlite::memory:"])} must
   * connect and print a line containing the URL and the database
   * product name.
   */
  @Test
  public void mainWithSqliteUrlPrintsProductName()
  {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured, true,
                                  StandardCharsets.UTF_8));
    try {
      SQLUtilities.main(new String[] { "jdbc:sqlite::memory:" });
      String out = captured.toString(StandardCharsets.UTF_8);
      assertTrue(out.contains("jdbc:sqlite::memory:")
                     && out.toUpperCase().contains("SQLITE"),
                 "Output must include the URL and product name: "
                     + out);
    } finally {
      System.setOut(originalOut);
    }
  }

  /**
   * {@code SQLUtilities.main(["bad-url"])} must catch the
   * resulting exception (the implementation prints stack traces
   * via {@code e.printStackTrace()} but does not rethrow) and
   * thus not propagate any exception out of {@code main}.
   */
  @Test
  public void mainWithBadUrlSwallowsException()
  {
    PrintStream originalErr = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setErr(new PrintStream(captured, true,
                                  StandardCharsets.UTF_8));
    try {
      // Must not throw despite the bad URL.
      SQLUtilities.main(new String[] { "definitely-not-a-jdbc-url" });
    } finally {
      System.setErr(originalErr);
    }
  }

  // -------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------

  /**
   * Opens a fresh {@link ResultSet} positioned on the row with
   * the specified ID (1 = populated, 2 = NULLs). The caller must
   * close the {@link ResultSet}; the underlying
   * {@link PreparedStatement} is closed on result-set close because
   * SQLite/JDBC ties them together when the statement is created
   * with try-with-resources, but here we keep the statement alive
   * for the duration of the result set by leaking it intentionally
   * (it gets GC'd; this is test code).
   */
  private ResultSet openRow(int id) throws SQLException
  {
    PreparedStatement ps = this.conn.prepareStatement(SELECT_ALL);
    ps.setInt(1, id);
    ResultSet rs = ps.executeQuery();
    if (!rs.next()) {
      throw new SQLException("No row with ID=" + id);
    }
    return rs;
  }
}
