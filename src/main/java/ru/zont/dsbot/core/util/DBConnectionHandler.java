package ru.zont.dsbot.core.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DBConnectionHandler {
    private static final int CHECK_TIMEOUT = 10;
    private final String conString;

    private final ArrayList<Connection> pool = new ArrayList<>(10);

    public DBConnectionHandler(String conString) throws SQLException {
        this.conString = conString;
    }

    public <T> T withPrepStatement(String query, SqlPreparedStatementFunction<T> function) {
        try {
            Connection con = getConnection();
            PreparedStatement st = con.prepareStatement(query);
            T res = function.apply(st);
            st.close();
            releaseConnection(con);
            return res;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T withStatement(SqlStatementFunction<T> function) {
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            T res = function.apply(st);
            st.close();
            releaseConnection(con);
            return res;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void withPrepStatement(String query, SqlPreparedStatementSupplier function) {
        try {
            Connection con = getConnection();
            PreparedStatement st = con.prepareStatement(query);
            function.apply(st);
            st.close();
            releaseConnection(con);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void withStatement(SqlStatementSupplier function) {
        try {
            Connection con = getConnection();
            Statement st = con.createStatement();
            function.apply(st);
            st.close();
            releaseConnection(con);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (pool.size() > 0)
            return checkReturnConnection(pool.remove(pool.size() - 1));
        else return createConnection();
    }

    private synchronized void releaseConnection(Connection connection) {
        pool.add(connection);
    }

    private Connection checkReturnConnection(Connection connection) throws SQLException {
        if (!connection.isValid(CHECK_TIMEOUT))
            return createConnection();
        else return connection;
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(conString);
    }

    public interface SqlStatementFunction<T> {
        T apply(Statement st) throws SQLException;
    }

    public interface SqlPreparedStatementFunction<T> {
        T apply(PreparedStatement st) throws SQLException;
    }

    public interface SqlStatementSupplier {
        void apply(Statement st) throws SQLException;
    }

    public interface SqlPreparedStatementSupplier {
        void apply(PreparedStatement st) throws SQLException;
    }
}
