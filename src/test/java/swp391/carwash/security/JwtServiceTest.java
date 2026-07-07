package swp391.carwash.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import swp391.carwash.common.exception.ApiException;

/**
 * Unit test cho JwtService: validate cấu hình, loại token, chữ ký
 * và nội dung claims (không lộ thông tin nhạy cảm).
 */
class JwtServiceTest {
    private static final String SECRET = "test-secret-with-at-least-32-characters";

    // ===== Cấu hình & validation =====

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
        String refreshToken = jwtService.createRefreshToken(userDetails(123, List.of("CUSTOMER"), List.of(1)));

        ApiException exception = assertThrows(ApiException.class, () -> jwtService.extractUserId(refreshToken, "access"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Loại Token không hợp lệ", exception.getMessage());
    }

    @Test
    void extractUserIdRejectsTamperedToken() {
        JwtService jwtService = new JwtService(SECRET, 60, 7);
        String token = jwtService.createAccessToken(userDetails(123, List.of("CUSTOMER"), List.of(1)));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        ApiException exception = assertThrows(ApiException.class, () -> jwtService.extractUserId(tampered, "access"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    // ===== Claims & chữ ký =====

    @Test
    void accessTokenIsHmacSignedWithExpectedClaims() {
        JwtService jwtService = new JwtService(SECRET, 60, 7);

        String token = jwtService.createAccessToken(userDetails(123, List.of("CUSTOMER", "STAFF"), List.of(7, 8)));
        Claims claims = parse(token);
        String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);

        assertTrue(headerJson.contains("\"alg\":\"HS"));
        assertEquals("123", claims.getSubject());
        assertEquals("access", claims.get("typ", String.class));
        assertEquals(List.of("CUSTOMER", "STAFF"), claims.get("roles", List.class));
        assertEquals(List.of(7, 8), claims.get("garageIds", List.class));
        assertNotNull(claims.getId());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void refreshTokenHasRefreshTypeAndLongerLifetime() {
        JwtService jwtService = new JwtService(SECRET, 60, 7);
        AppUserDetails details = userDetails(123, List.of("CUSTOMER"), List.of());

        Claims access = parse(jwtService.createAccessToken(details));
        Claims refresh = parse(jwtService.createRefreshToken(details));

        assertEquals("refresh", refresh.get("typ", String.class));
        assertTrue(refresh.getExpiration().after(access.getExpiration()));
        assertEquals(3600L, jwtService.getAccessTokenSeconds());
        assertEquals(604800L, jwtService.getRefreshTokenSeconds());
    }

    @Test
    void tokenDoesNotExposePasswordOrEmailClaims() {
        JwtService jwtService = new JwtService(SECRET, 60, 7);

        Claims claims = parse(jwtService.createAccessToken(userDetails(123, List.of("CUSTOMER"), List.of())));

        assertFalse(claims.containsKey("password"));
        assertFalse(claims.containsKey("passwordHash"));
        assertFalse(claims.containsKey("email"));
    }

    private AppUserDetails userDetails(Integer id, List<String> roles, List<Integer> garageIds) {
        AppUserDetails userDetails = org.mockito.Mockito.mock(AppUserDetails.class);
        when(userDetails.getId()).thenReturn(id);
        when(userDetails.getRoleNames()).thenReturn(roles);
        when(userDetails.getGarageIds()).thenReturn(garageIds);
        return userDetails;
    }

    private Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
