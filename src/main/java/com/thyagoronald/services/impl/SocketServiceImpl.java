package com.thyagoronald.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;

import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.services.DbService;
import com.thyagoronald.services.SocketService;
import com.thyagoronald.utils.GetIt;
import com.thyagoronald.utils.Logger;
import com.thyagoronald.utils.ProtocolConst;

public class SocketServiceImpl implements SocketService {
    private final int port;
    private volatile boolean running = true;

    public SocketServiceImpl(int port) {
        this.port = port;
    }

    @Override
    public void startServer() {
        Logger.info("Iniciando servidor socket na porta " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                Logger.info("Cliente conectado: " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            Logger.error("Erro no servidor: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains(ProtocolConst.HEARTBEAT_REQUEST)) {
                    handleHeartbeat(out);
                } else if (inputLine.contains(ProtocolConst.REPLICATE_PREFIX)) {
                    handleReplicate(inputLine, out);
                } else if (inputLine.contains(ProtocolConst.QUERY_PREFIX)) {
                    handleQuery(inputLine, out);
                }
            }
        } catch (IOException e) {
            Logger.error("Erro ao tratar cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Logger.error("Erro ao fechar conexão com cliente: " + e.getMessage());
            }
        }
    }

    private void handleHeartbeat(PrintWriter out) {
        out.println(ProtocolConst.HEARTBEAT_RESPONSE);
    }

    private void handleReplicate(String input, PrintWriter out) {
        DbService dbService = GetIt.getInstance().find(DbService.class);
        String query = input.replaceFirst("^REPLICATE\\s+", "").trim();

        try {
            dbService.execute(query);
            out.println(ProtocolConst.REPLICATE_SUCCESS);
        } catch (SQLException e) {
            Logger.error("Erro ao executar REPLICATION: " + e.getMessage());
            out.println(ProtocolConst.ERROR_RESPONSE);
        }
    }

    private void handleQuery(String input, PrintWriter out) {
        DbService dbService = GetIt.getInstance().find(DbService.class);
        CommunicationService communicationService = GetIt.getInstance().find(CommunicationService.class);
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
