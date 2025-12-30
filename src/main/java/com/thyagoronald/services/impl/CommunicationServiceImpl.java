package com.thyagoronald.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

import com.thyagoronald.AppContext;
import com.thyagoronald.pojos.HostPojo;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.services.DbService;
import com.thyagoronald.utils.GetIt;
import com.thyagoronald.utils.Logger;
import com.thyagoronald.utils.ProtocolConst;

public class CommunicationServiceImpl implements CommunicationService {
    private final DbService dbService;

    public CommunicationServiceImpl(DbService dbService) {
        this.dbService = dbService;
    }

    @Override
    public void sendHeartbeat() {
        AppContext appContext = GetIt.getInstance().find(AppContext.class);
        List<HostPojo> hosts = appContext.getHosts();

        for (HostPojo host : hosts) {
            if (host.isLocal() || !host.isAlive())
                continue;

            try (Socket socket = new Socket(host.getHost(), host.getPort());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out.println(ProtocolConst.HEARTBEAT_PREFIX);

                String response = in.readLine();
                if (!ProtocolConst.HEARTBEAT_SUCCESS.equals(response))
                    throw new IOException("Resposta inesperada: " + response);

                host.setAlive(true);

                Logger.info("Heartbeat enviado para " + host.getHost() + ":" + host.getPort());
            } catch (IOException e) {
                host.setAlive(false);

                Logger.error("Falha ao enviar heartbeat para " + host.getHost() + ":" + host.getPort() + " - "
                        + e.getMessage());
            }
        }
    }

    @Override
    public void handleHeartbeat(PrintWriter out) {
        out.println(ProtocolConst.HEARTBEAT_SUCCESS);
    }

    @Override
    public void handleReplicate(String input, PrintWriter out) {
        String query = input.replaceFirst("^REPLICATE\\s+", "").trim();

        try {
            dbService.execute(query);
            out.println(ProtocolConst.REPLICATE_SUCCESS);
        } catch (SQLException e) {
            Logger.error("Erro ao executar REPLICATION: " + e.getMessage());
            out.println(ProtocolConst.ERROR_RESPONSE);
        }
    }

    @Override
    public void sendReplication(String data) {
        AppContext appContext = GetIt.getInstance().find(AppContext.class);
        List<HostPojo> hosts = appContext.getHosts();

        try {
            for (HostPojo host : hosts) {
                if (host.isLocal() || !host.isAlive())
                    continue;

                try (Socket socket = new Socket(host.getHost(), host.getPort());
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    out.println(ProtocolConst.REPLICATE_PREFIX + data);

                    String response = in.readLine();

                    // Criar um schedule caso o host esteja offline para tentar reenviar depois
                    // Lembrar de criar uma fila de mensagens pendentes para cada host

                    Logger.info("sendReplication response: " + response);
                }
            }
        } catch (IOException ex) {
            Logger.error("Erro de I/O ao enviar replicação: " + ex.getMessage());
        }
    }

    @Override
    public String sendLoadBalancer(String data) {
        AppContext appContext = GetIt.getInstance().find(AppContext.class);
        List<HostPojo> hosts = appContext.getHosts();

        try {
            for (HostPojo host : hosts) {
                if (host.isLocal() || !host.isAlive())
                    continue;

                try (Socket socket = new Socket(host.getHost(), host.getPort());
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    out.println(ProtocolConst.LOADBALANCER_PREFIX + data);

                    String response = in.readLine();

                    Logger.info("sendLoadBalancer response: " + response);

                    return response;
                }
            }
        } catch (IOException ex) {
            Logger.error("Erro de I/O ao enviar load balancer: " + ex.getMessage());
            return "Erro de I/O ao enviar load balancer: " + ex.getMessage();
        }

        return null;
    }
}
