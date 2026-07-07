package swp391.carwash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.carwash.dto.response.Reward.RewardRedemptionResponse;
import swp391.carwash.entity.Reward;
import swp391.carwash.entity.RewardRedemption;
import swp391.carwash.repository.AppUserRepository;
import swp391.carwash.repository.LoyaltyAccountRepository;
import swp391.carwash.repository.RewardRedemptionRepository;
import swp391.carwash.repository.RewardRepository;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class RewardRedemptionService {
    private final RewardRepository rewardRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final RewardRedemptionRepository redemptionRepository;
    private final AppUserRepository userRepository; // Dùng để tìm user từ email

    @Transactional
    public RewardRedemptionResponse redeemReward(Integer garageId, Integer rewardId, String email) {
        // 1. Lấy thông tin User từ email
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // 2. Lấy thông tin phần thưởng
        Reward reward = rewardRepository.findByRewardIdAndGarageId(rewardId, garageId)
                .orElseThrow(() -> new RuntimeException("Phần thưởng không tồn tại tại garage này"));

        // 3. Lấy thông tin tài khoản tích điểm (loyalty)
        var account = loyaltyAccountRepository.findByUserIdAndGarageId(user.getId(), garageId)
                .orElseThrow(() -> new RuntimeException("Tài khoản tích điểm không hợp lệ"));

        // 4. KIỂM TRA NGHIỆP VỤ (Business Validation)
        if (!"ACTIVE".equals(reward.getStatus())) {
            throw new RuntimeException("Phần thưởng này đã bị ngưng cung cấp");
        }
        if (reward.getStock() <= 0) {
            throw new RuntimeException("Phần thưởng đã hết hàng");
        }
        if (account.getAvailablePoints() < reward.getPointsRequired()) {
            throw new RuntimeException("Bạn không đủ điểm để đổi quà này");
        }

        // 5. CẬP NHẬT DỮ LIỆU
        // Trừ điểm
        account.setAvailablePoints(account.getAvailablePoints() - reward.getPointsRequired());
        loyaltyAccountRepository.save(account);

        // Trừ kho
        reward.setStock(reward.getStock() - 1);
        if (reward.getStock() == 0) {
            reward.setStatus("OUT_OF_STOCK");
        }
        rewardRepository.save(reward);

        // 6. GHI NHẬN GIAO DỊCH
        var redemption = RewardRedemption.builder()
                .loyaltyAccount(account) // Biến 'account' đã query ở bước 3
                .garage(reward.getGarage()) // Lấy Garage từ chính object Reward
                .reward(reward)             // Object reward đã query
                .pointsUsed(reward.getPointsRequired())
                .status("PENDING")
                .redeemedAt(ZonedDateTime.now()) // Dùng ZonedDateTime vì entity dùng kiểu này
                .build();

        RewardRedemption savedRedemption = redemptionRepository.save(redemption);
        // 7. TRẢ VỀ DTO (Convert từ Entity -> Response)
        return RewardRedemptionResponse.fromEntity(savedRedemption);
    }
}
