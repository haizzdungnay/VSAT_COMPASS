package com.vsatcompass.api.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsatcompass.api.dto.common.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiting filter for auth endpoints.
 * Limits per client IP:
 * - /auth/login:    10 requests per minute
 * - /auth/register: 5 requests per hour
 * - /auth/refresh:  30 requests per minute
 *
 * Uses X-Forwarded-For (first IP) for client identification behind Render proxy.
 * Single-instance only (in-memory). For horizontal scaling, swap for Redis-backed.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        String method = request.getMethod();

        // Only rate-limit POST on auth endpoints
        if (!"POST".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = null;

        if (path.equals("/auth/login")) {
            bucket = loginBuckets.computeIfAbsent(clientIp, k -> createLoginBucket());
        } else if (path.equals("/auth/register")) {
            bucket = registerBuckets.computeIfAbsent(clientIp, k -> createRegisterBucket());
        } else if (path.equals("/auth/refresh")) {
            bucket = refreshBuckets.computeIfAbsent(clientIp, k -> createRefreshBucket());
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP {} on path {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "RATE_LIMIT_EXCEEDED",
                    "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau."
            );
            objectMapper.findAndRegisterModules();
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/auth/");
    }

    /**
     * Resolve client IP from X-Forwarded-For header (first IP = real client behind Render proxy).
     * Falls back to remoteAddr if header is absent.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            // First IP in the chain is the client's real IP
            String firstIp = xff.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }
        return request.getRemoteAddr();
    }

    // 10 requests per minute
    private Bucket createLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(10).refillIntervally(10, Duration.ofMinutes(1)).build())
                .build();
    }

    // 5 requests per hour
    private Bucket createRegisterBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(5).refillIntervally(5, Duration.ofHours(1)).build())
                .build();
    }

    // 30 requests per minute
    private Bucket createRefreshBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(30).refillIntervally(30, Duration.ofMinutes(1)).build())
                .build();
    }
}
