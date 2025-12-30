package com.thyagoronald.middlewares;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

import com.thyagoronald.AppContext;
import com.thyagoronald.contracts.IMiddleware;
import com.thyagoronald.pojos.HostPojo;
import com.thyagoronald.services.CommunicationService;
import com.thyagoronald.utils.GetIt;
import com.thyagoronald.utils.HttpResponse;
import com.thyagoronald.utils.Logger;
import com.thyagoronald.utils.ProtocolConst;

public class LoadBalancerMiddleware implements IMiddleware {
    public static final int MAX_CONNECTIONS = 100;

    @Override
    public void run(Socket socket, IMiddleware next) {
        AppContext appContext = GetIt.getInstance().find(AppContext.class);
        CommunicationService communicationService = GetIt.getInstance().find(CommunicationService.class);

        Logger.info("socket.isClosed(): " + socket.isClosed());

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String request = in.readLine();

            StringBuilder body = new StringBuilder();
            int c;
            while ((c = in.read()) != -1) {
                body.append((char) c);
            }
            String bodyString = body.toString();

            Logger.info(bodyString);

            if (request.contains(ProtocolConst.QUERY_PREFIX) && appContext.getTotalConnections() >= MAX_CONNECTIONS) {
                Logger.info("Redirecionando conexão devido ao balanceamento de carga.");
                HostPojo hostPojo = appContext.getLeastLoadedHost();

                if (Objects.nonNull(hostPojo) && hostPojo.getConnections() < MAX_CONNECTIONS) {
                    String response = communicationService.sendLoadBalancer(request);
                    out.print(response);
                    return;
                } else {
                    String busyMessage = ProtocolConst.ERROR_SERVER_BUSY_RESPONSE;
                    out.print(busyMessage);
                    return;
                }
            }

            next.run(socket, null);
        } catch (NullPointerException | IOException ex) {
            Logger.error("Erro no LoadBalancerMiddleware: " + ex.getMessage());

            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                String response = HttpResponse.buildResponse(500, "Internal Server Error", "Erro interno no servidor");
                out.print(response);
                out.flush();
            } catch (IOException e) {
                Logger.error("Erro ao enviar resposta de erro: " + e.getMessage());
            }
        }
    }
}
