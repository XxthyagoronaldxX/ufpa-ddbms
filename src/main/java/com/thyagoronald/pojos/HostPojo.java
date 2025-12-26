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
    private boolean isAlive;
    private boolean isLocal;

    public HostPojo(String host, int port, boolean isAlive) {
        this.host = host;
        this.port = port;
        this.isAlive = isAlive;

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String hostAddress = inetAddress.getHostAddress();
            System.out.println(hostAddress);
            System.out.println(host);
            this.isLocal = hostAddress.equals(host);
        } catch (UnknownHostException ex) {
            Logger.error("Erro ao obter o endereço do host local: " + ex.getMessage());
        }
    }
}
