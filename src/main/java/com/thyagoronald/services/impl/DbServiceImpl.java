package com.thyagoronald.services.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.thyagoronald.configs.DbConfig;
import com.thyagoronald.services.DbService;

@Component
public class DbServiceImpl implements DbService {
    @Override
    public Optional<List<Map<String, Object>>> execute(String query) throws SQLException {
        try (Connection connection = DbConfig.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                if (query.startsWith("SELECT")) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    ResultSet rs = ps.executeQuery();
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = meta.getColumnName(i);
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }

                        result.add(row);
                    }

                    return Optional.of(result);
                } else {
                    ps.executeUpdate();
                }
            }
        }

        return Optional.empty();
    }
}
