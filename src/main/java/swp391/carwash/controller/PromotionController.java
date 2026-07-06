package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.entity.Promotion;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.PromotionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    @GetMapping("/AvailablePromotions")
    public List<Promotion> getAvailablePromotions(
            @RequestParam Integer garageId,
            @AuthenticationPrincipal AppUserDetails principal) {
        return promotionService.getAvailablePromotions(garageId,principal);
    }

    @GetMapping("/manage/all")
    public List<Promotion> getAllPromotionsByGarage(@RequestParam Integer garageId) {
        return promotionService.getAllPromotionsByGarage(garageId);
    }
}
