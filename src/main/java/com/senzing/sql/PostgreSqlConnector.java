package com.senzing.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Implements a {@link Connector} that will format the JDBC URL for connecting
 * to a PostgreSql database.
 */
public class PostgreSqlConnector implements Connector {
  /**
   * The JDBC url for connecting.
   */
  private String jdbcUrl;

  /**
   * The additional connection properties.
   */
  private Properties properties;

  /**
   * Constructs with the specified connection parameters.
   *
   * @param host The host name or IP address of the database server.
   * @param port The port number for the database server.
   * @param database The name of the PostgreSql database.
   * @param user The user name for authentication with the PostgreSql server.
   * @param password The password for authentication with the PostgreSql server.
   */
  public PostgreSqlConnector(String   host,
                             int      port,
                             String   database,
                             String   user,
                             String   password)
  {
    this.jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
    this.properties = new Properties();
    this.properties.put("user", user);
    this.properties.put("password", password);
  }

  /**
   * Constructs with the specified connection parameters.
   *
   * @param host The host name or IP address of the database server.
   * @param port The port number for the database server.
   * @param database The name of the PostgreSql database.
   * @param user The user name for authentication with the PostgreSql server.
   * @param password The password for authentication with the PostgreSql server.
   * @oaran addlProperties The additional properties for connecting to the database.
   */
  public PostgreSqlConnector(String     host,
                             int        port,
                             String     database,
                             String     user,
                             String     password,
                             Properties addlProperties)
  {
    this.jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
    this.properties = new Properties();
    this.properties.putAll(addlProperties);
    this.properties.put("user", user);
    this.properties.put("password", password);
  }

  @Override
  public Connection openConnection() throws SQLException {
    Connection conn = DriverManager.getConnection(this.jdbcUrl,
                                                  this.properties);
    conn.setAutoCommit(false);
    return conn;
  }
}
