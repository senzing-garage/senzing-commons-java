package com.senzing.sql;

import java.sql.Connection;
import java.sql.SQLException;

import static java.sql.Connection.*;

/**
 * Enumerates the various transaction isolation levels including their
 * corresponding JDBC integer constants.
 */
public enum TransactionIsolation {
  /**
   * Corresponds to {@link Connection#TRANSACTION_READ_UNCOMMITTED}.
   */
  READ_UNCOMMITTED(TRANSACTION_READ_UNCOMMITTED),

  /**
   * Corresponds to {@link Connection#TRANSACTION_READ_COMMITTED}.
   */
  READ_COMMITTED(TRANSACTION_READ_COMMITTED),

  /**
   * Corresponds to {@link Connection#TRANSACTION_REPEATABLE_READ}.
   */
  REPEATABLE_READ(TRANSACTION_REPEATABLE_READ),

  /**
   * Corresponds to {@link Connection#TRANSACTION_SERIALIZABLE}.
   */
  SERIALIZABLE(TRANSACTION_SERIALIZABLE);

  /**
   * Sets the isolation level on the specified {@link Connection} if it is not
   * already set.
   *
   * @param conn The {@link Connection} on which to enforce the transaction
   *             isolation level.
   * @throws SQLException If a failure occurs.
   */
  public void applyTo(Connection conn) throws SQLException {
    int intValue = this.getIntegerValue();
    if (conn.getTransactionIsolation() != intValue) {
      conn.setTransactionIsolation(intValue);
    }
  }

  /**
   * The integer value representation.
   */
  private int intValue;

  /**
   * Return the integer value representation from {@link Connection}.
   *
   * @return The integer value representation from {@link Connection}.
   */
  public int getIntegerValue() {
    return this.intValue;
  }

  /**
   * Constructs with the integer value representation from {@link Connection}.
   *
   * @param intValue The integer value representation.
   */
  TransactionIsolation(int intValue) {
    this.intValue = intValue;
  }
}
