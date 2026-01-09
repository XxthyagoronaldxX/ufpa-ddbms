package com.thyagoronald.services;

import java.io.BufferedReader;
import java.io.PrintWriter;

public interface CommunicationService {
    void sendHeartbeat();

    void sendReplication(String query);

    void handleHeartbeat(BufferedReader in, PrintWriter out);

    void handleReplicate(BufferedReader in, PrintWriter out);
}
