package com.senzing.sql;

import java.util.Objects;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Enumerates some of the common database types.
 */
public enum DatabaseType {
  /**
   * The specific database type is unknown and the best we can do is handle
   * things as generic SQL.
   */
  GENERIC,

  /**
   * The database type is SQLite.
   */
  SQLITE,

  /**
   * The database type is PostgreSQL.
   */
  POSTGRESQL,

  /**
   * The database type is MySQL.
   */
  MYSQL,

  /**
   * The database type is Oracle.
   */
  ORACLE,

  /**
   * The database type is DB2.
   */
  DB2;

  /**
   * Detects the {@link DatabaseType} from the specified {@link Connection}.
   * If the specified {@link Connection} is <code>null</code> then
   * <code>null</code> is returned.  If the database {@linkplain
   * DatabaseMetaData#getDatabaseProductName() product name} for the {@link
   * Connection} is not recognized then {@link #GENERIC} is returned.
   *
   * @param conn The JDBC {@link Connection} from which to detect the
   *             {@link DatabaseType}.
   *
   * @return The {@link DatabaseType} for the specified {@link Connection} if
   *         the product name is recognized, {@link #GENERIC} if the product
   *         name is not recognized, or <code>null</code> if the specified
   *         {@link Connection} is <code>null</code>.
   *
   * @throws SQLException If a JDBC failure occurs.
   */
  public static DatabaseType detect(Connection conn)
    throws SQLException
  {
    if (conn == null) return null;
    String productName = conn.getMetaData().getDatabaseProductName();
    switch (productName.toUpperCase()) {
      case "SQLITE":
        return SQLITE;
      case "POSTGRESQL":
        return POSTGRESQL;
      case "MYSQL":
        return MYSQL;
      case "ORACLE":
        return ORACLE;
      case "DB2":
        return DB2;
      default:
        return GENERIC;
    }
  }

  /**
   * Gets the SQL text for binding a {@link Timestamp} value to a {@link
   * PreparedStatement} for this database type.
   *
   * @return The SQL text for binding a {@link Timestamp} value to a {@link
   *         PreparedStatement} for this database type.
   */
  public String getTimestampBindingSQL() {
    switch (this) {
      case SQLITE:
        return "(STRFTIME('%Y-%m-%d %H:%M:%f', ?))";
      default:
        return "?";
    }
  }

  /**
   * Binds a timestamp value on the specified {@link PreparedStatement} at the
   * specified index using the UTC {@link Calendar}.
   *
   * @param ps The {@link PreparedStatement} to bind to.
   * @param index The index at which to bind the value.
   * @param value The {@link Timestamp} value to bind.
   * @throws SQLException If a JDBC failure occurs.
   */
  public void setTimestamp(PreparedStatement ps, int index, Timestamp value)
    throws SQLException
  {
    switch (this) {
      case SQLITE:
      {
        Instant instant = Instant.ofEpochMilli(value.getTime());
        ZonedDateTime zonedDateTime = instant.atZone(UTC_ZONE);

        ps.setString(index, DATE_TIME_FORMATTER.format(zonedDateTime));
      }
      break;
      default:
        ps.setTimestamp(index, value, UTC_CALENDAR);
    }
  }

  /**
   * Binds a timestamp value on the specified {@link CallableStatement} at the
   * specified index using the UTC {@link Calendar}.
   *
   * @param cs The {@link CallableStatement} to bind to.
   * @param index The index at which to bind the value.
   * @param value The {@link Timestamp} value to bind.
   * @throws SQLException If a JDBC failure occurs.
   */
  public void setTimestamp(CallableStatement cs, int index, Timestamp value)
      throws SQLException
  {
    switch (this) {
      case SQLITE:
      {
        Instant instant = Instant.ofEpochMilli(value.getTime());
        ZonedDateTime zonedDateTime = instant.atZone(UTC_ZONE);

        cs.setString(index, DATE_TIME_FORMATTER.format(zonedDateTime));
      }
      default:
        cs.setTimestamp(index, value, UTC_CALENDAR);
    }
  }

  /**
   * Provides a generic way to format the equivalent of the SQL 
   * "least" function for the database type.
   * 
   * @param first The first parameter to the LEAST function.
   * @param second The second parameter to the LEAST function.
   * @param other The optional other parameters to the LEAST
   *              function for variable length parameters.
   * @return The formatted SQL "least" function for the respective
   *         database. 
   * @throws NullPointerException If either the first or second
   *                              parameters is <code>null</code>.
   */
  public String sqlLeast(String first, String second, String... other) 
    throws NullPointerException
  {
    Objects.requireNonNull(first, 
      "The first parameter to LEAST cannot be null");
    Objects.requireNonNull(first, 
      "The second parameter to LEAST cannot be null");
    StringBuilder sb = new StringBuilder();
    switch (this) {
      case SQLITE:
        sb.append("MIN");
        break;
      default:
        sb.append("LEAST");
    }
    sb.append("(").append(first).append(", ").append(second);
    if (other != null) {
      for (String param: other) {
        sb.append(", ").append(param);
      }
    }
    sb.append(")");
    return sb.toString();
  }

    /**
   * Provides a generic way to format the equivalent of the SQL 
   * "least" function for the database type.
   * 
   * @param first The first parameter to the LEAST function.
   * @param second The second parameter to the LEAST function.
   * @param other The optional other parameters to the LEAST
   *              function for variable length parameters.
   * @return The formatted SQL "least" function for the respective
   *         database. 
   * @throws NullPointerException If either the first or second
   *                              parameters is <code>null</code>.
   */
  public String sqlGreatest(String first, String second, String... other) 
    throws NullPointerException
  {
    Objects.requireNonNull(first, 
      "The first parameter to GREATEST cannot be null");
    Objects.requireNonNull(first, 
      "The second parameter to GREATEST cannot be null");
    StringBuilder sb = new StringBuilder();
    switch (this) {
      case SQLITE:
        sb.append("MAX");
        break;
      default:
        sb.append("GREATEST");
    }
    sb.append("(").append(first).append(", ").append(second);
    if (other != null) {
      for (String param: other) {
        sb.append(", ").append(param);
      }
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * The {@link Calendar} to use for retrieving timestamps from the database.
   */
  private static final Calendar UTC_CALENDAR
      = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  /**
   * The date-time pattern used to bind timestamp values as strings.
   */
  private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

  /**
   * The {@link ZoneId} for UTC time zone.
   */
  private static final ZoneId UTC_ZONE = ZoneId.of("UTC");

  /**
   * The {@link DateTimeFormatter} used to bind timestamp values as strings.
   */
  private static final DateTimeFormatter DATE_TIME_FORMATTER
      = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

}
