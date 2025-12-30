package com.thyagoronald.pojos;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.thyagoronald.utils.Logger;

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
    private int port;
    private int connections;
    private boolean isAlive;
    private boolean isLocal;

    public HostPojo(String host, int port, boolean isAlive) {
        this.host = host;
        this.port = port;
        this.isAlive = isAlive;
        this.connections = 0;

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String hostAddress = inetAddress.getHostAddress();
            this.isLocal = hostAddress.equals(host);
        } catch (UnknownHostException ex) {
            Logger.error("Erro ao obter o endereço do host local: " + ex.getMessage());
        }
    }
}
