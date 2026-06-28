package swp391.carwash.security;

import java.util.List;

/**
 * Centralized registry of public (unauthenticated) request paths.
 * Used by both SecurityConfig and JwtAuthenticationFilter to avoid duplication.
 */
public final class PublicPaths {
    private PublicPaths() {}

    private static final List<String> AUTH_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/google",
            "/api/auth/otp/request",
            "/api/auth/otp/verify",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/password/forgot",
            "/api/auth/password/reset"
    );

    private static final List<String> COMMON_PATHS = List.of(
            "/",
            "/api/health",
            "/actuator/health",
            "/error",
            "/favicon.ico"
    );

    private static final List<String> PAYMENT_PATHS = List.of(
            "/api/payments/vnpay/ipn",
            "/api/payments/vnpay/return"
    );

    public static boolean isPublicAuthPath(String path) {
        return AUTH_PATHS.contains(path);
    }

    public static boolean isPublicPath(String path) {
        return COMMON_PATHS.contains(path)
                || AUTH_PATHS.contains(path)
                || PAYMENT_PATHS.contains(path);
    }

    public static boolean isDocsPath(String path) {
        return path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/webjars/")
                || path.startsWith("/swagger-resources/");
    }

    public static String stripContextPath(String requestUri, String contextPath) {
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
