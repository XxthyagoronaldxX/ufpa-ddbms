package com.thyagoronald.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.thyagoronald.configs.DbConfig;
import com.thyagoronald.dtos.CreateUserDTO;
import com.thyagoronald.models.UserModel;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UserService {
    public void createTable() throws SQLException {
        try (Connection connection = DbConfig.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "name VARCHAR(100) NOT NULL," +
                            "email VARCHAR(100) NOT NULL" +
                            ")");
            ps.executeUpdate();
        }
    }

    public UserModel create(CreateUserDTO dto) throws SQLException {
        UserModel user = new UserModel();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        try (Connection connection = DbConfig.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (name, email) VALUES (?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);

                    user.setId(id);
                }
            }
        }

        return user;
    }
}
