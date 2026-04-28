package com.senzing.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.senzing.sql.TransactionIsolation.READ_COMMITTED;
import static com.senzing.sql.TransactionIsolation.READ_UNCOMMITTED;
import static com.senzing.sql.TransactionIsolation.REPEATABLE_READ;
import static com.senzing.sql.TransactionIsolation.SERIALIZABLE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link TransactionIsolation}.
 *
 * <p>Each test asserts the documented contract from the enum's
 * javadoc: the four enum values map to their corresponding JDBC
 * {@link Connection} integer constants, {@link
 * TransactionIsolation#applyTo(Connection)} sets the isolation level
 * on the connection (when different from the current setting),
 * and {@link SQLException} propagates from the underlying driver.
 *
 * <p>Uses an in-memory SQLite connection for the happy-path tests
 * (the xerial sqlite-jdbc driver supports
 * {@link Connection#TRANSACTION_SERIALIZABLE} and
 * {@link Connection#TRANSACTION_READ_UNCOMMITTED}), and a dynamic
 * proxy {@link Connection} for the {@link SQLException} propagation
 * test so the assertion is deterministic regardless of driver
 * behavior.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class TransactionIsolationTest
{
  // -------------------------------------------------------------------
  // Enum value count
  // -------------------------------------------------------------------

  /**
   * The enum must declare exactly four constants per its javadoc:
   * {@code READ_UNCOMMITTED}, {@code READ_COMMITTED},
   * {@code REPEATABLE_READ}, {@code SERIALIZABLE}.
   */
  @Test
  public void enumDeclaresFourConstants()
  {
    assertEquals(4, TransactionIsolation.values().length,
                 "TransactionIsolation must declare exactly 4 constants");
  }

  // -------------------------------------------------------------------
  // getIntegerValue() — must match the JDBC TRANSACTION_* constants
  // -------------------------------------------------------------------

  /**
   * {@code READ_UNCOMMITTED.getIntegerValue()} must equal
   * {@link Connection#TRANSACTION_READ_UNCOMMITTED}.
   */
  @Test
  public void readUncommittedMapsToJdbcConstant()
  {
    assertEquals(TRANSACTION_READ_UNCOMMITTED,
                 READ_UNCOMMITTED.getIntegerValue());
  }

  /**
   * {@code READ_COMMITTED.getIntegerValue()} must equal
   * {@link Connection#TRANSACTION_READ_COMMITTED}.
   */
  @Test
  public void readCommittedMapsToJdbcConstant()
  {
    assertEquals(TRANSACTION_READ_COMMITTED,
                 READ_COMMITTED.getIntegerValue());
  }

  /**
   * {@code REPEATABLE_READ.getIntegerValue()} must equal
   * {@link Connection#TRANSACTION_REPEATABLE_READ}.
   */
  @Test
  public void repeatableReadMapsToJdbcConstant()
  {
    assertEquals(TRANSACTION_REPEATABLE_READ,
                 REPEATABLE_READ.getIntegerValue());
  }

  /**
   * {@code SERIALIZABLE.getIntegerValue()} must equal
   * {@link Connection#TRANSACTION_SERIALIZABLE}.
   */
  @Test
  public void serializableMapsToJdbcConstant()
  {
    assertEquals(TRANSACTION_SERIALIZABLE,
                 SERIALIZABLE.getIntegerValue());
  }

  // -------------------------------------------------------------------
  // applyTo(Connection) contract
  // -------------------------------------------------------------------

  /**
   * {@link TransactionIsolation#applyTo(Connection)} must set the
   * connection's transaction isolation when it differs from the
   * current value.
   */
  @Test
  public void applyToChangesIsolationWhenDifferent() throws Exception
  {
    try (Connection conn = openSqliteConnection()) {
      int initial = conn.getTransactionIsolation();
      TransactionIsolation target =
          (initial == TRANSACTION_SERIALIZABLE)
              ? READ_UNCOMMITTED : SERIALIZABLE;
      target.applyTo(conn);
      assertEquals(target.getIntegerValue(),
                   conn.getTransactionIsolation(),
                   "applyTo must change isolation to the target value");
    }
  }

  /**
   * {@link TransactionIsolation#applyTo(Connection)} must be a no-op
   * (no exception, level unchanged) when the connection is already at
   * the target isolation level.
   */
  @Test
  public void applyToIsNoOpWhenAlreadySet() throws Exception
  {
    try (Connection conn = openSqliteConnection()) {
      conn.setTransactionIsolation(TRANSACTION_READ_UNCOMMITTED);
      READ_UNCOMMITTED.applyTo(conn);
      assertEquals(TRANSACTION_READ_UNCOMMITTED,
                   conn.getTransactionIsolation(),
                   "applyTo with matching level must leave isolation"
                       + " unchanged");
    }
  }

  /**
   * {@link TransactionIsolation#applyTo(Connection)} must propagate
   * {@link SQLException} from the underlying driver. Drives this with
   * a proxy {@link Connection} that throws on
   * {@code setTransactionIsolation}.
   */
  @Test
  public void applyToPropagatesSqlException()
  {
    Connection throwing = throwingConnection();
    assertThrows(SQLException.class,
                 () -> READ_UNCOMMITTED.applyTo(throwing));
  }

  // -------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------

  private static Connection openSqliteConnection() throws SQLException
  {
    return DriverManager.getConnection("jdbc:sqlite::memory:");
  }

  /**
   * Returns a dynamic-proxy {@link Connection} where
   * {@code getTransactionIsolation()} reports a value distinct from
   * every {@link TransactionIsolation} constant (so {@code applyTo}
   * always takes the "different — call setter" branch) and
   * {@code setTransactionIsolation(int)} throws {@link SQLException}.
   */
  private static Connection throwingConnection()
  {
    return (Connection) Proxy.newProxyInstance(
        TransactionIsolation.class.getClassLoader(),
        new Class<?>[] { Connection.class },
        (proxy, method, args) -> {
          String name = method.getName();
          if ("getTransactionIsolation".equals(name)) {
            return Integer.MIN_VALUE;
          }
          if ("setTransactionIsolation".equals(name)) {
            throw new SQLException("simulated driver failure");
          }
          return null;
        });
  }
}
