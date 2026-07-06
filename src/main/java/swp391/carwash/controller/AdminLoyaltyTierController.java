package swp391.carwash.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.request.Account.LoyaltyTierRequest;
import swp391.carwash.dto.response.vehicles.LoyaltyTierResponse;
import swp391.carwash.service.LoyaltyTierService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/loyalty-tiers")
@RequiredArgsConstructor
@Tag(name = "Admin Loyalty Tier Management", description = "APIs dành cho Admin cấu hình các hạng thành viên")  
public class AdminLoyaltyTierController {

    private final LoyaltyTierService loyaltyTierService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả các hạng thành viên")
    public ResponseEntity<List<LoyaltyTierResponse>> getAllTiers(Integer garageId) {
        return ResponseEntity.ok(loyaltyTierService.getAllTiers(garageId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loyalty tier by id")
    public ResponseEntity<LoyaltyTierResponse> getById(
            @RequestParam Integer garageId,
            @PathVariable Integer id) {

        return ResponseEntity.ok(loyaltyTierService.getById(garageId, id));
    }

    @PostMapping
    @Operation(summary = "Tạo mới một hạng thành viên")
    public ResponseEntity<LoyaltyTierResponse> createTier(
            Integer garageId,
            @Valid @RequestBody LoyaltyTierRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loyaltyTierService.createTier(garageId,request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin hạng thành viên theo ID")
    public ResponseEntity<LoyaltyTierResponse> updateTier(
            Integer garageId,
            @PathVariable Integer id,
            @Valid @RequestBody LoyaltyTierRequest request) {

        return ResponseEntity.ok(loyaltyTierService.updateTier(garageId,id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa hạng thành viên (Chỉ xóa khi không có user nào thuộc hạng này)")
    public ResponseEntity<Void> deleteTier(
            Integer garageId,
            @PathVariable Integer id) {
        loyaltyTierService.deleteTier(garageId,id);
        return ResponseEntity.noContent().build();
    }
}
