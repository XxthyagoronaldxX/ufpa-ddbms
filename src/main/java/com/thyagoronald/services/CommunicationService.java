package com.thyagoronald.services;

public interface CommunicationService {
    void sendHeartbeat();

    void sendReplication(String data);
}
