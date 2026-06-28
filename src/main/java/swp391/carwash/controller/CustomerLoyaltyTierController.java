package swp391.carwash.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.request.vehicles.LoyaltyTierRequest;
import swp391.carwash.dto.response.vehicles.LoyaltyTierResponse;
import swp391.carwash.service.LoyaltyTierService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/loyalty")
@RequiredArgsConstructor
@Tag(name = "Customer Loyalty Profile", description = "APIs dành cho Khách hàng xem điểm và lịch sử tích/đổi điểm")
public class
CustomerLoyaltyTierController {

    private final LoyaltyTierService loyaltyTierService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả các hạng thành viên")
    public ResponseEntity<List<LoyaltyTierResponse>> getAllTiers() {
        return ResponseEntity.ok(loyaltyTierService.getAllTiers());
    }

    @PostMapping
    @Operation(summary = "Tạo mới một hạng thành viên")
    public ResponseEntity<LoyaltyTierResponse> createTier(@Valid @RequestBody LoyaltyTierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loyaltyTierService.createTier(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin hạng thành viên theo ID")
    public ResponseEntity<LoyaltyTierResponse> updateTier(
            @PathVariable Integer id,
            @Valid @RequestBody LoyaltyTierRequest request) {
        return ResponseEntity.ok(loyaltyTierService.updateTier(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa hạng thành viên (Chỉ xóa khi không có user nào thuộc hạng này)")
    public ResponseEntity<Void> deleteTier(@PathVariable Integer id) {
        loyaltyTierService.deleteTier(id);
        return ResponseEntity.noContent().build();
    }
}
