package com.thyagoronald;

import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.services.DbService;
import com.thyagoronald.services.HeartbeatService;
import com.thyagoronald.services.SocketService;
import com.thyagoronald.services.impl.CommunicationServiceImpl;
import com.thyagoronald.services.impl.DbServiceImpl;
import com.thyagoronald.services.impl.HeartbeatServiceImpl;
import com.thyagoronald.services.impl.SocketServiceImpl;
import com.thyagoronald.utils.GetIt;
import com.thyagoronald.utils.Logger;

public class App {
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);

            DbService dbService = new DbServiceImpl();
            CommunicationService communicationService = new CommunicationServiceImpl(port);

            GetIt.getInstance().addSingleton(dbService);
            GetIt.getInstance().addSingleton(communicationService);

            HeartbeatService heartbeatService = new HeartbeatServiceImpl(10);
            SocketService socketService = new SocketServiceImpl(port);

            new Thread(heartbeatService::start).start();

            socketService.startServer();
        } catch (NumberFormatException ex) {
            Logger.error(ex.getMessage());
        }
    }
}