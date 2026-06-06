package swp391.carwash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarageRequest {

    @NotBlank(message = "Garage name must not be blank")
    @Size(max = 255, message = "Garage name must be at most 255 characters")
    private String name;

    @NotBlank(message = "Address must not be blank")
    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    @NotBlank(message = "Phone number must not be blank")
    @Pattern(regexp = "^(0|\\+84)(\\d{9,10})$", message = "Invalid phone number format (e.g. 09xxxxxxxx or +849xxxxxxxx)")
    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phone;

    @Builder.Default
    private String status = "ACTIVE";
}
