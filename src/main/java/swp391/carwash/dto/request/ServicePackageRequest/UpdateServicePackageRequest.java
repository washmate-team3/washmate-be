package swp391.carwash.dto.request.ServicePackageRequest;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class UpdateServicePackageRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMinutes;
    private String status; // Thêm trạng thái để bật/tắt gói dịch vụ (ACTIVE/INACTIVE)
}
