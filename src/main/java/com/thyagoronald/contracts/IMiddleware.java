package com.thyagoronald.contracts;

import java.net.Socket;

public interface IMiddleware {
    void run(Socket socket, IMiddleware next);
}
