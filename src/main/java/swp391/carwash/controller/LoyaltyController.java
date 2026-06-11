package swp391.carwash.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import swp391.carwash.dto.LoyaltyAccountResponse;
import swp391.carwash.dto.LoyaltyTransactionResponse;
import swp391.carwash.security.AppUserDetails;
import swp391.carwash.service.LoyaltyService;

@RestController
@RequiredArgsConstructor
public class LoyaltyController {
    private final LoyaltyService loyaltyService;

    @GetMapping("/api/loyalty/me")
    public List<LoyaltyAccountResponse> getMyLoyaltyAccounts(@AuthenticationPrincipal AppUserDetails principal) {
        return loyaltyService.getMyAccounts(principal);
    }

    @GetMapping("/api/loyalty/transactions")
    public List<LoyaltyTransactionResponse> getMyLoyaltyTransactions(@AuthenticationPrincipal AppUserDetails principal) {
        return loyaltyService.getMyTransactions(principal);
    }
}
