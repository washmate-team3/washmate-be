package swp391.carwash.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import swp391.carwash.entity.RewardRedemption;

public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption,Integer> {
    // Admin/Staff xem toàn bộ đơn đổi quà của một Garage
    Page<RewardRedemption> findByGarage_Id(Integer garageId, Pageable pageable);

    // Admin/Staff xem đơn đổi quà của một Garage và lọc theo trạng thái (PENDING, APPROVED, COMPLETED, REJECTED)
    Page<RewardRedemption> findByGarage_IdAndStatus(Integer garageId, String status, Pageable pageable);

    // Customer tự xem lịch sử đổi quà cá nhân tại một Garage cụ thể (quét qua quan hệ nested: loyaltyAccount -> appUser -> userId)
    Page<RewardRedemption> findByLoyaltyAccount_User_IdAndGarage_Id(Integer userId, Integer garageId, Pageable pageable);
}

