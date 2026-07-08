package swp391.carwash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.request.PromotionReward.PromotionRewardCreateRequest;
import swp391.carwash.dto.request.PromotionReward.PromotionRewardUpdateRequest;
import swp391.carwash.dto.response.Reward.RewardRedemptionResponse;
import swp391.carwash.dto.response.Reward.RewardResponse;
import swp391.carwash.service.AdminPromotionRewardService;

@RestController
@RequestMapping("/api/v1/admin/promotion-rewards")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OWNER','MANAGER')")
public class AdminPromotionRewardController {

    private final AdminPromotionRewardService adminPromotionRewardService;

    @PostMapping
    public ResponseEntity<RewardResponse> create(
            @Valid @RequestBody PromotionRewardCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminPromotionRewardService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<RewardResponse>> getAll(
            @RequestParam Integer garageId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                adminPromotionRewardService.getAll(
                        garageId,
                        status,
                        pageable
                )
        );
    }

    @PutMapping("/{rewardId}")
    public ResponseEntity<RewardResponse> update(
            @PathVariable Integer rewardId,
            @Valid @RequestBody PromotionRewardUpdateRequest request) {

        return ResponseEntity.ok(
                adminPromotionRewardService.update(rewardId, request)
        );
    }

    @DeleteMapping("/{rewardId}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer rewardId) {

        adminPromotionRewardService.delete(rewardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/redemptions")
    public ResponseEntity<Page<RewardRedemptionResponse>> getRedemptions(
            @RequestParam Integer garageId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                adminPromotionRewardService.getRedemptions(
                        garageId,
                        status,
                        pageable
                )
        );
    }
}