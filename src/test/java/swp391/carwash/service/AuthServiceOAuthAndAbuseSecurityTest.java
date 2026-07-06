package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.AuthResponse;
import swp391.carwash.dto.GoogleLoginRequest;
import swp391.carwash.dto.LoginRequest;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.UserRole;
import swp391.carwash.enums.AuthProvider;
import swp391.carwash.enums.UserStatus;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.RoleRepository;
import swp391.carwash.repository.UserRoleRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceOAuthAndAbuseSecurityTest {
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OtpService otpService;
    @Mock
    private TokenService tokenService;
    @Mock
    private GoogleAuthService googleAuthService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                appUserRepository,
                roleRepository,
                userRoleRepository,
                passwordEncoder,
                otpService,
                tokenService,
                googleAuthService);
        ReflectionTestUtils.setField(authService, "loginMaxFailedAttempts", 3);
        ReflectionTestUtils.setField(authService, "loginLockMinutes", 15L);
    }

    @Nested
    class AccountLinkingAndOAuth {
        @Test
        void should_linkExistingLocalAccount_when_googleEmailAlreadyRegisteredWithPassword() {
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setEmail("USER@example.com");
            payload.setEmailVerified(true);
            AppUser localUser = AppUser.builder()
                    .id(10)
                    .email("user@example.com")
                    .fullName("Local User")
                    .passwordHash("hash")
                    .provider(AuthProvider.LOCAL)
                    .status(UserStatus.ACTIVE)
                    .failedLoginCount(2)
                    .build();
            AuthResponse expected = new AuthResponse("access", "refresh", "Bearer", 3600, null);
            when(googleAuthService.verifyIdToken("google-id-token")).thenReturn(payload);
            when(appUserRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(localUser));
            when(tokenService.issueTokens(10)).thenReturn(expected);

            AuthResponse response = authService.loginWithGoogle(new GoogleLoginRequest("google-id-token"));

            assertEquals(expected, response);
            assertEquals(AuthProvider.LOCAL, localUser.getProvider());
            assertEquals(0, localUser.getFailedLoginCount());
            assertNotNull(localUser.getLastLoginAt());
            verify(appUserRepository).save(localUser);
            verify(userRoleRepository, never()).save(any(UserRole.class));
        }

        @Test
        void should_returnUnauthorized_when_googleIdTokenRejectedByVerifier() {
            when(googleAuthService.verifyIdToken("fake-token"))
                    .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token"));

            ApiException exception = assertThrows(ApiException.class,
                    () -> authService.loginWithGoogle(new GoogleLoginRequest("fake-token")));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
            verify(appUserRepository, never()).findByEmailIgnoreCase(any());
        }

        @Test
        void should_returnUnauthorized_when_googleEmailNotVerified() {
            when(googleAuthService.verifyIdToken("unverified-token"))
                    .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Google email is not verified"));

            ApiException exception = assertThrows(ApiException.class,
                    () -> authService.loginWithGoogle(new GoogleLoginRequest("unverified-token")));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
            verify(appUserRepository, never()).findByEmailIgnoreCase(any());
        }

        @Test
        @Disabled("No provider_id column or avatar_url mapping is present in Google login; enable after adding provider identity/avatar linking.")
        void should_storeProviderIdAndAvatarUrl_when_googleLoginSucceeds() {
            throw new AssertionError("Enable after provider_id and Google picture claims are persisted.");
        }
    }

    @Nested
    class AbuseAndInputEdges {
        @Test
        void should_lockAccount_when_passwordFailsConfiguredLimit() {
            AppUser user = AppUser.builder()
                    .id(10)
                    .email("user@example.com")
                    .passwordHash("hash")
                    .status(UserStatus.ACTIVE)
                    .failedLoginCount(2)
                    .build();
            when(appUserRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

            ApiException exception = assertThrows(ApiException.class,
                    () -> authService.login(new LoginRequest("user@example.com", null, "wrong")));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
            assertEquals(3, user.getFailedLoginCount());
            assertNotNull(user.getLockedUntil());
        }

        @Test
        void should_returnLocked_when_loginAttemptOccursBeforeLockExpires() {
            AppUser user = AppUser.builder()
                    .id(10)
                    .email("user@example.com")
                    .passwordHash("hash")
                    .status(UserStatus.ACTIVE)
                    .failedLoginCount(3)
                    .lockedUntil(java.time.OffsetDateTime.now().plusMinutes(10))
                    .build();
            when(appUserRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

            ApiException exception = assertThrows(ApiException.class,
                    () -> authService.login(new LoginRequest("user@example.com", null, "secret123")));

            assertEquals(HttpStatus.LOCKED, exception.getStatus());
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        void should_returnUnauthorized_when_loginIdentifierContainsSqlInjectionPayload() {
            String payload = "admin@example.com' OR '1'='1";
            when(appUserRepository.findByEmailIgnoreCase(payload.toLowerCase(Locale.ROOT))).thenReturn(Optional.empty());

            ApiException exception = assertThrows(ApiException.class,
                    () -> authService.login(new LoginRequest(payload, null, "anything")));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
            verify(passwordEncoder, never()).matches(any(), any());
        }
    }
}
