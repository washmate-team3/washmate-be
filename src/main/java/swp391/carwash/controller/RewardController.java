package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.respone.vehicles.RedemptionResponse;
import swp391.carwash.dto.respone.vehicles.RewardResponse;
import swp391.carwash.service.RedemptionService;
import swp391.carwash.service.RewardService;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class RewardCustomerController {

    private final RewardService rewardService;
    private final RedemptionService redemptionService;

    /**
     * Khách hàng xem các phần thưởng đang mở (ACTIVE) và còn hàng (STOCK > 0) tại một Garage
     */
    @GetMapping("/rewards")
    public ResponseEntity<Page<RewardResponse>> getActiveRewards(
            @RequestParam Integer garageId,
            @PageableDefault(size = 12) Pageable pageable) {
        Page<RewardResponse> rewards = rewardService.getActiveRewardsByGarage(garageId, pageable);
        return ResponseEntity.ok(rewards);
    }

    /**
     * Khách hàng gửi yêu cầu đổi quà bằng điểm
     */
    @PostMapping("/redemptions")
    public ResponseEntity<RedemptionResponse> redeemReward(
            @RequestBody CustomerRedeemRequest request,
            Authentication authentication) {

        // Không truyền userId từ Body. Lấy thông tin User động qua đối tượng Authentication (Từ JWT Token)
        String userEmail = authentication.getName();

        RedemptionResponse response = redemptionService.createRedemption(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Khách hàng tự theo dõi lịch sử đổi quà của bản thân tại một Garage
     */
    @GetMapping("/redemptions/history")
    public ResponseEntity<Page<RedemptionResponse>> getMyRedemptionHistory(
            @RequestParam Integer garageId,
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable) {

        String userEmail = authentication.getName();
        Page<RedemptionResponse> history = redemptionService.getCustomerHistory(userEmail, garageId, pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * Khách hàng tự hủy yêu cầu đổi quà (chỉ hợp lệ khi trạng thái đơn đang là PENDING)
     */
    @PostMapping("/redemptions/{redemptionId}/cancel")
    public ResponseEntity<Void> cancelRedemption(
            @PathVariable Integer redemptionId,
            Authentication authentication) {

        String userEmail = authentication.getName();
        redemptionService.cancelRedemptionByCustomer(redemptionId, userEmail);
        return ResponseEntity.noContent().build();
    }
}