package com.thyagoronald.middleware;

import java.io.IOException;
import java.util.Objects;

import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.thyagoronald.AppContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@Component
@Order(1)
@AllArgsConstructor
public class LeaderFilter extends OncePerRequestFilter {
    private final AppContext appContext;    

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String loadBalanced = request.getParameter("loadBalanced");

        if (appContext.isLeader() || Objects.nonNull(loadBalanced)) {
            filterChain.doFilter(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "This node is not the leader.");
        }
    }
}
