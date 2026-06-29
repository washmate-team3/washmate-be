package swp391.carwash.service;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.AvatarUploadResponse;
import swp391.carwash.dto.MeResponse;
import swp391.carwash.dto.UpdateProfileRequest;
import swp391.carwash.dto.UpdateUserStatusRequest;
import swp391.carwash.entity.AppUser;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.RefreshTokenRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final AvatarValidator avatarValidator;

    @Transactional
    public MeResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        AppUser user = appUserRepository.findWithUserRolesById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));

        user.setFullName(request.fullName());
        if (StringUtils.hasText(request.phone())) {
            appUserRepository.findByPhone(request.phone())
                    .filter(existingUser -> !existingUser.getId().equals(userId))
                    .ifPresent(existingUser -> {
                        throw new ApiException(HttpStatus.CONFLICT, "Phone number is already used");
                    });
            user.setPhone(request.phone());
        } else {
            user.setPhone(null);
        }
        user.setAddress(request.address());

        return MeResponse.from(appUserRepository.save(user));
    }

    @Transactional
    public AvatarUploadResponse uploadAvatar(Integer userId, MultipartFile file) {
        avatarValidator.validate(file);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        String oldAvatarUrl = user.getAvatarUrl();
        SupabaseStorageService.UploadedAvatar uploadedAvatar =
                supabaseStorageService.uploadAvatar(userId, file, avatarValidator.resolveExtension(file));
        try {
            user.setAvatarUrl(uploadedAvatar.publicUrl());
            appUserRepository.saveAndFlush(user);
        } catch (RuntimeException e) {
            supabaseStorageService.deleteByPath(uploadedAvatar.path());
            throw e;
        }

        supabaseStorageService.deleteByPublicUrl(oldAvatarUrl);
        return new AvatarUploadResponse(uploadedAvatar.publicUrl());
    }

    @Transactional
    public AvatarUploadResponse deleteAvatar(Integer userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        String oldAvatarUrl = user.getAvatarUrl();
        if (!StringUtils.hasText(oldAvatarUrl)) {
            return new AvatarUploadResponse(null);
        }

        user.setAvatarUrl(null);
        appUserRepository.saveAndFlush(user);
        supabaseStorageService.deleteByPublicUrl(oldAvatarUrl);
        return new AvatarUploadResponse(null);
    }

    @Transactional
    public void updateUserStatus(Integer userId, UpdateUserStatusRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));

        user.setStatus(request.status());
        appUserRepository.save(user);
        if (request.status() != swp391.carwash.enums.UserStatus.ACTIVE) {
            refreshTokenRepository.revokeAllActiveByUserId(userId, OffsetDateTime.now());
        }
    }

    @Transactional(readOnly = true)
    public Page<MeResponse> getAllUsers(Pageable pageable) {
        return appUserRepository.findAll(pageable).map(MeResponse::from);
    }
}
