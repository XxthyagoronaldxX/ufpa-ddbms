package com.thyagoronald.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.thyagoronald.pojos.HostPojo;
import com.thyagoronald.utils.Logger;

import lombok.Builder;

@Builder
public class HeartbeatService {
    private final List<HostPojo> hosts;
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
        for (HostPojo host : hosts) {
            try (Socket socket = new Socket(host.getHost(), host.getPort());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println("HBEAT");

                Logger.info("Heartbeat enviado para " + host + ":" + host.getPort());

                host.setAlive(true);
            } catch (IOException e) {
                host.setAlive(false);
                Logger.error("Falha ao enviar heartbeat para " + host + ":" + host.getPort() + " - " + e.getMessage());
            }
        }
    }
}
