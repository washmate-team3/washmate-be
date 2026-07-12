package swp391.carwash.service;

import swp391.carwash.entity.Promotion;
import swp391.carwash.security.AppUserDetails;

import java.util.List;

public interface PromotionService {

    List<Promotion> getAvailablePromotions(
            Integer garageId,
            AppUserDetails principal
    );

    List<Promotion> getAllPromotionsByGarage(
            Integer garageId
    );
}