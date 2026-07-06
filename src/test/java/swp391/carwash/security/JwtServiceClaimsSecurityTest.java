package swp391.carwash.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JwtServiceClaimsSecurityTest {
    private static final String SECRET = "test-secret-with-at-least-32-characters";

    @Nested
    class JwtClaimsAndSigning {
        @Test
        void should_createHmacSignedAccessTokenWithExpectedClaims_when_userDetailsProvided() {
            JwtService jwtService = new JwtService(SECRET, 60, 7);
            AppUserDetails userDetails = org.mockito.Mockito.mock(AppUserDetails.class);
            when(userDetails.getId()).thenReturn(123);
            when(userDetails.getRoleNames()).thenReturn(List.of("CUSTOMER", "STAFF"));
            when(userDetails.getGarageIds()).thenReturn(List.of(7, 8));

            String token = jwtService.createAccessToken(userDetails);
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
        void should_createRefreshTokenWithRefreshTypeAndLongerLifetime_when_userDetailsProvided() {
            JwtService jwtService = new JwtService(SECRET, 60, 7);
            AppUserDetails userDetails = org.mockito.Mockito.mock(AppUserDetails.class);
            when(userDetails.getId()).thenReturn(123);
            when(userDetails.getRoleNames()).thenReturn(List.of("CUSTOMER"));
            when(userDetails.getGarageIds()).thenReturn(List.of());

            Claims access = parse(jwtService.createAccessToken(userDetails));
            Claims refresh = parse(jwtService.createRefreshToken(userDetails));

            assertEquals("refresh", refresh.get("typ", String.class));
            assertTrue(refresh.getExpiration().after(access.getExpiration()));
            assertEquals(3600L, jwtService.getAccessTokenSeconds());
            assertEquals(604800L, jwtService.getRefreshTokenSeconds());
        }

        @Test
        void should_notExposePasswordOrEmailClaims_when_tokenCreated() {
            JwtService jwtService = new JwtService(SECRET, 60, 7);
            AppUserDetails userDetails = org.mockito.Mockito.mock(AppUserDetails.class);
            when(userDetails.getId()).thenReturn(123);
            when(userDetails.getRoleNames()).thenReturn(List.of("CUSTOMER"));
            when(userDetails.getGarageIds()).thenReturn(List.of());

            Claims claims = parse(jwtService.createAccessToken(userDetails));

            assertFalse(claims.containsKey("password"));
            assertFalse(claims.containsKey("passwordHash"));
            assertFalse(claims.containsKey("email"));
        }
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
