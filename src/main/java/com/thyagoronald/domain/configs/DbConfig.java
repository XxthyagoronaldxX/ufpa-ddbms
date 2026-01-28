package com.thyagoronald.domain.configs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConfig {
    private DbConfig() {
    }

    public static Connection getConnection() throws SQLException {
        final String dbHost = System.getenv("MYSQL_DB_HOST");
        final String dbPort = System.getenv("MYSQL_DB_PORT");
        final String dbUser = System.getenv("MYSQL_DB_USER");
        final String dbPass = System.getenv("MYSQL_DB_PASSWORD");
        final String url = "jdbc:mysql://" + dbHost
            + ":" + dbPort
            + "/school_db?useSSL=false&createDatabaseIfNotExist=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        return DriverManager.getConnection(url, dbUser, dbPass);
    }
}
