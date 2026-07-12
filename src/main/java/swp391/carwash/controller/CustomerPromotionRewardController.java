package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.response.Reward.RewardRedemptionResponse;
import swp391.carwash.dto.response.Reward.RewardResponse;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.CustomerPromotionRewardService;

@RestController
@RequestMapping("/api/v1/customer/promotion-rewards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPromotionRewardController {

    private final CustomerPromotionRewardService service;

    @GetMapping
    public ResponseEntity<Page<RewardResponse>> getRedeemablePromotions(
            @RequestParam Integer garageId,
            @PageableDefault(
                    size = 10,
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                service.getRedeemablePromotions(
                        garageId,
                        pageable
                )
        );
    }

    @PostMapping("/{rewardId}/redeem")
    public ResponseEntity<RewardRedemptionResponse> redeemPromotionReward(
            @RequestParam Integer garageId,
            @PathVariable Integer rewardId,
            @AuthenticationPrincipal AppUserDetails principal
    ) {
        return ResponseEntity.ok(
                service.redeemPromotionReward(
                        principal.getId(),
                        garageId,
                        rewardId
                )
        );
    }

    @GetMapping("/my-redemptions")
    public ResponseEntity<Page<RewardRedemptionResponse>>
    getMyPromotionRedemptions(
            @RequestParam Integer garageId,
            @AuthenticationPrincipal AppUserDetails principal,
            @PageableDefault(
                    size = 10,
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                service.getMyPromotionRedemptions(
                        principal.getId(),
                        garageId,
                        pageable
                )
        );
    }
}