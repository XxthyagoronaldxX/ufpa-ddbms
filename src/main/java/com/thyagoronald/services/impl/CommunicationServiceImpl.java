package com.thyagoronald.services.impl;

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
import com.thyagoronald.configs.DbConfig;
import com.thyagoronald.middleware.LoadBalancingFilter;
import com.thyagoronald.pojos.HeartbeatPojo;
import com.thyagoronald.pojos.HostPojo;
import com.thyagoronald.pojos.RequestPojo;
import com.thyagoronald.pojos.ResponsePojo;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.utils.ChecksumUtil;
import com.thyagoronald.utils.Logger;
import com.thyagoronald.utils.ProtocolConst;

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
                appContext.refreshLeader();

                response.sendLine(ProtocolConst.HEARTBEAT_SUCCESS);
            } else {
                Logger.error("INVALID CHECKSUM DETECTED");
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
}
