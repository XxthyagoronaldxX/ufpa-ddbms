package com.thyagoronald.services;

import java.sql.SQLException;

public interface DbService {
    public void execute(String query) throws SQLException;
}
