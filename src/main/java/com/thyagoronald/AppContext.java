package com.thyagoronald;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.thyagoronald.pojos.HostPojo;
import com.thyagoronald.utils.HostsFileReader;
import com.thyagoronald.utils.Logger;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AppContext {
    private final List<HostPojo> hosts = new ArrayList<>();
    private final AtomicInteger connections = new AtomicInteger(0);

    public void initHosts(int port) {
        try {
            this.hosts.addAll(HostsFileReader.readHosts("hosts.txt").stream()
                    .map(host -> new HostPojo(host, port, false))
                    .toList());
        } catch (IOException e) {
            Logger.error("Erro ao ler o arquivo de hosts: " + e.getMessage());
        }
    }

    public HostPojo getLeastLoadedHost() {
        return hosts.stream()
                .filter(HostPojo::isAlive)
                .filter(host -> !host.isLocal())
                .min((h1, h2) -> Integer.compare(h1.getConnections(), h2.getConnections()))
                .orElse(null);
    }

    public int getTotalConnections() {
        return connections.get();
    }

    public void newConnection() {
        connections.incrementAndGet();
    }

    public void closeConnection() {
        connections.decrementAndGet();
    }

    public List<HostPojo> getHosts() {
        return hosts;
    }

    public void addHost(HostPojo host) {
        hosts.add(host);
    }

    public void removeHost(HostPojo host) {
        hosts.remove(host);
    }
}
