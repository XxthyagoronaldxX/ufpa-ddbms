package com.thyagoronald;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import com.thyagoronald.pojos.HeartbeatPojo;
import com.thyagoronald.pojos.HostPojo;
import com.thyagoronald.utils.HostsFileReader;
import com.thyagoronald.utils.Logger;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class AppContext {
    private final ApplicationArguments args;
    private static final AtomicInteger leaderId = new AtomicInteger(0);
    private static final AtomicInteger nodeId = new AtomicInteger(0);
    private static final List<HostPojo> hosts = new ArrayList<>();
    private static final AtomicInteger connections = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        initHosts();
        initNode();
    }

    public void initNode() {
        if (args.containsOption("id")) {
            String idNode = args.getOptionValues("id").get(0);
            nodeId.set(Integer.parseInt(idNode));
        }

        Logger.info("Node ID: " + nodeId.get());
    }

    public void initHosts() {
        try {
            hosts.addAll(HostsFileReader.readHosts("hosts.txt").stream()
                .map(HostPojo::new)
                .toList());
        } catch (IOException e) {
            Logger.error("Erro ao ler o arquivo de hosts: " + e.getMessage());
        }
    }

    public HostPojo getLeastLoadedHost() {
        return hosts.stream()
            .filter(HostPojo::isAlive)
            .min((h1, h2) -> Integer.compare(h1.getConnections(), h2.getConnections()))
            .orElse(null);
    }

    public void updateCtxBy(HeartbeatPojo heartbeat, String host) {
        for (HostPojo hostPojo : hosts) {
            if (hostPojo.getHost().equals(host)) {
                hostPojo.setConnections(heartbeat.getConnections());
                hostPojo.setNodeId(heartbeat.getNodeId());
                return;
            }
        }
    }

    public void refreshLeader() {
        int actualLeaderId = leaderId.get();

        HostPojo actualLeader = hosts.stream()
            .filter(HostPojo::isAlive)
            .filter(h -> h.getNodeId() == actualLeaderId)
            .findFirst()
            .orElse(null);

        HostPojo newLeader = hosts.stream()
            .filter(HostPojo::isAlive)
            .max((h1, h2) -> Integer.compare(h1.getNodeId(), h2.getNodeId()))
            .orElse(null);
        
        if (Objects.isNull(actualLeader) && Objects.nonNull(newLeader)) {
            leaderId.set(newLeader.getNodeId());
        } else {
            if (Objects.nonNull(newLeader) && newLeader.getNodeId() > actualLeaderId) {
                leaderId.set(newLeader.getNodeId());
            }
        }

        if (actualLeaderId != leaderId.get()) {
            if (leaderId.get() == nodeId.get()) {
                Logger.info("Este nó é o novo líder. Node ID: " + nodeId.get());
            } else {
                Logger.info("Novo líder definido. Node ID: " + leaderId.get());
            }
        }
    }

    public int getTotalConnections() {
        return connections.get();
    }

    public int getNodeId() {
        return nodeId.get();
    }

    public int getLeaderId() {
        return leaderId.get();
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

    public List<HostPojo> getHostAlives() {
        return hosts.stream().filter(HostPojo::isAlive).toList();
    }

    public boolean isLeader() {
        return leaderId.get() == nodeId.get();
    }

    public void addHost(HostPojo host) {
        hosts.add(host);
    }

    public void removeHost(HostPojo host) {
        hosts.remove(host);
    }
}
