package com.senzing.util;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Provides utilities for working with Senzing environment settings.
 */
public class SzSettingsUtilities {
    /**
     * The <b>unmodifiable</b> {@link Set} of legal prefixes for URI
     * prefixes for the database URI's for the Senzing repository.
     */
    public static final Set<String> DATABASE_URI_PREFIXES = Set.of(
        "sqlite3://", "postgresql://", "mysql://", "db2://", "oci://", "mssql://");
    
    /**
     * Private default constructor.
     */
    private SzSettingsUtilities() {
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

}