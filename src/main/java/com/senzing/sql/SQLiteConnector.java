package com.senzing.sql;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

// NOTE: these imports require that we make SQLite JDBC driver a 
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import static com.senzing.sql.Connector.formatConnectionProperties;

/**
 * Provides a simple {@link Connector} implementation for SQLite that provides
 * some sensible default settings on the {@link Connection} after opening it.
 */
public class SQLiteConnector implements Connector {
    /**
     * The mode query parameter value for an in-memory database.
     */
    private static final String MEMORY_MODE = "memory";

    /**
     * The query parameter to control the mode.
     */
    private static final String MODE_KEY = "mode";

    /**
     * Setup the {@link SQLiteConfig} for multi-threaded access.
     */
    private static final Properties SQLITE_CONFIG_PROPERTIES;
    static {
        SQLiteConfig config = new SQLiteConfig();
        // NOTE: We want to use SQLite in multi-thread mode since this is typically used
        // with a connection pool and therefore we do not use a Connection object (or 
        // PreparedStatement) in more than one thread at any given time.  To do this at
        // run-time we need to set SQLiteOpenMode.NOMUTEX.
        // see: https://sqlite.org/threadsafe.html
        config.setOpenMode(SQLiteOpenMode.NOMUTEX);
        SQLITE_CONFIG_PROPERTIES = config.toProperties();
    }

    /**
     * The <b>unmodifiable</b> {@link List} of {@link String} <code>PRAGMA</code>
     * statements that are run on the {@link Connection} after opening it. These
     * are as follows:
     * <ol>
     * <li><code>PRAGMA foreign_keys = ON;</code></li>
     * <li><code>PRAGMA journal_mode = WAL;</code></li>
     * <li><code>PRAGMA synchronous = 1;</code></li>
     * <li><code>PRAGMA secure_delete = 0;</code></li>
     * <li><code>PRAGMA automatic_index = 0;</code></li>
     * </ol>
     */
    public static final List<String> DEFAULT_PRAGMA_FEATURES_LIST = List.of(
            "PRAGMA foreign_keys = ON;",    // enable foreign keys
            "PRAGMA journal_mode = WAL;",   // use journal to recover in case of failure
            "PRAGMA synchronous = 1;",      // normal data durability since we use journal
            "PRAGMA secure_delete = 0;",    // we don't need secure deletion
            "PRAGMA automatic_index = 0;"); // don't create temporary indexes

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
     * @param connProperties The {@link Map} of {@link String} key to {@link String} 
     *                       values for the connection properties, or <code>null</code>
     *                       if none.
     * @return The {@link File} object for the specified file path.
     */
    private static File newFile(String filePath, Map<String, String> connProperties) 
    {
        if (connProperties != null && MEMORY_MODE.equals(connProperties.get(MODE_KEY))) {
            return (filePath == null) ? null : new File(filePath);
        }

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
     * The connection properties for the SQLite connection.
     */
    private Map<String, String> connProperties;

    /**
     * Default constructor using a temporary file that will be deleted upon
     * exit of the executable.
     */
    public SQLiteConnector() {
        this(createTempFile());
    }

    /**
     * Constructs with the specified file name.
     * 
     * @param filePath The file path for the SQLite file.
     * 
     * @throws NullPointerException     If the specified file path is
     *                                  <code>null</code>.
     * @throws IllegalArgumentException If the specified file path describes a
     *                                  path to an existing directory.
     */
    public SQLiteConnector(String filePath) {
        this(newFile(filePath, null));
    }

    /**
     * Constructs with the specified {@link File} to use for the SQLite database
     * file.
     *
     * @param file The {@link File} for the SQLite database file.
     * 
     * @throws NullPointerException     If the specified file path is <code>null</code>
     *                                  when not using in-memory mode.
     * @throws IllegalArgumentException If the specified file path describes a
     *                                  path to an existing directory.
     */
    public SQLiteConnector(File file) {
        this(file, null);
    }

    /**
     * Constructs with the specified file name and connection properties.
     * 
     * @param filePath The file path for the SQLite file.
     * @param connProperties The {@link Map} of {@link String} keys to 
     *                       {@link String} values for the connection properties.
     * @throws NullPointerException     If the specified file path is <code>null</code>
     *                                  when not using in-memory mode.
     * @throws IllegalArgumentException If the specified file path describes a
     *                                  path to an existing directory.
     */
    public SQLiteConnector(String filePath, Map<String, String> connProperties) {
        this(newFile(filePath, connProperties), connProperties);
    }

    /**
     * Constructs with the specified {@link File} and connection properties.
     * 
     * @param file The {@link File} for the SQLite database file.
     * @param connProperties The {@link Map} of {@link String} keys to 
     *                       {@link String} values for the connection properties.
     * @throws NullPointerException     If the specified file path is <code>null</code>
     *                                  when not using in-memory mode.
     * @throws IllegalArgumentException If the specified file path describes a
     *                                  path to an existing directory.
     */
    public SQLiteConnector(File file, Map<String, String> connProperties) {
        if (connProperties == null || !MEMORY_MODE.equals(connProperties.get(MODE_KEY))) 
        {
            Objects.requireNonNull(
                    file, "The specified file cannot be null");
        }
        this.sqliteFile = file;
        this.connProperties = connProperties;
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
     * <p>
     * <b>WARNING:</b> The returned {@link Connection} is appropriate to use
     * in a {@link ConnectionPool} with {@link Connection}'s being acquired 
     * from the pool for use by a single thread at any given time and then 
     * returned to the pool (via {@link Connection#close()}) when a unit of
     * work is complete.  The returned {@link Connection} should <b>NOT</b> 
     * be used <b>concurrently</b> in multiple threads.  This is what SQLite
     * documentation refers to as <a href="https://sqlite.org/threadsafe.html"
     * >"multi-threaded"</a> mode which provides improved performance via the
     * runtime setting {@link SQLiteOpenMode#NOMUTEX} so long as {@link
     * Connection} objects are only used by a single thread at any given time.
     * In this mode, SQLite will ensure operations (especially database writes)
     * are thread-safe at the driver level with multiple active {@link Connection} 
     * instances to the same database being leveraged in different threads.
     *
     * @return The newly established and initialized {@link Connection}.
     * @throws SQLException If a JDBC failure occurs.
     */
    @Override
    public Connection openConnection() throws SQLException {
        boolean memoryMode = this.connProperties != null 
            && "memory".equals(this.connProperties.get("mode"));

        String jdbcUrl = "jdbc:sqlite:" 
            + (memoryMode && this.sqliteFile != null ? "file:" : "")
            + (this.sqliteFile == null ? ":memory:" : this.sqliteFile.getPath())
            + formatConnectionProperties(this.connProperties);

        Connection conn = null;

        // create a statement
        Statement statement = null;
        try {
            conn = DriverManager.getConnection(jdbcUrl, SQLITE_CONFIG_PROPERTIES);
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
