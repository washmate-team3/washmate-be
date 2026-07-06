package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.AuthResponse;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.RefreshToken;
import swp391.carwash.enums.UserStatus;
import swp391.carwash.repository.RefreshTokenRepository;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.security.AppUserDetailsService;
import swp391.carwash.security.JwtService;

@ExtendWith(MockitoExtension.class)
class TokenServiceSecurityLifecycleTest {
    @Mock
    private AppUserDetailsService userDetailsService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AppUserDetails userDetails;
    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private TokenService tokenService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(userDetailsService, refreshTokenRepository, jwtService, transactionManager);
        user = AppUser.builder()
                .id(10)
                .email("user@example.com")
                .fullName("User")
                .phone("0911111111")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Nested
    class RefreshTokenFlow {
        @Test
        void should_rotateAndRevokeCurrentRefreshToken_when_refreshTokenValid() {
            RefreshToken current = activeRefreshToken();
            when(jwtService.extractUserId("old-refresh", "refresh")).thenReturn(10);
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(current));
            stubActiveUserDetails();
            when(jwtService.createAccessToken(userDetails)).thenReturn("new-access");
            when(jwtService.createRefreshToken(userDetails)).thenReturn("new-refresh");
            when(jwtService.getAccessTokenSeconds()).thenReturn(3600L);
            when(jwtService.getRefreshTokenSeconds()).thenReturn(604800L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AuthResponse response = tokenService.rotateRefreshToken("old-refresh");

            assertEquals("new-access", response.accessToken());
            assertEquals("new-refresh", response.refreshToken());
            assertNotNull(current.getRevokedAt());
            assertNotNull(current.getReplacedByTokenHash());
        }

        @Test
        void should_returnUnauthorized_when_refreshTokenExpired() {
            RefreshToken expired = activeRefreshToken();
            expired.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
            when(jwtService.extractUserId("old-refresh", "refresh")).thenReturn(10);
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

            ApiException exception = assertThrows(ApiException.class,
                    () -> tokenService.rotateRefreshToken("old-refresh"));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        void should_returnUnauthorized_when_refreshTokenRevoked() {
            RefreshToken revoked = activeRefreshToken();
            revoked.setRevokedAt(OffsetDateTime.now());
            when(jwtService.extractUserId("old-refresh", "refresh")).thenReturn(10);
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

            ApiException exception = assertThrows(ApiException.class,
                    () -> tokenService.rotateRefreshToken("old-refresh"));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        void should_revokeAllUserSessions_when_revokedTokenIsReused() {
            // Reuse detection: dùng lại token đã revoke → toàn bộ phiên của user bị thu hồi
            RefreshToken revoked = activeRefreshToken();
            revoked.setRevokedAt(OffsetDateTime.now());
            when(jwtService.extractUserId("stolen-refresh", "refresh")).thenReturn(10);
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

            ApiException exception = assertThrows(ApiException.class,
                    () -> tokenService.rotateRefreshToken("stolen-refresh"));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
            verify(refreshTokenRepository).revokeAllActiveByUserId(any(), any());
        }

        @Test
        void should_returnUnauthorized_when_accessTokenUsedForRefresh() {
            when(jwtService.extractUserId("access-token", "refresh"))
                    .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Loai Token khong hop le"));

            ApiException exception = assertThrows(ApiException.class,
                    () -> tokenService.rotateRefreshToken("access-token"));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        void should_revokeAllRefreshTokens_when_adminRevokesUserSessions() {
            tokenService.revokeAllTokensForUser(10);

            verify(refreshTokenRepository).revokeAllActiveByUserId(any(), any());
        }

        @Test
        @Disabled("Current implementation only stores refresh tokens; access tokens cannot be invalidated by session revoke until user status changes or token expires.")
        void should_returnUnauthorized_when_accessTokenUsedAfterRefreshTokensRevoked() {
            throw new AssertionError("Enable after introducing access-token blacklist, session version, or jti validation.");
        }
    }

    private void stubActiveUserDetails() {
        when(userDetailsService.loadUserById(10)).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(user);
        when(userDetails.getRoleNames()).thenReturn(List.of("CUSTOMER"));
        when(userDetails.getGarageIds()).thenReturn(List.of());
    }

    private RefreshToken activeRefreshToken() {
        return RefreshToken.builder()
                .user(user)
                .tokenHash("current-hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .build();
    }
}
