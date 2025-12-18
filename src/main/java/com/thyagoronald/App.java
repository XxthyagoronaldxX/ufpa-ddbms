package com.thyagoronald;

import java.util.Arrays;
import java.util.List;

import com.thyagoronald.pojos.HostPojo;
import com.thyagoronald.services.HeartbeatService;
import com.thyagoronald.services.SocketService;
import com.thyagoronald.utils.HostsFileReader;
import com.thyagoronald.utils.Logger;

public class App {
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            List<HostPojo> hosts = HostsFileReader.readHosts("hosts.txt").stream()
                .map(host -> new HostPojo(host, port, false))
                .toList();

            HeartbeatService heartbeatService = HeartbeatService.builder()
                .hosts(hosts)
                .intervalSeconds(10)
                .build();
            SocketService socketService = new SocketService(port);

            new Thread(heartbeatService::start).start();

            socketService.startServer();
        } catch (Exception ex) {
            Logger.error(ex.getMessage());
        }
    }
}