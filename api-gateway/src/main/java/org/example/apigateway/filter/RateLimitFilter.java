package org.example.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    static final int MAX_REQUESTS = 5;
    static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!"/api/auth/login".equals(path) && !"/api/auth/register".equals(path)) {
            chain.doFilter(request, response);
            return;
        }
        String ip = resolveClientIp(request);
        if (isLoopback(ip)) {
            // request from internal service (e.g. frontend-service) — rate limiting handled downstream
            chain.doFilter(request, response);
            return;
        }
        if (!isAllowed(ip, path)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAllowed(String ip, String path) {
        long now = System.currentTimeMillis();
        String key = ip + "|" + path;
        buckets.putIfAbsent(key, new ArrayDeque<>());
        Deque<Long> timestamps = buckets.get(key);
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                return false;
            }
            timestamps.addLast(now);
        }
        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
    }
}
