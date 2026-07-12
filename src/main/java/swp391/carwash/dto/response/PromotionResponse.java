package swp391.carwash.dto.response;

import swp391.carwash.entity.Promotion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PromotionResponse(
        Integer promotionId,
        Integer garageId,
        String promoCode,
        String discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscount,
        BigDecimal minOrderValue,
        Integer usageLimit,
        Integer usedCount,
        String status,
        OffsetDateTime startDate,
        OffsetDateTime endDate
) {

    public static PromotionResponse from(Promotion promotion) {
        return new PromotionResponse(
                promotion.getPromotionId(),
                promotion.getGarageId(),
                promotion.getPromoCode(),
                promotion.getDiscountType().name(),
                promotion.getDiscountValue(),
                promotion.getMaxDiscount(),
                promotion.getMinOrderValue(),
                promotion.getUsageLimit(),
                promotion.getUsedCount(),
                promotion.getStatus().name(),
                promotion.getStartDate(),
                promotion.getEndDate()
        );
    }
}
