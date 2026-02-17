package com.civics.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/civics?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=15000&autoReconnect=true";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBUtil] MySQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            if (conn == null || conn.isClosed()) {
                throw new SQLException("Failed to establish database connection");
            }
            return conn;
        } catch (SQLException e) {
            System.err.println("[DBUtil] Connection failed: " + e.getMessage());
            throw e;
        }
    }
}
