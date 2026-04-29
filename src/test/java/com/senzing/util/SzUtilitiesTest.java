package com.senzing.util;

import com.senzing.sql.SQLiteConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SzUtilities}.
 *
 * <p>Each test asserts the documented contract: the URI prefix
 * predicate returns {@code true} for each supported prefix and
 * {@code false} otherwise; the JSON-building helpers
 * ({@link SzUtilities#bootstrapSettings()},
 * {@link SzUtilities#basicSettingsFromDatabaseUri(String)} and the
 * licensed overloads) produce the documented JSON shape, validate
 * their inputs, and reject illegal URIs; and
 * {@link SzUtilities#ensureSenzingSQLiteSchema(Connection)} is
 * idempotent, validates the connection type, and locates the schema
 * file via the install discovery.
 *
 * <p>Shares the {@link FakeSenzingInstall} fixture with
 * {@link SzInstallLocationsTest} via the
 * {@code com.senzing.util} package-private helper. Tests mutate
 * {@code senzing.*} system properties and run single-threaded.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class SzUtilitiesTest
{
  private static final String[] PROPERTIES = {
      "senzing.path",
      "senzing.config.dir",
      "senzing.support.dir",
      "senzing.resource.dir",
  };

  private FakeSenzingInstall install;

  // -------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------

  @BeforeAll
  public void setUpFakeInstall() throws IOException
  {
    this.install = FakeSenzingInstall.build();
  }

  @AfterAll
  public void tearDownFakeInstall() throws IOException
  {
    if (this.install != null) {
      this.install.close();
    }
  }

  @AfterEach
  public void clearProperties()
  {
    for (String key : PROPERTIES) {
      System.clearProperty(key);
    }
  }

  // -------------------------------------------------------------------
  // startsWithDatabaseUriPrefix() contract
  // -------------------------------------------------------------------

  /**
   * Every URI matching one of the documented
   * {@link SzUtilities#DATABASE_URI_PREFIXES} prefixes must yield
   * {@code true}.
   */
  @Test
  public void startsWithDatabaseUriPrefixAcceptsLegalPrefixes()
  {
    assertTrue(SzUtilities.startsWithDatabaseUriPrefix(
        "sqlite3:///tmp/x.db"));
    assertTrue(SzUtilities.startsWithDatabaseUriPrefix(
        "postgresql://host:5432/db"));
    assertTrue(SzUtilities.startsWithDatabaseUriPrefix(
        "mysql://host:3306/db"));
    assertTrue(SzUtilities.startsWithDatabaseUriPrefix(
        "db2://host:50000/db"));
    assertTrue(SzUtilities.startsWithDatabaseUriPrefix(
        "oci://host/orcl"));
    assertTrue(SzUtilities.startsWithDatabaseUriPrefix(
        "mssql://host:1433/db"));
  }

  /**
   * A URI with no recognized prefix must yield {@code false}.
   */
  @Test
  public void startsWithDatabaseUriPrefixRejectsUnknownPrefix()
  {
    assertFalse(SzUtilities.startsWithDatabaseUriPrefix(
        "https://example.com/db"));
    assertFalse(SzUtilities.startsWithDatabaseUriPrefix("just text"));
    assertFalse(SzUtilities.startsWithDatabaseUriPrefix(""));
  }

  /**
   * The URI prefix list must contain exactly the six documented
   * prefixes, unmodifiable.
   */
  @Test
  public void databaseUriPrefixesContainsExpectedSet()
  {
    assertEquals(6, SzUtilities.DATABASE_URI_PREFIXES.size());
    assertTrue(SzUtilities.DATABASE_URI_PREFIXES.contains("sqlite3://"));
    assertTrue(SzUtilities.DATABASE_URI_PREFIXES.contains(
        "postgresql://"));
    assertTrue(SzUtilities.DATABASE_URI_PREFIXES.contains("mysql://"));
    assertTrue(SzUtilities.DATABASE_URI_PREFIXES.contains("db2://"));
    assertTrue(SzUtilities.DATABASE_URI_PREFIXES.contains("oci://"));
    assertTrue(SzUtilities.DATABASE_URI_PREFIXES.contains("mssql://"));

    assertThrows(UnsupportedOperationException.class,
                 () -> SzUtilities.DATABASE_URI_PREFIXES.add("foo://"));
  }

  // -------------------------------------------------------------------
  // bootstrapSettings() contract
  // -------------------------------------------------------------------

  /**
   * With a discoverable install, {@link SzUtilities#bootstrapSettings()}
   * must return JSON containing only the {@code PIPELINE} object with
   * {@code SUPPORTPATH}, {@code CONFIGPATH}, and {@code RESOURCEPATH}
   * entries — and no {@code SQL} section since the bootstrap variant
   * does not include a database URI.
   */
  @Test
  public void bootstrapSettingsProducesPipelineOnlyJson() throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());

    String json = SzUtilities.bootstrapSettings();
    JsonObject obj = parse(json);
    assertTrue(obj.containsKey("PIPELINE"),
               "Bootstrap settings must have a PIPELINE object");
    assertFalse(obj.containsKey("SQL"),
                "Bootstrap settings must NOT have an SQL object");

    JsonObject pipeline = obj.getJsonObject("PIPELINE");
    assertEquals(install.supportDir().toFile().getCanonicalPath(),
                 pipeline.getString("SUPPORTPATH"));
    assertEquals(install.configDir().toFile().getCanonicalPath(),
                 pipeline.getString("CONFIGPATH"));
    assertEquals(install.resourceDir().toFile().getCanonicalPath(),
                 pipeline.getString("RESOURCEPATH"));
  }

  // -------------------------------------------------------------------
  // basicSettingsFromDatabaseUri(uri) contract
  // -------------------------------------------------------------------

  /**
   * {@link SzUtilities#basicSettingsFromDatabaseUri(String)} with a
   * null URI must throw {@link NullPointerException} per the
   * javadoc.
   */
  @Test
  public void basicSettingsFromDatabaseUriThrowsNpeForNullUri()
  {
    assertThrows(NullPointerException.class,
                 () -> SzUtilities.basicSettingsFromDatabaseUri(null));
  }

  /**
   * {@link SzUtilities#basicSettingsFromDatabaseUri(String)} with a
   * URI that does not match any documented prefix must throw
   * {@link IllegalArgumentException}.
   */
  @Test
  public void basicSettingsFromDatabaseUriThrowsIaeForBadPrefix()
  {
    System.setProperty("senzing.path", install.root().toString());
    assertThrows(IllegalArgumentException.class,
                 () -> SzUtilities.basicSettingsFromDatabaseUri(
                     "ftp://nope/db"));
  }

  /**
   * With a legal prefix and a discoverable install, the JSON must
   * contain both the {@code PIPELINE} object and the
   * {@code SQL.CONNECTION} entry set to the supplied URI.
   */
  @Test
  public void basicSettingsFromDatabaseUriProducesPipelineAndSql()
      throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());
    String uri = "sqlite3:///tmp/test.db";

    String json = SzUtilities.basicSettingsFromDatabaseUri(uri);
    JsonObject obj = parse(json);
    assertTrue(obj.containsKey("PIPELINE"));
    assertTrue(obj.containsKey("SQL"));
    assertEquals(uri, obj.getJsonObject("SQL").getString("CONNECTION"));
  }

  // -------------------------------------------------------------------
  // basicSettingsFromDatabaseUri(uri, licenseBase64) contract
  // -------------------------------------------------------------------

  /**
   * {@code basicSettingsFromDatabaseUri(uri, licenseBase64)} with
   * a null URI must throw {@link NullPointerException}.
   */
  @Test
  public void basicSettingsFromDatabaseUriBase64ThrowsNpeForNullUri()
  {
    assertThrows(NullPointerException.class,
                 () -> SzUtilities.basicSettingsFromDatabaseUri(
                     null, "base64"));
  }

  /**
   * Passing a non-null Base-64 license string must add a
   * {@code LICENSESTRINGBASE64} entry to the {@code PIPELINE}
   * object.
   */
  @Test
  public void basicSettingsFromDatabaseUriBase64IncludesLicenseString()
      throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());

    String json = SzUtilities.basicSettingsFromDatabaseUri(
        "sqlite3:///tmp/test.db", "TEST_BASE64_LICENSE");
    JsonObject pipeline = parse(json).getJsonObject("PIPELINE");
    assertEquals("TEST_BASE64_LICENSE",
                 pipeline.getString("LICENSESTRINGBASE64"));
  }

  // -------------------------------------------------------------------
  // basicSettingsFromDatabaseUri(uri, licenseFile) contract
  // -------------------------------------------------------------------

  /**
   * {@code basicSettingsFromDatabaseUri(uri, licenseFile)} with
   * a null URI must throw {@link NullPointerException}.
   */
  @Test
  public void basicSettingsFromDatabaseUriFileThrowsNpeForNullUri()
  {
    assertThrows(NullPointerException.class,
                 () -> SzUtilities.basicSettingsFromDatabaseUri(
                     null, new File("/tmp/license")));
  }

  /**
   * Passing a non-null license {@link File} must add a
   * {@code LICENSEFILE} entry containing its canonical path to the
   * {@code PIPELINE} object.
   */
  @Test
  public void basicSettingsFromDatabaseUriFileIncludesLicenseFile()
      throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());
    File licenseFile = new File(install.root().toFile(), "license.txt");
    licenseFile.createNewFile();

    String json = SzUtilities.basicSettingsFromDatabaseUri(
        "sqlite3:///tmp/test.db", licenseFile);
    JsonObject pipeline = parse(json).getJsonObject("PIPELINE");
    assertEquals(licenseFile.getCanonicalPath(),
                 pipeline.getString("LICENSEFILE"));
  }

  // -------------------------------------------------------------------
  // ensureSenzingSQLiteSchema(Connection) contract
  // -------------------------------------------------------------------

  /**
   * {@code ensureSenzingSQLiteSchema(null)} must throw
   * {@link NullPointerException}.
   */
  @Test
  public void ensureSenzingSqliteSchemaThrowsNpeForNullConnection()
  {
    assertThrows(NullPointerException.class,
                 () -> SzUtilities.ensureSenzingSQLiteSchema(null));
  }

  /**
   * Calling with a non-SQLite connection must throw
   * {@link IllegalArgumentException} per the javadoc.
   */
  @Test
  public void ensureSenzingSqliteSchemaThrowsIaeForNonSqliteConnection()
      throws Exception
  {
    Connection nonSqlite = nonSqliteConnectionProxy();
    assertThrows(IllegalArgumentException.class,
                 () -> SzUtilities.ensureSenzingSQLiteSchema(nonSqlite));
  }

  /**
   * Happy path: on a fresh SQLite connection,
   * {@code ensureSenzingSQLiteSchema} must execute the schema
   * statements and return {@code true}.
   */
  @Test
  public void ensureSenzingSqliteSchemaCreatesSchemaOnFreshDb(
      @org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());

    SQLiteConnector connector = new SQLiteConnector(
        tempDir.resolve("schema.db").toString());
    try (Connection conn = connector.openConnection()) {
      boolean created = SzUtilities.ensureSenzingSQLiteSchema(conn);
      assertTrue(created,
                 "First call must create schema and return true");
    }
  }

  /**
   * Idempotency: a second call against the same database must
   * detect the existing fixture table and return {@code false}
   * without re-running the schema.
   */
  @Test
  public void ensureSenzingSqliteSchemaIsIdempotent(
      @org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());

    SQLiteConnector connector = new SQLiteConnector(
        tempDir.resolve("schema-idempotent.db").toString());
    try (Connection conn = connector.openConnection()) {
      assertTrue(SzUtilities.ensureSenzingSQLiteSchema(conn),
                 "First call returns true");
      assertFalse(SzUtilities.ensureSenzingSQLiteSchema(conn),
                  "Second call must detect existing schema and"
                      + " return false");
    }
  }

  // -------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------

  private static JsonObject parse(String json)
  {
    try (JsonReader reader = Json.createReader(new StringReader(json))) {
      return reader.readObject();
    }
  }

  /**
   * Returns a dynamic-proxy {@link Connection} whose
   * {@link DatabaseMetaData#getDatabaseProductName()} reports
   * "PostgreSQL" so {@link com.senzing.sql.DatabaseType#detect}
   * yields a non-SQLITE type. All other methods return null /
   * default values.
   */
  private static Connection nonSqliteConnectionProxy()
  {
    DatabaseMetaData md = (DatabaseMetaData) Proxy.newProxyInstance(
        SzUtilitiesTest.class.getClassLoader(),
        new Class<?>[] { DatabaseMetaData.class },
        (proxy, method, args) -> {
          if ("getDatabaseProductName".equals(method.getName())) {
            return "PostgreSQL";
          }
          return null;
        });
    return (Connection) Proxy.newProxyInstance(
        SzUtilitiesTest.class.getClassLoader(),
        new Class<?>[] { Connection.class },
        (proxy, method, args) -> {
          if ("getMetaData".equals(method.getName())) {
            return md;
          }
          // for primitive returns, return null/default as appropriate
          if (method.getReturnType() == boolean.class) return false;
          if (method.getReturnType().isPrimitive()) return 0;
          return null;
        });
  }

}
