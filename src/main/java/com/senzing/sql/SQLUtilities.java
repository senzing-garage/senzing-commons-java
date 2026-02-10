package com.senzing.sql;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Utilities for using the JDBC API to work with SQL databases.
 */
public class SQLUtilities {
    /**
     * A {@link Calendar} that can be used for retrieving timestamps
     * from the database.
     */
    public static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /**
     * Private constructor.
     */
    private SQLUtilities() {
        // do nothing
    }

    /**
     * Rolls back the transaction on the specified {@link Connection}, catching
     * any {@link SQLException} and logging it without rethrowing. This allows
     * you to more easily rollback when handling an exception and still rethrow
     * the original exception. If the specified {@link Connection} is
     * <code>null</code> then this method does nothing.
     *
     * @param conn The {@link Connection} to roll back, or <code>null</code> if
     *             the connection has not yet been obtained.
     */
    public static void rollback(Connection conn) {
        if (conn == null)
            return;
        try {
            conn.rollback();
        } catch (SQLException e) {
            System.err.println();
            System.err.println("***************************************************");
            System.err.println("Exception caught when rolling back transaction:");
            e.printStackTrace();
        }
    }

    /**
     * Closes the specified {@link Connection} catches and ignores any
     * {@link Exception} and returns <code>null</code> for easy semantics in
     * closing JDBC connections. If the specified parameter is <code>null</code>
     * then this method does nothing.
     *
     * <pre>
     * conn = SQLUtilities.close(conn);
     * </pre>
     *
     * @param conn The {@link Connection} to close.
     * @return Always returns <code>null</code>.
     */
    public static Connection close(Connection conn) {
        // check if null
        if (conn == null) {
            return null;
        }

        // check if closed already
        try {
            if (conn.isClosed()) {
                return null;
            }
        } catch (Exception e) {
            // ignore
        }

        // just close the exception if we can
        try {
            conn.close();
        } catch (Exception e) {
            // ignore
        }

        // return null
        return null;
    }

