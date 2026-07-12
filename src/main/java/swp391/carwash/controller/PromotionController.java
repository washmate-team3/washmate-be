package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<Promotion> getAvailablePromotions(
            @RequestParam Integer garageId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        return promotionService.getAvailablePromotions(
                garageId,
                principal
        );
    }

    @GetMapping("/manage/all")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
    public List<Promotion> getAllPromotionsByGarage(
            @RequestParam Integer garageId
    ) {
        return promotionService.getAllPromotionsByGarage(
                garageId
        );
    }
}