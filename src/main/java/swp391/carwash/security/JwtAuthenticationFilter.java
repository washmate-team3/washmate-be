package swp391.carwash.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AppUserDetailsService userDetailsService,
            SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = PublicPaths.stripContextPath(request.getRequestURI(), request.getContextPath());
        return PublicPaths.isPublicPath(path);
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
            log.debug("JWT authentication failed for {}: {}", request.getRequestURI(), ex.getMessage());
            securityErrorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }
}
