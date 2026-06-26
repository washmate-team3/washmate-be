package swp391.carwash.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.Reward;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Integer> {
    // Tìm các phần thưởng thuộc 1 garage cụ thể và còn hàng (stock > 0), trạng thái ACTIVE
    List<Reward> findByGarageIdAndStatusAndStockGreaterThan(Integer garageId, String status, Integer stock);
    // Phục vụ admin/staff xem quà theo garage và lọc theo trạng thái (ACTIVE, OUT_OF_STOCK, INACTIVE)
    Page<Reward> findByGarageIdAndStatus(Integer garageId, String status, Pageable pageable);

    // Phục vụ xem toàn bộ quà của garage ngoại trừ những quà đã bị xóa mềm (DELETED)
    Page<Reward> findByGarageIdAndStatusNot(Integer garageId, String status, Pageable pageable);

    Optional<Reward> findByRewardIdAndGarageId(Integer rewardId, Integer garageId);



}
