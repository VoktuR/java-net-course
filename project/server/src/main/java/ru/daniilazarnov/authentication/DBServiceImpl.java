package ru.daniilazarnov.authentication;

import java.sql.*;

public class DBServiceImpl implements DBService {

    @Override
    public boolean findUser(String login, String password) {
        Connection connection = DBConnection.getConnection();

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM vault_users WHERE login = '" + login
                    + " AND password = " + password + "';");

            if (resultSet != null) {
                return true;
            }
            return false;

        } catch (SQLException e) {
            throw new RuntimeException("SWW", e);
        } finally {
            DBConnection.close(connection);
        }
    }

    @Override
    public void addUser(String login, String password) {
        Connection connection = DBConnection.getConnection();

        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO vault_users (login, password) VALUES (?, ?);"
            );

            statement.setString(1, login);
            statement.setString(2, password);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("SWW", e);
        } finally {
            DBConnection.close(connection);
        }
    }
}
