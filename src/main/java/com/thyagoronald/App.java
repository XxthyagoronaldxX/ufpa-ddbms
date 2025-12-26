package com.thyagoronald;

import com.thyagoronald.services.ApiService;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.services.DbService;
import com.thyagoronald.services.HeartbeatService;
import com.thyagoronald.services.SocketService;
import com.thyagoronald.services.impl.ApiServiceImpl;
import com.thyagoronald.services.impl.CommunicationServiceImpl;
import com.thyagoronald.services.impl.DbServiceImpl;
import com.thyagoronald.services.impl.HeartbeatServiceImpl;
import com.thyagoronald.services.impl.SocketServiceImpl;
import com.thyagoronald.utils.Logger;

public class App {
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);

            DbService dbService = new DbServiceImpl();
            CommunicationService communicationService = new CommunicationServiceImpl(dbService, port);
            ApiService apiService = new ApiServiceImpl(dbService, communicationService);
            SocketService socketService = new SocketServiceImpl(communicationService, apiService, port);
            HeartbeatService heartbeatService = new HeartbeatServiceImpl(communicationService, 10);

            new Thread(heartbeatService::start).start();

            socketService.startServer();
        } catch (NumberFormatException ex) {
            Logger.error(ex.getMessage());
        }
    }
}