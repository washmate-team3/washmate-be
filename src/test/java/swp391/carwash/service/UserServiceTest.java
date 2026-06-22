package swp391.carwash.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swp391.carwash.dto.UpdateUserStatusRequest;
import swp391.carwash.entity.AppUser;
import swp391.carwash.enums.UserStatus;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private UserService userService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        userService = new UserService(appUserRepository, refreshTokenRepository);
        user = AppUser.builder().id(10).status(UserStatus.ACTIVE).build();
        when(appUserRepository.findById(10)).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);
    }

    @Test
    void blockingUserRevokesAllActiveRefreshTokens() {
        userService.updateUserStatus(10, new UpdateUserStatusRequest(UserStatus.BLOCKED));

        verify(refreshTokenRepository).revokeAllActiveByUserId(org.mockito.ArgumentMatchers.eq(10), any(OffsetDateTime.class));
    }

    @Test
    void activatingUserDoesNotRevokeRefreshTokens() {
        userService.updateUserStatus(10, new UpdateUserStatusRequest(UserStatus.ACTIVE));

        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any(), any());
    }
}
