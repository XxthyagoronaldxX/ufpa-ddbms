package com.thyagoronald.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.thyagoronald.utils.Logger;

import lombok.Builder;

@Builder
public class HeartbeatService {
    private final List<String> hosts;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final int intervalSeconds;

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendHeartbeats, 0, intervalSeconds, TimeUnit.SECONDS);
        Logger.info("Heartbeat iniciado para hosts: " + hosts);
    }

    public void stop() {
        scheduler.shutdownNow();
        Logger.info("Heartbeat parado.");
    }

    private void sendHeartbeats() {
        for (String hostAux : hosts) {
            String host = hostAux.split(":")[0];
            int port = Integer.parseInt(hostAux.split(":")[1]);

            try (Socket socket = new Socket(host, port);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println("HBEAT");

                Logger.info("Heartbeat enviado para " + host + ":" + port);
            } catch (IOException e) {
                Logger.error("Falha ao enviar heartbeat para " + host + ":" + port + " - " + e.getMessage());
            }
        }
    }
}
