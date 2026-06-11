package swp391.carwash.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.respone.vehicles.RedemptionResponse;
import swp391.carwash.dto.respone.vehicles.RewardResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rewards")
@Tag(name = "Reward & Redemption System", description = "APIs xem danh sách quà tặng và thực hiện đổi điểm lấy quà")
public class RewardRedemptionController {
    @GetMapping
    @Operation(summary = "Xem danh sách các phần thưởng/voucher đang khả dụng")
    public ResponseEntity<List<RewardResponse>> getAllRewards() {
        // TODO: Gọi sang rewardService.getAvailableRewards()
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/{rewardId}/redeem")
    @Operation(summary = "Khách hàng đổi điểm lấy quà/voucher",
            description = "Trừ current_points, tạo PointTransaction REDEEMED và sinh mã voucher")
    public ResponseEntity<RedemptionResponse> redeemReward(@PathVariable Integer rewardId) {
        Integer userId = 1; // Lấy từ JWT
        // TODO: Gọi sang rewardService.redeem(userId, rewardId)
        return ResponseEntity.ok(new RedemptionResponse());
    }
}
