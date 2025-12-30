package com.thyagoronald;

import com.thyagoronald.contracts.MiddlewareManager;
import com.thyagoronald.middlewares.ApiMiddleware;
import com.thyagoronald.middlewares.LoadBalancerMiddleware;
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
import com.thyagoronald.utils.GetIt;
import com.thyagoronald.utils.Logger;

public class App {
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);

            DbService dbService = new DbServiceImpl();
            CommunicationService communicationService = new CommunicationServiceImpl(dbService);
            ApiService apiService = new ApiServiceImpl(dbService, communicationService);
            HeartbeatService heartbeatService = new HeartbeatServiceImpl(communicationService, 10);

            // Configurando Middlewares
            MiddlewareManager middlewareManager = new MiddlewareManager();
            middlewareManager.addMiddleware(new LoadBalancerMiddleware());
            middlewareManager.addMiddleware(new ApiMiddleware());

            // Configurando Servidor
            SocketService socketService = new SocketServiceImpl(communicationService, middlewareManager, apiService,
                    port);

            // Configurando contexto do AppContext
            AppContext appContext = new AppContext();
            appContext.initHosts(port);

            // Configurando o Injetor de Dependências
            GetIt.getInstance().addSingleton(appContext);
            GetIt.getInstance().addSingleton(communicationService);

            new Thread(heartbeatService::start).start();

            socketService.startServer();
        } catch (NumberFormatException ex) {
            Logger.error(ex.getMessage());
        }
    }
}