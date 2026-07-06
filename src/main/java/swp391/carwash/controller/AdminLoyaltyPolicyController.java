package swp391.carwash.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.request.Loyalty.LoyaltyPolicyRequest;
import swp391.carwash.dto.response.Loyaty.LoyaltyPolicyResponse;
import swp391.carwash.service.LoyaltyPolicyService;

@RestController
@RequestMapping("/api/v1/admin/loyalty/policy")
@RequiredArgsConstructor
@Tag(name = "Admin Loyalty Policy")
public class AdminLoyaltyPolicyController {

    private final LoyaltyPolicyService loyaltyPolicyService;

    @GetMapping
    @Operation(summary = "Get loyalty policy")
    public ResponseEntity<LoyaltyPolicyResponse> getPolicy(
            @RequestParam Integer garageId) {

        return ResponseEntity.ok(loyaltyPolicyService.getPolicy(garageId));
    }

    @PostMapping
    @Operation(summary = "Create loyalty policy")
    public ResponseEntity<LoyaltyPolicyResponse> create(
            @RequestParam Integer garageId,
            @Valid @RequestBody LoyaltyPolicyRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loyaltyPolicyService.create(garageId, request));
    }

    @PutMapping
    @Operation(summary = "Update loyalty policy")
    public ResponseEntity<LoyaltyPolicyResponse> update(
            @RequestParam Integer garageId,
            @Valid @RequestBody LoyaltyPolicyRequest request) {

        return ResponseEntity.ok(
                loyaltyPolicyService.update(garageId, request));
    }

    @DeleteMapping
    @Operation(summary = "Delete loyalty policy")
    public ResponseEntity<Void> delete(
            @RequestParam Integer garageId) {

        loyaltyPolicyService.delete(garageId);

        return ResponseEntity.noContent().build();
    }
}