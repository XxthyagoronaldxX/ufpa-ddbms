package com.thyagoronald.services;

import java.io.PrintWriter;

public interface CommunicationService {
    void sendHeartbeat();

    void sendReplication(String query);

    void handleHeartbeat(PrintWriter out);

    void handleReplicate(String input, PrintWriter out);
}
