package com.senzing.util;

import static com.senzing.io.IOUtilities.UTF_8_CHARSET;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.senzing.sql.DatabaseType;
import com.senzing.sql.SQLiteConnector;

/**
 * Provides utilities for working with Senzing environment settings
 * and repositories.
 */
public class SzUtilities {
    /**
     * The <b>unmodifiable</b> {@link Set} of legal prefixes for URI
     * prefixes for the database URI's for the Senzing repository.
     */
    public static final Set<String> DATABASE_URI_PREFIXES = Set.of(
        "sqlite3://", "postgresql://", "mysql://", "db2://", "oci://", "mssql://");
    
    /**
     * Private default constructor.
     */
    private SzUtilities() {
        // do nothing
    }

    /**
     * Checks if the specified text appears to be a database URI
     * for the Senzing repository.
     * 
     * @param text The text to check.
     * @return <code>true</code> if the specified text starts with a legal database
     *         URI prefix, otherwise <code>false</code>.
     */
    public static boolean startsWithDatabaseUriPrefix(String text) {
        for (String prefix : DATABASE_URI_PREFIXES) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds basic JSON settings for the Senzing environment initialization
     * from the specified database URI for the Senzing repository.
     * <p>
     * Settings will be of the form:
     * <pre>
     *      {
     *          "PIPELINE": {
     *              "SUPPORTPATH": "...",
     *              "CONFIGPATH": "...",
     *              "RESOURCEPATH": "..."
     *          }
     *          "SQL": {
     *              "CONNECTION": "your-database-uri"
     *          }
     *      }
     * </pre>
     * <p>
     * <b>NOTE:</b>
     * This will attempt to find the Senzing installation directories on the
     * system using Java system properties, environment variables and defaults
     * for the operating system with the following order of precedence:
     * <ul>
     *  <li><code>SUPPORTPATH</code>: 
     *      <ol>
     *          <li><code>senzing.support.dir</code> System Property</li>
     *          <li><code>SENZING_SUPPORT_DIR</code> Environment Variable</li>
     *          <li><code>[senzing-path]/data</code> (see <code>[senzing-path]</code> below)</li>
     *      </ol>
     *  </li>
     *  <li><code>CONFIGPATH</code>: 
     *      <ol>
     *          <li><code>senzing.config.dir</code> System Property</li>
     *          <li><code>SENZING_CONFIG_DIR</code> Environment Variable</li>
     *          <li>Linux Only: <code>/etc/opt/senzing</code> (if the directory exists)<li>
     *          <li><code>[senzing-path]/etc</code> (see <code>[senzing-path]</code> below)</li>
     *      </ol>
     *  </li>
     *  <li><code>RESOURCEPATH</code>: 
     *      <ol>
     *          <li><code>senzing.resource.dir</code> System Property</li>
     *          <li><code>SENZING_RESOURCE_DIR</code> Environment Variable</li>
     *          <li><code>[senzing-path]/resources</code> (see <code>[senzing-path]</code> below)</li>
     *      </ol>
     *  </li>
     *  <li><code>[senzing-path]</code>:
     *      <ol>
     *          <li><code>senzing.path</code> System Property</li>
     *          <li><code>SENZING_PATH</code> Environment Variable</li>
     *          <li>The default directory directory for the operating system:
     *              <ul>
     *                  <li>Linux: <code>/opt/senzing</code></li>
     *                  <li>macOS: <code>$HOME/senzing</code></li>
     *                  <li>Windows: <code>%UserProfile%\senzing</code>
     *              </ul>
     *          </li>
     *      </ol>
     *  </li>
     * </ul>
     * 
     * @param uri The database URI to use for the settings.
     * 
     * @return The basic Senzing settings created from the specified database URI.
     * 
     * @throws NullPointerException If the specified URI is <code>null</code>.
     * @throws IllegalArgumentException If the specified URI does not begin with
     *                                  a legal prefix for a Senzing repository.
     * @throws IllegalStateException If the Senzing installation cannot be found.
     */
    public static String basicSettingsFromDatabaseUri(String uri) 
        throws IllegalArgumentException, IllegalStateException
    {
        return basicSettingsFromDatabaseUri(uri, null, null);
    }

    /**
     * Equivalent to {@link #basicSettingsFromDatabaseUri(String)} with the additional 
     * parameter to specify the Base-64 Senzing License {@link String}.  This will produce
     * the settings in the same way as {@link #basicSettingsFromDatabaseUri(String)}, 
     * except that if the specified Base-64 Senzing license is <b>not</b> <code>null</code>
     * then it will add the <code>LICENSESTRINGBASE64</code> property as follows:
     * <pre>
     *      {
     *          "PIPELINE": {
     *              "SUPPORTPATH": "...",
     *              "CONFIGPATH": "...",
     *              "RESOURCEPATH": "...",
     *              "LICENSESTRINGBASE64": "..."
     *          }
     *          "SQL": {
     *              "CONNECTION": "your-database-uri"
     *          }
     *      }
     * </pre>
     * 
     * @param uri The database URI to use for the settings.
     * 
     * @param licenseBase64 The optional base-64 encoded Senzing license string,
     *                      or <code>null</code> if <code>LICENSESTRINGBASE64</code>
     *                      is to be excluded.
     * 
     * @return The basic Senzing settings created from the specified database URI
     *         and base-64 encoded license string.
     * 
     * @throws NullPointerException If the specified URI is <code>null</code>.
     * @throws IllegalArgumentException If the specified URI does not begin with
     *                                  a legal prefix for a Senzing repository.
     * @throws IllegalStateException If the Senzing installation cannot be found.
     */
    public static String basicSettingsFromDatabaseUri(String    uri,
                                                      String    licenseBase64)
        throws IllegalArgumentException, IllegalStateException
    {
        return basicSettingsFromDatabaseUri(uri, licenseBase64, null);
    }

    /**
     * Equivalent to {@link #basicSettingsFromDatabaseUri(String)} with the additional 
     * parameter to specify the Senzing license file.  This will produce the settings in
     * the same way as {@link #basicSettingsFromDatabaseUri(String)}, except that if the
     * specified Senzing license file is <b>not</b> <code>null</code> then it will add 
     * the <code>LICENSEFILE</code> property as follows:
     * <pre>
     *      {
     *          "PIPELINE": {
     *              "SUPPORTPATH": "...",
     *              "CONFIGPATH": "...",
     *              "RESOURCEPATH": "...",
     *              "LICENSEFILE": "..."
     *          }
     *          "SQL": {
     *              "CONNECTION": "your-database-uri"
     *          }
     *      }
     * </pre>
     * 
     * @param uri The database URI to use for the settings.
     * 
     * @param licenseFile The optional Senzing license file, or <code>null</code> if
     *                    <code>LICENSEFILE</code> is to be excluded.
     * 
     * @return The basic Senzing settings created from the specified database URI
     *         and base-64 encoded license string.
     * 
     * @throws NullPointerException If the specified URI is <code>null</code>.
     * @throws IllegalArgumentException If the specified URI does not begin with
     *                                  a legal prefix for a Senzing repository.
     * @throws IllegalStateException If the Senzing installation cannot be found.
     */
    public static String basicSettingsFromDatabaseUri(String    uri,
                                                      File      licenseFile)
        throws IllegalArgumentException, IllegalStateException
    {
        return basicSettingsFromDatabaseUri(uri, null, licenseFile);
    }
    
    /**
     * Internal method to produce the Senzing settings with either an optional
     * <code>LICENSESTRINGBASE64</code> or <code>LICENSEFILE</code> property
     * (but not both).
     * 
     * @param uri The database URI to use for the settings.
     * 
     * @param licenseBase64 The optional base-64 encoded Senzing license string,
     *                      or <code>null</code> if <code>LICENSESTRINGBASE64</code>
     *                      is to be excluded.
     * 
     * @param licenseFile The optional Senzing license file, or <code>null</code> if
     *                    <code>LICENSEFILE</code> is to be excluded.
     * 
     * @return The basic Senzing settings created from the specified database URI
     *         and base-64 encoded license string.
     * 
     * @throws NullPointerException If the specified URI is <code>null</code>.
     * @throws IllegalArgumentException If the specified URI does not begin with
     *                                  a legal prefix for a Senzing repository.
     * @throws IllegalStateException If the Senzing installation cannot be found.
     */
    private static String basicSettingsFromDatabaseUri(String   uri,
                                                       String   licenseBase64,
                                                       File     licenseFile)
    {
        try {
            Objects.requireNonNull(uri, "The specified URI cannot be null");
            if (!startsWithDatabaseUriPrefix(uri)) {
                throw new IllegalArgumentException(
                    "The specified database URI does not appear legal: " + uri);
            }
            if (licenseBase64 != null && licenseFile != null) {
                throw new IllegalArgumentException(
                    "Cannot specify both the license file and the base-64-encoded license");
            }

            JsonObjectBuilder mainBuilder = Json.createObjectBuilder();
            JsonObjectBuilder pipelineBuilder = Json.createObjectBuilder();
            JsonObjectBuilder sqlBuilder = Json.createObjectBuilder();
            
            SzInstallLocations locations = SzInstallLocations.findLocations();
            
            pipelineBuilder.add("SUPPORTPATH", locations.getSupportDirectory().getCanonicalPath());
            pipelineBuilder.add("CONFIGPATH", locations.getConfigDirectory().getCanonicalPath());
            pipelineBuilder.add("RESOURCEPATH", locations.getResourceDirectory().getCanonicalPath());
            if (licenseBase64 != null) {
                pipelineBuilder.add("LICENSESTRINGBASE64", licenseBase64);
            } else if (licenseFile != null) {
                pipelineBuilder.add("LICENSEFILE", licenseFile.getCanonicalPath());
            }
            mainBuilder.add("PIPELINE", pipelineBuilder);
            sqlBuilder.add("CONNECTION", uri);
            mainBuilder.add("SQL", sqlBuilder);

            JsonObject jsonObject = mainBuilder.build();

            return JsonUtilities.toJsonText(jsonObject);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Should not fail in getting canonical path from install locations files.", e);
        }
    }

    /**
     * Ensures the Senzing schema is setup on the SQLite database accessible
     * via the specified {@link Connection}.  This method will attempt
     * to find the SQLite schema file at the following path: 
     * <code>[resource-path]/schema/szcore-schema-sqlite-create.sql</code>
     * directory.
     * <p>
     * The <code>[resource-path]</code> from the Senzing installation is 
     * located using Java system properties, environment variables and
     * defaults for the operating system with the following order of precedence:
     * <ol>
     *     <li><code>senzing.resource.dir</code> System Property</li>
     *     <li><code>SENZING_RESOURCE_DIR</code> Environment Variable</li>
     *     <li><code>[senzing-path]/resources</code> (see <code>[senzing-path]</code> below)</li>
     * </ol>
     * Where <code>[senzing-path]</code> is determined as:
     * <ol>
     *     <li><code>senzing.path</code> System Property</li>
     *     <li><code>SENZING_PATH</code> Environment Variable</li>
     *     <li>The default directory directory for the operating system:
     *         <ul>
     *             <li>Linux: <code>/opt/senzing</code></li>
     *             <li>macOS: <code>$HOME/senzing</code></li>
     *             <li>Windows: <code>%UserProfile%\senzing</code>
     *         </ul>
     *     </li>
     * </ol>
     * <p>
     * <b>NOTE:</b> On Linux operating systems the <code>resources/schema</code>
     * files are only present if you have installed the <code>senzingsdk-setup</code>
     * package.  They are not available with the <code>senzingsdk-runtime</code>.
     * 
     * <p>
     * <b>ALSO NOTE:</b> If this method determines that the Senzing SQLite 
     * schema is already setup in the connected database, then this method will
     * do nothing.
     * 
     * @param conn The non-null {@link Connection} to a SQLite database.
     * 
     * @return <code>true</code> if the schema was created, or <code>false</code>
     *         if the schema was detected and no changes were made.
     * 
     * @throws SQLException If a JDBC failure occurs.
     * 
     * @throws NullPointerException If the specified {@link Connection} is
     *                              <code>null</code>.
     * 
     * @throws IllegalArgumentException If the specified {@link Connection} is
     *                                  not connected to a SQLite database.
     * 
     * @throws IllegalStateException If the schema file cannot be found.
     */
    public static boolean ensureSenzingSQLiteSchema(Connection conn) 
        throws SQLException, IllegalStateException
    {
        Objects.requireNonNull(conn, "The connection cannot be null");
        DatabaseType type = DatabaseType.detect(conn);
        if (type != DatabaseType.SQLITE) {
            throw new IllegalArgumentException(
                "The specified Connection is not a SQLite connection: " 
                + type);
        }

        // first find the file
        SzInstallLocations installLocations = SzInstallLocations.findLocations();
        
        // get the resource directory
        File resourceDir = installLocations.getResourceDirectory();
        File schemaDir = new File(resourceDir, "schema");
        if (!schemaDir.exists() || !schemaDir.isDirectory()) {
            throw new IllegalStateException(
                "Unable to find schema directory at: " + schemaDir);
        }
        File sqlFile = new File(schemaDir, "szcore-schema-sqlite-create.sql");
        if (!sqlFile.exists() || !sqlFile.isFile()) {
            throw new IllegalStateException(
                "Unable to find schema file at: " + sqlFile);
        }

        // get the set of known tables
        Set<String> knownTables = new HashSet<>();
        String[] dbTypes = {"TABLE"};
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getTables(null, null, "%", dbTypes))
        {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                knownTables.add(tableName.toUpperCase());
            }
        }

