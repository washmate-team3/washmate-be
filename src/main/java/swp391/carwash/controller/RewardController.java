package swp391.carwash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.request.Reward.RewardCreateRequest;
import swp391.carwash.dto.request.Reward.RewardUpdateRequest;
import swp391.carwash.dto.response.Reward.RewardRedemptionResponse;
import swp391.carwash.dto.response.Reward.RewardResponse;
import swp391.carwash.service.RewardRedemptionService;
import swp391.carwash.service.RewardService;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;
    private final RewardRedemptionService rewardRedemptionService;

    // 1. [CREATE] Thêm mới phần thưởng (Admin/Staff)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<RewardResponse> createReward(@RequestBody RewardCreateRequest request) {
        RewardResponse response = rewardService.createReward(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. [READ DETAILED] Xem chi tiết 1 món quà theo ID (Tất cả các role đều xem được)
    @GetMapping("/{rewardId}")
    public ResponseEntity<RewardResponse> getRewardById(@PathVariable Integer rewardId) {
        RewardResponse response = rewardService.getRewardById(rewardId);
        return ResponseEntity.ok(response);
    }

    // 3. [UPDATE] Cập nhật thông tin quà (Admin/Staff)
    @PutMapping("/{rewardId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<RewardResponse> updateReward(
            @PathVariable Integer rewardId,
            @RequestBody RewardUpdateRequest request) {
        RewardResponse response = rewardService.updateReward(rewardId, request);
        return ResponseEntity.ok(response);
    }

    // 4. [DELETE] Xóa mềm phần thưởng (Admin/Staff)
    @DeleteMapping("/{rewardId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteReward(@PathVariable Integer rewardId) {
        rewardService.deleteReward(rewardId);
        return ResponseEntity.noContent().build();
    }

    // 5. [READ LIST] Lấy danh sách quà theo từng Garage kèm phân trang & lọc động theo trạng thái
    // Không hard-code cấu hình phân trang, cho phép FE truyền động qua URL (ví dụ: ?page=0&size=10&sort=pointsRequired,asc)
    @GetMapping("/garage/{garageId}")
    public ResponseEntity<Page<RewardResponse>> getAllRewardsByGarage(
            @PathVariable Integer garageId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RewardResponse> rewards = rewardService.getAllRewards(garageId, status, pageable);
        return ResponseEntity.ok(rewards);
    }
    @PostMapping("/{rewardId}/redeem")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RewardRedemptionResponse> redeemReward(
            @PathVariable Integer garageId,
            @PathVariable Integer rewardId,
            Authentication authentication) {

        String email = authentication.getName();

        // Controller chỉ gọi Service, Service đã lo hết việc tạo DTO rồi
        RewardRedemptionResponse response = rewardRedemptionService.redeemReward(garageId, rewardId, email);

        // Trả về cho người dùng
        return ResponseEntity.ok(response);
    }
}