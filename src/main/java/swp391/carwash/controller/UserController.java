package swp391.carwash.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.dto.MeResponse;
import swp391.carwash.security.AppUserDetails;

@RestController
@RequestMapping("/api/users")
@lombok.RequiredArgsConstructor
public class UserController {
    private final swp391.carwash.service.UserService userService;

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return new MeResponse(
                principal.getId(),
                principal.getUser().getEmail(),
                principal.getUser().getFullName(),
                principal.getUser().getPhone(),
                principal.getUser().getStatus().name(),
                principal.getRoleNames(),
                principal.getGarageIds()
        );
    }

    @org.springframework.web.bind.annotation.PutMapping("/me")
    public MeResponse updateProfile(
            @AuthenticationPrincipal AppUserDetails principal,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody swp391.carwash.dto.UpdateProfileRequest request
    ) {
        return userService.updateProfile(principal.getId(), request);
    }
}
