package swp391.carwash.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.AuthResponse;
import swp391.carwash.dto.LoginRequest;
import swp391.carwash.dto.OtpResponse;
import swp391.carwash.dto.OtpVerifyRequest;
import swp391.carwash.dto.RegisterRequest;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.Role;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.enums.RoleName;
import swp391.carwash.enums.UserStatus;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.RoleRepository;
import swp391.carwash.repository.UserRoleRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
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
        authService = new AuthService(appUserRepository, roleRepository, userRoleRepository, passwordEncoder, otpService, tokenService, googleAuthService);
        ReflectionTestUtils.setField(authService, "loginMaxFailedAttempts", 3);
        ReflectionTestUtils.setField(authService, "loginLockMinutes", 15L);
    }

    @Test
    void registerCreatesPendingUserAndRequestsEmailOtp() {
        RegisterRequest request = new RegisterRequest("USER@example.com", "secret123", "User", "0911111111");
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(roleRepository.findByRoleName(RoleName.CUSTOMER)).thenReturn(Optional.of(Role.builder()
                .id(1)
                .roleName(RoleName.CUSTOMER)
                .status(RecordStatus.ACTIVE)
                .build()));


        OtpResponse response = authService.register(request);

        assertEquals("user@example.com", response.identifier());
        verify(appUserRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getStatus() == UserStatus.PENDING_VERIFY
                        && user.getEmail().equals("user@example.com")
                        && user.getPasswordHash().equals("hash")));
        verify(otpService).requestOtp("user@example.com");
    }

    @Test
    void loginRejectsPendingVerificationUser() {
        AppUser user = AppUser.builder()
                .id(10)
                .email("user@example.com")
                .passwordHash("hash")
                .status(UserStatus.PENDING_VERIFY)
                .failedLoginCount(0)
                .build();
        when(appUserRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> authService.login(new LoginRequest("user@example.com", null, "secret123")));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Account verification required", exception.getMessage());
    }

    @Test
    void loginWithEmailOrPhoneIssuesTokensForActiveUser() {
        AppUser user = AppUser.builder()
                .id(10)
                .email("user@example.com")
                .phone("0911111111")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .failedLoginCount(2)
                .build();
        AuthResponse expected = new AuthResponse("access", "refresh", "Bearer", 3600, null);
        when(appUserRepository.findByPhone("0911111111")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(tokenService.issueTokens(10)).thenReturn(expected);

        AuthResponse response = authService.login(new LoginRequest("0911111111", null, "secret123"));

        assertEquals(expected, response);
        assertEquals(0, user.getFailedLoginCount());
        verify(tokenService).issueTokens(10);
    }

    @Test
    void verifyOtpActivatesPendingUserAndIssuesTokens() {
        AppUser user = AppUser.builder()
                .id(10)
                .email("user@example.com")
                .status(UserStatus.PENDING_VERIFY)
                .build();
        AuthResponse expected = new AuthResponse("access", "refresh", "Bearer", 3600, null);
        when(otpService.verifyOtp("user@example.com", "123456")).thenReturn("user@example.com");
        when(appUserRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(tokenService.issueTokens(10)).thenReturn(expected);

        AuthResponse response = authService.verifyOtp(new OtpVerifyRequest("user@example.com", null, null, "123456", null));

        assertEquals(expected, response);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void failedLoginLocksAccountAfterConfiguredAttempts() {
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
        org.junit.jupiter.api.Assertions.assertNotNull(user.getLockedUntil());
    }
}
