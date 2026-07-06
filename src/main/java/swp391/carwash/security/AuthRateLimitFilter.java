package swp391.carwash.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit theo IP cho các auth endpoint công khai (login, OTP, forgot/reset password).
 * Fixed-window in-memory — đủ dùng cho single instance. Nếu deploy nhiều node,
 * thay bằng Redis-based limiter.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    /** Các path bị rate limit (POST) — subset của PublicPaths.AUTH_PATHS. */
    private static final List<String> RATE_LIMITED_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/otp/request",
            "/api/auth/otp/verify",
            "/api/auth/password/forgot",
            "/api/auth/password/reset");

    private static final int MAX_TRACKED_KEYS = 100_000;

    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Value("${washmate.security.ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${washmate.security.ratelimit.max-requests:10}")
    private int maxRequests;

    @Value("${washmate.security.ratelimit.window-seconds:60}")
    private long windowSeconds;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = PublicPaths.stripContextPath(request.getRequestURI(), request.getContextPath());
        return !RATE_LIMITED_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = PublicPaths.stripContextPath(request.getRequestURI(), request.getContextPath());
        String key = clientIp(request) + "|" + path;
        long nowWindow = System.currentTimeMillis() / (windowSeconds * 1000);

        Window window = windows.compute(key, (k, existing) ->
                existing == null || existing.windowId != nowWindow
                        ? new Window(nowWindow)
                        : existing);

        if (window.count.incrementAndGet() > maxRequests) {
            log.warn("Rate limit exceeded: key={}", key);
            securityErrorResponseWriter.write(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Quá nhiều yêu cầu, vui lòng thử lại sau");
            return;
        }

        // Chống memory leak: dọn map khi quá lớn (hiếm khi xảy ra)
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(e -> e.getValue().windowId != nowWindow);
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final long windowId;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long windowId) {
            this.windowId = windowId;
        }
    }
}
