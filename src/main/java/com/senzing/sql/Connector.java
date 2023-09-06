package com.senzing.sql;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for obtaining a new JDBC {@link Connection} to a database.
 * This is used by {@link ConnectionPool} to grow the {@link ConnectionPool}
 * size, but can be used for other purposes.
 */
public interface Connector {
  /**
   * Opens a new JDBC {@link Connection} to a database, hiding the details
   * of how the {@link Connection} is established.  When the caller is done
   * using the {@link Connection}, {@link Connection#close()} should be called.
   *
   * @return The {@link Connection} that was opened.
   *
   * @throws SQLException If a failure occurs.
   */
  Connection openConnection() throws SQLException;
}
