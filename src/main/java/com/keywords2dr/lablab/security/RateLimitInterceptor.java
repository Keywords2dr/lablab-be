package com.keywords2dr.lablab.security;

import com.keywords2dr.lablab.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MILLIS = 60_000L;

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/forgot-password"
    );

    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String path = request.getRequestURI();
        if (!RATE_LIMITED_PATHS.contains(path)) {
            return true;
        }

        String key = getClientIp(request) + ":" + path;

        RequestCounter counter = counters.compute(key, (k, existing) -> {
            long now = Instant.now().toEpochMilli();
            if (existing == null || now - existing.windowStart > WINDOW_MILLIS) {
                return new RequestCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > MAX_REQUESTS) {
            throw new RateLimitExceededException(
                    "Quá nhiều yêu cầu. Vui lòng thử lại sau 60 giây.");
        }

        return true;
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