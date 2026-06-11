package swp391.carwash.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import swp391.carwash.common.exception.ApiException;

class JwtServiceTest {
    private static final String SECRET = "test-secret-with-at-least-32-characters";

    @Test
    void constructorRejectsShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 60, 7));
    }

    @Test
    void constructorRejectsNonPositiveLifetime() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET, 0, 7));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET, 60, 0));
    }

    @Test
    void extractUserIdRejectsWrongTokenType() {
        JwtService jwtService = new JwtService(SECRET, 60, 7);
        AppUserDetails userDetails = org.mockito.Mockito.mock(AppUserDetails.class);
        when(userDetails.getId()).thenReturn(123);
        when(userDetails.getRoleNames()).thenReturn(List.of("CUSTOMER"));
        when(userDetails.getGarageIds()).thenReturn(List.of(1));

        String refreshToken = jwtService.createRefreshToken(userDetails);

        ApiException exception = assertThrows(ApiException.class, () -> jwtService.extractUserId(refreshToken, "access"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid token type", exception.getMessage());
    }

    @Test
    void extractUserIdRejectsTamperedToken() {
        JwtService jwtService = new JwtService(SECRET, 60, 7);
        AppUserDetails userDetails = org.mockito.Mockito.mock(AppUserDetails.class);
        when(userDetails.getId()).thenReturn(123);
        when(userDetails.getRoleNames()).thenReturn(List.of("CUSTOMER"));
        when(userDetails.getGarageIds()).thenReturn(List.of(1));

        String token = jwtService.createAccessToken(userDetails);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        ApiException exception = assertThrows(ApiException.class, () -> jwtService.extractUserId(tampered, "access"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }
}
