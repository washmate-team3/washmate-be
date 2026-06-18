package swp391.carwash.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.AuthResponse;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.RefreshToken;
import swp391.carwash.enums.UserStatus;
import swp391.carwash.repository.RefreshTokenRepository;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.security.AppUserDetailsService;
import swp391.carwash.security.JwtService;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final AppUserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse issueTokens(Integer userId) {
        AppUserDetails details = userDetailsService.loadUserById(userId);
        ensureActive(details.getUser());
        String accessToken = jwtService.createAccessToken(details);
        String refreshToken = jwtService.createRefreshToken(details);
        persistRefreshToken(details.getUser(), refreshToken);
        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenSeconds(), summary(details));
    }

    @Transactional
    public AuthResponse rotateRefreshToken(String rawRefreshToken) {
        Integer userId = jwtService.extractUserId(rawRefreshToken, "refresh");
        String currentHash = hashToken(rawRefreshToken);
        OffsetDateTime now = OffsetDateTime.now();
        RefreshToken current = refreshTokenRepository.findByTokenHash(currentHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (!current.getUser().getId().equals(userId) || current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        AppUserDetails details = userDetailsService.loadUserById(userId);
        ensureActive(details.getUser());
        String accessToken = jwtService.createAccessToken(details);
        String refreshToken = jwtService.createRefreshToken(details);
        RefreshToken replacement = persistRefreshToken(details.getUser(), refreshToken);
        current.setRevokedAt(now);
        current.setReplacedByTokenHash(replacement.getTokenHash());
        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenSeconds(), summary(details));
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(OffsetDateTime.now()));
    }

    private RefreshToken persistRefreshToken(AppUser user, String rawRefreshToken) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtService.getRefreshTokenSeconds()))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private AuthResponse.UserSummary summary(AppUserDetails details) {
        AppUser user = details.getUser();
        return new AuthResponse.UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), details.getRoleNames(), details.getGarageIds());
    }

    private void ensureActive(AppUser user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "User is not active");
        }
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot hash token");
        }
    }
}
