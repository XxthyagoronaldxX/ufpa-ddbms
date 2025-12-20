package com.thyagoronald.factories;

import java.util.List;

import com.thyagoronald.pojos.HostPojo;

public class HostFactory {
    public static List<HostPojo> createHostPojos(String[] hostStrings) {
        return List.of(hostStrings).stream().map(hostStr -> {
            String[] parts = hostStr.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            return HostPojo.builder()
                .host(host)
                .port(port)
                .isAlive(false)
                .build();
        }).toList();
    }
}
