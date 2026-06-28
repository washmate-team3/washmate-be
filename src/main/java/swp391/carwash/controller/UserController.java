package swp391.carwash.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import swp391.carwash.dto.AvatarUploadResponse;
import swp391.carwash.dto.MeResponse;
import swp391.carwash.dto.UpdateProfileRequest;
import swp391.carwash.security.AppUserDetails;

@RestController
@RequestMapping("/api/users")
@lombok.RequiredArgsConstructor
public class UserController {
    private final swp391.carwash.service.UserService userService;

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return MeResponse.from(principal.getUser());
    }

    @PatchMapping("/me")
    public MeResponse updateProfile(
            @AuthenticationPrincipal AppUserDetails principal,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(principal.getId(), request);
    }

    @PutMapping("/me")
    public MeResponse replaceProfile(
            @AuthenticationPrincipal AppUserDetails principal,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(principal.getId(), request);
    }

    @PostMapping(value = "/me/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public AvatarUploadResponse uploadAvatar(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestPart("file") MultipartFile file
    ) {
        String publicBaseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return userService.uploadAvatar(principal.getId(), file, publicBaseUrl);
    }
}