    /**
     * Closes the specified {@link Statement} catches and ignores any
     * {@link Exception} and returns <code>null</code> for easy semantics in
     * closing JDBC statements. If the specified parameter is <code>null</code>
     * then this method does nothing.
     *
     * <pre>
     * stmt = SQLUtilities.close(stmt);
     * </pre>
     *
     * @param stmt The {@link Statement} to close.
     * @return Always returns <code>null</code>.
     */
    public static Statement close(Statement stmt) {
        if (stmt == null)
            return null;
        try {
            if (stmt.isClosed())
                return null;
            stmt.close();
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * Closes the specified {@link PreparedStatement} catches and ignores
     * any {@link Exception} and returns <code>null</code> for
     * easy semantics in closing JDBC prepared statements. If the specified
     * parameter is <code>null</code> then this method does nothing.
     *
     * <pre>
     * ps = SQLUtilities.close(ps);
     * </pre>
     *
     * @param ps The {@link PreparedStatement} to close.
     * @return Always returns <code>null</code>.
     */
    public static PreparedStatement close(PreparedStatement ps) {
        if (ps == null)
            return null;
        try {
            if (ps.isClosed())
                return null;
            ps.close();
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * Closes the specified {@link CallableStatement} catches and ignores
     * any {@link Exception} and returns <code>null</code> for
     * easy semantics in closing JDBC callable statements. If the specified
     * parameter is <code>null</code> then this method does nothing.
     *
     * <pre>
     * cs = SQLUtilities.close(cs);
     * </pre>
     *
     * @param cs The {@link CallableStatement} to close.
     * @return Always returns <code>null</code>.
     */
    public static CallableStatement close(CallableStatement cs) {
        if (cs == null)
            return null;
        try {
            if (cs.isClosed())
                return null;
            cs.close();
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * Closes the specified {@link ResultSet} catches and ignores any
     * {@link Exception} and returns <code>null</code> for easy semantics in
     * closing JDBC result sets. If the specified parameter is <code>null</code>
     * then this method does nothing.
     *
     * <pre>
     * rs = SQLUtilities.close(rs);
     * </pre>
     *
     * @param rs The {@link ResultSet} to close.
     * @return Always returns <code>null</code>.
     */
    public static ResultSet close(ResultSet rs) {
        if (rs == null)
            return null;
        try {
            if (rs.isClosed())
                return null;
            rs.close();
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * Gets the {@link BigDecimal} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link BigDecimal} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static BigDecimal getBigDecimal(ResultSet rs, int index)
            throws SQLException {
        BigDecimal result = rs.getBigDecimal(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Boolean} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link Boolean} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Boolean getBoolean(ResultSet rs, int index)
            throws SQLException {
        boolean result = rs.getBoolean(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Byte} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link Byte} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Byte getByte(ResultSet rs, int index)
            throws SQLException {
        byte result = rs.getByte(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Date} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link Date} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Date getDate(ResultSet rs, int index)
            throws SQLException {
        Date result = rs.getDate(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Date} value from the specified {@link ResultSet}
     * at the specified column index using the specified {@link Calendar}.
     * If the value {@linkplain ResultSet#wasNull() was null} then
     * <code>null</code> is returned, otherwise the value obtained is returned.
     *
     * @param rs       The {@link ResultSet} to get the value from.
     * @param index    The column index for the value.
     * @param calendar The {@link Calendar} to use for creating the {@link Date}.
     *
     * @return The {@link Date} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Date getDate(ResultSet rs, int index, Calendar calendar)
            throws SQLException {
        Date result = rs.getDate(index, calendar);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Double} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link Double} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Double getDouble(ResultSet rs, int index)
            throws SQLException {
        double result = rs.getDouble(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Float} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link Float} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Float getFloat(ResultSet rs, int index)
            throws SQLException {
        float result = rs.getFloat(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Integer} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link Integer} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Integer getInt(ResultSet rs, int index)
            throws SQLException {
        int result = rs.getInt(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Long} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link Long} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Long getLong(ResultSet rs, int index)
            throws SQLException {
        long result = rs.getLong(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Short} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link Short} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Short getShort(ResultSet rs, int index)
            throws SQLException {
        short result = rs.getShort(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link String} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link String} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static String getString(ResultSet rs, int index)
            throws SQLException {
        String result = rs.getString(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Time} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link Time} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Time getTime(ResultSet rs, int index)
            throws SQLException {
        Time result = rs.getTime(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Time} value from the specified {@link ResultSet}
     * at the specified column index using the specified {@link Calendar}. If
     * the value {@linkplain ResultSet#wasNull() was null} then <code>null</code>
     * is returned, otherwise the value obtained is returned.
     *
     * @param rs       The {@link ResultSet} to get the value from.
     * @param index    The column index for the value.
     * @param calendar The {@link Calendar} to use for creating the {@link Time}.
     *
     * @return The {@link Time} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Time getTime(ResultSet rs, int index, Calendar calendar)
            throws SQLException {
        Time result = rs.getTime(index, calendar);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Timestamp} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs    The {@link ResultSet} to get the value from.
     * @param index The column index for the value.
     *
     * @return The {@link Timestamp} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Timestamp getTimestamp(ResultSet rs, int index)
            throws SQLException {
        Timestamp result = rs.getTimestamp(index);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Timestamp} value from the specified {@link ResultSet}
     * at the specified column index using the specified {@link Calendar}. If
     * the value {@linkplain ResultSet#wasNull() was null} then <code>null</code>
     * is returned, otherwise the value obtained is returned.
     *
     * @param rs       The {@link ResultSet} to get the value from.
     * @param index    The column index for the value.
     * @param calendar The {@link Calendar} to use for creating the {@link
     *                 Timestamp}.
     *
     * @return The {@link Timestamp} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Timestamp getTimestamp(ResultSet rs,
            int index,
            Calendar calendar)
            throws SQLException {
        Timestamp result = rs.getTimestamp(index, calendar);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link BigDecimal} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link BigDecimal} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static BigDecimal getBigDecimal(ResultSet rs, String columnName)
            throws SQLException {
        BigDecimal result = rs.getBigDecimal(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Boolean} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link Boolean} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Boolean getBoolean(ResultSet rs, String columnName)
            throws SQLException {
        boolean result = rs.getBoolean(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Byte} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link Byte} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Byte getByte(ResultSet rs, String columnName)
            throws SQLException {
        byte result = rs.getByte(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Date} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link Date} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Date getDate(ResultSet rs, String columnName)
            throws SQLException {
        Date result = rs.getDate(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Date} value from the specified {@link ResultSet}
     * at the specified column index using the specified {@link Calendar}.
     * If the value {@linkplain ResultSet#wasNull() was null} then
     * <code>null</code> is returned, otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     * @param calendar   The {@link Calendar} to use for creating the {@link Date}.
     *
     * @return The {@link Date} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Date getDate(ResultSet rs, String columnName, Calendar calendar)
            throws SQLException {
        Date result = rs.getDate(columnName, calendar);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Double} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link Double} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Double getDouble(ResultSet rs, String columnName)
            throws SQLException {
        double result = rs.getDouble(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Float} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link Float} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Float getFloat(ResultSet rs, String columnName)
            throws SQLException {
        float result = rs.getFloat(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Integer} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link Integer} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Integer getInt(ResultSet rs, String columnName)
            throws SQLException {
        int result = rs.getInt(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Long} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link Long} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Long getLong(ResultSet rs, String columnName)
            throws SQLException {
        long result = rs.getLong(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Short} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link Short} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Short getShort(ResultSet rs, String columnName)
            throws SQLException {
        short result = rs.getShort(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link String} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link String} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static String getString(ResultSet rs, String columnName)
            throws SQLException {
        String result = rs.getString(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Time} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link Time} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Time getTime(ResultSet rs, String columnName)
            throws SQLException {
        Time result = rs.getTime(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Time} value from the specified {@link ResultSet}
     * at the specified column index using the specified {@link Calendar}. If
     * the value {@linkplain ResultSet#wasNull() was null} then <code>null</code>
     * is returned, otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     * @param calendar   The {@link Calendar} to use for creating the {@link Time}.
     *
     * @return The {@link Time} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Time getTime(ResultSet rs, String columnName, Calendar calendar)
            throws SQLException {
        Time result = rs.getTime(columnName, calendar);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Timestamp} value from the specified {@link ResultSet}
     * at the specified column index. If the value {@linkplain
     * ResultSet#wasNull() was null} then <code>null</code> is returned,
     * otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     *
     * @return The {@link Timestamp} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Timestamp getTimestamp(ResultSet rs, String columnName)
            throws SQLException {
        Timestamp result = rs.getTimestamp(columnName);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * Gets the {@link Timestamp} value from the specified {@link ResultSet}
     * at the specified column index using the specified {@link Calendar}. If
     * the value {@linkplain ResultSet#wasNull() was null} then <code>null</code>
     * is returned, otherwise the value obtained is returned.
     *
     * @param rs         The {@link ResultSet} to get the value from.
     * @param columnName The column name for the value.
     * @param calendar   The {@link Calendar} to use for creating the {@link
     *                   Timestamp}.
     *
     * @return The {@link Timestamp} value or <code>null</code> if the value
     *         {@linkplain ResultSet#wasNull() was null}.
     *
     * @throws SQLException If a JDBC failure occurs.
     */
    public static Timestamp getTimestamp(ResultSet rs,
            String columnName,
            Calendar calendar)
            throws SQLException {
        Timestamp result = rs.getTimestamp(columnName, calendar);
        if (rs.wasNull())
            return null;
        return result;
    }

    /**
     * The test main function for this class.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        try {
            for (String jdbcUrl : args) {
                Connection conn = DriverManager.getConnection(jdbcUrl);
                System.out.println(
                        jdbcUrl + " : " + conn.getMetaData().getDatabaseProductName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
