package com.thyagoronald.services.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.services.HeartbeatService;
import com.thyagoronald.utils.GetIt;
import com.thyagoronald.utils.Logger;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class HeartbeatServiceImpl implements HeartbeatService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final int intervalSeconds;

    @Override
    public void start() {
        CommunicationService communicationService = GetIt.getInstance().find(CommunicationService.class);
        scheduler.scheduleAtFixedRate(communicationService::sendHeartbeat, 0, intervalSeconds, TimeUnit.SECONDS);
        Logger.info("Heartbeat iniciado.");
    }

    @Override
    public void stop() {
        scheduler.shutdownNow();
        Logger.info("Heartbeat parado.");
    }
}
