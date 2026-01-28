package com.thyagoronald.domain.pojos;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.thyagoronald.domain.utils.Logger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class HostPojo {
    private String host;
    private int apiPort;
    private int port;
    private int connections;
    private int nodeId;
    private boolean isAlive;
    private boolean isLocal;
    private List<String> history;

    public HostPojo(String host) {
        String[] parts = host.split(":");

        this.host = parts[0];
        this.apiPort = Integer.parseInt(parts[1]);
        this.port = 8001;
        this.isAlive = false;
        this.connections = 0;
        this.nodeId = 0;
        this.history = new ArrayList<>();

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String hostAddress = inetAddress.getHostAddress();
            this.isLocal = hostAddress.equals(this.host);
        } catch (UnknownHostException ex) {
            Logger.error("Erro ao obter o endereço do host local: " + ex.getMessage());
        }
    }

    public void incrementConnections() {
        this.connections++;
    }

    public void decrementConnections() {
        if (this.connections > 0) {
            this.connections--;
        }
    }
}
