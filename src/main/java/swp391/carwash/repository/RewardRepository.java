package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.Reward;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Integer> {
    // Tìm các phần thưởng thuộc 1 garage cụ thể và còn hàng (stock > 0), trạng thái ACTIVE
    List<Reward> findByGarageIdAndStatusAndStockGreaterThan(Integer garageId, String status, Integer stock);
}