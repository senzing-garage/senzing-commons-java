package com.senzing.sql;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PostgreSqlConnector}.
 *
 * <p>Each test asserts the documented contract: the JDBC URL is
 * built from {@code host:port/database}, the connection {@link Properties}
 * carry {@code user}/{@code password} (and any
 * {@code addlProperties}), and {@link
 * PostgreSqlConnector#openConnection()} produces a usable
 * {@link Connection} with auto-commit disabled.
 *
 * <p>Uses {@link EmbeddedPostgres} (Zonky IO) to spin up a real
 * in-process PostgreSQL once per test class so connection-level behavior is
 * verified against the actual driver rather than mocks.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class PostgreSqlConnectorTest
{
  /** Embedded PostgreSQL started once for the whole test class. */
  private EmbeddedPostgres pg;

  /** Port on which the embedded PostgreSQL listens. */
  private int port;

  // -------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------

  @BeforeAll
  public void startPostgres() throws IOException
  {
    this.pg   = EmbeddedPostgres.builder().start();
    this.port = pg.getPort();
  }

  @AfterAll
  public void stopPostgres() throws IOException
  {
    if (this.pg != null) {
      this.pg.close();
    }
  }

  // -------------------------------------------------------------------
  // openConnection() contract
  // -------------------------------------------------------------------

  /**
   * The five-arg constructor must produce a connector whose
   * {@code openConnection()} returns a usable {@link Connection} when
   * given valid host/port/database/user/password against the embedded
   * PostgreSQL.
   */
  @Test
  public void openConnectionReturnsUsableConnection() throws SQLException
  {
    PostgreSqlConnector connector = new PostgreSqlConnector(
        "localhost", port, "postgres", "postgres", "");
    try (Connection conn = connector.openConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT 1"))
         {
      assertNotNull(conn);
      assertTrue(rs.next(), "SELECT 1 must produce a row");
      assertEquals(1, rs.getInt(1));
    }
  }

  /**
   * {@link PostgreSqlConnector#openConnection()} must return a
   * {@link Connection} with auto-commit disabled per the
   * implementation's {@code conn.setAutoCommit(false)} call.
   */
  @Test
  public void openConnectionDisablesAutoCommit() throws SQLException
  {
    PostgreSqlConnector connector = new PostgreSqlConnector(
        "localhost", port, "postgres", "postgres", "");
    try (Connection conn = connector.openConnection()) {
      assertFalse(conn.getAutoCommit(),
                  "openConnection must disable auto-commit");
    }
  }

  /**
   * The six-arg constructor must merge {@code addlProperties} into the
   * connection {@link Properties} so they affect the resulting
   * {@link Connection}. Verified by passing a non-default
   * {@code application_name} and reading it back via
   * {@code current_setting()}.
   */
  @Test
  public void sixArgConstructorMergesAddlProperties() throws SQLException
  {
    Properties addl = new Properties();
    addl.setProperty("ApplicationName", "senzing-commons-test");

    PostgreSqlConnector connector = new PostgreSqlConnector(
        "localhost", port, "postgres", "postgres", "", addl);
    try (Connection conn = connector.openConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(
             "SELECT current_setting('application_name')"))
             {
      assertTrue(rs.next());
      assertEquals("senzing-commons-test", rs.getString(1),
                   "Addl 'ApplicationName' property must be applied to"
                       + " the connection");
    }
  }

  /**
   * When {@code addlProperties} contains a {@code user} (or
   * {@code password}) entry, the explicit {@code user}/{@code
   * password} arguments to the six-arg constructor must win because they are
   * {@code put} after the {@code putAll} of
   * {@code addlProperties} per the implementation. Verified by
   * connecting with a valid {@code user} argument despite an intentionally
   * invalid {@code user} key in
   * {@code addlProperties}.
   */
  @Test
  public void explicitUserOverridesAddlPropertiesUser() throws SQLException
  {
    Properties addl = new Properties();
    addl.setProperty("user", "nonexistent_user");

    PostgreSqlConnector connector = new PostgreSqlConnector(
        "localhost", port, "postgres", "postgres", "", addl);
    try (Connection conn = connector.openConnection()) {
      assertNotNull(conn,
                    "Explicit user 'postgres' must override the "
                        + "addlProperties 'user' entry");
    }
  }

  /**
   * {@link PostgreSqlConnector#openConnection()} must throw
   * {@link SQLException} when the JDBC driver cannot connect — for
   * example, when the database name doesn't exist on the server.
   */
  @Test
  public void openConnectionThrowsSqlExceptionForUnknownDatabase()
  {
    PostgreSqlConnector connector = new PostgreSqlConnector(
        "localhost", port, "definitely_not_a_database",
        "postgres", "");
    assertThrows(SQLException.class, connector::openConnection);
  }

  /**
   * {@link PostgreSqlConnector#openConnection()} must throw
   * {@link SQLException} when given bad credentials. Driving with a
   * non-existent username (default Zonky auth permits the
   * {@code postgres} user with any password but rejects unknown
   * usernames).
   */
  @Test
  public void openConnectionThrowsSqlExceptionForUnknownUser()
  {
    PostgreSqlConnector connector = new PostgreSqlConnector(
        "localhost", port, "postgres",
        "definitely_not_a_real_user", "");
    assertThrows(SQLException.class, connector::openConnection);
  }
}
