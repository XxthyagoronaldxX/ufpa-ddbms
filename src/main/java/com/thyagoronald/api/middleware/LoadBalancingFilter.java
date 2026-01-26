package com.thyagoronald.api.middleware;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thyagoronald.AppContext;
import com.thyagoronald.domain.pojos.HostPojo;
import com.thyagoronald.domain.pojos.QueryPojo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(2)
public class LoadBalancingFilter extends OncePerRequestFilter {
    private static final boolean ISDOCKER = true;
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final AppContext appContext;

    public LoadBalancingFilter(AppContext appContext) {
        this.appContext = appContext;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/api/write");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String loadBalanced = request.getParameter("loadBalanced");
            HostPojo hostPojo = appContext.getLeastLoadedHost();
            InetAddress inetAddress = InetAddress.getLocalHost();
            String hostAddress = inetAddress.getHostAddress();

            if (!hostPojo.getHost().equals(hostAddress) && Objects.isNull(loadBalanced)) {
                ObjectMapper objectMapper = new ObjectMapper();
                QueryPojo queryPojo = objectMapper.readValue(request.getInputStream(), QueryPojo.class);
                String host = ISDOCKER ? "localhost" : hostPojo.getHost();
                String redirectUrl = UriComponentsBuilder
                    .fromHttpUrl("http://" + host + ":" + hostPojo.getApiPort())
                    .path("/api/read")
                    .queryParam("loadBalanced", true)
                    .queryParam("query", queryPojo.getQuery())
                    .build()
                    .toUriString();
                
                hostPojo.incrementConnections();

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
