package swp391.carwash.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.*;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.entity.AppUser;
import swp391.carwash.entity.Role;
import swp391.carwash.entity.UserRole;
import swp391.carwash.enums.RecordStatus;
import swp391.carwash.enums.RoleName;
import swp391.carwash.enums.UserStatus;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.RoleRepository;
import swp391.carwash.repository.UserRoleRepository;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.security.AppUserDetailsService;
import swp391.carwash.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();

    @Value("${washmate.security.otp.mock-code}")
    private String mockOtp;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (appUserRepository.existsByPhone(request.phone())) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone already exists");
        }
        AppUser user = AppUser.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .status(UserStatus.ACTIVE)
                .build();
        appUserRepository.save(user);
        assignRole(user, RoleName.CUSTOMER);
        return issueTokens(user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        ensureActive(user);
        return issueTokens(user.getId());
    }

    public OtpResponse requestOtp(OtpRequest request) {
        otpStore.put(request.phone(), mockOtp);
        return new OtpResponse(request.phone(), mockOtp, "Mock OTP generated for development/demo");
    }

    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        String expected = otpStore.get(request.phone());
        if (expected == null || !expected.equals(request.otp())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
        }
        AppUser user = appUserRepository.findByPhone(request.phone()).orElseGet(() -> {
            AppUser created = AppUser.builder()
                    .fullName(request.fullName() == null || request.fullName().isBlank() ? "OTP Customer" : request.fullName())
                    .phone(request.phone())
                    .status(UserStatus.ACTIVE)
                    .build();
            appUserRepository.save(created);
            assignRole(created, RoleName.CUSTOMER);
            return created;
        });
        ensureActive(user);
        otpStore.remove(request.phone());
        return issueTokens(user.getId());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        Integer userId = jwtService.extractUserId(request.refreshToken(), "refresh");
        return issueTokens(userId);
    }

    private void assignRole(AppUser user, RoleName roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName(roleName)
                        .description(roleName.name() + " role")
                        .status(RecordStatus.ACTIVE)
                        .build()));
        userRoleRepository.save(UserRole.builder().user(user).role(role).status(RecordStatus.ACTIVE).build());
    }

    private AuthResponse issueTokens(Integer userId) {
        AppUserDetails details = userDetailsService.loadUserById(userId);
        ensureActive(details.getUser());
        String accessToken = jwtService.createAccessToken(details);
        String refreshToken = jwtService.createRefreshToken(details);
        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenSeconds(), summary(details));
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
}
