package com.thyagoronald.services;

import java.io.PrintWriter;

public interface CommunicationService {
    void sendHeartbeat();

    void sendReplication(String query);

    String sendLoadBalancer(String data);

    void handleHeartbeat(PrintWriter out);

    void handleReplicate(String input, PrintWriter out);
}
