package swp391.carwash.dto.request.Garages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateGarageRequest(
        @NotBlank(message = "Tên Garage không được để trống")
        String name,

        @NotBlank(message = "Địa chỉ Garage không được để trống")
        String address,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ (Phải từ 10-11 số)")
        String phone
) {}