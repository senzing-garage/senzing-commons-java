package com.senzing.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static com.senzing.text.TextUtilities.urlEncodeUtf8;

/**
 * Interface for obtaining a new JDBC {@link Connection} to a database.
 * This is used by {@link ConnectionPool} to grow the {@link ConnectionPool}
 * size, but can be used for other purposes.
 */
public interface Connector {
    /**
     * Opens a new JDBC {@link Connection} to a database, hiding the details
     * of how the {@link Connection} is established. When the caller is done
     * using the {@link Connection}, {@link Connection#close()} should be called.
     *
     * @return The {@link Connection} that was opened.
     *
     * @throws SQLException If a failure occurs.
     */
    Connection openConnection() throws SQLException;

    /**
     * Formats the specified {@link Map} of connection properties as a
     * URL-encoded query string with the <code>"?"</code> prefix. This
     * returns empty string if there are no query options.
     * 
     * @param connProperties The {@link Map} of connection properties to
     *                       be formatted as a URL-encoded query string.
     * 
     * @return The encoded query string, or empty-string if no query options.
     */
    static String formatConnectionProperties(Map<String, String> connProperties) {
        if (connProperties == null || connProperties.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        connProperties.forEach((key, value) -> {
            // check if this is not the first query option
            if (sb.length() > 1) {
                sb.append("&");
            }
            sb.append(urlEncodeUtf8(key));
            sb.append("=");
            sb.append(urlEncodeUtf8(value));
        });
        return sb.toString();
    }

}
