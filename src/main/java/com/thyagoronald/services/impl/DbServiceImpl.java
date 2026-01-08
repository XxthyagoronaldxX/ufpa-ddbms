package com.thyagoronald.services.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.stereotype.Component;

import com.thyagoronald.configs.DbConfig;
import com.thyagoronald.services.DbService;

@Component
public class DbServiceImpl implements DbService {
    @Override
    public void execute(String query) throws SQLException {
        try (Connection connection = DbConfig.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.executeUpdate();
            }
        }
    }
}
