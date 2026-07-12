package swp391.carwash.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.carwash.dto.request.promotion.PromotionCreateRequest;
import swp391.carwash.dto.request.promotion.PromotionUpdateRequest;
import swp391.carwash.dto.response.PromotionResponse;

public interface AdminPromotionService {
    PromotionResponse create(PromotionCreateRequest request);

    PromotionResponse update(
            Integer promotionId,
            PromotionUpdateRequest request
    );

    PromotionResponse getById(Integer promotionId);

    Page<PromotionResponse> getAll(
            Integer garageId,
            Pageable pageable
    );

    void delete(Integer promotionId);
}
