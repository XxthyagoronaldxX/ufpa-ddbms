package com.thyagoronald.services;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DbService {
    public Optional<List<Map<String, Object>>> execute(String query) throws SQLException;
}
