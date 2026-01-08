package com.thyagoronald.configs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.springframework.context.annotation.Configuration;

import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.utils.Logger;
import com.thyagoronald.utils.ProtocolConst;

import jakarta.annotation.PostConstruct;

@Configuration
public class SocketConfig {
    private final CommunicationService communicationService;
    private static final int PORT = 8081;
    private volatile boolean running = true;

    public SocketConfig(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @PostConstruct
    public void init() {
        new Thread(this::start).start();
    }

    public void start() {
        Logger.info("Iniciando servidor socket na porta " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (running) {
                Socket clientSocket = serverSocket.accept();

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
                if (inputLine.contains(ProtocolConst.HEARTBEAT_PREFIX)) {
                    Logger.info(inputLine);

                    communicationService.handleHeartbeat(out);
                } else if (inputLine.contains(ProtocolConst.REPLICATE_PREFIX)) {
                    communicationService.handleReplicate(inputLine, out);
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
