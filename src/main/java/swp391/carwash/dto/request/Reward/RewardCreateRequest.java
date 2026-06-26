package swp391.carwash.dto.request.Reward;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RewardCreateRequest(
        @NotNull(message = "Garage ID không được để trống")
        Integer garageId,

        @NotBlank(message = "Tên phần thưởng không được để trống")
        String name,

        String description,

        @NotNull(message = "Số điểm yêu cầu không được để trống")
        @Min(value = 0, message = "Số điểm yêu cầu phải lớn hơn hoặc bằng 0")
        Integer pointsRequired,

        @NotNull(message = "Số lượng tồn kho không được để trống")
        @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
        Integer stock
) {}