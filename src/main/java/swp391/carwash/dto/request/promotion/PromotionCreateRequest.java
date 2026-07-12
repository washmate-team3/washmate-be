package swp391.carwash.dto.request.promotion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import swp391.carwash.enums.DiscountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PromotionCreateRequest(

        @NotNull(message = "Garage không được để trống.")
        Integer garageId,

        @NotBlank(message = "Mã khuyến mãi không được để trống.")
        @Size(max = 50, message = "Mã khuyến mãi không được vượt quá 50 ký tự.")
        String promoCode,

        @NotNull(message = "Loại giảm giá không được để trống.")
        DiscountType discountType,

        @NotNull(message = "Giá trị giảm không được để trống.")
        @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0.")
        BigDecimal discountValue,

        @DecimalMin(value = "0.00", message = "Giảm tối đa không được âm.")
        BigDecimal maxDiscount,

        @DecimalMin(value = "0.00", message = "Giá trị đơn tối thiểu không được âm.")
        BigDecimal minOrderValue,

        @Min(value = 1, message = "Số lượt sử dụng phải lớn hơn 0.")
        Integer usageLimit,

        @NotNull(message = "Ngày bắt đầu không được để trống.")
        OffsetDateTime startDate,

        @NotNull(message = "Ngày kết thúc không được để trống.")
        @Future(message = "Ngày kết thúc phải ở tương lai.")
        OffsetDateTime endDate
) {
}