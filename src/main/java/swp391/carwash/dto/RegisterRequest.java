package swp391.carwash.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank String fullName,
        @NotBlank @Pattern(regexp = "^[0-9+]{9,20}$") String phone,
        @Schema(hidden = true) @JsonAlias({"role", "roleName"}) String requestedRole,
        @Schema(hidden = true) List<String> roles
) {
    public RegisterRequest(String email, String password, String fullName, String phone) {
        this(email, password, fullName, phone, null, null);
    }

    public boolean hasRequestedRole() {
        return (requestedRole != null && !requestedRole.isBlank())
                || (roles != null && !roles.isEmpty());
    }
}
