package com.thyagoronald.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.thyagoronald.pojos.HostPojo;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.services.DbService;
import com.thyagoronald.utils.HostsFileReader;
import com.thyagoronald.utils.Logger;
import com.thyagoronald.utils.ProtocolConst;

public class CommunicationServiceImpl implements CommunicationService {
    private final DbService dbService;
    private final List<HostPojo> hosts = new ArrayList<>();

    public CommunicationServiceImpl(DbService dbService, int port) {
        this.dbService = dbService;

        try {
            this.hosts.addAll(HostsFileReader.readHosts("hosts.txt").stream()
                .map(host -> new HostPojo(host, port, false))
                .toList());
        } catch (IOException e) {
            Logger.error("Erro ao ler o arquivo de hosts: " + e.getMessage());
        }
    }

    @Override
    public void sendHeartbeat() {
        for (HostPojo host : hosts) {
            if (host.isLocal())
                continue;

            try (Socket socket = new Socket(host.getHost(), host.getPort());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out.println(ProtocolConst.HEARTBEAT_REQUEST);

                String response = in.readLine();
                if (!ProtocolConst.HEARTBEAT_RESPONSE.equals(response))
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
        out.println(ProtocolConst.HEARTBEAT_RESPONSE);
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
        try {
            for (HostPojo host : hosts) {
                if (host.isLocal())
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
}
