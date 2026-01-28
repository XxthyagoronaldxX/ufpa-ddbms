package com.thyagoronald.api.middleware;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thyagoronald.AppContext;
import com.thyagoronald.domain.pojos.HostPojo;
import com.thyagoronald.domain.pojos.QueryPojo;
import com.thyagoronald.domain.pojos.QueryResponsePojo;
import com.thyagoronald.domain.services.RequestService;
import com.thyagoronald.domain.utils.Logger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(2)
public class LoadBalancingFilter extends OncePerRequestFilter {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final AppContext appContext;
    private final RequestService requestService;

    public LoadBalancingFilter(AppContext appContext, RequestService requestService) {
        this.appContext = appContext;
        this.requestService = requestService;
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
                String host = hostPojo.getHost();
                String targetUrl = "http://" + host + ":" + 8080 + "/api/read?loadBalanced=true";
                
                hostPojo.incrementConnections();

                Logger.info("FORWARDING TO NODE: " + hostPojo.getNodeId());

                try {
                    ResponseEntity<QueryResponsePojo> responseEntity = requestService.doPost(
                        targetUrl,
                        queryPojo,
                        QueryResponsePojo.class
                    );

                    response.setStatus(responseEntity.getStatusCode().value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(objectMapper.writeValueAsString(responseEntity.getBody()));
                } catch (RuntimeException ex) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\": \"Failed to forward request - " + ex.getMessage() + "\"}");
                } finally {
                    hostPojo.decrementConnections();
                    response.getWriter().flush();
                }

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
