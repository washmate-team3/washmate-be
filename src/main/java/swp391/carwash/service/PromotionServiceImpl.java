package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.entity.Promotion;
import swp391.carwash.repository.PromotionRepository;
import swp391.carwash.security.AppUserDetails;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl
        implements PromotionService {

    private final PromotionRepository promotionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> getAvailablePromotions(
            Integer garageId,
            AppUserDetails principal
    ) {
        return promotionRepository.findAvailablePromotions(
                garageId,
                principal.getId(),
                OffsetDateTime.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> getAllPromotionsByGarage(
            Integer garageId
    ) {
        return promotionRepository.findByGarageId(
                garageId
        );
    }
}