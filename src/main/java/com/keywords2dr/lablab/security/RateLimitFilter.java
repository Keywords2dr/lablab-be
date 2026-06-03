package com.keywords2dr.lablab.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Cấu hình: tối đa 5 request / 60 giây cho mỗi IP
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MILLIS = 60_000L;

    private static final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();

    private static final java.util.Set<String> RATE_LIMITED_PATHS = java.util.Set.of(
            "/api/auth/login",
            "/api/auth/forgot-password"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!RATE_LIMITED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = clientIp + ":" + path;

        RequestCounter counter = counters.compute(key, (k, existing) -> {
            long now = Instant.now().toEpochMilli();
            if (existing == null || now - existing.windowStart > WINDOW_MILLIS) {
                return new RequestCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\"," +
                            "\"message\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau 60 giây.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestCounter {
        final long windowStart;
        final AtomicInteger count;

        RequestCounter(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}