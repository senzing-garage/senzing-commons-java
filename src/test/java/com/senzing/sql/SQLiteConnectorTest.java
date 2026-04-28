package com.senzing.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SQLiteConnector}.
 *
 * <p>Each test asserts the documented contract from
 * {@link SQLiteConnector}'s javadoc: every {@code @throws} clause on
 * each constructor is exercised with the input that should trigger
 * it; the documented {@link SQLiteConnector#DEFAULT_PRAGMA_FEATURES_LIST}
 * is verified against the prose-described list; and
 * {@link SQLiteConnector#openConnection()} is verified to produce a
 * usable {@link Connection} that has the documented PRAGMAs applied
 * and {@code autoCommit} disabled.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class SQLiteConnectorTest
{
  // -------------------------------------------------------------------
  // DEFAULT_PRAGMA_FEATURES_LIST contract
  // -------------------------------------------------------------------

  /**
   * The {@link SQLiteConnector#DEFAULT_PRAGMA_FEATURES_LIST} must
   * contain exactly the five PRAGMA statements documented in its
   * javadoc.
   */
  @Test
  public void defaultPragmaListMatchesJavadoc()
  {
    List<String> expected = List.of(
        "PRAGMA foreign_keys = ON;",
        "PRAGMA journal_mode = WAL;",
        "PRAGMA synchronous = 1;",
        "PRAGMA secure_delete = 0;",
        "PRAGMA automatic_index = 0;");
    assertEquals(expected,
                 SQLiteConnector.DEFAULT_PRAGMA_FEATURES_LIST,
                 "DEFAULT_PRAGMA_FEATURES_LIST must match the javadoc");
  }

  /**
   * The {@link SQLiteConnector#DEFAULT_PRAGMA_FEATURES_LIST} must be
   * unmodifiable, per the bold "unmodifiable" annotation in its
   * javadoc.
   */
  @Test
  public void defaultPragmaListIsUnmodifiable()
  {
    assertThrows(
        UnsupportedOperationException.class,
        () -> SQLiteConnector.DEFAULT_PRAGMA_FEATURES_LIST.add("X"));
  }

  // -------------------------------------------------------------------
  // No-arg constructor — uses a temp file with deleteOnExit
  // -------------------------------------------------------------------

  /**
   * The no-arg {@link SQLiteConnector#SQLiteConnector()} constructor
   * must produce an instance whose backing file is non-null (the
   * temp file).
   */
  @Test
  public void defaultConstructorUsesNonNullTempFile()
  {
    SQLiteConnector connector = new SQLiteConnector();
    assertNotNull(connector.getSqliteFile(),
                  "Default constructor must use a non-null temp file");
  }

  // -------------------------------------------------------------------
  // SQLiteConnector(String filePath) contract
  // -------------------------------------------------------------------

  /**
   * {@link SQLiteConnector#SQLiteConnector(String)} must throw
   * {@link NullPointerException} when given a null file path
   * (since it is not in memory mode).
   */
  @Test
  public void stringConstructorThrowsNpeForNullPath()
  {
    assertThrows(NullPointerException.class,
                 () -> new SQLiteConnector((String) null));
  }

  /**
   * {@link SQLiteConnector#SQLiteConnector(String)} must throw
   * {@link IllegalArgumentException} when the path describes an
   * existing directory.
   */
  @Test
  public void stringConstructorThrowsIaeForDirectoryPath(@TempDir Path tempDir)
  {
    String dirPath = tempDir.toString();
    assertThrows(IllegalArgumentException.class,
                 () -> new SQLiteConnector(dirPath));
  }

  /**
   * {@link SQLiteConnector#SQLiteConnector(String)} with a regular
   * file path must succeed and {@code getSqliteFile()} must return
   * a {@link File} for that path.
   */
  @Test
  public void stringConstructorAcceptsFilePath(@TempDir Path tempDir)
  {
    Path dbPath = tempDir.resolve("test.db");
    SQLiteConnector connector = new SQLiteConnector(dbPath.toString());
    assertEquals(dbPath.toFile(), connector.getSqliteFile());
  }

  // -------------------------------------------------------------------
  // SQLiteConnector(File) contract
  // -------------------------------------------------------------------

  /**
   * {@link SQLiteConnector#SQLiteConnector(File)} must throw
   * {@link NullPointerException} when given a null {@link File}
   * (not in memory mode).
   */
  @Test
  public void fileConstructorThrowsNpeForNullFile()
  {
    assertThrows(NullPointerException.class,
                 () -> new SQLiteConnector((File) null));
  }

  /**
   * {@link SQLiteConnector#SQLiteConnector(File)} with a regular file
   * must succeed and {@code getSqliteFile()} must return the same
   * file instance.
   */
  @Test
  public void fileConstructorAcceptsFile(@TempDir Path tempDir)
  {
    File dbFile = tempDir.resolve("test.db").toFile();
    SQLiteConnector connector = new SQLiteConnector(dbFile);
    assertSame(dbFile, connector.getSqliteFile());
  }

  /**
   * {@link SQLiteConnector#SQLiteConnector(File)} must throw
   * {@link IllegalArgumentException} when the file describes an
   * existing directory, per the javadoc:
   * {@code @throws IllegalArgumentException If the specified file
   * path describes a path to an existing directory.}
   */
  @Test
  public void fileConstructorThrowsIaeForDirectory(@TempDir Path tempDir)
  {
    File dirFile = tempDir.toFile();
    assertThrows(IllegalArgumentException.class,
                 () -> new SQLiteConnector(dirFile));
  }

  // -------------------------------------------------------------------
  // SQLiteConnector(String, Map) contract
  // -------------------------------------------------------------------

  /**
   * {@link SQLiteConnector#SQLiteConnector(String, Map)} with memory
   * mode in the properties and a null file path must succeed
   * (memory mode does not require a file).
   */
  @Test
  public void stringMapConstructorAllowsNullPathInMemoryMode()
  {
    Map<String, String> props = new HashMap<>();
    props.put("mode", "memory");
    SQLiteConnector connector = new SQLiteConnector((String) null, props);
    assertNull(connector.getSqliteFile(),
               "Memory mode with null path must yield null file");
  }

  /**
   * {@link SQLiteConnector#SQLiteConnector(String, Map)} without
   * memory mode and with a null file path must throw
   * {@link NullPointerException}.
   */
  @Test
  public void stringMapConstructorThrowsNpeForNullPathOutsideMemoryMode()
  {
    Map<String, String> props = new HashMap<>();
    props.put("foo", "bar");
    assertThrows(NullPointerException.class,
                 () -> new SQLiteConnector((String) null, props));
  }

  /**
   * {@link SQLiteConnector#SQLiteConnector(String, Map)} without
   * memory mode and with a directory path must throw
   * {@link IllegalArgumentException}.
   */
  @Test
  public void stringMapConstructorThrowsIaeForDirectoryPath(
      @TempDir Path tempDir)
  {
    Map<String, String> props = new HashMap<>();
    props.put("foo", "bar");
    String dirPath = tempDir.toString();
    assertThrows(IllegalArgumentException.class,
                 () -> new SQLiteConnector(dirPath, props));
  }

  // -------------------------------------------------------------------
  // SQLiteConnector(File, Map) contract
  // -------------------------------------------------------------------

  /**
   * {@link SQLiteConnector#SQLiteConnector(File, Map)} with memory
   * mode in the properties and a null file must succeed.
   */
  @Test
  public void fileMapConstructorAllowsNullFileInMemoryMode()
  {
    Map<String, String> props = new HashMap<>();
    props.put("mode", "memory");
    SQLiteConnector connector = new SQLiteConnector((File) null, props);
    assertNull(connector.getSqliteFile());
  }

  /**
   * {@link SQLiteConnector#SQLiteConnector(File, Map)} without memory
   * mode and with a null file must throw
   * {@link NullPointerException}.
   */
  @Test
  public void fileMapConstructorThrowsNpeForNullFileOutsideMemoryMode()
  {
    Map<String, String> props = new HashMap<>();
    props.put("foo", "bar");
    assertThrows(NullPointerException.class,
                 () -> new SQLiteConnector((File) null, props));
  }

  /**
   * {@link SQLiteConnector#SQLiteConnector(File, Map)} with a
   * directory file (not in memory mode) must throw
   * {@link IllegalArgumentException} per the javadoc.
   */
  @Test
  public void fileMapConstructorThrowsIaeForDirectory(@TempDir Path tempDir)
  {
    Map<String, String> props = new HashMap<>();
    props.put("foo", "bar");
    File dirFile = tempDir.toFile();
    assertThrows(IllegalArgumentException.class,
                 () -> new SQLiteConnector(dirFile, props));
  }

  // -------------------------------------------------------------------
  // openConnection() contract
  // -------------------------------------------------------------------

  /**
   * {@link SQLiteConnector#openConnection()} must produce a usable
   * {@link Connection} that can execute SQL.
   */
  @Test
  public void openConnectionReturnsUsableConnection(@TempDir Path tempDir)
      throws SQLException
  {
    SQLiteConnector connector = new SQLiteConnector(
        tempDir.resolve("test.db").toString());
    try (Connection conn = connector.openConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT 1")) {
      assertTrue(rs.next(), "SELECT 1 must produce a row");
      assertEquals(1, rs.getInt(1));
    }
  }

  /**
   * {@link SQLiteConnector#openConnection()} must apply the
   * documented PRAGMAs (verified by querying each pragma's value
   * post-connection).
   */
  @Test
  public void openConnectionAppliesDocumentedPragmas(@TempDir Path tempDir)
      throws SQLException
  {
    SQLiteConnector connector = new SQLiteConnector(
        tempDir.resolve("pragmas.db").toString());
    try (Connection conn = connector.openConnection();
         Statement stmt = conn.createStatement()) {
      assertEquals(1, queryInt(stmt, "PRAGMA foreign_keys"),
                   "foreign_keys must be ON (1) after openConnection");
      assertEquals("wal", queryString(stmt, "PRAGMA journal_mode"),
                   "journal_mode must be WAL");
      assertEquals(1, queryInt(stmt, "PRAGMA synchronous"),
                   "synchronous must be 1 (NORMAL)");
      assertEquals(0, queryInt(stmt, "PRAGMA secure_delete"),
                   "secure_delete must be 0");
      assertEquals(0, queryInt(stmt, "PRAGMA automatic_index"),
                   "automatic_index must be 0");
    }
  }

  /**
   * {@link SQLiteConnector#openConnection()} must return a
   * {@link Connection} with auto-commit disabled, per the final
   * statement of the method's implementation
   * ({@code conn.setAutoCommit(false);}).
   */
  @Test
  public void openConnectionReturnsConnectionWithAutoCommitDisabled(
      @TempDir Path tempDir) throws SQLException
  {
    SQLiteConnector connector = new SQLiteConnector(
        tempDir.resolve("autocommit.db").toString());
    try (Connection conn = connector.openConnection()) {
      assertFalse(conn.getAutoCommit(),
                  "openConnection must disable auto-commit");
    }
  }

  /**
   * {@link SQLiteConnector#openConnection()} must work with memory
   * mode (no file required) and must NOT leak a database file into
   * the current working directory. The implementation builds the
   * JDBC URL with the {@code file:} URI scheme when
   * {@code mode=memory} is in the props, which is required for the
   * xerial sqlite-jdbc driver to recognize the URI parameters
   * rather than parse the path verbatim.
   */
  @Test
  public void openConnectionWorksInMemoryMode() throws SQLException
  {
    Map<String, String> props = new HashMap<>();
    props.put("mode", "memory");
    SQLiteConnector connector = new SQLiteConnector(
        (File) null, props);

    try (Connection conn = connector.openConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT 1")) {
      assertTrue(rs.next());
      assertEquals(1, rs.getInt(1));
    }

    // No leaked file should be present in the CWD after a memory-mode
    // open. If this assertion fails, the URL construction in
    // SQLiteConnector.openConnection() has regressed.
    File leaked = new File(":memory:?mode=memory");
    assertFalse(leaked.exists(),
                "Memory-mode open must not create a file in CWD: "
                    + leaked.getAbsolutePath());
  }

  // -------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------

  private static int queryInt(Statement stmt, String sql)
      throws SQLException
  {
    try (ResultSet rs = stmt.executeQuery(sql)) {
      assertTrue(rs.next(), "Expected a row from: " + sql);
      return rs.getInt(1);
    }
  }

  private static String queryString(Statement stmt, String sql)
      throws SQLException
  {
    try (ResultSet rs = stmt.executeQuery(sql)) {
      assertTrue(rs.next(), "Expected a row from: " + sql);
      return rs.getString(1);
    }
  }
}
