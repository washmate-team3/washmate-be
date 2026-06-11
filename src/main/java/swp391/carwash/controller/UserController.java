package swp391.carwash.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.dto.MeResponse;
import swp391.carwash.security.AppUserDetails;

@RestController
@RequestMapping("/api/users")
public class UserController {
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
}
