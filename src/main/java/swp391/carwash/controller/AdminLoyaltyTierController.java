package swp391.carwash.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.request.vehicles.LoyaltyTierRequest;
import swp391.carwash.dto.respone.vehicles.LoyaltyTierResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/loyalty-tiers")
@Tag(name = "Admin Loyalty Tier Management", description = "APIs dành cho Admin cấu hình các hạng thành viên")  
public class AdminLoyaltyTierController {
    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả các hạng thành viên")
    public ResponseEntity<List<LoyaltyTierResponse>> getAllTiers() {
        // TODO: Gọi sang loyaltyTierService.getAllTiers()
        return ResponseEntity.ok(List.of());
    }

    // Tạo mới một hạng thành viên
    @PostMapping
    @Operation(summary = "Tạo mới một hạng thành viên")
    public ResponseEntity<LoyaltyTierResponse> createTier(@Valid @RequestBody LoyaltyTierRequest request) {
        // TODO: Gọi sang loyaltyTierService.createTier(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(new LoyaltyTierResponse());
    }

    // Cập nhật thông tin hạng thành viên
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin hạng thành viên theo ID")
    public ResponseEntity<LoyaltyTierResponse> updateTier(
            @PathVariable Integer id,
            @Valid @RequestBody LoyaltyTierRequest request) {
        // TODO: Gọi sang loyaltyTierService.updateTier(id, request)
        return ResponseEntity.ok(new LoyaltyTierResponse());
    }

    // Xóa hạng thành viên
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa hạng thành viên (Chỉ xóa khi không có user nào thuộc hạng này)")
    public ResponseEntity<Void> deleteTier(@PathVariable Integer id) {
        // TODO: Gọi sang loyaltyTierService.deleteTier(id)
        return ResponseEntity.noContent().build();
    }
}
