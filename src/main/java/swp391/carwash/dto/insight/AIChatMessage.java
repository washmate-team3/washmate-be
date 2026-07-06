package swp391.carwash.dto.insight;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AIChatMessage(
        @NotBlank
        @Pattern(regexp = "(?i)^(user|assistant)$", message = "role must be user or assistant")
        String role,

        @NotBlank
        @Size(max = 1000)
        String content
) {
}
