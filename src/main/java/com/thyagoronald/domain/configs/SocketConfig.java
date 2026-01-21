package com.thyagoronald.domain.configs;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.springframework.context.annotation.Configuration;

import com.thyagoronald.domain.services.CommunicationService;
import com.thyagoronald.domain.pojos.RequestPojo;
import com.thyagoronald.domain.pojos.ResponsePojo;
import com.thyagoronald.domain.utils.Logger;
import com.thyagoronald.domain.utils.ProtocolConst;

import jakarta.annotation.PostConstruct;

@Configuration
public class SocketConfig {
    private final CommunicationService communicationService;
    private static final int PORT = 8001;
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
        try (RequestPojo request = new RequestPojo(clientSocket);
                ResponsePojo response = new ResponsePojo(clientSocket)) {
            switch (request.getMethod()) {
                case ProtocolConst.HEARTBEAT_METHOD -> communicationService.handleHeartbeat(request, response);
                case ProtocolConst.REPLICATE_METHOD -> communicationService.handleReplicate(request, response);
                case ProtocolConst.RECOVER_METHOD -> communicationService.handleRecover(request, response);
                case ProtocolConst.HISTORY_METHOD -> communicationService.handleHistory(request, response);
                case ProtocolConst.HISTORY_RESET_METHOD -> communicationService.handleHistoryReset(request, response);
                default -> {
                    Logger.error("UNKNOWN: " + request.getMethod());
                    response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.FAILURE_METHOD);
                }
            }
        } catch (IOException e) {
            Logger.error("Erro ao tratar cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Logger.error("Não foi possível fechar conexão com cliente: " + e.getMessage());
            }
        }
    }
}
