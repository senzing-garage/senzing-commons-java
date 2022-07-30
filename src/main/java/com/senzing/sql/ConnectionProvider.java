package com.senzing.sql;

import com.senzing.naming.Registry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Abstracts the method by which a JDBC {@link Connection} is obtained.
 * Typically, the {@link Connection} will be obtained from a {@link
 * ConnectionPool}, {@link DriverManager} or a {@link javax.sql.DataSource}).
 * The contract is that once the {@link Connection} is obtained and has been
 * used to perform the required work, that the caller will call {@link
 * Connection#close()} to indicate that the {@link Connection} is no longer
 * needed.
 */
public interface ConnectionProvider {
  /**
   * A simple global registry for non-null {@link ConnectionProvider} instances.
   * This acts as a simplified version of {@link javax.naming.Context} that
   * is dedicated to instances of {@link ConnectionProvider} and confined to an
   * in-memory naming registry.
   *
   * @see Registry
   */
  Registry<ConnectionProvider> REGISTRY = new Registry<>();

  /**
   * Gets the {@link Connection} to use.  Call {@link Connection#close()} when
   * the {@link Connection} is no longer needed.
   *
   * @return The {@link Connection} to use.
   * @throws SQLException If the {@link Connection} could not be obtained due to
   *                      some failure.
   */
  Connection getConnection() throws SQLException;
}