        // now iterate through the file and get all the statements
        // while looking for table names that already exist
        List<String> statements = new LinkedList<>();
        try (FileReader reader = new FileReader(sqlFile, UTF_8_CHARSET);
                BufferedReader br = new BufferedReader(reader))
        {
            int lineNumber = 0;
            for (String sql = br.readLine(); sql != null; sql = br.readLine())
            {
                lineNumber++;
                // trim the line
                sql = sql.trim();

                // check if empty
                if (sql.length() == 0) {
                    continue;
                }

                // check if this is a CREATE TABLE statement
                if (sql.toUpperCase().startsWith("CREATE TABLE ")) {
                    // get the suffix and find the table name
                    String suffix = sql.substring("CREATE TABLE ".length()).trim();
                    int space = suffix.indexOf(" ");
                    if (space < 0) {
                        throw new IllegalStateException(
                            "Failed to parse schema file at line (" + lineNumber + "): "
                            + sql);
                    }
                    String tableName = suffix.substring(0, space).toUpperCase();

                    // if the table already exists then cowardly refuse to proceed
                    if (knownTables.contains(tableName)) {
                        return false;
                    }
                }

                // store the non-empty SQL statement so we can execute later
                statements.add(sql);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to read schema file: " + sqlFile, e);
        }

        // finally execute the statements
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                stmt.execute(sql);
            }
        }
        conn.commit();

        // if we get here then we installed the schema
        return true;
    }
}