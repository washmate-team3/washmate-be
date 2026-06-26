package swp391.carwash.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import swp391.carwash.common.exception.ApiException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/")
                || path.equals("/api/health")
                || path.equals("/actuator/health")
                || path.equals("/api/payments/vnpay/ipn")
                || path.equals("/api/payments/vnpay/return")
                || isPublicAuthPath(path);
    }

    private boolean isPublicAuthPath(String path) {
        return path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/otp/request")
                || path.equals("/api/auth/otp/verify")
                || path.equals("/api/auth/refresh")
                || path.equals("/api/auth/logout")
                || path.equals("/api/auth/password/forgot")
                || path.equals("/api/auth/password/reset");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ") || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Integer userId = jwtService.extractUserId(header.substring(7), "access");
            AppUserDetails userDetails = userDetailsService.loadUserById(userId);
            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Account is inactive or blocked");
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ApiException | UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
            securityErrorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }
}
