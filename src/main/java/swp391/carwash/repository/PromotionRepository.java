package swp391.carwash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.carwash.entity.Promotion;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    /**
     * LUỒNG CUSTOMER: Chỉ lấy các mã khuyến mãi ĐANG CÓ HIỆU LỰC
     * (Thỏa mãn: đúng garage, trạng thái ACTIVE, chưa hết lượt dùng, và thời gian hiện tại nằm trong khoảng start-end)
     */
    @Query("SELECT p FROM Promotion p WHERE p.garageId = :garageId " +
            "AND p.status = 'ACTIVE' " +
            "AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit) " +
            "AND :now BETWEEN p.startDate AND p.endDate " +
            "ORDER BY p.discountValue DESC")
    List<Promotion> findAvailablePromotions(@Param("garageId") Integer garageId, @Param("now") OffsetDateTime now);

    /**
     * LUỒNG QUẢN LÝ / ĐƠN GIẢN: Xuất TOÀN BỘ danh sách mã của một garage cụ thể
     * (Lấy hết tất cả các mã ACTIVE, INACTIVE, EXPIRED... mà không kèm điều kiện lọc thời gian)
     */
    List<Promotion> findByGarageId(Integer garageId);
}
