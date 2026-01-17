package com.thyagoronald.domain.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.thyagoronald.domain.services.CommunicationService;
import com.thyagoronald.domain.utils.Logger;

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
