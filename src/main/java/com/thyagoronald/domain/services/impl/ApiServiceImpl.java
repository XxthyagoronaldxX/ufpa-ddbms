package com.thyagoronald.domain.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.thyagoronald.AppContext;
import com.thyagoronald.domain.configs.DbConfig;
import com.thyagoronald.domain.constants.FlagEnum;
import com.thyagoronald.domain.errors.ReplicateHostException;
import com.thyagoronald.domain.pojos.HistoryPojo;
import com.thyagoronald.domain.pojos.HostPojo;
import com.thyagoronald.domain.pojos.QueryPojo;
import com.thyagoronald.domain.pojos.QueryResponsePojo;
import com.thyagoronald.domain.services.ApiService;
import com.thyagoronald.domain.services.CommunicationService;
import com.thyagoronald.domain.utils.ChecksumUtil;
import com.thyagoronald.domain.utils.DbUtil;
import com.thyagoronald.domain.utils.Logger;
import com.thyagoronald.domain.utils.ProtocolConst;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ApiServiceImpl implements ApiService {
    private final CommunicationService communicationService;
    private final AppContext appContext;

    @Override
    public ResponseEntity<QueryResponsePojo> doQueryRead(QueryPojo dto) {
        try {
            String query = dto.getQuery();
            Connection connection = DbConfig.getConnection();
            connection.setAutoCommit(false);

            return executeQueryRead(query, connection);
        } catch (SQLException ex) {
            return ResponseEntity.status(500).body(buildErrorResponse("Error executing query: " + ex.getMessage()));
        }
    }

    private ResponseEntity<QueryResponsePojo> executeQueryRead(String query, Connection connection) {
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();

            QueryResponsePojo response = QueryResponsePojo.builder()
                    .node(appContext.getNodeId())
                    .message("SQL EXECUTED SUCCESSFULLY")
                    .result(DbUtil.findDynamicData(rs))
                    .build();

            return ResponseEntity.ok(response);
        } catch (SQLException ex) {
            return ResponseEntity.status(500).body(buildErrorResponse("SQL EXECUTION ERROR: " + ex.getMessage()));
        }
    }

    @Override
    public ResponseEntity<QueryResponsePojo> doQueryWrite(QueryPojo dto) {
        List<HostPojo> hostAlives = appContext.getHostAlives();
        List<FlagEnum> hostSuccessFlags = new ArrayList<>(hostAlives.stream().map(host -> FlagEnum.PENDING).toList());
        List<Thread> threads = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(hostAlives.size());

        for (int i = 0; i < hostAlives.size(); i++) {
            final HostPojo host = hostAlives.get(i);
            final int index = i;

            Thread thread = createReplicateThread(host, dto.getQuery(), hostSuccessFlags, index, latch);
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException | ReplicateHostException e) {
                Thread.currentThread().interrupt();
                return ResponseEntity.status(500).body(buildErrorResponse("REPLICATION THREAD FAILED"));
            }
        }

        if (hostSuccessFlags.contains(FlagEnum.FAILURE))
            return ResponseEntity.status(500).body(buildErrorResponse("REPLICATION FAILED"));

        for (HostPojo host : appContext.getHostNotAlives()) {
            communicationService.sendHistory(
                HistoryPojo.builder()
                    .nodeId(host.getNodeId())
                    .query(dto.getQuery())
                    .build()
            );
        }

        QueryResponsePojo response = QueryResponsePojo.builder()
                .message("SQL EXECUTED SUCCESSFULLY")
                .build();

        return ResponseEntity.ok(response);
    }

    private Thread createReplicateThread(
            HostPojo host,
            String data,
            List<FlagEnum> hostSuccess,
            int index,
            CountDownLatch latch) {
        return new Thread(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host.getHost(), host.getPort()), 5000);
                socket.setSoTimeout(5000);

                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.REPLICATE_METHOD);
                    out.println(ProtocolConst.CRC_ATTR + ChecksumUtil.generateChecksum(data));
                    out.println(ProtocolConst.CONTENT_ATTR + data);

                    String method = in.readLine();

                    if (method.contains(ProtocolConst.SUCCESS_METHOD)) {
                        Logger.info("[STATUS] REPLICATION SUCCESS");
                        hostSuccess.set(index, FlagEnum.SUCCESS);

                        latch.countDown();
                        latch.await();

                        if (!hostSuccess.contains(FlagEnum.PENDING) && hostSuccess.contains(FlagEnum.SUCCESS)) {
                            out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.REPLICATE_COMMIT_METHOD);
                        } else {
                            out.println(ProtocolConst.PROTOCOL_VERSION + " " + ProtocolConst.REPLICATE_ABORT_METHOD);
                        }
                    } else {
                        Logger.error("[STATUS] REPLICATION FAILED");
                        hostSuccess.set(index, FlagEnum.FAILURE);
                    }
                }
            } catch (IOException | InterruptedException ex) {
                Logger.error("[THREAD - EXCEPTION]: " + ex.getMessage());
                hostSuccess.set(index, FlagEnum.FAILURE);
                Thread.currentThread().interrupt();
            }
        });
    }

    private QueryResponsePojo buildErrorResponse(String message) {
        return QueryResponsePojo.builder()
                .node(appContext.getNodeId())
                .message(message)
                .result(null)
                .build();
    }
}
