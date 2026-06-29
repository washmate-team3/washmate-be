package swp391.carwash.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.dao.DataIntegrityViolationException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swp391.carwash.dto.AvatarUploadResponse;
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
    @Mock
    private SupabaseStorageService supabaseStorageService;
    @Mock
    private AvatarValidator avatarValidator;

    private UserService userService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        userService = new UserService(appUserRepository, refreshTokenRepository, supabaseStorageService, avatarValidator);
        user = AppUser.builder().id(10).status(UserStatus.ACTIVE).build();
    }

    @Test
    void uploadAvatarSavesNewUrlThenDeletesOldAvatar() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes());
        user.setAvatarUrl("https://project.supabase.co/storage/v1/object/public/avatars/user-10/old.jpg");
        when(appUserRepository.findById(10)).thenReturn(Optional.of(user));
        when(avatarValidator.resolveExtension(file)).thenReturn("jpg");
        when(supabaseStorageService.uploadAvatar(10, file, "jpg"))
                .thenReturn(new SupabaseStorageService.UploadedAvatar(
                        "https://project.supabase.co/storage/v1/object/public/avatars/user-10/new.jpg",
                        "user-10/new.jpg"
                ));
        when(appUserRepository.saveAndFlush(user)).thenReturn(user);

        AvatarUploadResponse response = userService.uploadAvatar(10, file);

        org.junit.jupiter.api.Assertions.assertEquals(
                "https://project.supabase.co/storage/v1/object/public/avatars/user-10/new.jpg",
                response.avatarUrl()
        );
        verify(avatarValidator).validate(file);
        verify(appUserRepository).saveAndFlush(user);
        verify(supabaseStorageService).deleteByPublicUrl("https://project.supabase.co/storage/v1/object/public/avatars/user-10/old.jpg");
        verify(supabaseStorageService, never()).deleteByPath("user-10/new.jpg");
    }

    @Test
    void uploadAvatarDeletesNewUploadWhenDatabaseSaveFails() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes());
        when(appUserRepository.findById(10)).thenReturn(Optional.of(user));
        when(avatarValidator.resolveExtension(file)).thenReturn("jpg");
        when(supabaseStorageService.uploadAvatar(10, file, "jpg"))
                .thenReturn(new SupabaseStorageService.UploadedAvatar(
                        "https://project.supabase.co/storage/v1/object/public/avatars/user-10/new.jpg",
                        "user-10/new.jpg"
                ));
        when(appUserRepository.saveAndFlush(user)).thenThrow(new DataIntegrityViolationException("boom"));

        org.junit.jupiter.api.Assertions.assertThrows(DataIntegrityViolationException.class, () -> userService.uploadAvatar(10, file));

        verify(supabaseStorageService).deleteByPath("user-10/new.jpg");
        verify(supabaseStorageService, never()).deleteByPublicUrl(any());
    }

    @Test
    void deleteAvatarClearsDatabaseBeforeDeletingStoredObject() {
        user.setAvatarUrl("https://project.supabase.co/storage/v1/object/public/avatars/user-10/old.jpg");
        when(appUserRepository.findById(10)).thenReturn(Optional.of(user));
        when(appUserRepository.saveAndFlush(user)).thenReturn(user);

        AvatarUploadResponse response = userService.deleteAvatar(10);

        org.junit.jupiter.api.Assertions.assertNull(response.avatarUrl());
        org.junit.jupiter.api.Assertions.assertNull(user.getAvatarUrl());
        verify(appUserRepository).saveAndFlush(user);
        verify(supabaseStorageService).deleteByPublicUrl("https://project.supabase.co/storage/v1/object/public/avatars/user-10/old.jpg");
    }

    @Test
    void blockingUserRevokesAllActiveRefreshTokens() {
        when(appUserRepository.findById(10)).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);

        userService.updateUserStatus(10, new UpdateUserStatusRequest(UserStatus.BLOCKED));

        verify(refreshTokenRepository).revokeAllActiveByUserId(org.mockito.ArgumentMatchers.eq(10), any(OffsetDateTime.class));
    }

    @Test
    void activatingUserDoesNotRevokeRefreshTokens() {
        when(appUserRepository.findById(10)).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);

        userService.updateUserStatus(10, new UpdateUserStatusRequest(UserStatus.ACTIVE));

        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any(), any());
    }

    private byte[] jpegBytes() {
        return new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        };
    }
}
