package com.senzing.sql;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Objects;

/**
 * Provides a simple {@link Connector} implementation for SQLite that provides
 * some sensible default settings on the {@link Connection} after opening it.
 */
public class SQLiteConnector implements Connector {
  /**
   * The <b>unmodifiable</b> {@link List} of {@link String} <code>PRAGMA</code>
   * statements that are run on the {@link Connection} after opening it.  These
   * are as follows:
   * <ol>
   *   <li><code>PRAGMA foreign_keys = ON;</code></li>
   *   <li><code>PRAGMA journal_mode = WAL;</code></li>
   *   <li><code>PRAGMA synchronous = 0;</code></li>
   *   <li><code>PRAGMA secure_delete = 0;</code></li>
   *   <li><code>PRAGMA automatic_index = 0;</code></li>
   * </ol>
   */
  public static final List<String> DEFAULT_PRAGMA_FEATURES_LIST = List.of(
      "PRAGMA foreign_keys = ON;",
      "PRAGMA journal_mode = WAL;",
      "PRAGMA synchronous = 0;",
      "PRAGMA secure_delete = 0;",
      "PRAGMA automatic_index = 0;");

  /**
   * Creates a temporary file and traps any {@link IOException}, rethrowing it
   * as a {@link RuntimeException}.
   *
   * @return The {@link File} object describing the temporary file.
   */
  private static File createTempFile() {
    try {

      File file = File.createTempFile("sqlite-", ".db");
      file.deleteOnExit();
      return file;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Utility method to check for <code>null</code> file path before constructing
   * the {@link File} object to use.
   *
   * @param filePath The non-null file path.
   * @return The {@link File} object for the specified file path.
   */
  private static File newFile(String filePath) {
    Objects.requireNonNull(
        filePath, "The specified file path cannot be null");
    File file = new File(filePath);
    if (file.exists() && file.isDirectory()) {
      throw new IllegalArgumentException(
          "The specified file path exists and is a directory: " + filePath);
    }
    return file;
  }

  /**
   * The SQLite file used by this instance.
   */
  private File sqliteFile = null;

  /**
   * Default constructor using a temporary file that will be deleted upon
   * exit of the executable.
   */
  public SQLiteConnector() {
    this(createTempFile());
  }

  /**
   * Constructs with the specified file name.
   * @param filePath The file path for the SQLite file.
   * @throws NullPointerException If the specified file path is
   *                              <code>null</code>.
   * @throws IllegalArgumentException If the specified file path describes a
   *                                  path to an existing directory.
   */
  public SQLiteConnector(String filePath) {
    this(newFile(filePath));
  }

  /**
   * Constructs with the specified {@link File} to use for the SQLite database
   * file.
   *
   * @param sqliteFile The {@link File} for the SQLite database file.
   */
  public SQLiteConnector(File sqliteFile) {
    Objects.requireNonNull(
        sqliteFile, "The specified file cannot be null");
    this.sqliteFile = sqliteFile;
  }

  /**
   * Gets the {@link File} instance describing the SQLite database file
   * used by this instance.
   *
   * @return The {@link File} instance describing the SQLite database file
   *         used by this instance.
   */
  public File getSqliteFile() {
    return this.sqliteFile;
  }

  /**
   * Uses the {@link DriverManager} to establish a SQLite JDBC {@link
   * Connection} to the file with which this instance was constructed.
   * After establishing the {@link Connection} this {@link Connector} will
   * initialize it with the <code>PRAGMA</code> features defined by the
   * {@link #getPragmaFeatureStatements()}.
   *
   * @return The newly established and initialized {@link Connection}.
   * @throws SQLException If a JDBC failure occurs.
   */
  @Override
  public Connection openConnection() throws SQLException {
    String jdbcUrl = "jdbc:sqlite:" + this.sqliteFile.getPath();

    Connection conn = null;

    // create a statement
    Statement statement = null;
    try {
      conn = DriverManager.getConnection(jdbcUrl);
      statement = conn.createStatement();

      // initialize the connection before use
      for (String sql : this.getPragmaFeatureStatements()) {
        statement.execute(sql);
      }

    } catch (SQLException e) {
      statement = SQLUtilities.close(statement);
      conn = SQLUtilities.close(conn);
      throw e;

    } finally {
      statement = SQLUtilities.close(statement);
    }

    // set auto-commit to false
    conn.setAutoCommit(false);

    // return the connection
    return conn;
  }

  /**
   * Gets the {@link List} of the SQL statements to run to initialize a
   * SQLite session after opening a {@link Connection}.
   *
   * @return The {@link List} of {@link String} SQL statements to run on a
   *         newly established SQLite JDBC {@link Connection}.
   */
  protected List<String> getPragmaFeatureStatements() {
    return DEFAULT_PRAGMA_FEATURES_LIST;
  }
}
