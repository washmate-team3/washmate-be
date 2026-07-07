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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.AuthResponse;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.RefreshToken;
import swp391.carwash.enums.UserStatus;
import swp391.carwash.repository.RefreshTokenRepository;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.security.AppUserDetailsService;
import swp391.carwash.security.JwtService;

/**
 * Unit test cho TokenService: issue, rotate, revoke và các case bảo mật
 * (token hết hạn, token bị thu hồi, reuse detection, sai loại token).
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    @Mock
    private AppUserDetailsService userDetailsService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AppUserDetails userDetails;
    @Mock
    private PlatformTransactionManager transactionManager;

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

    @Test
    void issueTokensPersistsRefreshTokenHash() {
        stubActiveUserDetails();
        stubTokenCreation("access", "refresh");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = tokenService.issueTokens(10);

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        verify(refreshTokenRepository).save(org.mockito.ArgumentMatchers.argThat(token ->
                token.getUser() == user
                        && token.getTokenHash() != null
                        && !token.getTokenHash().equals("refresh")
                        && token.getExpiresAt() != null));
    }

    @Test
    void issueTokensRejectsBlockedUser() {
        user.setStatus(UserStatus.BLOCKED);
        when(userDetailsService.loadUserById(10)).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(user);

        ApiException exception = assertThrows(ApiException.class, () -> tokenService.issueTokens(10));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void rotateRefreshTokenRevokesCurrentAndPersistsReplacement() {
        RefreshToken current = activeRefreshToken();
        when(jwtService.extractUserId("old-refresh", "refresh")).thenReturn(10);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(current));
        stubActiveUserDetails();
        stubTokenCreation("new-access", "new-refresh");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = tokenService.rotateRefreshToken("old-refresh");

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
        assertNotNull(current.getRevokedAt());
        assertNotNull(current.getReplacedByTokenHash());
    }

    @Test
    void rotateRefreshTokenRejectsExpiredToken() {
        RefreshToken expired = activeRefreshToken();
        expired.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        when(jwtService.extractUserId("old-refresh", "refresh")).thenReturn(10);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        ApiException exception = assertThrows(ApiException.class,
                () -> tokenService.rotateRefreshToken("old-refresh"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void rotateRefreshTokenReuseRevokesAllUserSessions() {
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
    void rotateRefreshTokenRejectsAccessTokenType() {
        when(jwtService.extractUserId("access-token", "refresh"))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Loại Token không hợp lệ"));

        ApiException exception = assertThrows(ApiException.class,
                () -> tokenService.rotateRefreshToken("access-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void revokeRefreshTokenIsIdempotent() {
        RefreshToken current = activeRefreshToken();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(current));

        tokenService.revokeRefreshToken("refresh");
        tokenService.revokeRefreshToken("unknown");

        assertNotNull(current.getRevokedAt());
    }

    @Test
    void revokeAllTokensForUserDelegatesToRepository() {
        tokenService.revokeAllTokensForUser(10);

        verify(refreshTokenRepository).revokeAllActiveByUserId(any(), any());
    }

    private void stubActiveUserDetails() {
        when(userDetailsService.loadUserById(10)).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(user);
        when(userDetails.getRoleNames()).thenReturn(List.of("CUSTOMER"));
        when(userDetails.getGarageIds()).thenReturn(List.of());
    }

    private void stubTokenCreation(String accessToken, String refreshToken) {
        when(jwtService.createAccessToken(userDetails)).thenReturn(accessToken);
        when(jwtService.createRefreshToken(userDetails)).thenReturn(refreshToken);
        when(jwtService.getAccessTokenSeconds()).thenReturn(3600L);
        when(jwtService.getRefreshTokenSeconds()).thenReturn(604800L);
    }

    private RefreshToken activeRefreshToken() {
        return RefreshToken.builder()
                .user(user)
                .tokenHash("current-hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .build();
    }
}
