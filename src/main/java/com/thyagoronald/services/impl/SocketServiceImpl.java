package com.thyagoronald.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.thyagoronald.services.ApiService;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.services.SocketService;
import com.thyagoronald.utils.Logger;
import com.thyagoronald.utils.ProtocolConst;

public class SocketServiceImpl implements SocketService {
    private final CommunicationService communicationService;
    private final ApiService apiService;
    private final int port;
    private volatile boolean running = true;

    public SocketServiceImpl(CommunicationService communicationService, ApiService apiService, int port) {
        this.communicationService = communicationService;
        this.apiService = apiService;
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
                    communicationService.handleHeartbeat(out);
                } else if (inputLine.contains(ProtocolConst.REPLICATE_PREFIX)) {
                    communicationService.handleReplicate(inputLine, out);
                } else if (inputLine.contains(ProtocolConst.QUERY_PREFIX)) {
                    apiService.handleQuery(inputLine, out);
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
}
