package org.vessl.sql.handle;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlSession {

   static ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();

    public static <T> T getMapper(Class<T> tClass) {
        return MapperManager.getMapper(tClass);
    }

    public static void beginTrans() throws SQLException {
        DataSource dataSource = MapperManager.getDataSource();
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        connectionThreadLocal.set(connection);
    }

    public static void commit() throws SQLException {
        Connection connection = connectionThreadLocal.get();
        connectionThreadLocal.remove();
        try (connection){
            if (connection != null) {
                connection.commit();
            }
        }


    }
    public static void rollback() throws SQLException {
        Connection connection = connectionThreadLocal.get();
        connectionThreadLocal.remove();
        try (connection){
            if (connection != null) {
                connection.rollback();
            }
        }
    }

    public static int executeSql(String sql) throws SQLException {
        boolean isTrans = true;
        Connection connection = SqlSession.connectionThreadLocal.get();
        if (connection == null) {
            isTrans = false;
            connection = MapperManager.getDataSource().getConnection();
        }
        try {
            Statement statement = connection.createStatement();
            return statement.executeUpdate(sql);
        } finally {
            if (!isTrans) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
