package swp391.carwash.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import swp391.carwash.dto.LoyaltyAccountResponse;
import swp391.carwash.dto.LoyaltyTransactionResponse;
import swp391.carwash.dto.request.Account.LoyaltyTierRequest;
import swp391.carwash.dto.response.CustomerLoyaltySummaryResponse;
import swp391.carwash.dto.response.Loyaty.LoyaltyPolicyResponse;
import swp391.carwash.dto.response.vehicles.LoyaltyTierResponse;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.CustomerLoyaltyService;
import swp391.carwash.service.LoyaltyTierService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/loyalty")
@RequiredArgsConstructor
@Tag(name = "Customer Loyalty Profile", description = "APIs dành cho Khách hàng xem điểm và lịch sử tích/đổi điểm")
public class
CustomerLoyaltyTierController {

    private final CustomerLoyaltyService customerLoyaltyService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my loyalty account")
    public ResponseEntity<LoyaltyAccountResponse> getMyLoyalty(
            @AuthenticationPrincipal AppUserDetails principal) {

        return ResponseEntity.ok(
                customerLoyaltyService.getMyLoyalty(principal.getId()));
    }

    @GetMapping("/tiers")
    public ResponseEntity<List<LoyaltyTierResponse>> getTiers(
            @RequestParam Integer garageId) {

        return ResponseEntity.ok(
                customerLoyaltyService.getTiers(garageId));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my loyalty transactions")
    public ResponseEntity<List<LoyaltyTransactionResponse>> getTransactions(
            @AuthenticationPrincipal AppUserDetails principal) {

        return ResponseEntity.ok(
                customerLoyaltyService.getTransactions(principal.getId()));
    }

    @GetMapping("/policy")
    public ResponseEntity<LoyaltyPolicyResponse> getPolicy(
            @RequestParam Integer garageId) {

        return ResponseEntity.ok(
                customerLoyaltyService.getPolicy(garageId));
    }
    @GetMapping("/summary")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my loyalty summary")
    public ResponseEntity<CustomerLoyaltySummaryResponse> getSummary(
            @RequestParam Integer garageId,
            @AuthenticationPrincipal AppUserDetails principal) {

        return ResponseEntity.ok(
                customerLoyaltyService.getSummary(
                        principal.getId(),
                        garageId
                )
        );
    }
}
