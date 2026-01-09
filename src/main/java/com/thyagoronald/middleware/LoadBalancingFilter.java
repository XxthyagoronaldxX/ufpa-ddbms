package com.thyagoronald.middleware;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.thyagoronald.AppContext;
import com.thyagoronald.pojos.HostPojo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoadBalancingFilter extends OncePerRequestFilter {
    private static final int MAX_REQUESTS = 10;
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final AppContext appContext;

    public LoadBalancingFilter(AppContext appContext) {
        this.appContext = appContext;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String loadBalanced = request.getParameter("loadBalanced");
            HostPojo hostPojo = appContext.getLeastLoadedHost();

            if ((Objects.isNull(loadBalanced) || !"true".equals(loadBalanced))
                    && counter.get() > MAX_REQUESTS
                    && Objects.nonNull(hostPojo)
                    && hostPojo.isAlive()) {
                String redirectUrl = "http://" + hostPojo.getHost() + ":" + hostPojo.getPort()
                        + request.getRequestURI() + "?loadBalanced=true";

                response.sendRedirect(redirectUrl);

                return;
            }

            counter.incrementAndGet();
            filterChain.doFilter(request, response);
        } finally {
            safeDecrement();
        }
    }

    private static void safeDecrement() {
        int prev;
        int next;

        do {
            prev = counter.get();
            if (prev == 0)
                return;
            next = prev - 1;
        } while (!counter.compareAndSet(prev, next));
    }

    public static int getCurrentRequestCount() {
        return counter.get();
    }
}
