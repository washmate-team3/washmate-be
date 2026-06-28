package swp391.carwash.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${washmate.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

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
    public AvatarUploadResponse uploadAvatar(Integer userId, MultipartFile file, String publicBaseUrl) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Avatar file is required");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Avatar file must be 5MB or smaller");
        }
        String contentType = file.getContentType();
        if (!ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only jpg, png, and webp avatar images are allowed");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String filename = "user-%d-%s%s".formatted(userId, UUID.randomUUID(), extension);

        try {
            Path uploadDir = Path.of(avatarDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename).normalize();
            if (!target.startsWith(uploadDir)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid avatar filename");
            }
            file.transferTo(target);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload avatar");
        }

        String avatarUrl = publicBaseUrl.replaceAll("/+$", "") + "/uploads/avatars/" + filename;
        user.setAvatarUrl(avatarUrl);
        appUserRepository.save(user);
        return new AvatarUploadResponse(avatarUrl);
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
