package com.thyagoronald.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.thyagoronald.AppContext;
import com.thyagoronald.configs.DbConfig;
import com.thyagoronald.errors.ReplicateHostException;
import com.thyagoronald.middleware.LoadBalancingFilter;
import com.thyagoronald.pojos.HostPojo;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.utils.ChecksumUtil;
import com.thyagoronald.utils.Logger;
import com.thyagoronald.utils.ProtocolConst;
import com.thyagoronald.utils.ProtocolUtil;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class CommunicationServiceImpl implements CommunicationService {
    private final AppContext appContext;

    @Override
    public void sendHeartbeat() {
        List<HostPojo> hosts = appContext.getHosts();

        for (HostPojo host : hosts) {
            if (host.isLocal())
                continue;

            try (Socket socket = new Socket(host.getHost(), host.getPort());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String content = ProtocolConst.CONTENT_ATTR + LoadBalancingFilter.getCurrentRequestCount();

                out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.HEARTBEAT_METHOD);
                out.println(ProtocolConst.CRC_ATTR + ChecksumUtil.generateChecksum(content));
                out.println(content);

                String response = in.readLine();
                if (!ProtocolConst.HEARTBEAT_SUCCESS.equals(response))
                    throw new IOException("Resposta inesperada: " + response);

                host.setAlive(true);

                Logger.info("HEARTBEAT enviado para " + host.getHost() + ":" + host.getPort());
            } catch (IOException e) {
                host.setAlive(false);

                Logger.error("Falha ao enviar heartbeat para " + host.getHost() + ":" + host.getPort() + " - "
                        + e.getMessage());
            }
        }
    }

    @Override
    public void sendReplication(String data) {
        List<HostPojo> hosts = appContext.getHosts().stream()
                .filter(HostPojo::isAlive)
                .filter(host -> !host.isLocal())
                .toList();
        List<Integer> hostSuccess = new ArrayList<>(hosts.stream().map(host -> 0).toList());
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < hosts.size(); i++) {
            final HostPojo host = hosts.get(i);
            final int index = i;

            Thread thread = createReplicateThread(host, data, hostSuccess, index);
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException | ReplicateHostException e) {
                Thread.currentThread().interrupt();
                Logger.error("REPLICATE THREAD ERROR: " + e.getMessage());
                throw new ReplicateHostException("Replication failed on one or more hosts");
            }
        }

        Logger.info("TCKUBRIIM");

        if (hostSuccess.contains(-1))
            throw new ReplicateHostException("Replication failed on one or more hosts");
    }

    private Thread createReplicateThread(HostPojo host, String data, List<Integer> hostSuccess, int index) {
        return new Thread(() -> {
            try (Socket socket = new Socket(host.getHost(), host.getPort());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                socket.setSoTimeout(10000);
                String content = ProtocolConst.CONTENT_ATTR + data;

                out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.REPLICATE_METHOD);
                out.println(ProtocolConst.CRC_ATTR + ChecksumUtil.generateChecksum(content));
                out.println(content);

                String method = in.readLine();

                if (method.contains(ProtocolConst.SUCCESS_METHOD)) {
                    Logger.info("[STATUS] REPLICATION SUCCESS");
                    hostSuccess.set(index, 1);

                    int cont = 0;
                    while (hostSuccess.contains(0)) {
                        Logger.info("[HOST - THREAD] Waiting for other hosts...");
                        Thread.sleep(1000);
                        cont++;
                        if (cont == 10) {
                            throw new IOException("SOCKET TIMEOUT");
                        }
                    }

                    if (!hostSuccess.contains(0) && hostSuccess.contains(1)) {
                        out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.REPLICATE_COMMIT_METHOD);
                    } else {
                        out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.REPLICATE_ABORT_METHOD);
                    }
                } else {
                    Logger.error("[STATUS] REPLICATION FAILED");
                    hostSuccess.set(index, -1);
                }
            } catch (IOException | InterruptedException ex) {
                Logger.error("[THREAD - EXCEPTION]: " + ex.getMessage());
                hostSuccess.set(index, -1);
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public void handleHeartbeat(BufferedReader in, PrintWriter out) {
        try {
            String crcLine = in.readLine();
            String crcStr = ProtocolUtil.getData(crcLine, ProtocolConst.CRC_ATTR);
            long crc = Long.parseLong(crcStr);
            String contentLine = in.readLine();

            if (ChecksumUtil.validateChecksum(contentLine, crc)) {
                out.println(ProtocolConst.HEARTBEAT_SUCCESS);
            } else {
                Logger.error("INVALID CHECKSUM DETECTED");
                out.println(ProtocolConst.ERROR_CRC_RESPONSE);
            }
        } catch (IOException ex) {
            Logger.error("[EXCEPTION] HEARTBEAT: " + ex.getMessage());
            out.println(ProtocolConst.ERROR_INTERNAL_SERVER_RESPONSE);
        }
    }

    @Override
    public void handleReplicate(BufferedReader in, PrintWriter out) {
        try {
            String crcLine = in.readLine();
            String crcStr = ProtocolUtil.getData(crcLine, ProtocolConst.CRC_ATTR);
            long crc = Long.parseLong(crcStr);
            String contentLine = in.readLine();
            String content = ProtocolUtil.getData(contentLine, ProtocolConst.CONTENT_ATTR);

            if (ChecksumUtil.validateChecksum(contentLine, crc)) {
                try (Connection connection = DbConfig.getConnection()) {
                    connection.setAutoCommit(false);

                    try (PreparedStatement ps = connection.prepareStatement(content)) {
                        ps.executeUpdate();
                        out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.SUCCESS_METHOD);
                        String result = in.readLine();

                        if (Objects.isNull(result) || result.contains(ProtocolConst.REPLICATE_ABORT_METHOD)) {
                            connection.rollback();
                            Logger.info("[RECV] ABORT REPLICATION");
                        } else if (result.contains(ProtocolConst.REPLICATE_COMMIT_METHOD)) {
                            connection.commit();
                            Logger.info("[RECV] COMMIT REPLICATION");
                        }
                    }
                }
            } else {
                throw new IOException("INVALID CHECKSUM DETECTED");
            }
        } catch (IOException | SQLException ex) {
            Logger.error("[EXCEPTION] REPLICATE: " + ex.getMessage());
            out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.REPLICATE_ABORT_METHOD);
        }
    }
}
