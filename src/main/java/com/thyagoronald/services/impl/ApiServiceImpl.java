package com.thyagoronald.services.impl;

import java.io.PrintWriter;
import java.sql.SQLException;

import com.thyagoronald.services.ApiService;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.services.DbService;
import com.thyagoronald.utils.Logger;
import com.thyagoronald.utils.ProtocolConst;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ApiServiceImpl implements ApiService {
    private final DbService dbService;
    private final CommunicationService communicationService;

    @Override
    public void handleQuery(String input, PrintWriter out) {
        String query = input.replaceFirst("^QUERY\\s+", "").trim();

        try {
            dbService.execute(query);
            communicationService.sendReplication(query);
            out.println(ProtocolConst.QUERY_SUCCESS);
        } catch (SQLException e) {
            Logger.error("Erro ao executar QUERY: " + e.getMessage());
            out.println(ProtocolConst.ERROR_RESPONSE);
        }
    }
}
