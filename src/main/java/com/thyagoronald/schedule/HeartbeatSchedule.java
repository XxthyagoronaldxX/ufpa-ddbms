package com.thyagoronald.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.utils.Logger;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class HeartbeatSchedule {
    private final CommunicationService communicationService;

    @Scheduled(fixedDelay = 5000)
    public void initHeartbeat() {
        Logger.info("Iniciando envio de heartbeat...");
        communicationService.sendHeartbeat();
    }
}
