package swp391.carwash.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.carwash.entity.Promotion;
import swp391.carwash.repository.PromotionRepository;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    /**
     * Lấy danh sách mã khuyến mãi khả dụng cho Khách hàng tại 1 garage cụ thể
     */
    @Transactional()
    public List<Promotion> getAvailablePromotions(Integer garageId) {
        OffsetDateTime now = OffsetDateTime.now();
        return promotionRepository.findAvailablePromotions(garageId, now);
    }

    /**
     * Lấy TOÀN BỘ danh sách mã khuyến mãi của 1 garage (Dành cho Manager/Owner)
     */
    @Transactional()
    public List<Promotion> getAllPromotionsByGarage(Integer garageId) {
        return promotionRepository.findByGarageId(garageId);
    }
}