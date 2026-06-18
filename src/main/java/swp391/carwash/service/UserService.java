package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.common.exception.ApiException;
import swp391.carwash.dto.MeResponse;
import swp391.carwash.dto.UpdateProfileRequest;
import swp391.carwash.dto.UpdateUserStatusRequest;
import swp391.carwash.entity.AppUser;
import swp391.carwash.repository.AppUserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository appUserRepository;

    @Transactional
    public MeResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));

        // Update fields
        user.setFullName(request.fullName());
        user.setPhone(request.phone());

        AppUser savedUser = appUserRepository.save(user);
        return mapToMeResponse(savedUser);
    }

    @Transactional
    public void updateUserStatus(Integer userId, UpdateUserStatusRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));

        user.setStatus(request.status());
        appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<MeResponse> getAllUsers(Pageable pageable) {
        return appUserRepository.findAll(pageable).map(this::mapToMeResponse);
    }

    private MeResponse mapToMeResponse(AppUser user) {
        List<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleName().name())
                .collect(Collectors.toList());

        List<Integer> garageIds = user.getUserRoles().stream()
                .filter(ur -> ur.getGarage() != null)
                .map(ur -> ur.getGarage().getId())
                .collect(Collectors.toList());

        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus().name(),
                roles,
                garageIds
        );
    }
}
