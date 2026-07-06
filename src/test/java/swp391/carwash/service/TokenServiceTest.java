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

    @Test
    void issueTokensPersistsRefreshTokenHash() {
        when(userDetailsService.loadUserById(10)).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(user);
        when(userDetails.getRoleNames()).thenReturn(List.of("CUSTOMER"));
        when(userDetails.getGarageIds()).thenReturn(List.of());
        when(jwtService.createAccessToken(userDetails)).thenReturn("access");
        when(jwtService.createRefreshToken(userDetails)).thenReturn("refresh");
        when(jwtService.getAccessTokenSeconds()).thenReturn(3600L);
        when(jwtService.getRefreshTokenSeconds()).thenReturn(604800L);
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
    void rotateRefreshTokenRevokesCurrentAndPersistsReplacement() {
        RefreshToken current = RefreshToken.builder()
                .user(user)
                .tokenHash("current-hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .build();
        when(jwtService.extractUserId("old-refresh", "refresh")).thenReturn(10);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(current));
        when(userDetailsService.loadUserById(10)).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(user);
        when(userDetails.getRoleNames()).thenReturn(List.of("CUSTOMER"));
        when(userDetails.getGarageIds()).thenReturn(List.of());
        when(jwtService.createAccessToken(userDetails)).thenReturn("new-access");
        when(jwtService.createRefreshToken(userDetails)).thenReturn("new-refresh");
        when(jwtService.getAccessTokenSeconds()).thenReturn(3600L);
        when(jwtService.getRefreshTokenSeconds()).thenReturn(604800L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = tokenService.rotateRefreshToken("old-refresh");

        assertEquals("new-refresh", response.refreshToken());
        assertNotNull(current.getRevokedAt());
        assertNotNull(current.getReplacedByTokenHash());
    }

    @Test
    void rotateRefreshTokenRejectsRevokedToken() {
        RefreshToken current = RefreshToken.builder()
                .user(user)
                .tokenHash("current-hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revokedAt(OffsetDateTime.now())
                .build();
        when(jwtService.extractUserId("old-refresh", "refresh")).thenReturn(10);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(current));

        ApiException exception = assertThrows(ApiException.class, () -> tokenService.rotateRefreshToken("old-refresh"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void revokeRefreshTokenIsIdempotent() {
        RefreshToken current = RefreshToken.builder()
                .user(user)
                .tokenHash("current-hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(current));

        tokenService.revokeRefreshToken("refresh");
        tokenService.revokeRefreshToken("unknown");

        assertNotNull(current.getRevokedAt());
    }

    @Test
    void issueTokensRejectsBlockedUser() {
        user.setStatus(UserStatus.BLOCKED);
        when(userDetailsService.loadUserById(10)).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(user);

        ApiException exception = assertThrows(ApiException.class, () -> tokenService.issueTokens(10));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }
}
