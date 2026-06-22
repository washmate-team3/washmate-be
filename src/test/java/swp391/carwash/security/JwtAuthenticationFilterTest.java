package swp391.carwash.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    private JwtService jwtService;
    @Mock
    private AppUserDetailsService userDetailsService;
    @Mock
    private SecurityErrorResponseWriter securityErrorResponseWriter;
    @Mock
    private AppUserDetails userDetails;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, securityErrorResponseWriter);
        request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setServletPath("/api/users/me");
        request.addHeader("Authorization", "Bearer access-token");
        response = new MockHttpServletResponse();
        when(jwtService.extractUserId("access-token", "access")).thenReturn(10);
        when(userDetailsService.loadUserById(10)).thenReturn(userDetails);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeUnlockedUserIsAuthenticated() throws Exception {
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetails.isAccountNonLocked()).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void inactiveUserIsRejected() throws Exception {
        when(userDetails.isEnabled()).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(securityErrorResponseWriter).write(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void blockedUserIsRejected() throws Exception {
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetails.isAccountNonLocked()).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(securityErrorResponseWriter).write(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
