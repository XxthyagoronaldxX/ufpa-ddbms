package com.thyagoronald.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.thyagoronald.utils.Logger;

public class SocketService {
    private int port;

    public SocketService(int port) {
        this.port = port;
    }

    public void startServer() {
        Logger.info("Iniciando servidor socket na porta " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
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
                Logger.info("Recebido: " + inputLine);
                
                if (inputLine.contains("HBEAT")) {

                } else if (inputLine.contains("REPLICATE")) {

                } else if (inputLine.contains("QUERY")) {
                    
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
