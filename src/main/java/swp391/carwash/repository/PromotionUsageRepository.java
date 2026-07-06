package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.PromotionUsage;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Integer> {

    boolean existsByUser_IdAndPromotion_PromotionId(Integer userId, Integer promotionId);

}
