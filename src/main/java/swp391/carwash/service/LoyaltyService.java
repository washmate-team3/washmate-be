package swp391.carwash.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.LoyaltyAccountResponse;
import swp391.carwash.dto.LoyaltyTransactionResponse;
import swp391.carwash.repository.LoyaltyAccountRepository;
import swp391.carwash.repository.LoyaltyTransactionRepository;
import swp391.carwash.security.AppUserDetails;

@Service
@RequiredArgsConstructor
public class LoyaltyService {
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Transactional(readOnly = true)
    public List<LoyaltyAccountResponse> getMyAccounts(AppUserDetails principal) {
        return loyaltyAccountRepository.findByUserIdOrderByGarageNameAsc(principal.getId()).stream()
                .map(LoyaltyAccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponse> getMyTransactions(AppUserDetails principal) {
        return loyaltyTransactionRepository.findByAccountUserIdOrderByCreatedAtDesc(principal.getId()).stream()
                .map(LoyaltyTransactionResponse::from)
                .toList();
    }
}
