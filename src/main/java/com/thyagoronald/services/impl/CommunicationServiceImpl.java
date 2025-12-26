package com.thyagoronald.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.thyagoronald.pojos.HostPojo;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.utils.HostsFileReader;
import com.thyagoronald.utils.Logger;
import com.thyagoronald.utils.ProtocolConst;

public class CommunicationServiceImpl implements CommunicationService {
    private List<HostPojo> hosts = new ArrayList<>();

    public CommunicationServiceImpl(int port) {
        try {
            this.hosts = HostsFileReader.readHosts("hosts.txt").stream()
                    .map(host -> new HostPojo(host, port, false))
                    .toList();
        } catch (IOException e) {
            Logger.error("Erro ao ler o arquivo de hosts: " + e.getMessage());
            this.hosts = new ArrayList<>();
        }
    }

    @Override
    public void sendHeartbeat() {
        for (HostPojo host : hosts) {
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
    public void sendReplication(String data) {
        for (HostPojo host : hosts) {
            try (Socket socket = new Socket(host.getHost(), host.getPort());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out.println(ProtocolConst.REPLICATE_PREFIX + data);

                String response = in.readLine();

                // Criar um schedule caso o host esteja offline para tentar reenviar depois
                // Lembrar de criar uma fila de mensagens pendentes para cada host

                Logger.info("sendReplication response: " + response);
            } catch (IOException e) {
                Logger.error("Falha ao enviar replicação para " + host.getHost() + ":" + host.getPort() + " - "
                        + e.getMessage());
            }
        }
    }
}
