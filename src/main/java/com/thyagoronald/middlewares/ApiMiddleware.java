package com.thyagoronald.middlewares;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.thyagoronald.contracts.IMiddleware;
import com.thyagoronald.utils.Logger;

public class ApiMiddleware implements IMiddleware {
    @Override
    public void run(Socket socket, IMiddleware next) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String request = in.readLine();
            String[] parts = request.split(" ");
            String method = parts[0];
            String resource = parts[1];

            if (method.equals("POST") && resource.equals("/query")) {
                String line;
                int contentLength = 0;
                while (!(line = in.readLine()).isEmpty()) {
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.split(":")[1].trim());
                    }
                }

                String body = null;
                if (contentLength > 0) {
                    char[] bodyChars = new char[contentLength];
                    in.read(bodyChars, 0, contentLength);
                    body = new String(bodyChars);
                }

                Logger.info("body recebido: " + body);
                out.print(body);
                return;
            }

            next.run(socket, null);
        } catch (IOException ex) {
            Logger.error("Erro no ApiMiddleware: " + ex.getMessage());
        }
    }
}
