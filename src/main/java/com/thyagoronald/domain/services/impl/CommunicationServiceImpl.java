package com.thyagoronald.domain.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thyagoronald.AppContext;
import com.thyagoronald.api.middleware.LoadBalancingFilter;
import com.thyagoronald.domain.configs.DbConfig;
import com.thyagoronald.domain.pojos.HeartbeatPojo;
import com.thyagoronald.domain.pojos.HistoryPojo;
import com.thyagoronald.domain.pojos.HostPojo;
import com.thyagoronald.domain.pojos.RequestPojo;
import com.thyagoronald.domain.pojos.ResponsePojo;
import com.thyagoronald.domain.services.CommunicationService;
import com.thyagoronald.domain.utils.ChecksumUtil;
import com.thyagoronald.domain.utils.Logger;
import com.thyagoronald.domain.utils.ProtocolConst;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class CommunicationServiceImpl implements CommunicationService {
    private static final int TIMEOUT_MS = 2000;
    private final AppContext appContext;

    @Override
    public void sendHeartbeat() {
        List<HostPojo> hosts = appContext.getHosts();

        Logger.info("LEADER ID: " + appContext.getLeaderId());

        for (HostPojo host : hosts) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host.getHost(), host.getPort()), TIMEOUT_MS);
                socket.setSoTimeout(TIMEOUT_MS);

                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    HeartbeatPojo heartbeat = new HeartbeatPojo();

                    heartbeat.setNodeId(appContext.getNodeId());
                    heartbeat.setConnections(LoadBalancingFilter.getCurrentRequestCount());

                    String content = new ObjectMapper().writeValueAsString(heartbeat);

                    out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.HEARTBEAT_METHOD);
                    out.println(ProtocolConst.CRC_ATTR + ChecksumUtil.generateChecksum(content));
                    out.println(ProtocolConst.CONTENT_ATTR + content);

                    String response = in.readLine();
                    if (!ProtocolConst.HEARTBEAT_SUCCESS.equals(response))
                        throw new IOException(response);

                    host.setAlive(true);

                    Logger.info("HEARTBEAT: " + host.getHost() + ":" + host.getPort());
                }
            } catch (IOException e) {
                host.setAlive(false);
                Logger.error("HEARTBEAT FAILED: " + host.getHost() + ":" + host.getPort() + " - " + e.getMessage());
            }
        }
    }

    @Override
    public void handleHeartbeat(RequestPojo request, ResponsePojo response) {
        try {
            if (ChecksumUtil.validateChecksum(request.getContent(), request.getCrc())) {
                HeartbeatPojo heartbeatPojo = new ObjectMapper().readValue(request.getContent(), HeartbeatPojo.class);

                appContext.updateCtxBy(heartbeatPojo, request.getHost());
                if (appContext.isLeader()) {
                    for (HostPojo host : appContext.getHostAlives()) {
                        if (!host.getHistory().isEmpty() && sendRecover(host)) {
                            sendHistoryReset(host.getNodeId());
                        }
                    }
                }
                appContext.refreshLeader();

                response.sendLine(ProtocolConst.HEARTBEAT_SUCCESS);
            } else {
                Logger.error("HEARTBEAT EXCEPTION: INVALID CHECKSUM DETECTED");
                response.sendLine(ProtocolConst.ERROR_CRC_RESPONSE);
            }
        } catch (NumberFormatException | IOException ex) {
            Logger.error("HEARTBEAT EXCEPTION: " + ex.getMessage());
            response.sendLine(ProtocolConst.ERROR_INTERNAL_SERVER_RESPONSE);
        }
    }

    @Override
    public void handleReplicate(RequestPojo request, ResponsePojo response) {
        try {
            if (ChecksumUtil.validateChecksum(request.getContent(), request.getCrc())) {
                try (Connection connection = DbConfig.getConnection()) {
                    connection.setAutoCommit(false);

                    try (PreparedStatement ps = connection.prepareStatement(request.getContent())) {
                        ps.executeUpdate();
                        response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.SUCCESS_METHOD);
                        String result = request.readLine();

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
            response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.REPLICATE_ABORT_METHOD);
        }
    }

    @Override
    public void sendHistory(HistoryPojo history) {
        List<HostPojo> hosts = appContext.getHosts();

        for (HostPojo host : hosts) {
            if (!host.isAlive())
                continue;

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host.getHost(), host.getPort()), TIMEOUT_MS);
                socket.setSoTimeout(TIMEOUT_MS);

                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String content = new ObjectMapper().writeValueAsString(history);

                    out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.HISTORY_METHOD);
                    out.println(ProtocolConst.CRC_ATTR + ChecksumUtil.generateChecksum(content));
                    out.println(ProtocolConst.CONTENT_ATTR + content);

                    String response = in.readLine();

                    if (!response.contains(ProtocolConst.SUCCESS_METHOD))
                        throw new IOException(response);

                    Logger.info("HISTORY SENT: " + host.getHost() + ":" + host.getPort());
                }
            } catch (IOException ex) {
                Logger.error("HISTORY SEND FAILED: " + host.getHost() + ":" + host.getPort() + " - " + ex.getMessage());
            }
        }
    }

    @Override
    public void handleHistory(RequestPojo request, ResponsePojo response) {
        try {
            if (ChecksumUtil.validateChecksum(request.getContent(), request.getCrc())) {
                HistoryPojo historyPojo = new ObjectMapper().readValue(request.getContent(), HistoryPojo.class);

                appContext.addHistory(historyPojo);

                response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.SUCCESS_METHOD);
            } else {
                Logger.error("INVALID CHECKSUM DETECTED");
                response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.ERROR_CRC_RESPONSE);
            }
        } catch (NumberFormatException | IOException ex) {
            Logger.error("HISTORY EXCEPTION: " + ex.getMessage());
            response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.ERROR_INTERNAL_SERVER_RESPONSE);
        }
    }

    @Override
    public void handleRecover(RequestPojo request, ResponsePojo response) {
        try {
            if (ChecksumUtil.validateChecksum(request.getContent(), request.getCrc())) {
                List<String> queries = List.of(new ObjectMapper().readValue(request.getContent(), String[].class));

                try (Connection connection = DbConfig.getConnection()) {
                    for (String query : queries) {
                        try (PreparedStatement ps = connection.prepareStatement(query)) {
                            ps.executeUpdate();
                        }
                    }
                }

                response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.SUCCESS_METHOD);
            } else {
                throw new IOException("INVALID CHECKSUM DETECTED");
            }
        } catch (IOException | SQLException ex) {
            Logger.error("[EXCEPTION] RECOVER: " + ex.getMessage());
            response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.FAILURE_METHOD);
        }
    }

    @Override
    public boolean sendRecover(HostPojo host) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host.getHost(), host.getPort()), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String content = new ObjectMapper().writeValueAsString(host.getHistory());

                out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.RECOVER_METHOD);
                out.println(ProtocolConst.CRC_ATTR + ChecksumUtil.generateChecksum(content));
                out.println(ProtocolConst.CONTENT_ATTR + content);

                String response = in.readLine();

                if (!response.contains(ProtocolConst.SUCCESS_METHOD))
                    throw new IOException(response);

                Logger.info("RECOVER SENT: " + host.getHost() + ":" + host.getPort());

                return true;
            }
        } catch (IOException ex) {
            Logger.error("RECOVER SEND FAILED: " + host.getHost() + ":" + host.getPort() + " - " + ex.getMessage());
            return false;
        }
    }

    @Override
    public void sendHistoryReset(int nodeId) {
        List<HostPojo> hosts = appContext.getHosts();

        for (HostPojo host : hosts) {
            if (!host.isAlive())
                continue;

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host.getHost(), host.getPort()), TIMEOUT_MS);
                socket.setSoTimeout(TIMEOUT_MS);

                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.HISTORY_RESET_METHOD);
                    out.println(ProtocolConst.CRC_ATTR + ChecksumUtil.generateChecksum(String.valueOf(nodeId)));
                    out.println(ProtocolConst.CONTENT_ATTR + String.valueOf(nodeId));

                    String response = in.readLine();

                    if (!response.contains(ProtocolConst.SUCCESS_METHOD))
                        throw new IOException(response);

                    Logger.info("HISTORY RESET SENT: " + host.getHost() + ":" + host.getPort());
                }
            } catch (IOException ex) {
                Logger.error("HISTORY RESET SEND FAILED: " + host.getHost() + ":" + host.getPort() + " - "
                        + ex.getMessage());
            }
        }
    }

    @Override
    public void handleHistoryReset(RequestPojo request, ResponsePojo response) {
        try {
            if (ChecksumUtil.validateChecksum(request.getContent(), request.getCrc())) {
                int nodeId = Integer.parseInt(request.getContent());

                for (HostPojo host : appContext.getHosts()) {
                    if (host.getNodeId() == nodeId) {
                        host.getHistory().clear();
                        break;
                    }
                }

                response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.SUCCESS_METHOD);
            } else {
                Logger.error("INVALID CHECKSUM DETECTED");
                response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.ERROR_CRC_RESPONSE);
            }
        } catch (NumberFormatException ex) {
            Logger.error("HISTORY RESET EXCEPTION: " + ex.getMessage());
            response.sendLine(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.ERROR_INTERNAL_SERVER_RESPONSE);
        }
    }
}
