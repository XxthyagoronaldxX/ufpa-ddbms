package com.thyagoronald.contracts;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MiddlewareManager implements IMiddleware {
    private final List<IMiddleware> middlewares;
    private final int index;

    private MiddlewareManager(List<IMiddleware> middlewares, int index) {
        this.middlewares = middlewares;
        this.index = index;
    }

    public MiddlewareManager() {
        this(new ArrayList<>(), 0);
    }

    public void addMiddleware(IMiddleware middleware) {
        this.middlewares.add(middleware);
    }

    @Override
    public void run(Socket socket, IMiddleware next) {
        if (index < middlewares.size()) {
            IMiddleware current = middlewares.get(index);
            IMiddleware nextChain = new MiddlewareManager(middlewares, index + 1);
            current.run(socket, nextChain);
        } else if (next != null) {
            next.run(socket, null);
        }
    }
}
